package com.fuckass.browser.storage

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileBrowserDataStoreTest {
    @Test fun bookmarksAndHistorySurviveStoreRecreation() {
        val directory = Files.createTempDirectory("browser-store").toFile()
        try {
            val first = FileBrowserDataStore(directory)
            first.saveBookmark("https://example.test", "Example")
            first.addHistory("https://example.test/page", 100L)
            first.addHistory("https://example.test/other", 200L)

            val reopened = FileBrowserDataStore(directory)
            assertEquals(listOf("https://example.test" to "Example"), reopened.bookmarks())
            assertEquals(
                listOf("https://example.test/other" to 200L, "https://example.test/page" to 100L),
                reopened.history()
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun removeBookmarkAndHistoryLimitAreApplied() {
        val directory = Files.createTempDirectory("browser-store").toFile()
        try {
            val store = FileBrowserDataStore(directory)
            store.saveBookmark("https://example.test", "Example")
            store.removeBookmark("https://example.test")
            store.addHistory("https://a.test", 1L)
            store.addHistory("https://b.test", 2L)
            assertTrue(store.bookmarks().isEmpty())
            assertEquals(listOf("https://b.test" to 2L), store.history(1))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun clearHistoryPersistsWithoutDeletingBookmarks() {
        val directory = Files.createTempDirectory("browser-store").toFile()
        try {
            val store = FileBrowserDataStore(directory)
            store.saveBookmark("https://example.test", "Example")
            store.addHistory("https://example.test/page", 10L)

            store.clearHistory()

            val reopened = FileBrowserDataStore(directory)
            assertTrue(reopened.history().isEmpty())
            assertEquals(listOf("https://example.test" to "Example"), reopened.bookmarks())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun invalidHistoryLimitIsRejected() {
        val directory = Files.createTempDirectory("browser-store").toFile()
        try {
            val store = FileBrowserDataStore(directory)
            try {
                store.history(-1)
                throw AssertionError("Expected negative limit rejection")
            } catch (_: IllegalArgumentException) { }
        } finally {
            directory.deleteRecursively()
        }
    }
}
