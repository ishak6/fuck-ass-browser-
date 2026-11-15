package com.fuckass.browser.engine.net

import com.fuckass.browser.adblock.NetworkFilterPolicy
import com.fuckass.browser.privacy.RequestPrivacyPolicy
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserHttpClientTest {
    @Test
    fun cleartextHttpIsRejectedBeforeConnecting() {
        val client = BrowserHttpClient()
        try {
            val error = assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("http://example.com"))
            }
            assertEquals("Cleartext HTTP is disabled", error.message)
        } finally {
            client.close()
        }
    }

    @Test
    fun unsupportedSchemesAreRejected() {
        val client = BrowserHttpClient()
        try {
            assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("file:///etc/passwd"))
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun userInfoInUrlIsRejected() {
        val client = BrowserHttpClient()
        try {
            val error = assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("https://user:pass@example.com"))
            }
            assertEquals("Invalid URL authority", error.message)
        } finally {
            client.close()
        }
    }

    @Test
    fun privacyPolicyCanRejectRequest() {
        val policy = object : RequestPrivacyPolicy {
            override fun sanitizeUrl(url: String): String? = null
            override fun requestHeaders(url: String, original: Map<String, String>) = original
        }
        val client = BrowserHttpClient(privacyPolicy = policy)
        try {
            assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("https://example.com"))
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun adblockPolicyCanRejectRequest() {
        val policy = NetworkFilterPolicy { _, _, _ -> true }
        val client = BrowserHttpClient(filterPolicy = policy)
        try {
            val error = assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("https://example.com"))
            }
            assertEquals("Request blocked by content policy", error.message)
        } finally {
            client.close()
        }
    }

    @Test
    fun parsedFilterListCanBeInjectedIntoNetworkTransport() {
        val policy = com.fuckass.browser.adblock.FilterList.parse("||ads.example^")
        val client = BrowserHttpClient(filterPolicy = policy)
        try {
            val error = assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("https://ads.example/banner.js", resourceType = "script"))
            }
            assertEquals("Request blocked by content policy", error.message)
        } finally {
            client.close()
        }
    }

    @Test
    fun callerCannotOverrideTransportFramingHeaders() {
        val client = BrowserHttpClient()
        try {
            val error = assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest(
                    "https://example.com",
                    headers = mapOf("Content-Length" to "0")
                ))
            }
            assertEquals("Invalid or restricted request header", error.message)
        } finally {
            client.close()
        }
    }

    @Test
    fun requestBodyIsBoundedBeforeConnecting() {
        val client = BrowserHttpClient(NetworkLimits(maxRequestBytes = 2))
        try {
            val error = assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest(
                    "https://example.com",
                    method = "POST",
                    body = byteArrayOf(1, 2, 3)
                ))
            }
            assertEquals("Request body exceeds configured size limit", error.message)
        } finally {
            client.close()
        }
    }

    @Test
    fun postRequiresBodyAndGetRejectsBody() {
        val client = BrowserHttpClient()
        try {
            assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("https://example.com", method = "POST"))
            }
            assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("https://example.com", body = byteArrayOf(1)))
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun closedClientRejectsNewRequests() {
        val client = BrowserHttpClient()
        client.close()
        assertThrows(IllegalStateException::class.java) {
            client.execute(BrowserHttpRequest("https://example.com"))
        }
    }

    @Test
    fun networkLimitsValidateBounds() {
        assertThrows(IllegalArgumentException::class.java) {
            NetworkLimits(maxRedirects = 21)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NetworkLimits(maxResponseBytes = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NetworkLimits(maxRequestBytes = -1)
        }
    }

    @Test
    fun crossOriginRedirectStripsAuthorizationAndCookie() {
        val client = BrowserHttpClient()
        val first = FakeConnection(URL("https://first.example/start"), 302,
            mapOf("Location" to "https://second.example/final"))
        val second = FakeConnection(URL("https://second.example/final"), 200, body = "ok".toByteArray())
        client.connectionFactory = { url -> if (url.host == "first.example") first else second }
        try {
            val result = client.execute(BrowserHttpRequest(
                "https://first.example/start",
                headers = mapOf("Authorization" to "Bearer secret", "Cookie" to "session=secret"),
                useCache = false
            ))
            assertEquals(200, result.statusCode)
            assertFalse(second.requestedHeaders.keys.any { it.equals("Authorization", true) })
            assertFalse(second.requestedHeaders.keys.any { it.equals("Cookie", true) })
            assertFalse(first.instanceFollowRedirects)
            assertTrue(first.disconnected)
            assertTrue(second.disconnected)
        } finally {
            client.close()
        }
    }

    @Test
    fun privacyHeadersAreRecomputedForEachRedirectDestination() {
        val policy = object : RequestPrivacyPolicy {
            override fun sanitizeUrl(url: String): String? = url
            override fun requestHeaders(url: String, original: Map<String, String>): Map<String, String> =
                original + ("X-Policy-Host" to URL(url).host)
        }
        val client = BrowserHttpClient(privacyPolicy = policy)
        val first = FakeConnection(URL("https://first.example/start"), 302,
            mapOf("Location" to "https://second.example/final"))
        val second = FakeConnection(URL("https://second.example/final"), 200, body = "ok".toByteArray())
        client.connectionFactory = { url -> if (url.host == "first.example") first else second }
        try {
            val result = client.execute(BrowserHttpRequest("https://first.example/start", useCache = false))
            assertEquals(200, result.statusCode)
            assertEquals("first.example", first.requestedHeaders["X-Policy-Host"])
            assertEquals("second.example", second.requestedHeaders["X-Policy-Host"])
        } finally {
            client.close()
        }
    }

    @Test
    fun urlOnlyCacheIsBypassedForCookieJarAndAuthorizationRequests() {
        val cookieClient = BrowserHttpClient(
            cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        )
        var cookieConnections = 0
        cookieClient.connectionFactory = { url ->
            cookieConnections++
            FakeConnection(
                url, 200,
                mapOf("Cache-Control" to "max-age=60"),
                "ok".toByteArray()
            )
        }
        try {
            repeat(2) {
                cookieClient.execute(BrowserHttpRequest("https://site.example/page"))
            }
            assertEquals(2, cookieConnections)
        } finally {
            cookieClient.close()
        }

        val authClient = BrowserHttpClient()
        var authConnections = 0
        authClient.connectionFactory = { url ->
            authConnections++
            FakeConnection(
                url, 200,
                mapOf("Cache-Control" to "max-age=60"),
                "ok".toByteArray()
            )
        }
        try {
            repeat(2) {
                authClient.execute(
                    BrowserHttpRequest(
                        "https://site.example/private",
                        headers = mapOf("Authorization" to "Bearer user-token")
                    )
                )
            }
            assertEquals(2, authConnections)
        } finally {
            authClient.close()
        }
    }

    @Test
    fun cookieJarStoresAndSendsCookiesOnlyToMatchingOrigin() {
        val cookies = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        val client = BrowserHttpClient(cookieManager = cookies)
        val setCookie = FakeConnection(
            URL("https://site.example/start"),
            200,
            mapOf("Set-Cookie" to "session=private; Path=/; Secure"),
            "ok".toByteArray()
        )
        val nextRequest = FakeConnection(URL("https://site.example/next"), 200, body = "ok".toByteArray())
        client.connectionFactory = { url -> if (url.path == "/start") setCookie else nextRequest }
        try {
            client.execute(BrowserHttpRequest("https://site.example/start", useCache = false))
            client.execute(BrowserHttpRequest("https://site.example/next", useCache = false))
            assertTrue(nextRequest.requestedHeaders["Cookie"].orEmpty().contains("session=private"))
            client.clearCookies()
            val thirdRequest = FakeConnection(URL("https://site.example/after-clear"), 200)
            client.connectionFactory = { thirdRequest }
            client.execute(BrowserHttpRequest("https://site.example/after-clear", useCache = false))
            assertFalse(thirdRequest.requestedHeaders.containsKey("Cookie"))
        } finally {
            client.close()
        }
    }

    @Test
    fun cancelActiveRequestsDisconnectsInFlightConnection() {
        val client = BrowserHttpClient()
        val enteredResponse = CountDownLatch(1)
        val connection = FakeConnection(
            URL("https://site.example/slow"),
            200,
            responseGate = enteredResponse
        )
        client.connectionFactory = { connection }
        val worker = Thread {
            runCatching {
                client.execute(BrowserHttpRequest("https://site.example/slow", useCache = false))
            }
        }
        worker.start()
        try {
            assertTrue(enteredResponse.await(2, TimeUnit.SECONDS))
            client.cancelActiveRequests()
            worker.join(2_000)
            assertFalse(worker.isAlive)
            assertTrue(connection.disconnected)
        } finally {
            client.close()
            worker.interrupt()
        }
    }

    @Test
    fun seeOtherRedirectPreservesHeadMethod() {
        val client = BrowserHttpClient()
        val first = FakeConnection(URL("https://first.example/start"), 303,
            mapOf("Location" to "https://second.example/final"))
        val second = FakeConnection(URL("https://second.example/final"), 200)
        client.connectionFactory = { url -> if (url.host == "first.example") first else second }
        try {
            val response = client.execute(BrowserHttpRequest(
                "https://first.example/start",
                method = "HEAD",
                useCache = false
            ))
            assertEquals(200, response.statusCode)
            assertEquals("HEAD", second.requestMethod)
        } finally {
            client.close()
        }
    }

    @Test
    fun redirectDestinationIsCheckedByContentPolicyBeforeConnecting() {
        val policy = NetworkFilterPolicy { url, _, _ -> url.endsWith("/blocked") }
        val client = BrowserHttpClient(filterPolicy = policy)
        val first = FakeConnection(URL("https://site.example/start"), 302, mapOf("Location" to "/blocked"))
        var connectionCount = 0
        client.connectionFactory = {
            connectionCount++
            first
        }
        try {
            val error = assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("https://site.example/start", useCache = false))
            }
            assertEquals("Redirect blocked by content policy", error.message)
            assertEquals(1, connectionCount)
        } finally {
            client.close()
        }
    }

    @Test
    fun redirectCountIsBounded() {
        val client = BrowserHttpClient(NetworkLimits(maxRedirects = 1))
        var connectionCount = 0
        client.connectionFactory = { url ->
            connectionCount++
            FakeConnection(url, 302, mapOf("Location" to "https://site.example/next"))
        }
        try {
            val error = assertThrows(BrowserNetworkException::class.java) {
                client.execute(BrowserHttpRequest("https://site.example/start", useCache = false))
            }
            assertEquals("Too many redirects", error.message)
            assertEquals(2, connectionCount)
        } finally {
            client.close()
        }
    }

    private class FakeConnection(
        url: URL,
        private val status: Int,
        private val responseHeaders: Map<String, String> = emptyMap(),
        private val body: ByteArray = ByteArray(0),
        private val responseGate: CountDownLatch? = null
    ) : HttpURLConnection(url) {
        val requestedHeaders = linkedMapOf<String, String>()
        @Volatile var disconnected = false

        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int {
            responseGate?.countDown()
            while (responseGate != null && !disconnected) Thread.sleep(5)
            if (disconnected) throw java.io.IOException("Connection cancelled")
            return status
        }
        override fun getHeaderField(name: String?): String? =
            responseHeaders.entries.firstOrNull { it.key.equals(name, true) }?.value
        override fun getHeaderFields(): MutableMap<String?, MutableList<String>> =
            responseHeaders.entries.associate { it.key as String? to mutableListOf(it.value) }.toMutableMap()
        override fun getInputStream(): java.io.InputStream = java.io.ByteArrayInputStream(body)
        override fun getErrorStream(): java.io.InputStream? =
            if (status >= 400) java.io.ByteArrayInputStream(body) else null
        override fun getContentEncoding(): String? = null
        override fun addRequestProperty(key: String?, value: String?) {
            if (key != null && value != null) {
                requestedHeaders[key] = requestedHeaders[key]?.let { it + ", " + value } ?: value
            }
        }
        override fun setRequestProperty(key: String?, value: String?) {
            if (key != null && value != null) requestedHeaders[key] = value
        }
    }

}
