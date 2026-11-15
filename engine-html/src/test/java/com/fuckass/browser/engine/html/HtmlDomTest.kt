package com.fuckass.browser.engine.html

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlDomTest {
    @Test fun buildsNestedTreeAndSupportsBasicDomLookup() {
        val document = HtmlTreeBuilder().build(
            HtmlTokenizer().tokenize("<html><body><div id='main'><p>Hello</p><img src=x></div></body></html>")
        )
        val main = document.getElementById("main")
        assertNotNull(main)
        assertEquals("div", main!!.tagName)
        assertEquals(1, document.getElementsByTagName("img").size)
        assertEquals("x", document.getElementsByTagName("img").single().getAttribute("src"))
    }

    @Test fun voidElementsDoNotBecomeParents() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div><img><span>ok</span></div>"))
        val div = document.getElementsByTagName("div").single()
        assertEquals(listOf("img", "span"), div.children.filterIsInstance<HtmlElement>().map { it.tagName })
        assertEquals("ok", (div.children[1] as HtmlElement).children.single().let { (it as HtmlText).data })
    }

    @Test fun impliedParagraphAndListItemClosuresAreApplied() {
        val document = HtmlTreeBuilder().build(
            HtmlTokenizer().tokenize("<p>one<div>two</div><ul><li>a<li>b</ul>")
        )
        assertEquals(1, document.getElementsByTagName("p").single().children.size)
        assertEquals(2, document.getElementsByTagName("li").size)
        assertTrue(document.getElementsByTagName("li").all { it.children.size == 1 })
    }

    @Test fun endTagClosesMatchingOpenElement() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div><span>x</div><p>y</p>"))
        val div = document.getElementsByTagName("div").single()
        assertEquals(1, div.children.size)
        assertEquals("span", (div.children.single() as HtmlElement).tagName)
        assertEquals(2, document.children.filterIsInstance<HtmlElement>().size)
    }

    @Test fun nodeRemovalClearsParentReference() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div><span></span></div>"))
        val div = document.getElementsByTagName("div").single()
        val span = div.children.single()
        assertTrue(div.removeChild(span))
        assertNull(span.parent)
    }

    @Test fun rejectsSelfAndAncestorCyclesDuringDomMutation() {
        val parent = HtmlElement("div")
        val child = HtmlElement("span")
        parent.appendChild(child)
        try {
            parent.appendChild(parent)
            throw AssertionError("Expected self-cycle rejection")
        } catch (_: IllegalArgumentException) { }
        try {
            child.appendChild(parent)
            throw AssertionError("Expected ancestor-cycle rejection")
        } catch (_: IllegalArgumentException) { }
        assertEquals(parent, child.parent)
        assertEquals(child, parent.children.single())
    }

    @Test fun nestingAndNodeLimitsAreEnforced() {
        try {
            HtmlTreeBuilder(HtmlTreeLimits(maxDepth = 1)).build(
                HtmlTokenizer().tokenize("<div><span></span></div>")
            )
            throw AssertionError("Expected depth limit")
        } catch (_: IllegalArgumentException) { }

        try {
            HtmlTreeBuilder(HtmlTreeLimits(maxNodes = 1)).build(
                HtmlTokenizer().tokenize("<div>text</div>")
            )
            throw AssertionError("Expected node limit")
        } catch (_: IllegalArgumentException) { }
    }
}
