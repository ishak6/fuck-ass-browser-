package com.fuckass.browser.search

import com.fuckass.browser.engine.net.BrowserHttpClient
import com.fuckass.browser.engine.net.BrowserHttpRequest
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ArchiveVideoResult(
    val identifier: String,
    val title: String,
    val description: String
)

/** Searches Internet Archive's public metadata API for openly listed movie items. */
class InternetArchiveVideoClient {
    fun search(client: BrowserHttpClient, query: String): List<ArchiveVideoResult> {
        val normalized = query.trim()
        require(normalized.isNotEmpty()) { "Video search query must not be blank" }
        require(normalized.length <= 160) { "Video search query is too long" }
        val phrase = escapeLucenePhrase(normalized)
        val expression = "mediatype:movies AND (title:\"$phrase\" OR description:\"$phrase\")"
        val endpoint = "https://archive.org/advancedsearch.php?q=" +
            encodeQuery(expression) +
            "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=description&rows=12&page=1&output=json"
        val response = client.execute(
            BrowserHttpRequest(url = endpoint, resourceType = "document", topLevelUrl = endpoint, useCache = false)
        )
        if (response.statusCode !in 200..299) {
            throw IllegalStateException("Video search service returned HTTP ${response.statusCode}")
        }
        require(response.body.size <= MAX_SEARCH_BYTES) { "Video search response exceeded the size limit" }
        val docs = JSONObject(response.body.toString(Charsets.UTF_8))
            .optJSONObject("response")?.optJSONArray("docs")
            ?: throw IllegalStateException("Video search service returned an invalid response")
        return buildList {
            for (index in 0 until minOf(docs.length(), MAX_RESULTS)) {
                val item = docs.optJSONObject(index) ?: continue
                val id = item.optString("identifier").trim()
                if (!id.matches(Regex("[A-Za-z0-9._-]{1,128}"))) continue
                val title = item.optString("title").trim().ifBlank { id }
                add(ArchiveVideoResult(id, title, cleanText(item.optString("description"))))
            }
        }
    }

    fun resolvePlayableMp4(client: BrowserHttpClient, identifier: String): String {
        require(identifier.matches(Regex("[A-Za-z0-9._-]{1,128}"))) { "Invalid archive identifier" }
        val endpoint = "https://archive.org/metadata/$identifier"
        val response = client.execute(
            BrowserHttpRequest(url = endpoint, resourceType = "document", topLevelUrl = endpoint, useCache = false)
        )
        if (response.statusCode !in 200..299) {
            throw IllegalStateException("Video metadata returned HTTP ${response.statusCode}")
        }
        require(response.body.size <= MAX_METADATA_BYTES) { "Video metadata exceeded the size limit" }
        val files = JSONObject(response.body.toString(Charsets.UTF_8)).optJSONArray("files")
            ?: throw IllegalStateException("No playable media metadata was found")
        for (index in 0 until files.length()) {
            val file = files.optJSONObject(index) ?: continue
            val name = file.optString("name")
            val format = file.optString("format")
            val isPrivate = file.optString("private").equals("true", true)
            if (isPrivate || !name.substringAfterLast('.', "").equals("mp4", true)) continue
            if (format.isNotBlank() && !format.contains("mpeg4", true) && !format.contains("mp4", true)) continue
            val encodedPath = name.split('/').joinToString("/") { encodePathSegment(it) }
            return "https://archive.org/download/$identifier/$encodedPath"
        }
        throw IllegalStateException("This archive item has no publicly listed MP4 file")
    }

    companion object {
        private const val MAX_RESULTS = 12
        private const val MAX_SEARCH_BYTES = 1_048_576
        private const val MAX_METADATA_BYTES = 4_194_304

        internal fun escapeLucenePhrase(value: String): String =
            value.replace("\\", " ").replace("\"", " ").replace(Regex("[\\r\\n\\t]"), " ")
                .replace(Regex("\\s+"), " ").trim()

        internal fun cleanText(value: String): String =
            value.replace(Regex("<[^>]{0,256}>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(500)

        private fun encodeQuery(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())

        private fun encodePathSegment(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}
