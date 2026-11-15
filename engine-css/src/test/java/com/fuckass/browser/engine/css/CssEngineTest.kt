package com.fuckass.browser.engine.css

import com.fuckass.browser.engine.html.HtmlTokenizer
import com.fuckass.browser.engine.html.HtmlTreeBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CssEngineTest {
    @Test fun parsesRulesDeclarationsAndImportantFlag() {
        val rules = CssParser().parse("/*x*/ p, .note { color: red; margin: 2px !important; }")
        assertEquals(1, rules.size)
        assertEquals(2, rules.single().selectors.size)
        assertEquals("red", rules.single().declarations[0].value)
        assertTrue(rules.single().declarations[1].important)
    }

    @Test fun matchesCompoundAndDescendantSelectors() {
        val document = HtmlTreeBuilder().build(
            HtmlTokenizer().tokenize("<main><p id='intro' class='note large' data-kind='x'>Hi</p></main>")
        )
        val p = document.getElementsByTagName("p").single()
        val parser = CssParser()
        assertTrue(parser.parse("main p.note").single().selectors.single().matches(p))
        assertTrue(parser.parse("p#intro.large[data-kind=x]").single().selectors.single().matches(p))
        assertFalse(parser.parse("section p").single().selectors.single().matches(p))
    }

    @Test fun unsupportedPseudoAndChildCombinatorsAreSkipped() {
        val rules = CssParser().parse("p:hover { color:red } main > p { color:blue } p { color:green }")
        assertEquals(1, rules.size)
        assertEquals("green", rules.single().declarations.single().value)
    }

    @Test fun cascadeUsesSpecificityImportanceAndInlineStyle() {
        val document = HtmlTreeBuilder().build(
            HtmlTokenizer().tokenize("<div><p id='x' style='color: blue'>text</p></div>")
        )
        val p = document.getElementsByTagName("p").single()
        val rules = CssParser().parse("p { color: black } #x { color: red !important }")
        val style = ComputedStyleResolver().compute(p, rules)
        assertEquals("red", style["color"])
        assertEquals("inline", style["display"])
    }

    @Test fun inheritedPropertiesAreCarriedForward() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div><span>text</span></div>"))
        val span = document.getElementsByTagName("span").single()
        val style = ComputedStyleResolver().compute(span, emptyList(), mapOf("color" to "purple", "display" to "block"))
        assertEquals("purple", style["color"])
        assertEquals("inline", style["display"])
    }

    @Test fun parserEnforcesInputAndRuleLimits() {
        try {
            CssParser(CssParserLimits(maxInputChars = 4)).parse("p { color:red }")
            throw AssertionError("Expected CSS input limit")
        } catch (_: IllegalArgumentException) { }
        val limited = CssParser(CssParserLimits(maxRules = 1)).parse("p {color:red} div {color:blue}")
        assertEquals(1, limited.size)
    }
}
