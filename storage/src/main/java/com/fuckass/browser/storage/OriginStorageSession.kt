package com.fuckass.browser.storage

import com.fuckass.browser.privacy.SiteOrigin

/**
 * Origin-bound host adapter for a single top-level browsing context.
 *
 * This is the native storage boundary intended for a future DOM/JS bridge. The current
 * interpreter has no host-object or DOM support, so this class does not claim web API integration.
 */
class OriginStorageSession(
    private val manager: OriginStorageManager,
    url: String
) : AutoCloseable {
    private val originUrl = requireNotNull(SiteOrigin.fromUrl(url)) {
        "A valid HTTPS origin is required"
    }.toString()

    private var closed = false

    val localStorage = OriginStorageArea(OriginStorageType.LOCAL)
    val sessionStorage = OriginStorageArea(OriginStorageType.SESSION)

    inner class OriginStorageArea internal constructor(private val type: OriginStorageType) {
        fun getItem(key: String): String? {
            checkOpen()
            return manager.getItem(originUrl, type, key)
        }

        fun setItem(key: String, value: String) {
            checkOpen()
            manager.setItem(originUrl, type, key, value)
        }

        fun removeItem(key: String) {
            checkOpen()
            manager.removeItem(originUrl, type, key)
        }

        fun clear() {
            checkOpen()
            manager.keys(originUrl, type).forEach { manager.removeItem(originUrl, type, it) }
        }

        fun key(index: Int): String? {
            checkOpen()
            require(index >= 0) { "Storage index must not be negative" }
            return manager.keys(originUrl, type).getOrNull(index)
        }

        fun length(): Int {
            checkOpen()
            return manager.keys(originUrl, type).size
        }
    }

    private fun checkOpen() {
        check(!closed) { "Storage session is closed" }
    }

    override fun close() {
        if (closed) return
        closed = true
        manager.clearSessionStorage(originUrl)
    }
}
