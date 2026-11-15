package com.fuckass.browser.privacy

import java.net.IDN
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

enum class ReferrerMode { NONE, ORIGIN, SAME_ORIGIN }

data class PrivacySettings(
    val removeTrackingParameters: Boolean = true,
    val referrerMode: ReferrerMode = ReferrerMode.ORIGIN,
    val stripCookieHeaders: Boolean = false
)

/**
 * Local, deterministic request privacy policy. It does not claim anonymity: the destination
 * still receives the device's network address and TLS connection metadata.
 */
class BrowserPrivacyPolicy(
    private val settings: PrivacySettings = PrivacySettings(),
    private val privateSites: PrivateSiteRules = PrivateSiteRules()
) : RequestPrivacyPolicy {

    override fun sanitizeUrl(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("https", "http")) return null
        if (uri.host.isNullOrBlank() || uri.rawUserInfo != null) return null

        val withoutFragment = url.substringBefore('#')
        if (!settings.removeTrackingParameters || uri.rawQuery == null) return withoutFragment

        val kept = uri.rawQuery.split('&').filter { pair ->
            val rawName = pair.substringBefore('=')
            val name = runCatching {
                URLDecoder.decode(rawName, StandardCharsets.UTF_8.name())
            }.getOrDefault(rawName).lowercase(Locale.ROOT)
            !isTrackingParameter(name)
        }
        val base = buildString {
            append(uri.scheme).append("://").append(uri.rawAuthority)
            append(uri.rawPath.orEmpty())
        }
        return if (kept.isEmpty()) base else base + "?" + kept.joinToString("&")
    }

    override fun requestHeaders(url: String, original: Map<String, String>): Map<String, String> {
        val destination = runCatching { URI(url) }.getOrNull() ?: return emptyMap()
        return original.filter { (name, _) ->
            if (name.equals("Referer", true)) return@filter false
            if (settings.stripCookieHeaders && name.equals("Cookie", true)) return@filter false
            true
        }.toMutableMap().also { headers ->
            val referer = original.entries.firstOrNull { it.key.equals("Referer", true) }?.value
            if (referer != null && settings.referrerMode != ReferrerMode.NONE) {
                val source = runCatching { URI(referer) }.getOrNull()
                if (source != null && source.host != null && source.rawUserInfo == null &&
                    source.scheme?.lowercase(Locale.ROOT) in setOf("http", "https")
                ) {
                    val sameOrigin = source.scheme.equals(destination.scheme, true) &&
                        source.host.equals(destination.host, true) &&
                        effectivePort(source) == effectivePort(destination)
                    val value = when (settings.referrerMode) {
                        ReferrerMode.NONE -> null
                        ReferrerMode.SAME_ORIGIN -> if (sameOrigin) source.toASCIIString().substringBefore('#') else null
                        ReferrerMode.ORIGIN -> originOf(source)
                    }
                    if (value != null) headers["Referer"] = value
                }
            }
        }
    }

    fun shouldOpenInPrivateMode(url: String): Boolean = privateSites.matches(url)

    private fun isTrackingParameter(name: String): Boolean =
        name.startsWith("utm_") || name in TRACKING_PARAMETERS

    private fun originOf(uri: URI): String {
        val port = effectivePort(uri)
        val defaultPort = (uri.scheme.equals("https", true) && port == 443) ||
            (uri.scheme.equals("http", true) && port == 80)
        val authority = if (defaultPort) uri.host else "${uri.host}:$port"
        return "${uri.scheme}://$authority/"
    }

    private fun effectivePort(uri: URI): Int = when {
        uri.port != -1 -> uri.port
        uri.scheme.equals("https", true) -> 443
        else -> 80
    }

    companion object {
        private val TRACKING_PARAMETERS = setOf(
            "fbclid", "gclid", "dclid", "msclkid", "mc_cid", "mc_eid",
            "igshid", "vero_id", "oly_anon_id", "oly_enc_id"
        )
    }
}

/** User-controlled host rules. Matching is exact-host or subdomain, never substring matching. */
class PrivateSiteRules(hosts: Set<String> = emptySet()) {
    private val normalizedHosts: Set<String> = hosts.mapNotNull(::normalizeHost).toSet()

    fun matches(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase(Locale.ROOT)?.trimEnd('.') }.getOrNull()
            ?: return false
        return normalizedHosts.any { host == it || host.endsWith(".$it") }
    }

    fun hosts(): Set<String> = normalizedHosts.toSet()

    companion object {
        private fun normalizeHost(value: String): String? {
            val input = value.trim().trimEnd('.')
            if (input.isBlank() || input.length > 253 || input.contains('/') || input.contains('@') ||
                input.contains(':') || input.contains('*')
            ) return null
            val host = runCatching { IDN.toASCII(input, IDN.USE_STD3_ASCII_RULES) }
                .getOrNull()?.lowercase(Locale.ROOT) ?: return null
            if (host.length > 253) return null
            val labels = host.split('.')
            if (labels.size < 2) return null
            if (labels.any { label ->
                    label.isEmpty() || label.length > 63 || label.first() == '-' || label.last() == '-' ||
                        label.any { !(it in 'a'..'z' || it in '0'..'9' || it == '-') }
                }) return null
            return host
        }
    }
}
