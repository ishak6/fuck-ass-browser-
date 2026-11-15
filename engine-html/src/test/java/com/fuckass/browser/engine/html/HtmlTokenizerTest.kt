package com.fuckass.browser.engine.html

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTokenizerTest {
    @Test fun tokenizesDocumentTagsTextAndDoctype() {
        val tokens = HtmlTokenizer().tokenize("<!doctype html><HTML><body>Hello</body></HTML>")
        assertTrue(tokens[0] is HtmlToken.Doctype)
        assertEquals(HtmlToken.StartTag("html", emptyMap(), false), tokens[1])
        assertEquals(HtmlToken.StartTag("body", emptyMap(), false), tokens[2])
        assertEquals(HtmlToken.Text("Hello"), tokens[3])
        assertEquals(HtmlToken.EndTag("body"), tokens[4])
    }

    @Test fun parsesQuotedUnquotedAndBooleanAttributes() {
        val tokens = HtmlTokenizer().tokenize("<a href='https://example.test/a?x=1' title=hello disabled>")
        val tag = tokens.single() as HtmlToken.StartTag
        assertEquals("https://example.test/a?x=1", tag.attributes["href"])
        assertEquals("hello", tag.attributes["title"])
        assertEquals("", tag.attributes["disabled"])
    }

    @Test fun firstDuplicateAttributeWinsAndNamesAreLowercase() {
        val tag = HtmlTokenizer().tokenize("<DIV ID=first id=second/>").single() as HtmlToken.StartTag
        assertEquals("first", tag.attributes["id"])
        assertEquals("div", tag.name)
        assertTrue(tag.selfClosing)
    }

    @Test fun decodesSupportedNamedAndNumericEntities() {
        val tokens = HtmlTokenizer().tokenize("A&amp;B &#65; &#x1F600; &unknown;")
        assertEquals(HtmlToken.Text("A&B A 😀 &unknown;"), tokens.single())
    }

    @Test fun commentsAndEndTagsAreRecognized() {
        val tokens = HtmlTokenizer().tokenize("<!-- safe --></SCRIPT>")
        assertEquals(HtmlToken.Comment(" safe "), tokens[0])
        assertEquals(HtmlToken.EndTag("script"), tokens[1])
    }

    @Test fun malformedLessThanIsPreservedAsText() {
        assertEquals(HtmlToken.Text("<"), HtmlTokenizer().tokenize("< 3")[0])
    }

    @Test fun inputAndTokenLimitsAreEnforced() {
        try {
            HtmlTokenizer(HtmlTokenizerLimits(maxInputChars = 3)).tokenize("1234")
            throw AssertionError("Expected input limit")
        } catch (_: IllegalArgumentException) { }

        try {
            HtmlTokenizer(HtmlTokenizerLimits(maxTokens = 1)).tokenize("a<b></b>")
            throw AssertionError("Expected token limit")
        } catch (_: IllegalArgumentException) { }
    }
}
