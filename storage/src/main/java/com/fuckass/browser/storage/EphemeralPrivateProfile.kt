package com.fuckass.browser.storage

import java.io.Closeable

data class PrivateProfileSnapshot(
    val cookieCount: Int,
    val cacheEntryCount: Int,
    val historyEntryCount: Int,
    val formEntryCount: Int,
    val downloadEntryCount: Int,
    val closed: Boolean
)

data class PrivateProfileLimits(
    val maxCookies: Int = 256,
    val maxCacheEntries: Int = 128,
    val maxCacheBytes: Int = 8 * 1024 * 1024,
    val maxHistoryEntries: Int = 500,
    val maxFormEntries: Int = 256,
    val maxDownloadEntries: Int = 128,
    val maxEntryChars: Int = 8_192
) {
    init {
        require(maxCookies > 0 && maxCacheEntries > 0 && maxCacheBytes >= 0)
        require(maxHistoryEntries > 0 && maxFormEntries > 0 && maxDownloadEntries > 0)
        require(maxEntryChars in 1..65_536)
    }
}

/**
 * Session-only private profile state. This class never writes to disk; callers must not mirror
 * its contents into persistent stores, logs, notifications or crash reports.
 */
class EphemeralPrivateProfile(
    private val limits: PrivateProfileLimits = PrivateProfileLimits()
) : Closeable {
    private val cookies = linkedSetOf<String>()
    private val cacheEntries = linkedMapOf<String, ByteArray>()
    private var cacheBytes = 0
    private val history = mutableListOf<String>()
    private val formData = linkedMapOf<String, String>()
    private val downloads = mutableListOf<String>()
    private var closed = false

    @Synchronized fun putCookie(cookie: String) {
        checkActive()
        require(cookie.length <= limits.maxEntryChars)
        if (cookie !in cookies && cookies.size >= limits.maxCookies) cookies.remove(cookies.first())
        cookies += cookie
    }

    @Synchronized fun putCache(key: String, value: ByteArray) {
        checkActive()
        require(key.isNotBlank() && key.length <= limits.maxEntryChars)
        require(value.size <= limits.maxCacheBytes)
        cacheEntries.remove(key)?.let { previous -> cacheBytes -= previous.size; previous.fill(0) }
        while (cacheEntries.isNotEmpty() &&
            (cacheEntries.size >= limits.maxCacheEntries || cacheBytes + value.size > limits.maxCacheBytes)
        ) {
            val eldest = cacheEntries.entries.iterator().next()
            cacheBytes -= eldest.value.size
            eldest.value.fill(0)
            cacheEntries.remove(eldest.key)
        }
        cacheEntries[key] = value.copyOf()
        cacheBytes += value.size
    }

    @Synchronized fun addHistory(url: String) {
        checkActive()
        require(url.length <= limits.maxEntryChars)
        if (history.size >= limits.maxHistoryEntries) history.removeAt(0)
        history += url
    }

    @Synchronized fun putFormData(key: String, value: String) {
        checkActive()
        require(key.isNotBlank() && key.length <= limits.maxEntryChars && value.length <= limits.maxEntryChars)
        if (key !in formData && formData.size >= limits.maxFormEntries) formData.remove(formData.keys.first())
        formData[key] = value
    }

    @Synchronized fun addDownload(filename: String) {
        checkActive()
        require(filename.isNotBlank() && filename.length <= limits.maxEntryChars)
        if (downloads.size >= limits.maxDownloadEntries) downloads.removeAt(0)
        downloads += filename
    }

    @Synchronized fun snapshot(): PrivateProfileSnapshot = PrivateProfileSnapshot(
        cookies.size, cacheEntries.size, history.size, formData.size, downloads.size, closed
    )

    @Synchronized fun clear() {
        cookies.clear()
        cacheEntries.values.forEach { it.fill(0) }
        cacheEntries.clear()
        cacheBytes = 0
        history.clear()
        formData.clear()
        downloads.clear()
    }

    @Synchronized override fun close() {
        if (!closed) {
            clear()
            closed = true
        }
    }

    private fun checkActive() = check(!closed) { "Private profile is closed" }
}

/** Narrow interface for future durable stores; private data must never be passed here. */
interface BrowserDataStore {
    fun saveBookmark(url: String, title: String)
    fun removeBookmark(url: String)
    fun bookmarks(): List<Pair<String, String>>
    fun addHistory(url: String, visitedAtEpochMs: Long)
    fun history(limit: Int = 100): List<Pair<String, Long>>
}
