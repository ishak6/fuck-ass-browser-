package com.fuckass.browser.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class OriginStorageManagerTest {
    @Test
    fun dataIsSharedWithinAnOriginButNotAcrossHostsPortsOrSchemes() {
        val storage = OriginStorageManager()
        storage.setItem("https://example.test/a", OriginStorageType.LOCAL, "token", "one")
        assertEquals("one", storage.getItem("https://example.test/b", OriginStorageType.LOCAL, "token"))
        assertNull(storage.getItem("https://sub.example.test/", OriginStorageType.LOCAL, "token"))
        assertNull(storage.getItem("https://example.test:444/", OriginStorageType.LOCAL, "token"))
        assertNull(storage.getItem("http://example.test/", OriginStorageType.LOCAL, "token"))
    }

    @Test
    fun localAndSessionStorageAreSeparate() {
        val storage = OriginStorageManager()
        storage.setItem("https://example.test/", OriginStorageType.LOCAL, "k", "local")
        storage.setItem("https://example.test/", OriginStorageType.SESSION, "k", "session")
        assertEquals("local", storage.getItem("https://example.test/", OriginStorageType.LOCAL, "k"))
        assertEquals("session", storage.getItem("https://example.test/", OriginStorageType.SESSION, "k"))
        storage.clearSessionStorage("https://example.test/")
        assertNull(storage.getItem("https://example.test/", OriginStorageType.SESSION, "k"))
        assertEquals("local", storage.getItem("https://example.test/", OriginStorageType.LOCAL, "k"))
    }

    @Test
    fun quotaRejectsWritesWithoutDestroyingExistingData() {
        val storage = OriginStorageManager(maxBytesPerOrigin = 12, maxBytesTotal = 24)
        storage.setItem("https://example.test/", OriginStorageType.LOCAL, "k", "1234")
        assertThrows(StorageQuotaExceededException::class.java) {
            storage.setItem("https://example.test/", OriginStorageType.LOCAL, "k", "0123456789")
        }
        assertEquals("1234", storage.getItem("https://example.test/", OriginStorageType.LOCAL, "k"))
    }

    @Test
    fun entryAndOriginCountsAreBounded() {
        val storage = OriginStorageManager(maxOrigins = 1, maxEntriesPerOrigin = 1)
        storage.setItem("https://a.test/", OriginStorageType.LOCAL, "a", "1")
        assertThrows(StorageQuotaExceededException::class.java) {
            storage.setItem("https://a.test/", OriginStorageType.SESSION, "b", "2")
        }
        assertThrows(StorageQuotaExceededException::class.java) {
            storage.setItem("https://b.test/", OriginStorageType.LOCAL, "b", "2")
        }
    }

    @Test
    fun clearOriginRemovesOnlyTheSelectedPartitionAndReleasesQuota() {
        val storage = OriginStorageManager()
        storage.setItem("https://a.test/", OriginStorageType.LOCAL, "k", "a")
        storage.setItem("https://b.test/", OriginStorageType.LOCAL, "k", "b")
        val before = storage.usageBytes()
        storage.clearOrigin("https://a.test/path")
        assertEquals(before - 2L, storage.usageBytes())
        assertNull(storage.getItem("https://a.test/", OriginStorageType.LOCAL, "k"))
        assertEquals("b", storage.getItem("https://b.test/", OriginStorageType.LOCAL, "k"))
    }

    @Test
    fun idnHostsAreCanonicalizedToTheSameOrigin() {
        val storage = OriginStorageManager()
        storage.setItem("https://bücher.example/", OriginStorageType.LOCAL, "k", "value")
        assertEquals("value", storage.getItem("https://xn--bcher-kva.example/path", OriginStorageType.LOCAL, "k"))
    }

    @Test
    fun malformedOriginsCannotBeWritten() {
        val storage = OriginStorageManager()
        assertThrows(IllegalArgumentException::class.java) {
            storage.setItem("javascript:alert(1)", OriginStorageType.LOCAL, "k", "v")
        }
    }
}
