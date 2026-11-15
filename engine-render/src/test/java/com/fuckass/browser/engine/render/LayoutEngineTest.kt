package com.fuckass.browser.engine.render

import com.fuckass.browser.engine.html.HtmlTokenizer
import com.fuckass.browser.engine.html.HtmlTreeBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutEngineTest {
    @Test fun laysOutBlockContentAndProducesDisplayCommands() {
        val document = HtmlTreeBuilder().build(
            HtmlTokenizer().tokenize("<html><body><div id='card'>Hello</div></body></html>")
        )
        val result = LayoutEngine().layout(
            document,
            LayoutViewport(360f, 640f),
            listOf("#card { height: 48px; background-color: #ffffff; color: #000000; }")
        )
        assertTrue(result.contentHeight >= 48f)
        assertTrue(result.commands.any { it is DisplayCommand.DrawText && it.text == "Hello" })
        assertTrue(result.commands.any { it is DisplayCommand.FillRect })
    }

    @Test fun backgroundsPrecedeChildTextInDisplayList() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div>Text</div>"))
        val result = LayoutEngine().layout(
            document,
            LayoutViewport(200f, 200f),
            listOf("div { height: 40px; background-color: red }")
        )
        assertTrue(result.commands.first() is DisplayCommand.FillRect)
        assertTrue(result.commands.last() is DisplayCommand.DrawText)
    }

    @Test fun displayNoneSuppressesSubtree() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div>hidden</div><p>visible</p>"))
        val result = LayoutEngine().layout(
            document,
            LayoutViewport(200f, 200f),
            listOf("div { display: none }")
        )
        assertTrue(result.commands.none { it is DisplayCommand.DrawText && it.text == "hidden" })
        assertTrue(result.commands.any { it is DisplayCommand.DrawText && it.text == "visible" })
    }

    @Test fun rejectsInvalidViewportAndEnforcesNodeLimit() {
        try {
            LayoutViewport(0f, 10f)
            throw AssertionError("Expected invalid viewport")
        } catch (_: IllegalArgumentException) { }

        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div>text</div>"))
        try {
            LayoutEngine(LayoutLimits(maxNodes = 1)).layout(document, LayoutViewport(100f, 100f))
            throw AssertionError("Expected layout node limit")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun inlineElementsAdvanceByTheirContentWidth() {
        val document = HtmlTreeBuilder().build(
            HtmlTokenizer().tokenize("<div><span>A</span><span>B</span></div>")
        )
        val texts = LayoutEngine().layout(document, LayoutViewport(200f, 100f))
            .commands.filterIsInstance<DisplayCommand.DrawText>()
        assertEquals(2, texts.size)
        assertTrue(texts[1].x > texts[0].x)
        assertTrue(texts[1].x < 20f)
    }

    @Test fun cssStyleEvaluationBudgetIsEnforced() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div><p>text</p></div>"))
        try {
            LayoutEngine(LayoutLimits(maxStyleEvaluations = 1)).layout(
                document,
                LayoutViewport(100f, 100f),
                listOf("div { color: red }")
            )
            throw AssertionError("Expected style evaluation budget")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun totalCssRuleCountIsBounded() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div></div>"))
        try {
            LayoutEngine(LayoutLimits(maxStyleRules = 1)).layout(
                document,
                LayoutViewport(100f, 100f),
                listOf("div { color: red }", "p { color: blue }")
            )
            throw AssertionError("Expected CSS rule limit")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun cssPixelAndPercentageLengthsAreBounded() {
        val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize("<div></div>"))
        val result = LayoutEngine().layout(
            document,
            LayoutViewport(100f, 100f),
            listOf("div { width: 50%; height: 25px; background-color: #abc }")
        )
        val background = result.commands.filterIsInstance<DisplayCommand.FillRect>().single()
        assertEquals(50f, background.rect.width)
        assertEquals(25f, background.rect.height)
    }
}
