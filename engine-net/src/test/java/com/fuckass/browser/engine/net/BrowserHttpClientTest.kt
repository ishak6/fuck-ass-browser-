package com.fuckass.browser.engine.net

import com.fuckass.browser.adblock.NetworkFilterPolicy
import com.fuckass.browser.privacy.RequestPrivacyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    }
}
