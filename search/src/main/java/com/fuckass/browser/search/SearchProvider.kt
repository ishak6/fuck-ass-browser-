package com.fuckass.browser.search

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SearchRequest(val query: String, val providerId: String) {
    init { require(query.isNotBlank()) { "Search query must not be blank" } }
}

interface SearchProvider {
    val id: String
    val displayName: String
    fun searchUri(query: String): URI
}

/** Opens ordinary provider result pages; it does not scrape or reverse-engineer private endpoints. */
class UrlTemplateSearchProvider(
    override val id: String,
    override val displayName: String,
    private val endpoint: String,
    private val queryParameter: String
) : SearchProvider {
    init {
        require(id.matches(Regex("[a-z0-9-]{2,32}")))
        require(endpoint.startsWith("https://"))
        require(queryParameter.matches(Regex("[a-zA-Z0-9_-]{1,32}")))
    }

    override fun searchUri(query: String): URI {
        require(query.isNotBlank()) { "Search query must not be blank" }
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        val separator = if (endpoint.contains('?')) '&' else '?'
        return URI.create("$endpoint$separator$queryParameter=$encoded")
    }
}

object BuiltInSearchProviders {
    val all: List<SearchProvider> = listOf(
        UrlTemplateSearchProvider("ddg", "DuckDuckGo", "https://duckduckgo.com/", "q"),
        UrlTemplateSearchProvider("google", "Google", "https://www.google.com/search", "q"),
        UrlTemplateSearchProvider("bing", "Bing", "https://www.bing.com/search", "q"),
        UrlTemplateSearchProvider("startpage", "Startpage", "https://www.startpage.com/sp/search", "query"),
        UrlTemplateSearchProvider("ecosia", "Ecosia", "https://www.ecosia.org/search", "q"),
        UrlTemplateSearchProvider("qwant", "Qwant", "https://www.qwant.com/", "q"),
        UrlTemplateSearchProvider("mojeek", "Mojeek", "https://www.mojeek.com/search", "q"),
        UrlTemplateSearchProvider("yandex", "Yandex", "https://yandex.com/search/", "text"),
        UrlTemplateSearchProvider("wikipedia", "Wikipedia", "https://en.wikipedia.org/w/index.php", "search")
    )

    fun find(id: String): SearchProvider? = all.firstOrNull { it.id == id }
}

data class MetaSearchSettings(val enabled: Boolean = false) {
    fun requireDisclosure(): Boolean = enabled
}

/** Minimal, explicit bang routing; unknown bangs remain part of the user's query. */
object SearchBangParser {
    private val shortcuts = mapOf(
        "!g" to "google",
        "!ddg" to "ddg",
        "!b" to "bing",
        "!w" to "wikipedia"
    )

    fun resolve(query: String, fallbackProviderId: String): SearchRequest {
        val trimmed = query.trim()
        val first = trimmed.substringBefore(' ', trimmed)
        val provider = shortcuts[first.lowercase()]
        return if (provider == null) SearchRequest(trimmed, fallbackProviderId)
        else {
            val remainder = trimmed.removePrefix(first).trim()
            SearchRequest(remainder.ifBlank { first }, provider)
        }
    }
}
