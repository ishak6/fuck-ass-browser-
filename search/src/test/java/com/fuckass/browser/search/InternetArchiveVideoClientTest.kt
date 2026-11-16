package com.fuckass.browser.search

import org.junit.Assert.assertEquals
import org.junit.Test

class InternetArchiveVideoClientTest {
    @Test fun lucenePhraseCannotInjectQuotesOrNewlines() {
        assertEquals("hello world", InternetArchiveVideoClient.escapeLucenePhrase("hello\" world\n"))
    }

    @Test fun descriptionsAreBoundedAndMarkupFree() {
        assertEquals("A public film", InternetArchiveVideoClient.cleanText("<b>A</b> public film"))
        assertEquals(500, InternetArchiveVideoClient.cleanText("x".repeat(600)).length)
    }
}
