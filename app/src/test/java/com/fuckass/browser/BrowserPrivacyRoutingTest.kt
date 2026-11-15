package com.fuckass.browser

import com.fuckass.browser.privacy.PrivateSiteRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserPrivacyRoutingTest {
    @Test fun manualPrivateModeAlwaysSelectsPrivateProfile() {
        assertTrue(requiresPrivateSession(true, "https://ordinary.example/", PrivateSiteRules()))
    }

    @Test fun configuredSiteAndSubdomainsAutomaticallySelectPrivateProfile() {
        val rules = PrivateSiteRules(setOf("accounts.example.com"))
        assertTrue(requiresPrivateSession(false, "https://accounts.example.com/login", rules))
        assertTrue(requiresPrivateSession(false, "https://secure.accounts.example.com/", rules))
        assertFalse(requiresPrivateSession(false, "https://example.com/", rules))
    }
}
