package com.fuckass.browser

import com.fuckass.browser.engine.css.CssParser
import com.fuckass.browser.engine.html.HtmlTokenizer
import com.fuckass.browser.engine.html.HtmlTreeBuilder
import com.fuckass.browser.engine.js.JsInterpreter
import com.fuckass.browser.engine.js.JsValue
import com.fuckass.browser.engine.render.DisplayCommand
import com.fuckass.browser.engine.render.LayoutEngine
import com.fuckass.browser.engine.render.LayoutViewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserPipelineIntegrationTest {
    @Test fun htmlCssAndCanvasDisplayListWorkTogether() {
        val document = HtmlTreeBuilder().build(
            HtmlTokenizer().tokenize("<html><body><main><p id='greeting'>Hello browser</p></main></body></html>")
        )
        val css = "main { background-color: #eeeeee } #greeting { color: blue; height: 24px }"
        assertTrue(CssParser().parse(css).isNotEmpty())
        val layout = LayoutEngine().layout(document, LayoutViewport(320f, 640f), listOf(css))
        assertTrue(layout.commands.any { it is DisplayCommand.DrawText && it.text == "Hello browser" })
        assertTrue(layout.commands.any { it is DisplayCommand.FillRect })
    }

    @Test fun scriptSubsetRunsWithoutHostAccess() {
        val result = JsInterpreter().execute("let count = 1; while (count < 4) { count = count + 1; } count")
        assertEquals(JsValue.NumberValue(4.0), result.value)
    }
}
