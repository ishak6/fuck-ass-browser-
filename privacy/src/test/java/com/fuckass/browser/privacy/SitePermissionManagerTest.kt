package com.fuckass.browser.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SitePermissionManagerTest {
    @Test
    fun unknownPermissionDefaultsToAskAndMalformedUrlsFailClosed() {
        val manager = SitePermissionManager()
        assertEquals(SitePermissionDecision.ASK, manager.decision("https://example.test/path", SitePermission.CAMERA))
        assertEquals(SitePermissionDecision.BLOCK, manager.decision("http://example.test/", SitePermission.CAMERA))
        assertEquals(SitePermissionDecision.BLOCK, manager.decision("https://user@example.test/", SitePermission.CAMERA))
    }

    @Test
    fun decisionsAreScopedToSchemeHostAndPortButNotPath() {
        val manager = SitePermissionManager()
        manager.setDecision("https://example.test/account", SitePermission.CAMERA, SitePermissionDecision.ALLOW)
        assertEquals(SitePermissionDecision.ALLOW, manager.decision("https://example.test/settings", SitePermission.CAMERA))
        assertEquals(SitePermissionDecision.ASK, manager.decision("https://example.test:444/", SitePermission.CAMERA))
        assertEquals(SitePermissionDecision.ASK, manager.decision("https://sub.example.test/", SitePermission.CAMERA))
    }

    @Test
    fun defaultPortAndHostCaseAreCanonicalized() {
        val manager = SitePermissionManager()
        manager.setDecision("https://EXAMPLE.test:443/a", SitePermission.LOCATION, SitePermissionDecision.BLOCK)
        assertEquals(SitePermissionDecision.BLOCK, manager.decision("https://example.test/b", SitePermission.LOCATION))
    }

    @Test
    fun internationalizedHostIsCanonicalized() {
        val manager = SitePermissionManager()
        manager.setDecision("https://bücher.example/", SitePermission.NOTIFICATIONS, SitePermissionDecision.ALLOW)
        assertEquals(SitePermissionDecision.ALLOW, manager.decision("https://xn--bcher-kva.example/other", SitePermission.NOTIFICATIONS))
    }

    @Test
    fun decisionsAreIndependentAndCanBeCleared() {
        val manager = SitePermissionManager()
        manager.setDecision("https://example.test/", SitePermission.CAMERA, SitePermissionDecision.ALLOW)
        manager.setDecision("https://example.test/", SitePermission.MICROPHONE, SitePermissionDecision.BLOCK)
        assertEquals(SitePermissionDecision.ALLOW, manager.decision("https://example.test/", SitePermission.CAMERA))
        assertEquals(SitePermissionDecision.BLOCK, manager.decision("https://example.test/", SitePermission.MICROPHONE))
        manager.clearSite("https://example.test/path")
        assertEquals(SitePermissionDecision.ASK, manager.decision("https://example.test/", SitePermission.CAMERA))
    }

    @Test
    fun storeCapacityIsBoundedAndEvictsOldestOrigin() {
        val store = InMemorySitePermissionStore(maxOrigins = 2)
        val manager = SitePermissionManager(store)
        manager.setDecision("https://a.test/", SitePermission.CAMERA, SitePermissionDecision.ALLOW)
        manager.setDecision("https://b.test/", SitePermission.CAMERA, SitePermissionDecision.BLOCK)
        manager.setDecision("https://c.test/", SitePermission.CAMERA, SitePermissionDecision.ALLOW)
        assertEquals(SitePermissionDecision.ASK, manager.decision("https://a.test/", SitePermission.CAMERA))
        assertEquals(SitePermissionDecision.BLOCK, manager.decision("https://b.test/", SitePermission.CAMERA))
        assertEquals(SitePermissionDecision.ALLOW, manager.decision("https://c.test/", SitePermission.CAMERA))
    }

    @Test
    fun invalidOriginCannotBePersisted() {
        val manager = SitePermissionManager()
        assertThrows(IllegalArgumentException::class.java) {
            manager.setDecision("javascript:alert(1)", SitePermission.JAVASCRIPT, SitePermissionDecision.ALLOW)
        }
    }
}
