package com.fuckass.browser.storage

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilePrivateSiteRulesStoreTest {
    @Test fun privateSiteRulesPersistAndNormalizeInternationalHostnames() {
        val directory = Files.createTempDirectory("private-sites").toFile()
        try {
            val first = FilePrivateSiteRulesStore(directory)
            first.add("bücher.example")
            first.add("LOGIN.Example.com.")
            val reopened = FilePrivateSiteRulesStore(directory)
            assertEquals(
                setOf("xn--bcher-kva.example", "login.example.com"),
                reopened.hosts()
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun privateSiteRulesCanBeRemovedAndRejectUrlInput() {
        val directory = Files.createTempDirectory("private-sites").toFile()
        try {
            val store = FilePrivateSiteRulesStore(directory)
            store.add("private.example")
            assertTrue(store.remove("private.example").isEmpty())
            try {
                store.add("https://bad.example/path")
                throw AssertionError("Expected URL input rejection")
            } catch (_: IllegalArgumentException) { }
            assertFalse(store.hosts().contains("bad.example"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
