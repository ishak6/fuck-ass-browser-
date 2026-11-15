package com.fuckass.browser.storage

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

/**
 * Small durable bookmark/history store. Pass the app's private files directory; this class
 * never stores private-session data and never writes to shared/external storage.
 */
class FileBrowserDataStore(private val directory: File) : BrowserDataStore {
    private val dataFile = File(directory, FILE_NAME)
    private val tempFile = File(directory, "$FILE_NAME.tmp")
    private val savedBookmarks = linkedMapOf<String, String>()
    private val savedHistory = mutableListOf<Pair<String, Long>>()

    init {
        require(directory.exists() || directory.mkdirs()) { "Could not create browser data directory" }
        require(directory.isDirectory) { "Browser data path is not a directory" }
        load()
    }

    @Synchronized
    override fun saveBookmark(url: String, title: String) {
        validateText(url, "url")
        validateText(title, "title")
        savedBookmarks[url] = title
        while (savedBookmarks.size > MAX_BOOKMARKS) savedBookmarks.remove(savedBookmarks.keys.first())
        persist()
    }

    @Synchronized
    override fun removeBookmark(url: String) {
        if (savedBookmarks.remove(url) != null) persist()
    }

    @Synchronized
    override fun bookmarks(): List<Pair<String, String>> =
        savedBookmarks.map { it.key to it.value }

    @Synchronized
    override fun addHistory(url: String, visitedAtEpochMs: Long) {
        validateText(url, "url")
        require(visitedAtEpochMs >= 0) { "Visit timestamp must not be negative" }
        savedHistory.removeAll { it.first == url }
        savedHistory += url to visitedAtEpochMs
        while (savedHistory.size > MAX_HISTORY) savedHistory.removeAt(0)
        persist()
    }

    @Synchronized
    override fun history(limit: Int): List<Pair<String, Long>> {
        require(limit >= 0) { "History limit must not be negative" }
        return savedHistory.asReversed().take(limit)
    }

    private fun load() {
        if (!dataFile.exists()) return
        if (dataFile.length() !in HEADER_BYTES.toLong()..MAX_FILE_BYTES.toLong()) {
            throw IOException("Browser data file size is invalid")
        }
        DataInputStream(BufferedInputStream(FileInputStream(dataFile))).use { input ->
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw IOException("Unsupported browser data format")
            }
            val bookmarkCount = input.readInt()
            if (bookmarkCount !in 0..MAX_BOOKMARKS) throw IOException("Invalid bookmark count")
            repeat(bookmarkCount) {
                val url = readText(input)
                val title = readText(input)
                savedBookmarks[url] = title
            }
            val historyCount = input.readInt()
            if (historyCount !in 0..MAX_HISTORY) throw IOException("Invalid history count")
            repeat(historyCount) {
                val url = readText(input)
                val timestamp = input.readLong()
                if (timestamp < 0) throw IOException("Invalid history timestamp")
                savedHistory += url to timestamp
            }
            if (input.read() != -1) throw IOException("Unexpected trailing browser data")
        }
    }

    private fun persist() {
        try {
            FileOutputStream(tempFile).use { fileOutput ->
                DataOutputStream(BufferedOutputStream(fileOutput)).use { output ->
                    output.writeInt(MAGIC)
                    output.writeInt(VERSION)
                    output.writeInt(savedBookmarks.size)
                    savedBookmarks.forEach { (url, title) ->
                        writeText(output, url)
                        writeText(output, title)
                    }
                    output.writeInt(savedHistory.size)
                    savedHistory.forEach { (url, timestamp) ->
                        writeText(output, url)
                        output.writeLong(timestamp)
                    }
                    output.flush()
                    fileOutput.fd.sync()
                }
            }
            try {
                Files.move(
                    tempFile.toPath(), dataFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                if (dataFile.exists() && !dataFile.delete()) throw IOException("Could not replace browser data")
                if (!tempFile.renameTo(dataFile)) throw IOException("Could not commit browser data")
            }
        } catch (failure: Exception) {
            tempFile.delete()
            throw IOException("Could not persist browser data", failure)
        }
    }

    private fun writeText(output: DataOutputStream, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_TEXT_BYTES) throw IOException("Browser data field is too large")
        output.writeInt(bytes.size)
        output.write(bytes)
    }

    private fun readText(input: DataInputStream): String {
        val size = input.readInt()
        if (size !in 1..MAX_TEXT_BYTES) throw IOException("Invalid browser data field length")
        val bytes = ByteArray(size)
        input.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun validateText(value: String, field: String) {
        require(value.isNotBlank() && value.toByteArray(StandardCharsets.UTF_8).size <= MAX_TEXT_BYTES) {
            "Invalid browser $field"
        }
    }

    companion object {
        private const val FILE_NAME = "browser-data.bin"
        private const val MAGIC = 0x43425257
        private const val VERSION = 1
        private const val MAX_TEXT_BYTES = 16 * 1024
        private const val MAX_BOOKMARKS = 2_000
        private const val MAX_HISTORY = 10_000
        private const val MAX_FILE_BYTES = 8 * 1024 * 1024
        private const val HEADER_BYTES = 12
    }
}
