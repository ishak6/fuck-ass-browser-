package com.fuckass.browser.engine.html

sealed class HtmlNode {
    var parent: HtmlElement? = null
        internal set
}

class HtmlText(val data: String) : HtmlNode()
class HtmlComment(val data: String) : HtmlNode()

class HtmlElement(
    val tagName: String,
    val attributes: Map<String, String> = emptyMap()
) : HtmlNode() {
    private val mutableChildren = mutableListOf<HtmlNode>()
    val children: List<HtmlNode> get() = mutableChildren.toList()

    fun appendChild(node: HtmlNode) {
        require(node !== this) { "An element cannot contain itself" }
        var ancestor: HtmlElement? = this
        while (ancestor != null) {
            require(ancestor !== node) { "Appending this node would create a DOM cycle" }
            ancestor = ancestor.parent
        }
        node.parent?.removeChild(node)
        node.parent = this
        mutableChildren += node
    }

    fun removeChild(node: HtmlNode): Boolean {
        val removed = mutableChildren.remove(node)
        if (removed) node.parent = null
        return removed
    }

    fun getAttribute(name: String): String? = attributes[name.lowercase()]
    fun hasAttribute(name: String): Boolean = attributes.containsKey(name.lowercase())
}

class HtmlDocument {
    private val mutableChildren = mutableListOf<HtmlNode>()
    val children: List<HtmlNode> get() = mutableChildren.toList()

    internal fun appendChild(node: HtmlNode) {
        node.parent?.removeChild(node)
        node.parent = null
        mutableChildren += node
    }

    fun getElementById(id: String): HtmlElement? {
        fun visit(nodes: List<HtmlNode>): HtmlElement? {
            for (node in nodes) {
                if (node is HtmlElement) {
                    if (node.getAttribute("id") == id) return node
                    visit(node.children)?.let { return it }
                }
            }
            return null
        }
        return visit(mutableChildren)
    }

    fun getElementsByTagName(tagName: String): List<HtmlElement> {
        val target = tagName.lowercase()
        val result = mutableListOf<HtmlElement>()
        fun visit(nodes: List<HtmlNode>) {
            nodes.forEach { node ->
                if (node is HtmlElement) {
                    if (target == "*" || node.tagName == target) result += node
                    visit(node.children)
                }
            }
        }
        visit(mutableChildren)
        return result
    }
}

data class HtmlTreeLimits(
    val maxNodes: Int = 100_000,
    val maxDepth: Int = 256
) {
    init {
        require(maxNodes > 0)
        require(maxDepth in 1..2048)
    }
}

/**
 * Small, bounded tree builder for the tokenizer subset. It is not a full WHATWG tree
 * construction algorithm; unsupported insertion modes and foster parenting are not claimed.
 */
class HtmlTreeBuilder(private val limits: HtmlTreeLimits = HtmlTreeLimits()) {
    fun build(tokens: List<HtmlToken>): HtmlDocument {
        require(tokens.size <= limits.maxNodes) { "HTML token count exceeds tree builder limit" }
        val document = HtmlDocument()
        val stack = mutableListOf<HtmlElement>()
        var nodeCount = 0

        fun append(node: HtmlNode) {
            if (++nodeCount > limits.maxNodes) throw IllegalArgumentException("HTML node limit exceeded")
            val parent = stack.lastOrNull()
            if (parent == null) document.appendChild(node) else parent.appendChild(node)
        }

        for (token in tokens) {
            when (token) {
                is HtmlToken.Text -> if (token.value.isNotEmpty()) append(HtmlText(token.value))
                is HtmlToken.Comment -> append(HtmlComment(token.value))
                is HtmlToken.Doctype -> Unit
                is HtmlToken.StartTag -> {
                    closeImplied(token.name, stack)
                    val element = HtmlElement(token.name, token.attributes)
                    append(element)
                    if (!token.selfClosing && token.name !in VOID_ELEMENTS) {
                        if (stack.size >= limits.maxDepth) {
                            throw IllegalArgumentException("HTML nesting depth exceeded")
                        }
                        stack += element
                    }
                }
                is HtmlToken.EndTag -> {
                    val matching = stack.indexOfLast { it.tagName == token.name }
                    if (matching >= 0) {
                        while (stack.size > matching) stack.removeAt(stack.lastIndex)
                    }
                }
            }
        }
        return document
    }

    private fun closeImplied(incoming: String, stack: MutableList<HtmlElement>) {
        val current = stack.lastOrNull()?.tagName
        val shouldClose = when {
            current == "p" && incoming in BLOCK_START_TAGS -> true
            current == "li" && incoming == "li" -> true
            current in setOf("dt", "dd") && incoming in setOf("dt", "dd") -> true
            current == "option" && incoming == "option" -> true
            current == "tr" && incoming == "tr" -> true
            current in setOf("td", "th") && incoming in setOf("td", "th") -> true
            else -> false
        }
        if (shouldClose) stack.removeAt(stack.lastIndex)
    }

    companion object {
        private val VOID_ELEMENTS = setOf(
            "area", "base", "br", "col", "embed", "hr", "img", "input", "link",
            "meta", "param", "source", "track", "wbr"
        )
        private val BLOCK_START_TAGS = setOf(
            "address", "article", "aside", "blockquote", "div", "dl", "fieldset",
            "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header",
            "hr", "main", "nav", "ol", "p", "pre", "section", "table", "ul"
        )
    }
}
