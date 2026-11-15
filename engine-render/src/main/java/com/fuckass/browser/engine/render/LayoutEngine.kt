package com.fuckass.browser.engine.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.util.IdentityHashMap
import com.fuckass.browser.engine.css.ComputedStyleResolver
import com.fuckass.browser.engine.css.CssParser
import com.fuckass.browser.engine.css.CssParserLimits
import com.fuckass.browser.engine.css.CssRule
import com.fuckass.browser.engine.html.HtmlComment
import com.fuckass.browser.engine.html.HtmlDocument
import com.fuckass.browser.engine.html.HtmlElement
import com.fuckass.browser.engine.html.HtmlNode
import com.fuckass.browser.engine.html.HtmlText

data class LayoutViewport(val width: Float, val height: Float) {
    init { require(width > 0 && height > 0 && width.isFinite() && height.isFinite()) }
}

data class LayoutRect(val x: Float, val y: Float, val width: Float, val height: Float)

sealed interface DisplayCommand {
    data class FillRect(val rect: LayoutRect, val color: Int) : DisplayCommand
    data class DrawText(val text: String, val x: Float, val baseline: Float, val color: Int, val size: Float) : DisplayCommand
}

data class LayoutResult(
    val contentWidth: Float,
    val contentHeight: Float,
    val commands: List<DisplayCommand>
)

data class LayoutLimits(
    val maxNodes: Int = 100_000,
    val maxDepth: Int = 256,
    val maxCommands: Int = 200_000,
    val maxStyleRules: Int = 10_000,
    val maxStyleEvaluations: Int = 1_000_000
) {
    init {
        require(maxNodes > 0 && maxDepth > 0 && maxCommands > 0)
        require(maxStyleRules > 0 && maxStyleEvaluations > 0)
    }
}

/**
 * Initial block/inline layout subset. It deliberately does not implement full CSS formatting
 * contexts, flex/grid, floats, pagination, bidi or font shaping.
 */
class LayoutEngine(private val limits: LayoutLimits = LayoutLimits()) {
    fun layout(
        document: HtmlDocument,
        viewport: LayoutViewport,
        stylesheets: List<String> = emptyList()
    ): LayoutResult {
        val rules = stylesheets.flatMap { CssParser(CssParserLimits(maxRules = limits.maxStyleRules)).parse(it) }
        require(rules.size <= limits.maxStyleRules) { "Layout CSS rule limit exceeded" }
        val resolver = ComputedStyleResolver()
        val styleCache = IdentityHashMap<HtmlElement, Map<String, String>>()
        var styleEvaluations = 0
        fun computedStyle(element: HtmlElement, inherited: Map<String, String>): Map<String, String> {
            styleCache[element]?.let { return it }
            styleEvaluations += maxOf(1, rules.size)
            if (styleEvaluations > limits.maxStyleEvaluations) {
                throw IllegalArgumentException("Style evaluation budget exceeded")
            }
            return resolver.compute(element, rules, inherited).also { styleCache[element] = it }
        }
        val commands = mutableListOf<DisplayCommand>()
        var visited = 0
        var cursorY = 0f

        fun add(command: DisplayCommand) {
            if (commands.size >= limits.maxCommands) throw IllegalArgumentException("Display command limit exceeded")
            commands += command
        }

        fun renderNode(node: HtmlNode, x: Float, y: Float, availableWidth: Float, inherited: Map<String, String>, depth: Int): Float {
            if (depth > limits.maxDepth) throw IllegalArgumentException("Layout depth exceeded")
            visited++
            if (visited > limits.maxNodes) throw IllegalArgumentException("Layout node limit exceeded")
            return when (node) {
                is HtmlComment -> 0f
                is HtmlText -> {
                    val text = node.data.trim()
                    if (text.isEmpty()) 0f else {
                        val size = inherited["font-size"]?.removeSuffix("px")?.toFloatOrNull()?.coerceIn(6f, 96f) ?: 16f
                        val color = parseColor(inherited["color"]) ?: 0xFF202124.toInt()
                        val estimatedWidth = (text.length * size * 0.55f).coerceAtMost(availableWidth)
                        add(DisplayCommand.DrawText(text, x, y + size, color, size))
                        estimatedWidth
                    }
                }
                is HtmlElement -> {
                    val style = computedStyle(node, inherited)
                    if (style["display"] == "none") return 0f
                    val display = style["display"] ?: if (node.tagName in BLOCK_TAGS) "block" else "inline"
                    val width = cssLength(style["width"], availableWidth) ?: availableWidth
                    val background = parseColor(style["background-color"])
                    val commandStart = commands.size
                    var childY = y
                    var inlineX = x
                    var maxInlineWidth = 0f
                    node.children.forEach { child ->
                        val childIsBlock = child is HtmlElement && (
                            computedStyle(child, style)["display"] ?: if (child.tagName in BLOCK_TAGS) "block" else "inline"
                        ) == "block"
                        if (childIsBlock) {
                            childY += renderNode(child, x, childY, width, style, depth + 1)
                            inlineX = x
                            maxInlineWidth = maxOf(maxInlineWidth, inlineX - x)
                        } else {
                            val childWidth = renderNode(child, inlineX, childY, (x + width - inlineX).coerceAtLeast(1f), style, depth + 1)
                            inlineX += childWidth
                            maxInlineWidth = maxOf(maxInlineWidth, inlineX - x)
                        }
                    }
                    val explicitHeight = cssLength(style["height"], viewport.height)
                    val height = explicitHeight ?: if (display == "block") {
                        maxOf(childY - y, if (maxInlineWidth > 0f) 20f else 0f)
                    } else 20f
                    if (background != null && height > 0f) {
                        if (commands.size >= limits.maxCommands) throw IllegalArgumentException("Display command limit exceeded")
                        commands.add(commandStart, DisplayCommand.FillRect(LayoutRect(x, y, width, height), background))
                    }
                    if (display == "block") height.coerceAtLeast(20f) else maxInlineWidth
                }
            }
        }

        document.children.forEach { node ->
            cursorY += renderNode(node, 0f, cursorY, viewport.width, emptyMap(), 0)
        }
        return LayoutResult(viewport.width, cursorY, commands.toList())
    }

    private fun cssLength(value: String?, reference: Float): Float? {
        if (value == null) return null
        return when {
            value.endsWith("px") -> value.removeSuffix("px").toFloatOrNull()
            value.endsWith("%") -> value.removeSuffix("%").toFloatOrNull()?.let { reference * it / 100f }
            else -> null
        }?.takeIf { it.isFinite() && it >= 0f }?.coerceAtMost(reference * 4f)
    }

    private fun parseColor(value: String?): Int? {
        if (value == null) return null
        val normalized = value.trim().lowercase()
        val named = when (normalized) {
            "black" -> 0xFF000000.toInt()
            "white" -> 0xFFFFFFFF.toInt()
            "red" -> 0xFFFF0000.toInt()
            "green" -> 0xFF008000.toInt()
            "blue" -> 0xFF0000FF.toInt()
            "transparent" -> 0x00000000
            else -> null
        }
        if (named != null) return named
        if (!normalized.startsWith("#")) return null
        val hex = normalized.drop(1)
        return runCatching {
            when (hex.length) {
                3 -> (0xFF shl 24) or
                    (hex[0].digitToInt(16) * 17 shl 16) or
                    (hex[1].digitToInt(16) * 17 shl 8) or
                    (hex[2].digitToInt(16) * 17)
                6 -> (0xFF000000L or hex.toLong(16)).toInt()
                8 -> hex.toLong(16).toInt()
                else -> return null
            }
        }.getOrNull()
    }

    companion object {
        private val BLOCK_TAGS = setOf(
            "address", "article", "aside", "blockquote", "body", "div", "dl", "fieldset",
            "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr",
            "main", "nav", "ol", "p", "pre", "section", "table", "ul", "li"
        )
    }
}

/** Android Canvas backend. The display list is immutable and page scripts never receive Canvas. */
class CanvasRenderer {
    fun draw(canvas: Canvas, result: LayoutResult) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        result.commands.forEach { command ->
            when (command) {
                is DisplayCommand.FillRect -> {
                    paint.color = command.color
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(
                        RectF(
                            command.rect.x, command.rect.y,
                            command.rect.x + command.rect.width, command.rect.y + command.rect.height
                        ), paint
                    )
                }
                is DisplayCommand.DrawText -> {
                    paint.color = command.color
                    paint.textSize = command.size
                    paint.style = Paint.Style.FILL
                    canvas.drawText(command.text, command.x, command.baseline, paint)
                }
            }
        }
    }
}
