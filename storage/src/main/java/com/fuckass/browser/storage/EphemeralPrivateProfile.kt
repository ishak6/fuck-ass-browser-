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

/**
 * Session-only private profile state. This class never writes to disk; callers must not mirror
 * its contents into persistent stores, logs, notifications or crash reports.
 */
class EphemeralPrivateProfile : Closeable {
    private val cookies = linkedSetOf<String>()
    private val cacheEntries = linkedMapOf<String, ByteArray>()
    private val history = mutableListOf<String>()
    private val formData = linkedMapOf<String, String>()
    private val downloads = mutableListOf<String>()
    private var closed = false

    @Synchronized fun putCookie(cookie: String) { checkActive(); cookies += cookie }
    @Synchronized fun putCache(key: String, value: ByteArray) { checkActive(); cacheEntries[key] = value.copyOf() }
    @Synchronized fun addHistory(url: String) { checkActive(); history += url }
    @Synchronized fun putFormData(key: String, value: String) { checkActive(); formData[key] = value }
    @Synchronized fun addDownload(filename: String) { checkActive(); downloads += filename }

    @Synchronized fun snapshot(): PrivateProfileSnapshot = PrivateProfileSnapshot(
        cookies.size, cacheEntries.size, history.size, formData.size, downloads.size, closed
    )

    @Synchronized fun clear() {
        cookies.clear()
        cacheEntries.values.forEach { it.fill(0) }
        cacheEntries.clear()
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
