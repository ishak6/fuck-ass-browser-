package com.fuckass.browser.search

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeSearchClientTest {
    @Test fun snippetMarkupIsRemovedAndEntitiesDecoded() {
        assertEquals(
            "Native browser & search",
            NativeSearchClient.cleanSnippet("<span>Native</span> browser &amp; search")
        )
    }

    @Test fun snippetLengthIsBounded() {
        assertEquals(500, NativeSearchClient.cleanSnippet("x".repeat(700)).length)
    }
}
