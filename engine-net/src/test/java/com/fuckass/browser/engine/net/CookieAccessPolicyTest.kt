package com.fuckass.browser.engine.net

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieAccessPolicyTest {
    @Test
    fun allowAllPreservesCookieAccess() {
        val policy = CookieAccessPolicy(CookieMode.ALLOW_ALL)
        assertTrue(policy.allows("https://cdn.example.test/a", "https://site.example.test/"))
    }

    @Test
    fun blockAllRejectsEveryCookieRequest() {
        val policy = CookieAccessPolicy(CookieMode.BLOCK_ALL)
        assertFalse(policy.allows("https://site.example.test/", "https://site.example.test/"))
    }

    @Test
    fun sameOriginModeRequiresMatchingSchemeHostAndEffectivePort() {
        val policy = CookieAccessPolicy(CookieMode.SAME_ORIGIN_ONLY)
        assertTrue(policy.allows("https://site.example.test:443/image", "https://site.example.test/"))
        assertFalse(policy.allows("https://cdn.example.test/image", "https://site.example.test/"))
        assertFalse(policy.allows("https://site.example.test:8443/image", "https://site.example.test/"))
        assertFalse(policy.allows("http://site.example.test/image", "https://site.example.test/"))
    }

    @Test
    fun sameOriginModeFailsClosedForMalformedOrNonHttpsUrls() {
        val policy = CookieAccessPolicy(CookieMode.SAME_ORIGIN_ONLY)
        assertFalse(policy.allows("http://site.example.test/", null))
        assertFalse(policy.allows("https://user@site.example.test/", "https://site.example.test/"))
        assertFalse(policy.allows("https://site.example.test/", "not a url"))
    }

    @Test
    fun modeCanBeChangedAtRuntime() {
        val policy = CookieAccessPolicy(CookieMode.ALLOW_ALL)
        assertTrue(policy.allows("https://third.example.test/", "https://site.example.test/"))
        policy.setMode(CookieMode.BLOCK_ALL)
        assertFalse(policy.allows("https://third.example.test/", "https://site.example.test/"))
    }
}
