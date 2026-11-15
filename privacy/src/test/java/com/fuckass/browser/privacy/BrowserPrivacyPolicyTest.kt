package com.fuckass.browser.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserPrivacyPolicyTest {
    @Test fun removesKnownTrackingParametersAndFragment() {
        val policy = BrowserPrivacyPolicy()
        assertEquals(
            "https://example.com/watch?id=7&keep=yes",
            policy.sanitizeUrl("https://example.com/watch?utm_source=news&id=7&fbclid=abc&keep=yes#player")
        )
    }

    @Test fun preservesOrdinaryQueryAndEncodedValues() {
        val policy = BrowserPrivacyPolicy()
        assertEquals(
            "https://example.com/?q=a%2Bb&x=1",
            policy.sanitizeUrl("https://example.com/?q=a%2Bb&x=1")
        )
    }

    @Test fun rejectsUnsupportedSchemesAndUserInfo() {
        val policy = BrowserPrivacyPolicy()
        assertNull(policy.sanitizeUrl("javascript:alert(1)"))
        assertNull(policy.sanitizeUrl("https://user:pass@example.com/"))
    }

    @Test fun stripsFullRefererToOriginByDefault() {
        val policy = BrowserPrivacyPolicy()
        val headers = policy.requestHeaders(
            "https://other.example/path",
            mapOf("Referer" to "https://source.example/a/b?secret=1", "Accept" to "text/html")
        )
        assertEquals("https://source.example/", headers["Referer"])
        assertEquals("text/html", headers["Accept"])
    }

    @Test fun sameOriginModeDoesNotSendCrossOriginReferer() {
        val policy = BrowserPrivacyPolicy(PrivacySettings(referrerMode = ReferrerMode.SAME_ORIGIN))
        val headers = policy.requestHeaders(
            "https://other.example/",
            mapOf("Referer" to "https://source.example/path")
        )
        assertFalse(headers.keys.any { it.equals("Referer", true) })
    }

    @Test fun noneModeAlwaysStripsReferer() {
        val policy = BrowserPrivacyPolicy(PrivacySettings(referrerMode = ReferrerMode.NONE))
        val headers = policy.requestHeaders(
            "https://source.example/next",
            mapOf("Referer" to "https://source.example/start")
        )
        assertFalse(headers.keys.any { it.equals("Referer", true) })
    }

    @Test fun cookieStrippingIsExplicitOptIn() {
        val policy = BrowserPrivacyPolicy(PrivacySettings(stripCookieHeaders = true))
        val headers = policy.requestHeaders(
            "https://example.com/",
            mapOf("Cookie" to "session=abc", "Accept" to "*/*")
        )
        assertFalse(headers.keys.any { it.equals("Cookie", true) })
        assertTrue(headers.containsKey("Accept"))
    }

    @Test fun privateSiteRulesMatchExactHostAndSubdomainsOnly() {
        val rules = PrivateSiteRules(setOf("private.example", "bad/path", "singlelabel"))
        assertTrue(rules.matches("https://private.example/page"))
        assertTrue(rules.matches("https://sub.private.example/page"))
        assertFalse(rules.matches("https://notprivate.example/page"))
        assertFalse(rules.matches("https://private.example.attacker.test/"))
    }

    @Test fun privateSiteRulesNormalizeInternationalizedDomainNames() {
        val rules = PrivateSiteRules(setOf("bücher.example", "invalid host", "https://bad.example"))
        assertTrue(rules.matches("https://xn--bcher-kva.example/account"))
        assertFalse(rules.matches("https://bad.example/account"))
    }

    @Test fun privateModeRoutingUsesConfiguredRules() {
        val policy = BrowserPrivacyPolicy(privateSites = PrivateSiteRules(setOf("example.org")))
        assertTrue(policy.shouldOpenInPrivateMode("https://login.example.org/"))
        assertFalse(policy.shouldOpenInPrivateMode("https://example.net/"))
    }
}
