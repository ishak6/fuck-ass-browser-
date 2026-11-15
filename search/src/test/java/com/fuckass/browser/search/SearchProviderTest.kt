package com.fuckass.browser.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchProviderTest {
    @Test fun providerEncodesQueryAndUsesHttps() {
        val provider = BuiltInSearchProviders.find("ddg")!!
        val uri = provider.searchUri("cats & dogs")
        assertEquals("https", uri.scheme)
        assertTrue(uri.toString().contains("q=cats+%26+dogs"))
    }

    @Test fun rejectsUnsafeSearchProviderEndpoints() {
        try {
            UrlTemplateSearchProvider("custom", "Custom", "https://user@search.example/", "q")
            throw AssertionError("Expected userinfo rejection")
        } catch (_: IllegalArgumentException) { }
        try {
            UrlTemplateSearchProvider("custom", "Custom", "https://search.example/#fragment", "q")
            throw AssertionError("Expected fragment rejection")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun builtInProvidersHaveUniqueIdsAndHttpsEndpoints() {
        assertEquals(BuiltInSearchProviders.all.size, BuiltInSearchProviders.all.map { it.id }.toSet().size)
        assertTrue(BuiltInSearchProviders.all.all { it.searchUri("test").scheme == "https" })
    }

    @Test fun blankQueryIsRejected() {
        val provider = BuiltInSearchProviders.find("google")!!
        try {
            provider.searchUri("  ")
            throw AssertionError("Expected blank query rejection")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun metaSearchIsDisabledByDefaultAndRequiresDisclosureWhenEnabled() {
        assertFalse(MetaSearchSettings().enabled)
        assertFalse(MetaSearchSettings().requireDisclosure())
        assertTrue(MetaSearchSettings(enabled = true).requireDisclosure())
    }

    @Test fun bangShortcutsSelectProviderAndRemoveShortcut() {
        val request = SearchBangParser.resolve("!g native browser engine", "ddg")
        assertEquals("google", request.providerId)
        assertEquals("native browser engine", request.query)
    }

    @Test fun bangShortcutsAreCaseInsensitive() {
        val request = SearchBangParser.resolve("!G Kotlin browser", "ddg")
        assertEquals("google", request.providerId)
        assertEquals("Kotlin browser", request.query)
    }

    @Test fun unknownBangRemainsInQuery() {
        val request = SearchBangParser.resolve("!unknown words", "ddg")
        assertEquals("ddg", request.providerId)
        assertEquals("!unknown words", request.query)
    }
}
