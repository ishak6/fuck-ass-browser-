package com.fuckass.browser.storage

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.IDN
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.util.Locale

/** Durable user-managed host list for automatic private-session routing. */
class FilePrivateSiteRulesStore(private val directory: File) {
    private val rulesFile = File(directory, FILE_NAME)
    private val tempFile = File(directory, "$FILE_NAME.tmp")
    private val rules = linkedSetOf<String>()

    init {
        require(directory.exists() || directory.mkdirs()) { "Could not create private-site rules directory" }
        require(directory.isDirectory) { "Private-site rules path is not a directory" }
        load()
    }

    @Synchronized fun hosts(): Set<String> = rules.toSet()

    @Synchronized fun add(host: String): Set<String> {
        val normalized = normalizeHost(host) ?: throw IllegalArgumentException("Enter a hostname, not a URL")
        if (normalized !in rules && rules.size >= MAX_RULES) {
            throw IllegalStateException("Private-site rule limit reached")
        }
        if (rules.add(normalized)) persist()
        return hosts()
    }

    @Synchronized fun remove(host: String): Set<String> {
        val normalized = normalizeHost(host) ?: return hosts()
        if (rules.remove(normalized)) persist()
        return hosts()
    }

    private fun load() {
        if (!rulesFile.exists()) return
        if (rulesFile.length() > MAX_FILE_BYTES) throw IOException("Private-site rules file is too large")
        rulesFile.readLines(StandardCharsets.UTF_8).forEach { line ->
            if (line.isNotBlank()) {
                val host = normalizeHost(line) ?: throw IOException("Invalid hostname in private-site rules")
                rules += host
                if (rules.size > MAX_RULES) throw IOException("Too many private-site rules")
            }
        }
    }

    private fun persist() {
        val bytes = rules.sorted().joinToString("\n").toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_FILE_BYTES) throw IOException("Private-site rules exceed storage limit")
        try {
            FileOutputStream(tempFile).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            try {
                Files.move(
                    tempFile.toPath(), rulesFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tempFile.toPath(), rulesFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (failure: Exception) {
            tempFile.delete()
            throw IOException("Could not save private-site rules", failure)
        }
    }

    private fun normalizeHost(value: String): String? {
        val input = value.trim().trimEnd('.')
        if (input.isBlank() || input.length > 253 || input.contains('/') || input.contains('@') ||
            input.contains(':') || input.contains('*') || input.contains(' ')
        ) return null
        val host = runCatching { IDN.toASCII(input, IDN.USE_STD3_ASCII_RULES) }
            .getOrNull()?.lowercase(Locale.ROOT) ?: return null
        val labels = host.split('.')
        if (labels.size < 2 || labels.any { label ->
                label.isEmpty() || label.length > 63 || label.first() == '-' || label.last() == '-' ||
                    label.any { !(it in 'a'..'z' || it in '0'..'9' || it == '-') }
            }) return null
        return host
    }

    companion object {
        private const val FILE_NAME = "private-sites.txt"
        private const val MAX_RULES = 500
        private const val MAX_FILE_BYTES = 128 * 1024
    }
}
