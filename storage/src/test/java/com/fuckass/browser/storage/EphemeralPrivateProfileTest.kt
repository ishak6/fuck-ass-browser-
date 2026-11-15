package com.fuckass.browser.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EphemeralPrivateProfileTest {
    @Test fun storesPrivateStateOnlyInSessionMemory() {
        val profile = EphemeralPrivateProfile()
        profile.putCookie("sid=secret")
        profile.putCache("https://example.test", byteArrayOf(1, 2, 3))
        profile.addHistory("https://example.test/page")
        profile.putFormData("email", "private@example.test")
        profile.addDownload("private.pdf")
        val snapshot = profile.snapshot()
        assertEquals(1, snapshot.cookieCount)
        assertEquals(1, snapshot.cacheEntryCount)
        assertEquals(1, snapshot.historyEntryCount)
        assertEquals(1, snapshot.formEntryCount)
        assertEquals(1, snapshot.downloadEntryCount)
        assertFalse(snapshot.closed)
    }

    @Test fun closeWipesEveryPrivateStoreAndIsIdempotent() {
        val profile = EphemeralPrivateProfile()
        profile.putCookie("sid=secret")
        profile.putCache("key", byteArrayOf(9, 8, 7))
        profile.addHistory("https://example.test")
        profile.putFormData("name", "private")
        profile.addDownload("file.bin")
        profile.close()
        profile.close()
        val snapshot = profile.snapshot()
        assertEquals(0, snapshot.cookieCount)
        assertEquals(0, snapshot.cacheEntryCount)
        assertEquals(0, snapshot.historyEntryCount)
        assertEquals(0, snapshot.formEntryCount)
        assertEquals(0, snapshot.downloadEntryCount)
        assertTrue(snapshot.closed)
    }

    @Test fun closedProfileRejectsFurtherWrites() {
        val profile = EphemeralPrivateProfile()
        profile.close()
        try {
            profile.addHistory("https://example.test")
            throw AssertionError("Expected closed profile rejection")
        } catch (_: IllegalStateException) { }
    }

    @Test fun clearWipesStateWithoutClosingProfile() {
        val profile = EphemeralPrivateProfile()
        profile.addHistory("https://example.test")
        profile.clear()
        assertEquals(0, profile.snapshot().historyEntryCount)
        assertFalse(profile.snapshot().closed)
        profile.addHistory("https://example.test/again")
        assertEquals(1, profile.snapshot().historyEntryCount)
    }
}
