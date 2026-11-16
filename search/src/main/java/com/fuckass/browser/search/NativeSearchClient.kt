package com.fuckass.browser.search

import com.fuckass.browser.engine.net.BrowserHttpClient
import com.fuckass.browser.engine.net.BrowserHttpRequest
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class NativeSearchResult(
    val title: String,
    val snippet: String,
    val url: String
)

/**
 * Uses Wikimedia's public MediaWiki Action API. This is a real native result
 * source, not HTML scraping; requests pass through the browser's network policy.
 */
class NativeSearchClient {
    fun search(client: BrowserHttpClient, query: String): List<NativeSearchResult> {
        val normalized = query.trim()
        require(normalized.isNotEmpty()) { "Search query must not be blank" }
        require(normalized.length <= 256) { "Search query is too long" }
        val encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8.name())
        val endpoint = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1&srlimit=15"
        val response = client.execute(
            BrowserHttpRequest(
                url = endpoint,
                resourceType = "document",
                topLevelUrl = endpoint,
                useCache = false
            )
        )
        if (response.statusCode !in 200..299) {
            throw IllegalStateException("Search service returned HTTP ${response.statusCode}")
        }
        require(response.body.size <= MAX_RESPONSE_BYTES) { "Search response exceeded the size limit" }
        val root = JSONObject(response.body.toString(Charsets.UTF_8))
        val entries = root.optJSONObject("query")?.optJSONArray("search")
            ?: throw IllegalStateException("Search service returned an invalid response")
        return buildList {
            for (index in 0 until minOf(entries.length(), MAX_RESULTS)) {
                val item = entries.optJSONObject(index) ?: continue
                val pageId = item.optLong("pageid", -1L)
                val title = item.optString("title").trim()
                if (pageId <= 0L || title.isBlank()) continue
                add(
                    NativeSearchResult(
                        title = title,
                        snippet = cleanSnippet(item.optString("snippet")),
                        url = "https://en.wikipedia.org/?curid=$pageId"
                    )
                )
            }
        }
    }

    companion object {
        private const val MAX_RESPONSE_BYTES = 1_048_576
        private const val MAX_RESULTS = 15

        internal fun cleanSnippet(value: String): String =
            value.replace(Regex("<[^>]{0,256}>"), " ")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(500)
    }
}
