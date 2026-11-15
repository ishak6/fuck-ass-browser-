package com.fuckass.browser.engine.css

import com.fuckass.browser.engine.html.HtmlElement

data class CssDeclaration(val property: String, val value: String, val important: Boolean = false)
data class CssAttributeSelector(val name: String, val value: String? = null)

data class CssCompoundSelector(
    val tag: String? = null,
    val id: String? = null,
    val classes: Set<String> = emptySet(),
    val attributes: List<CssAttributeSelector> = emptyList()
) {
    val specificity: Int get() = (if (id != null) 100 else 0) + classes.size * 10 +
        attributes.size * 10 + if (tag != null) 1 else 0

    fun matches(element: HtmlElement): Boolean {
        if (tag != null && tag != element.tagName) return false
        if (id != null && element.getAttribute("id") != id) return false
        val elementClasses = element.getAttribute("class").orEmpty().split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        if (!elementClasses.containsAll(classes)) return false
        return attributes.all { selector ->
            if (!element.hasAttribute(selector.name)) false
            else selector.value == null || element.getAttribute(selector.name) == selector.value
        }
    }
}

data class CssSelector(val compounds: List<CssCompoundSelector>) {
    val specificity: Int get() = compounds.sumOf { it.specificity }

    /** Supports compound selectors and descendant combinators only. */
    fun matches(element: HtmlElement): Boolean {
        if (compounds.isEmpty() || !compounds.last().matches(element)) return false
        var ancestor = element.parent
        var index = compounds.lastIndex - 1
        while (index >= 0) {
            var found: HtmlElement? = null
            while (ancestor != null) {
                if (compounds[index].matches(ancestor)) {
                    found = ancestor
                    ancestor = ancestor.parent
                    break
                }
                ancestor = ancestor.parent
            }
            if (found == null) return false
            index--
        }
        return true
    }
}

data class CssRule(
    val selectors: List<CssSelector>,
    val declarations: List<CssDeclaration>,
    val sourceOrder: Int
)

data class CssParserLimits(
    val maxInputChars: Int = 512 * 1024,
    val maxRules: Int = 10_000,
    val maxDeclarationsPerRule: Int = 128,
    val maxSelectorLength: Int = 512
) {
    init {
        require(maxInputChars > 0 && maxRules > 0)
        require(maxDeclarationsPerRule in 1..1024)
        require(maxSelectorLength in 1..4096)
    }
}

/** A bounded CSS subset parser. Unsupported selectors and at-rules are skipped, not guessed. */
class CssParser(private val limits: CssParserLimits = CssParserLimits()) {
    fun parse(source: String): List<CssRule> {
        require(source.length <= limits.maxInputChars) { "CSS input exceeds configured limit" }
        val css = source
        val rules = mutableListOf<CssRule>()
        var cursor = 0
        while (cursor < css.length && rules.size < limits.maxRules) {
            cursor = skipSpaceAndComments(css, cursor)
            if (cursor >= css.length) break
            if (css[cursor] == '@') {
                val open = css.indexOf('{', cursor)
                if (open < 0) break
                val close = findClosingBrace(css, open + 1)
                if (close < 0) break
                cursor = close + 1
                continue
            }
            val open = css.indexOf('{', cursor)
            if (open < 0) break
            val close = findClosingBrace(css, open + 1)
            if (close < 0) break
            val selectorText = css.substring(cursor, open).trim()
            val selectors = splitTopLevel(selectorText, ',')
                .mapNotNull { parseSelector(it.trim()) }
            val declarations = parseDeclarations(css.substring(open + 1, close))
            if (selectors.isNotEmpty() && declarations.isNotEmpty()) {
                rules += CssRule(selectors, declarations, rules.size)
            }
            cursor = close + 1
        }
        return rules
    }

    private fun parseSelector(source: String): CssSelector? {
        if (source.isBlank() || source.length > limits.maxSelectorLength ||
            source.any { it == '>' || it == '+' || it == '~' || it == ':' || it == '\\' }
        ) return null
        val parts = source.trim().split(Regex("\\s+"))
        val compounds = parts.map { parseCompound(it) ?: return null }
        return CssSelector(compounds)
    }

    private fun parseCompound(source: String): CssCompoundSelector? {
        var i = 0
        var tag: String? = null
        var id: String? = null
        val classes = linkedSetOf<String>()
        val attributes = mutableListOf<CssAttributeSelector>()
        if (source.startsWith("*")) i++
        else if (i < source.length && isIdentStart(source[i])) {
            val start = i
            while (i < source.length && isIdentChar(source[i])) i++
            tag = source.substring(start, i).lowercase()
        }
        while (i < source.length) {
            when (source[i]) {
                '#' -> {
                    i++
                    val start = i
                    while (i < source.length && isIdentChar(source[i])) i++
                    if (i == start || id != null) return null
                    id = source.substring(start, i)
                }
                '.' -> {
                    i++
                    val start = i
                    while (i < source.length && isIdentChar(source[i])) i++
                    if (i == start) return null
                    classes += source.substring(start, i)
                }
                '[' -> {
                    val end = source.indexOf(']', i + 1)
                    if (end < 0) return null
                    val raw = source.substring(i + 1, end).trim()
                    val equals = raw.indexOf('=')
                    val attrName = (if (equals < 0) raw else raw.substring(0, equals)).trim().lowercase()
                    if (!attrName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_:-]*"))) return null
                    val attrValue = if (equals < 0) null else raw.substring(equals + 1).trim()
                        .removeSurrounding("\"").removeSurrounding("'")
                    attributes += CssAttributeSelector(attrName, attrValue)
                    i = end + 1
                }
                else -> return null
            }
        }
        if (tag == null && id == null && classes.isEmpty() && attributes.isEmpty()) return null
        return CssCompoundSelector(tag, id, classes, attributes)
    }

    private fun parseDeclarations(block: String): List<CssDeclaration> =
        splitTopLevel(block, ';').take(limits.maxDeclarationsPerRule).mapNotNull { raw ->
            val colon = raw.indexOf(':')
            if (colon <= 0) return@mapNotNull null
            val property = raw.substring(0, colon).trim().lowercase()
            if (!property.matches(Regex("--[a-zA-Z0-9_-]+|[a-zA-Z][a-zA-Z0-9-]*"))) return@mapNotNull null
            var value = raw.substring(colon + 1).trim()
            if (value.isEmpty() || value.length > 2048) return@mapNotNull null
            val important = value.endsWith("!important", true)
            if (important) value = value.dropLast(10).trimEnd()
            if (value.isEmpty()) null else CssDeclaration(property, value, important)
        }

    private fun skipSpaceAndComments(css: String, start: Int): Int {
        var i = start
        while (i < css.length) {
            if (css[i].isWhitespace()) i++
            else if (css.startsWith("/*", i)) {
                val end = css.indexOf("*/", i + 2)
                if (end < 0) return css.length
                i = end + 2
            } else break
        }
        return i
    }

    private fun findClosingBrace(css: String, start: Int): Int {
        var depth = 1
        var quote: Char? = null
        var i = start
        while (i < css.length) {
            val c = css[i]
            if (quote != null) {
                if (c == quote && (i == 0 || css[i - 1] != '\\')) quote = null
            } else when (c) {
                '"', '\'' -> quote = c
                '{' -> depth++
                '}' -> if (--depth == 0) return i
            }
            i++
        }
        return -1
    }

    private fun splitTopLevel(value: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        var depth = 0
        var quote: Char? = null
        value.forEachIndexed { index, c ->
            if (quote != null) {
                if (c == quote && (index == 0 || value[index - 1] != '\\')) quote = null
            } else when (c) {
                '"', '\'' -> quote = c
                '(' -> depth++
                ')' -> if (depth > 0) depth--
                delimiter -> if (depth == 0) {
                    result += value.substring(start, index)
                    start = index + 1
                }
            }
        }
        result += value.substring(start)
        return result
    }

    private fun isIdentStart(c: Char): Boolean = c.isLetter() || c == '_' || c == '-'
    private fun isIdentChar(c: Char): Boolean = isIdentStart(c) || c.isDigit()
}

class ComputedStyleResolver {
    fun compute(
        element: HtmlElement,
        rules: List<CssRule>,
        inherited: Map<String, String> = emptyMap()
    ): Map<String, String> {
        data class Winner(val important: Boolean, val specificity: Int, val order: Int, val value: String)
        val winners = mutableMapOf<String, Winner>()
        val inheritedProperties = setOf("color", "font-family", "font-size", "font-style", "font-weight", "line-height", "text-align", "visibility")
        inherited.filterKeys { it in inheritedProperties }.forEach { (key, value) ->
            winners[key] = Winner(false, -1, -1, value)
        }

        rules.forEach { rule ->
            val specificity = rule.selectors.filter { it.matches(element) }.maxOfOrNull { it.specificity } ?: return@forEach
            rule.declarations.forEach { declaration ->
                val candidate = Winner(declaration.important, specificity, rule.sourceOrder, declaration.value)
                val current = winners[declaration.property]
                if (current == null || compareValuesBy(candidate, current, Winner::important, Winner::specificity, Winner::order) >= 0) {
                    winners[declaration.property] = candidate
                }
            }
        }

        val inline = element.getAttribute("style")
        if (!inline.isNullOrBlank()) {
            inline.split(';').forEachIndexed { index, item ->
                val colon = item.indexOf(':')
                if (colon > 0) {
                    val property = item.substring(0, colon).trim().lowercase()
                    var value = item.substring(colon + 1).trim()
                    val important = value.endsWith("!important", true)
                    if (important) value = value.dropLast(10).trimEnd()
                    if (property.matches(Regex("[a-z][a-z0-9-]*")) && value.isNotEmpty()) {
                        val candidate = Winner(important, 1000, index, value)
                        val current = winners[property]
                        if (current == null || compareValuesBy(candidate, current, Winner::important, Winner::specificity, Winner::order) >= 0) {
                            winners[property] = candidate
                        }
                    }
                }
            }
        }

        val result = winners.mapValues { it.value.value }.toMutableMap()
        if ("display" !in result) result["display"] = if (element.tagName in BLOCK_ELEMENTS) "block" else "inline"
        return result
    }

    companion object {
        private val BLOCK_ELEMENTS = setOf(
            "address", "article", "aside", "blockquote", "body", "div", "dl", "fieldset",
            "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr",
            "main", "nav", "ol", "p", "pre", "section", "table", "ul", "li"
        )
    }
}
