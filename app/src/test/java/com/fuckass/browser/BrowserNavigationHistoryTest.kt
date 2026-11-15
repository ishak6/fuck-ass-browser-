package com.fuckass.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserNavigationHistoryTest {
    @Test fun backForwardAndBranchingNavigationWork() {
        val history = BrowserNavigationHistory()
        history.record("https://a.test")
        history.record("https://b.test")
        history.record("https://c.test")
        assertEquals("https://b.test", history.goBack())
        assertEquals("https://c.test", history.nextUrl())
        history.record("https://d.test")
        assertNull(history.nextUrl())
        assertEquals(
            listOf("https://a.test", "https://b.test", "https://d.test"),
            history.snapshot().entries
        )
    }

    @Test fun duplicateCurrentUrlDoesNotCreateAnotherEntry() {
        val history = BrowserNavigationHistory()
        history.record("https://a.test")
        history.record("https://a.test")
        assertEquals(1, history.snapshot().entries.size)
    }

    @Test fun historyIsBoundedAndCanBeReset() {
        val history = BrowserNavigationHistory(maxEntries = 2)
        history.record("https://a.test")
        history.record("https://b.test")
        history.record("https://c.test")
        assertEquals(listOf("https://b.test", "https://c.test"), history.snapshot().entries)
        assertTrue(history.snapshot().canGoBack)
        history.reset()
        assertFalse(history.snapshot().canGoBack)
        assertNull(history.previousUrl())
    }
}
