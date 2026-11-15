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

    @Test fun privateHistorySnapshotIsReadOnlyAndCleared() {
        val profile = EphemeralPrivateProfile()
        profile.addHistory("https://private.example/one")
        val snapshot = profile.historyEntries()
        assertEquals(listOf("https://private.example/one"), snapshot)
        profile.clear()
        assertEquals(listOf("https://private.example/one"), snapshot)
        assertTrue(profile.historyEntries().isEmpty())
    }

    @Test fun privateStoresEnforceEntryAndMemoryBudgets() {
        val profile = EphemeralPrivateProfile(
            PrivateProfileLimits(
                maxCookies = 1,
                maxCacheEntries = 1,
                maxCacheBytes = 4,
                maxHistoryEntries = 2,
                maxFormEntries = 1,
                maxDownloadEntries = 1,
                maxEntryChars = 16
            )
        )
        profile.putCookie("sid=1")
        profile.putCookie("sid=2")
        profile.putCache("one", byteArrayOf(1, 2, 3))
        profile.putCache("two", byteArrayOf(4, 5, 6))
        profile.addHistory("https://a.test")
        profile.addHistory("https://b.test")
        profile.addHistory("https://c.test")
        profile.putFormData("a", "one")
        profile.putFormData("b", "two")
        profile.addDownload("one.bin")
        profile.addDownload("two.bin")
        val snapshot = profile.snapshot()
        assertEquals(1, snapshot.cookieCount)
        assertEquals(1, snapshot.cacheEntryCount)
        assertEquals(2, snapshot.historyEntryCount)
        assertEquals(1, snapshot.formEntryCount)
        assertEquals(1, snapshot.downloadEntryCount)
        try {
            profile.putCache("oversized", ByteArray(5))
            throw AssertionError("Expected cache size rejection")
        } catch (_: IllegalArgumentException) { }
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
