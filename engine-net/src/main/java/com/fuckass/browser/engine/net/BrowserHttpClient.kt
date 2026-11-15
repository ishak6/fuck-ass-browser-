package com.fuckass.browser.engine.net

import com.fuckass.browser.adblock.AllowAllNetworkFilterPolicy
import com.fuckass.browser.adblock.NetworkFilterPolicy
import com.fuckass.browser.privacy.NoOpRequestPrivacyPolicy
import com.fuckass.browser.privacy.RequestPrivacyPolicy
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream

/** A deliberately small, first-party HTTP transport. Calls block; invoke from an IO worker. */
data class BrowserHttpRequest(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val resourceType: String = "document",
    val topLevelUrl: String? = null,
    val useCache: Boolean = true
)

data class BrowserHttpResponse(
    val url: String,
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: ByteArray,
    val fromCache: Boolean
)

data class NetworkLimits(
    val connectTimeoutMs: Int = 10_000,
    val readTimeoutMs: Int = 15_000,
    val maxRedirects: Int = 8,
    val maxResponseBytes: Int = 5 * 1024 * 1024,
    val maxRequestBytes: Int = 2 * 1024 * 1024,
    val maxCacheBytes: Int = 8 * 1024 * 1024
) {
    init {
        require(connectTimeoutMs in 1..120_000)
        require(readTimeoutMs in 1..120_000)
        require(maxRedirects in 0..20)
        require(maxResponseBytes in 1..50 * 1024 * 1024)
        require(maxRequestBytes in 0..10 * 1024 * 1024)
        require(maxCacheBytes in 0..100 * 1024 * 1024)
    }
}

class BrowserNetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Uses Android/JVM's platform TLS trust store and hostname verification. It never installs
 * permissive trust managers and never follows redirects implicitly.
 */
class BrowserHttpClient(
    private val limits: NetworkLimits = NetworkLimits(),
    private val privacyPolicy: RequestPrivacyPolicy = NoOpRequestPrivacyPolicy,
    private val filterPolicy: NetworkFilterPolicy = AllowAllNetworkFilterPolicy
) : Closeable {
    private data class CacheEntry(val response: BrowserHttpResponse, val expiresAtMs: Long)
    private val closed = AtomicBoolean(false)
    private val cache = object : LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {}
    private var cacheBytes = 0

    // Internal seam for deterministic redirect tests; production uses the platform URLConnection.
    internal var connectionFactory: (URL) -> HttpURLConnection = { url ->
        (url.openConnection() as? HttpURLConnection)
            ?: throw BrowserNetworkException("Unsupported connection type")
    }

    @Synchronized
    override fun close() {
        closed.set(true)
        cache.clear()
        cacheBytes = 0
    }

    @Synchronized
    fun clearCache() {
        cache.clear()
        cacheBytes = 0
    }

    fun execute(request: BrowserHttpRequest): BrowserHttpResponse {
        check(!closed.get()) { "Network client is closed" }
        val method = request.method.uppercase(Locale.ROOT)
        if (method !in setOf("GET", "HEAD", "POST")) {
            throw BrowserNetworkException("Unsupported HTTP method")
        }
        if (method == "POST" && request.body == null) {
            throw BrowserNetworkException("POST requires a request body")
        }
        if (method != "POST" && request.body != null) {
            throw BrowserNetworkException("Request body is only supported for POST")
        }
        if ((request.body?.size ?: 0) > limits.maxRequestBytes) {
            throw BrowserNetworkException("Request body exceeds configured size limit")
        }

        val sanitized = privacyPolicy.sanitizeUrl(request.url)
            ?: throw BrowserNetworkException("Request rejected by privacy policy")
        val initial = validateUrl(sanitized)
        if (filterPolicy.shouldBlock(initial.toString(), request.resourceType, request.topLevelUrl)) {
            throw BrowserNetworkException("Request blocked by content policy")
        }

        val cacheKey = "$method $initial"
        if (method == "GET" && request.useCache) {
            cached(cacheKey)?.let { return it.copy(fromCache = true) }
        }

        var current = initial
        var currentMethod = method
        var currentBody = request.body
        var headers = privacyPolicy.requestHeaders(current.toString(), request.headers).toMutableMap()
        validateHeaders(headers)
        repeat(limits.maxRedirects + 1) { redirectIndex ->
            check(!closed.get()) { "Network client is closed" }
            val connection = connectionFactory(current)
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = limits.connectTimeoutMs
                connection.readTimeout = limits.readTimeoutMs
                connection.useCaches = false
                connection.requestMethod = currentMethod
                connection.setRequestProperty("Accept-Encoding", "gzip")
                connection.setRequestProperty("User-Agent", "CustomBrowser/0.1 (Android)")
                headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                if (currentMethod == "POST") {
                    connection.doOutput = true
                    connection.setFixedLengthStreamingMode(currentBody!!.size)
                    connection.outputStream.use { it.write(currentBody) }
                }

                val status = connection.responseCode
                if (status in REDIRECT_CODES) {
                    val location = connection.getHeaderField("Location")
                        ?: throw BrowserNetworkException("Redirect response without Location")
                    if (redirectIndex >= limits.maxRedirects) {
                        throw BrowserNetworkException("Too many redirects")
                    }
                    val redirected = privacyPolicy.sanitizeUrl(URL(current, location).toString())
                        ?: throw BrowserNetworkException("Redirect rejected by privacy policy")
                    val next = validateUrl(redirected)
                    if (filterPolicy.shouldBlock(next.toString(), request.resourceType, request.topLevelUrl)) {
                        throw BrowserNetworkException("Redirect blocked by content policy")
                    }
                    if (current.protocol == "https" && next.protocol == "http") {
                        throw BrowserNetworkException("HTTPS downgrade redirect blocked")
                    }
                    if (!sameOrigin(current, next)) {
                        headers = headers.filterKeys {
                            !it.equals("Authorization", true) && !it.equals("Cookie", true)
                        }.toMutableMap()
                    }
                    if (status == 303 || ((status == 301 || status == 302) && currentMethod == "POST")) {
                        currentMethod = "GET"
                        currentBody = null
                    }
                    // Re-run destination policy on every hop; never carry a stale policy result.
                    headers = privacyPolicy.requestHeaders(next.toString(), headers).toMutableMap()
                    validateHeaders(headers)
                    current = next
                    return@repeat
                }

                val stream = if (status >= 400) connection.errorStream else connection.inputStream
                val bytes = if (currentMethod == "HEAD" || stream == null) ByteArray(0) else
                    readBounded(if (connection.contentEncoding.equals("gzip", true)) GZIPInputStream(stream) else stream)
                val responseHeaders = connection.headerFields
                    .filterKeys { it != null }
                    .mapKeys { it.key ?: "" }
                    .mapValues { it.value?.toList() ?: emptyList() }
                val response = BrowserHttpResponse(current.toString(), status, responseHeaders, bytes, false)
                if (method == "GET" && request.useCache && status == 200 && CachePolicy.isCacheSafe(responseHeaders)) {
                    store(cacheKey, response, CachePolicy.lifetimeMs(responseHeaders))
                }
                return response
            } catch (e: BrowserNetworkException) {
                throw e
            } catch (e: Exception) {
                throw BrowserNetworkException("Network request failed", e)
            } finally {
                connection.disconnect()
            }
        }
        throw BrowserNetworkException("Redirect processing failed")
    }

    private fun validateUrl(value: String): URL {
        val url = try { URL(value) } catch (e: Exception) {
            throw BrowserNetworkException("Invalid URL", e)
        }
        if (url.protocol != "https" && url.protocol != "http") {
            throw BrowserNetworkException("Only HTTP and HTTPS URLs are supported")
        }
        if (url.host.isBlank() || runCatching { URI(value).rawUserInfo != null }.getOrDefault(true)) {
            throw BrowserNetworkException("Invalid URL authority")
        }
        if (url.protocol == "http") {
            throw BrowserNetworkException("Cleartext HTTP is disabled")
        }
        return url
    }

    private fun validateHeaders(headers: Map<String, String>) {
        headers.forEach { (name, value) ->
            if (name.isBlank() || !HEADER_NAME.matches(name) ||
                value.any { it == '\r' || it == '\n' } ||
                name.lowercase(Locale.ROOT) in FORBIDDEN_REQUEST_HEADERS
            ) {
                throw BrowserNetworkException("Invalid or restricted request header")
            }
        }
    }

    private fun readBounded(input: java.io.InputStream): ByteArray {
        input.use { source ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = source.read(buffer)
                if (count < 0) break
                total += count
                if (total > limits.maxResponseBytes) {
                    throw BrowserNetworkException("Response exceeds configured size limit")
                }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }

    @Synchronized
    private fun cached(key: String): BrowserHttpResponse? {
        val entry = cache[key] ?: return null
        if (entry.expiresAtMs <= System.currentTimeMillis()) {
            cache.remove(key)
            cacheBytes -= entry.response.body.size
            return null
        }
        return entry.response
    }

    @Synchronized
    private fun store(key: String, response: BrowserHttpResponse, lifetimeMs: Long) {
        if (lifetimeMs <= 0 || limits.maxCacheBytes == 0 || response.body.size > limits.maxCacheBytes) return
        val old = cache.remove(key)
        if (old != null) cacheBytes -= old.response.body.size
        while (cacheBytes + response.body.size > limits.maxCacheBytes && cache.isNotEmpty()) {
            val eldest = cache.entries.iterator().next()
            cacheBytes -= eldest.value.response.body.size
            cache.remove(eldest.key)
        }
        cache[key] = CacheEntry(response.copy(body = response.body.copyOf()), System.currentTimeMillis() + lifetimeMs)
        cacheBytes += response.body.size
    }

    private fun sameOrigin(a: URL, b: URL): Boolean =
        a.protocol.equals(b.protocol, true) && a.host.equals(b.host, true) && effectivePort(a) == effectivePort(b)

    private fun effectivePort(url: URL): Int =
        if (url.port != -1) url.port else if (url.protocol == "https") 443 else 80

    companion object {
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private val HEADER_NAME = Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
        private val FORBIDDEN_REQUEST_HEADERS = setOf(
            "host", "content-length", "transfer-encoding", "connection", "accept-encoding", "user-agent"
        )
    }
}


/** Conservative HTTP response-cache rules shared with deterministic unit tests. */
internal object CachePolicy {
    fun isCacheSafe(headers: Map<String, List<String>>): Boolean {
        val names = headers.keys.map { it.lowercase(Locale.ROOT) }.toSet()
        if ("set-cookie" in names) return false
        val vary = headers.entries.firstOrNull { it.key.equals("Vary", true) }?.value.orEmpty()
        return vary.none { it.trim() == "*" } && vary.isEmpty()
    }

    fun lifetimeMs(headers: Map<String, List<String>>): Long {
        val directives = headers.entries.firstOrNull { it.key.equals("Cache-Control", true) }
            ?.value?.joinToString(",")?.lowercase(Locale.ROOT) ?: return 0
        if ("no-store" in directives || "private" in directives || "no-cache" in directives) return 0
        val seconds = Regex("(?:^|,)\\s*max-age=(\\d+)").find(directives)
            ?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return 0
        return seconds.coerceAtMost(86_400).times(1000)
    }
}
