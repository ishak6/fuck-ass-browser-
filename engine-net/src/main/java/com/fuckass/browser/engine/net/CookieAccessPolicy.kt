package com.fuckass.browser.engine.net

import java.net.URI
import java.util.Locale

enum class CookieMode { ALLOW_ALL, SAME_ORIGIN_ONLY, BLOCK_ALL }

/**
 * Explicit, runtime-changeable cookie policy. SAME_ORIGIN_ONLY compares scheme, normalized
 * host and effective port exactly; it is intentionally stricter than registrable-site matching.
 */
class CookieAccessPolicy(initialMode: CookieMode = CookieMode.ALLOW_ALL) {
    @Volatile
    var mode: CookieMode = initialMode
        private set

    fun setMode(value: CookieMode) {
        mode = value
    }

    fun allows(requestUrl: String, topLevelUrl: String?): Boolean {
        return when (mode) {
            CookieMode.ALLOW_ALL -> true
            CookieMode.BLOCK_ALL -> false
            CookieMode.SAME_ORIGIN_ONLY -> {
                val request = parseHttps(requestUrl) ?: return false
                val top = parseHttps(topLevelUrl ?: requestUrl) ?: return false
                request.scheme.equals(top.scheme, true) &&
                    request.host.equals(top.host, true) &&
                    effectivePort(request) == effectivePort(top)
            }
        }
    }

    private fun parseHttps(value: String): URI? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", true) || uri.host.isNullOrBlank() || uri.rawUserInfo != null) {
            return null
        }
        return uri
    }

    private fun effectivePort(uri: URI): Int = if (uri.port == -1) 443 else uri.port
}
