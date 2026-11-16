package com.fuckass.browser.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class OriginStorageSessionTest {
    @Test
    fun storageAreasAreBoundToTheNormalizedOrigin() {
        val manager = OriginStorageManager()
        val session = OriginStorageSession(manager, "https://example.test:443/path")
        session.localStorage.setItem("theme", "dark")

        assertEquals("dark", session.localStorage.getItem("theme"))
        assertNull(manager.getItem("https://other.test/", OriginStorageType.LOCAL, "theme"))
        assertEquals("dark", manager.getItem("https://example.test/elsewhere", OriginStorageType.LOCAL, "theme"))
    }

    @Test
    fun localAndSessionAreasAreSeparateAndCloseClearsOnlySessionData() {
        val manager = OriginStorageManager()
        val session = OriginStorageSession(manager, "https://example.test/")
        session.localStorage.setItem("persist", "yes")
        session.sessionStorage.setItem("temporary", "yes")

        session.close()

        assertEquals("yes", manager.getItem("https://example.test/", OriginStorageType.LOCAL, "persist"))
        assertNull(manager.getItem("https://example.test/", OriginStorageType.SESSION, "temporary"))
        assertThrows(IllegalStateException::class.java) {
            session.localStorage.getItem("persist")
        }
    }

    @Test
    fun clearAndIndexedAccessBehaveDeterministically() {
        val session = OriginStorageSession(OriginStorageManager(), "https://example.test/")
        session.localStorage.setItem("a", "1")
        session.localStorage.setItem("b", "2")

        assertEquals(2, session.localStorage.length())
        assertEquals("a", session.localStorage.key(0))
        assertNull(session.localStorage.key(2))
        session.localStorage.clear()
        assertEquals(0, session.localStorage.length())
        assertThrows(IllegalArgumentException::class.java) {
            session.localStorage.key(-1)
        }
    }

    @Test
    fun insecureOrMalformedOriginsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            OriginStorageSession(OriginStorageManager(), "http://example.test/")
        }
    }
}
