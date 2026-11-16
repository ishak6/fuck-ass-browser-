package com.fuckass.browser.storage

import com.fuckass.browser.privacy.SiteOrigin
import java.nio.charset.StandardCharsets

enum class OriginStorageType { LOCAL, SESSION }

/** Thrown when a write would exceed an origin, entry-count or aggregate storage budget. */
class StorageQuotaExceededException(message: String) : IllegalStateException(message)

/**
 * Bounded same-origin key/value storage primitive.
 *
 * Keys are partitioned by normalized HTTPS origin (scheme, IDN-normalized host and effective
 * port), not by URL path. This is a memory-only engine primitive; it is not yet wired to DOM
 * localStorage/sessionStorage or durable persistence.
 */
class OriginStorageManager(
    private val maxOrigins: Int = DEFAULT_MAX_ORIGINS,
    private val maxBytesPerOrigin: Long = DEFAULT_BYTES_PER_ORIGIN,
    private val maxBytesTotal: Long = DEFAULT_BYTES_TOTAL,
    private val maxEntriesPerOrigin: Int = DEFAULT_ENTRIES_PER_ORIGIN,
    private val maxKeyBytes: Int = DEFAULT_KEY_BYTES,
    private val maxValueBytes: Int = DEFAULT_VALUE_BYTES
) {
    private data class Partition(
        val local: LinkedHashMap<String, String> = linkedMapOf(),
        val session: LinkedHashMap<String, String> = linkedMapOf(),
        var localBytes: Long = 0,
        var sessionBytes: Long = 0
    ) {
        val totalBytes: Long get() = localBytes + sessionBytes
    }

    private val partitions = linkedMapOf<SiteOrigin, Partition>()
    private var totalBytes = 0L

    init {
        require(maxOrigins in 1..MAX_ORIGINS)
        require(maxBytesPerOrigin in 1..MAX_BYTES_TOTAL)
        require(maxBytesTotal in maxBytesPerOrigin..MAX_BYTES_TOTAL)
        require(maxEntriesPerOrigin in 1..MAX_ENTRIES)
        require(maxKeyBytes in 1..MAX_KEY_BYTES)
        require(maxValueBytes in 1..MAX_VALUE_BYTES)
    }

    @Synchronized
    fun getItem(url: String, type: OriginStorageType, key: String): String? {
        validateKey(key)
        val origin = parseOrigin(url) ?: return null
        val partition = partitions[origin] ?: return null
        return bucket(partition, type)[key]
    }

    @Synchronized
    fun setItem(url: String, type: OriginStorageType, key: String, value: String) {
        validateKey(key)
        val valueBytes = value.toByteArray(StandardCharsets.UTF_8).size
        require(valueBytes <= maxValueBytes) { "Storage value exceeds the configured limit" }
        val origin = requireNotNull(parseOrigin(url)) { "A valid HTTPS origin is required" }
        var partition = partitions[origin]
        if (partition == null) {
            if (partitions.size >= maxOrigins) {
                throw StorageQuotaExceededException("Origin partition limit reached")
            }
            partition = Partition()
            partitions[origin] = partition
        }

        val target = bucket(partition, type)
        val oldValue = target[key]
        val oldBytes = if (oldValue == null) 0L else entryBytes(key, oldValue)
        val newBytes = entryBytes(key, value)
        val delta = newBytes - oldBytes
        if (oldValue == null && partition.local.size + partition.session.size >= maxEntriesPerOrigin) {
            throw StorageQuotaExceededException("Origin entry limit reached")
        }
        if (partition.totalBytes + delta > maxBytesPerOrigin) {
            throw StorageQuotaExceededException("Origin storage quota exceeded")
        }
        if (totalBytes + delta > maxBytesTotal) {
            throw StorageQuotaExceededException("Total storage quota exceeded")
        }

        target[key] = value
        if (type == OriginStorageType.LOCAL) partition.localBytes += delta
        else partition.sessionBytes += delta
        totalBytes += delta
    }

    @Synchronized
    fun removeItem(url: String, type: OriginStorageType, key: String) {
        validateKey(key)
        val origin = parseOrigin(url) ?: return
        val partition = partitions[origin] ?: return
        val removed = bucket(partition, type).remove(key) ?: return
        val bytes = entryBytes(key, removed)
        if (type == OriginStorageType.LOCAL) partition.localBytes -= bytes
        else partition.sessionBytes -= bytes
        totalBytes -= bytes
        removeEmptyPartition(origin, partition)
    }

    @Synchronized
    fun clearOrigin(url: String) {
        val origin = parseOrigin(url) ?: return
        val removed = partitions.remove(origin) ?: return
        totalBytes -= removed.totalBytes
    }

    @Synchronized
    fun clearSessionStorage(url: String) {
        val origin = parseOrigin(url) ?: return
        val partition = partitions[origin] ?: return
        totalBytes -= partition.sessionBytes
        partition.session.clear()
        partition.sessionBytes = 0
        removeEmptyPartition(origin, partition)
    }

    @Synchronized
    fun clearAll() {
        partitions.clear()
        totalBytes = 0
    }

    @Synchronized
    fun keys(url: String, type: OriginStorageType): List<String> {
        val origin = parseOrigin(url) ?: return emptyList()
        return partitions[origin]?.let { bucket(it, type).keys.toList() } ?: emptyList()
    }

    @Synchronized
    fun usageBytes(): Long = totalBytes

    @Synchronized
    fun originCount(): Int = partitions.size

    private fun parseOrigin(url: String): SiteOrigin? = SiteOrigin.fromUrl(url)

    private fun bucket(partition: Partition, type: OriginStorageType): LinkedHashMap<String, String> =
        if (type == OriginStorageType.LOCAL) partition.local else partition.session

    private fun entryBytes(key: String, value: String): Long =
        key.toByteArray(StandardCharsets.UTF_8).size.toLong() +
            value.toByteArray(StandardCharsets.UTF_8).size.toLong()

    private fun validateKey(key: String) {
        require(key.toByteArray(StandardCharsets.UTF_8).size <= maxKeyBytes) {
            "Storage key exceeds the configured limit"
        }
    }

    private fun removeEmptyPartition(origin: SiteOrigin, partition: Partition) {
        if (partition.local.isEmpty() && partition.session.isEmpty()) partitions.remove(origin)
    }

    companion object {
        private const val DEFAULT_MAX_ORIGINS = 1_000
        private const val MAX_ORIGINS = 10_000
        private const val DEFAULT_BYTES_PER_ORIGIN = 5L * 1024 * 1024
        private const val DEFAULT_BYTES_TOTAL = 20L * 1024 * 1024
        private const val MAX_BYTES_TOTAL = 64L * 1024 * 1024
        private const val DEFAULT_ENTRIES_PER_ORIGIN = 5_000
        private const val MAX_ENTRIES = 20_000
        private const val DEFAULT_KEY_BYTES = 1 * 1024
        private const val MAX_KEY_BYTES = 4 * 1024
        private const val DEFAULT_VALUE_BYTES = 256 * 1024
        private const val MAX_VALUE_BYTES = 1024 * 1024
    }
}
