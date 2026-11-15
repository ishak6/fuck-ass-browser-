package com.fuckass.browser.tools

import com.fuckass.browser.engine.html.HtmlTokenizer
import com.fuckass.browser.engine.html.HtmlTreeBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderModeExtractorTest {
    @Test fun prefersArticleAndExtractsTitleAndReadableText() {
        val document = HtmlTreeBuilder().build(
            HtmlTokenizer().tokenize(
                "<html><head><title>Example story</title></head><body><nav>Menu</nav>" +
                    "<article><h1>Story</h1><p>Hello world from the article.</p><script>secret()</script></article>" +
                    "<footer>Footer</footer></body></html>"
            )
        )
        val article = ReaderModeExtractor().extract(document)!!
        assertEquals("Example story", article.title)
        assertTrue(article.text.contains("Hello world from the article."))
        assertTrue(!article.text.contains("secret"))
        assertTrue(!article.text.contains("Menu"))
    }

    @Test fun fallsBackToMainAndReturnsNullForMissingContent() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<main><p>Readable text</p></main>"))
        assertEquals("Readable text", ReaderModeExtractor().extract(document)!!.text)
        val empty = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<html></html>"))
        assertEquals(null, ReaderModeExtractor().extract(empty))
    }
}
