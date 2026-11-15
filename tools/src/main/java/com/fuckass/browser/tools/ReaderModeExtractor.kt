package com.fuckass.browser.tools

import com.fuckass.browser.engine.html.HtmlComment
import com.fuckass.browser.engine.html.HtmlDocument
import com.fuckass.browser.engine.html.HtmlElement
import com.fuckass.browser.engine.html.HtmlNode
import com.fuckass.browser.engine.html.HtmlText

data class ReaderArticle(val title: String, val text: String, val wordCount: Int)

/** Basic DOM-based reader extraction; not a full readability/boilerplate classifier. */
class ReaderModeExtractor {
    fun extract(document: HtmlDocument): ReaderArticle? {
        val title = document.getElementsByTagName("title").firstOrNull()?.let(::textContent)
            ?.ifBlank { null }
            ?: document.getElementsByTagName("h1").firstOrNull()?.let(::textContent).orEmpty()

        val preferred = document.getElementsByTagName("article").firstOrNull()
            ?: document.getElementsByTagName("main").firstOrNull()
            ?: document.getElementsByTagName("body").firstOrNull()
        val source = preferred ?: return null
        val body = textContent(source).replace(Regex("\\s+"), " ").trim()
        if (body.isBlank()) return null
        return ReaderArticle(title.trim(), body, body.split(' ').count { it.isNotBlank() })
    }

    private fun textContent(element: HtmlElement): String = buildString {
        fun appendNode(node: HtmlNode) {
            when (node) {
                is HtmlText -> append(node.data).append(' ')
                is HtmlComment -> Unit
                is HtmlElement -> if (node.tagName !in IGNORED_TAGS) node.children.forEach(::appendNode)
            }
        }
        appendNode(element)
    }

    companion object {
        private val IGNORED_TAGS = setOf("script", "style", "nav", "header", "footer", "aside", "noscript")
    }
}
