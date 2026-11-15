package com.fuckass.browser

data class BrowserHistorySnapshot(
    val entries: List<String>,
    val index: Int
) {
    val canGoBack: Boolean get() = index > 0
    val canGoForward: Boolean get() = index in 0 until entries.lastIndex
}

/** Bounded in-memory navigation stack; durable history is stored separately by BrowserDataStore. */
class BrowserNavigationHistory(private val maxEntries: Int = 100) {
    private val entries = mutableListOf<String>()
    private var index = -1

    init { require(maxEntries > 0) }

    @Synchronized fun record(url: String) {
        require(url.isNotBlank())
        while (entries.lastIndex > index) entries.removeAt(entries.lastIndex)
        if (entries.getOrNull(index) != url) {
            entries += url
            if (entries.size > maxEntries) entries.removeAt(0)
            index = entries.lastIndex
        }
    }

    @Synchronized fun previousUrl(): String? =
        if (index > 0) entries[index - 1] else null

    @Synchronized fun nextUrl(): String? =
        if (index >= 0 && index < entries.lastIndex) entries[index + 1] else null

    @Synchronized fun goBack(): String? {
        if (index <= 0) return null
        index--
        return entries[index]
    }

    @Synchronized fun goForward(): String? {
        if (index < 0 || index >= entries.lastIndex) return null
        index++
        return entries[index]
    }

    @Synchronized fun reset() {
        entries.clear()
        index = -1
    }

    @Synchronized fun snapshot() = BrowserHistorySnapshot(entries.toList(), index)
}
