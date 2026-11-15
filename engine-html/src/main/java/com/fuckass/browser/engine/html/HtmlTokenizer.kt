package com.fuckass.browser.engine.html

sealed interface HtmlToken {
    data class StartTag(val name: String, val attributes: Map<String, String>, val selfClosing: Boolean) : HtmlToken
    data class EndTag(val name: String) : HtmlToken
    data class Text(val value: String) : HtmlToken
    data class Comment(val value: String) : HtmlToken
    data class Doctype(val value: String) : HtmlToken
}

data class HtmlTokenizerLimits(
    val maxInputChars: Int = 2 * 1024 * 1024,
    val maxTokens: Int = 100_000,
    val maxAttributesPerTag: Int = 128,
    val maxNameLength: Int = 128
) {
    init {
        require(maxInputChars > 0)
        require(maxTokens > 0)
        require(maxAttributesPerTag in 1..1024)
        require(maxNameLength in 1..1024)
    }
}

/**
 * Bounded, deliberately incomplete HTML tokenizer. It implements common document markup
 * primitives, not the full WHATWG tokenizer state machine. Malformed input is recovered
 * conservatively and resource limits are enforced before token allocation grows unbounded.
 */
class HtmlTokenizer(
    private val limits: HtmlTokenizerLimits = HtmlTokenizerLimits()
) {
    fun tokenize(source: String): List<HtmlToken> {
        require(source.length <= limits.maxInputChars) { "HTML input exceeds configured limit" }
        val tokens = ArrayList<HtmlToken>()
        var index = 0

        fun emit(token: HtmlToken) {
            if (tokens.size >= limits.maxTokens) throw IllegalArgumentException("HTML token limit exceeded")
            tokens += token
        }

        while (index < source.length) {
            if (source[index] != '<') {
                val next = source.indexOf('<', index).let { if (it < 0) source.length else it }
                emit(HtmlToken.Text(decodeEntities(source.substring(index, next))))
                index = next
                continue
            }

            if (source.startsWith("<!--", index)) {
                val end = source.indexOf("-->", index + 4)
                if (end < 0) {
                    emit(HtmlToken.Comment(source.substring(index + 4)))
                    index = source.length
                } else {
                    emit(HtmlToken.Comment(source.substring(index + 4, end)))
                    index = end + 3
                }
                continue
            }

            if (source.regionMatches(index, "<!doctype", 0, 9, ignoreCase = true)) {
                val end = source.indexOf('>', index + 9)
                if (end < 0) {
                    emit(HtmlToken.Text("<"))
                    index++
                } else {
                    emit(HtmlToken.Doctype(source.substring(index + 9, end).trim()))
                    index = end + 1
                }
                continue
            }

            if (source.startsWith("</", index)) {
                val parsed = parseEndTag(source, index + 2)
                if (parsed == null) {
                    emit(HtmlToken.Text("<"))
                    index++
                } else {
                    emit(HtmlToken.EndTag(parsed.first))
                    index = parsed.second
                }
                continue
            }

            if (index + 1 < source.length && isNameStart(source[index + 1])) {
                val parsed = parseStartTag(source, index + 1)
                if (parsed == null) {
                    emit(HtmlToken.Text("<"))
                    index++
                } else {
                    emit(parsed.first)
                    index = parsed.second
                }
                continue
            }

            emit(HtmlToken.Text("<"))
            index++
        }
        return tokens
    }

    private fun parseEndTag(source: String, start: Int): Pair<String, Int>? {
        var i = start
        while (i < source.length && source[i].isWhitespace()) i++
        val nameStart = i
        while (i < source.length && isNameChar(source[i])) i++
        if (i == nameStart || i - nameStart > limits.maxNameLength) return null
        val name = source.substring(nameStart, i).lowercase()
        val close = source.indexOf('>', i)
        return if (close < 0) null else name to (close + 1)
    }

    private fun parseStartTag(source: String, start: Int): Pair<HtmlToken.StartTag, Int>? {
        var i = start
        val nameStart = i
        while (i < source.length && isNameChar(source[i])) i++
        if (i == nameStart || i - nameStart > limits.maxNameLength) return null
        val name = source.substring(nameStart, i).lowercase()
        val attributes = linkedMapOf<String, String>()
        var selfClosing = false

        while (i < source.length) {
            while (i < source.length && source[i].isWhitespace()) i++
            if (i >= source.length) return null
            if (source[i] == '>') return HtmlToken.StartTag(name, attributes, selfClosing) to (i + 1)
            if (source[i] == '/' && i + 1 < source.length && source[i + 1] == '>') {
                selfClosing = true
                return HtmlToken.StartTag(name, attributes, true) to (i + 2)
            }

            val attrStart = i
            while (i < source.length && !source[i].isWhitespace() && source[i] !in charArrayOf('=', '/', '>')) i++
            if (i == attrStart) {
                i++
                continue
            }
            val attrName = source.substring(attrStart, i).lowercase()
            if (attrName.length > limits.maxNameLength) return null
            while (i < source.length && source[i].isWhitespace()) i++
            var value = ""
            if (i < source.length && source[i] == '=') {
                i++
                while (i < source.length && source[i].isWhitespace()) i++
                if (i < source.length && (source[i] == '"' || source[i] == '\'')) {
                    val quote = source[i++]
                    val valueStart = i
                    while (i < source.length && source[i] != quote) i++
                    value = source.substring(valueStart, i)
                    if (i < source.length) i++
                } else {
                    val valueStart = i
                    while (i < source.length && !source[i].isWhitespace() && source[i] != '>') i++
                    value = source.substring(valueStart, i).trimEnd('/')
                }
            }
            if (attributes.size < limits.maxAttributesPerTag && attrName !in attributes) {
                attributes[attrName] = decodeEntities(value)
            }
        }
        return null
    }

    private fun decodeEntities(value: String): String {
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            if (value[i] == '&') {
                val semi = value.indexOf(';', i + 1)
                if (semi in (i + 2)..(i + 12)) {
                    val entity = value.substring(i + 1, semi)
                    val decoded = decodeEntity(entity)
                    if (decoded != null) {
                        out.append(decoded)
                        i = semi + 1
                        continue
                    }
                }
            }
            out.append(value[i++])
        }
        return out.toString()
    }

    private fun decodeEntity(entity: String): String? {
        val named = when (entity) {
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            "nbsp" -> "\u00a0"
            else -> null
        }
        if (named != null) return named
        if (!entity.startsWith("#")) return null
        val hex = entity.startsWith("#x", true)
        val digits = entity.substring(if (hex) 2 else 1)
        val codePoint = digits.toIntOrNull(if (hex) 16 else 10) ?: return "\uFFFD"
        if (!Character.isValidCodePoint(codePoint) || codePoint in 0xD800..0xDFFF || codePoint == 0) return "\uFFFD"
        return String(Character.toChars(codePoint))
    }

    private fun isNameStart(char: Char): Boolean = char.isLetter()
    private fun isNameChar(char: Char): Boolean =
        char.isLetterOrDigit() || char in setOf('-', '_', ':', '.')

}
