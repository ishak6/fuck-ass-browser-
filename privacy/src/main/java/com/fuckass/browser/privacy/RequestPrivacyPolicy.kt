package com.fuckass.browser.privacy

/**
 * Privacy decisions consumed by the network layer.
 *
 * This module deliberately has no dependency on the network implementation.
 */
interface RequestPrivacyPolicy {
    /** Return a sanitized URL, or null to reject the request. */
    fun sanitizeUrl(url: String): String?

    /** Return headers safe to send for this destination. */
    fun requestHeaders(url: String, original: Map<String, String>): Map<String, String>
}

object NoOpRequestPrivacyPolicy : RequestPrivacyPolicy {
    override fun sanitizeUrl(url: String): String = url
    override fun requestHeaders(url: String, original: Map<String, String>): Map<String, String> =
        original.toMap()
}
