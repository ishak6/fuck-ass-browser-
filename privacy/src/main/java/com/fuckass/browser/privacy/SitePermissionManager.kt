package com.fuckass.browser.privacy

import java.net.IDN
import java.net.URI
import java.util.Locale

enum class SitePermission {
    CAMERA,
    MICROPHONE,
    LOCATION,
    NOTIFICATIONS,
    JAVASCRIPT,
    POPUPS,
    AUTOMATIC_DOWNLOADS
}

enum class SitePermissionDecision { ASK, ALLOW, BLOCK }

data class SiteOrigin(
    val scheme: String,
    val host: String,
    val port: Int
) {
    init {
        require(scheme == "https") { "Only secure HTTPS origins are supported" }
        require(host.isNotBlank() && port in 1..65535) { "Invalid site origin" }
    }

    override fun toString(): String = "$scheme://$host:$port"

    companion object {
        fun fromUrl(url: String): SiteOrigin? {
            val uri = runCatching { URI(url) }.getOrNull() ?: return null
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null) return null
            val authority = uri.rawAuthority ?: return null
            val rawHost: String
            val explicitPort: String?
            if (authority.startsWith("[")) {
                val closingBracket = authority.indexOf(']')
                if (closingBracket <= 1) return null
                rawHost = authority.substring(0, closingBracket + 1).lowercase(Locale.ROOT)
                val suffix = authority.substring(closingBracket + 1)
                if (suffix.isNotEmpty() && !suffix.startsWith(":")) return null
                explicitPort = suffix.removePrefix(":").ifEmpty { null }
            } else {
                val colon = authority.lastIndexOf(':')
                if (colon >= 0) {
                    if (authority.indexOf(':') != colon) return null
                    rawHost = authority.substring(0, colon)
                    explicitPort = authority.substring(colon + 1).ifEmpty { return null }
                } else {
                    rawHost = authority
                    explicitPort = null
                }
            }
            if (rawHost.isBlank()) return null
            val asciiHost = if (rawHost.startsWith("[")) rawHost else runCatching {
                IDN.toASCII(rawHost.trimEnd('.'), IDN.USE_STD3_ASCII_RULES)
                    .lowercase(Locale.ROOT)
            }.getOrNull() ?: return null
            if (asciiHost.isBlank() || asciiHost.length > 253) return null
            val port = if (explicitPort == null) 443 else explicitPort.toIntOrNull()
                ?.takeIf { it in 1..65535 } ?: return null
            return SiteOrigin("https", asciiHost, port)
        }
    }
}

interface SitePermissionStore {
    fun get(origin: SiteOrigin, permission: SitePermission): SitePermissionDecision?
    fun put(origin: SiteOrigin, permission: SitePermission, decision: SitePermissionDecision)
    fun remove(origin: SiteOrigin)
    fun clear()
}

/** Bounded process-local store; persistence is deliberately supplied by a later storage adapter. */
class InMemorySitePermissionStore(
    private val maxOrigins: Int = DEFAULT_MAX_ORIGINS
) : SitePermissionStore {
    private val entries = linkedMapOf<SiteOrigin, MutableMap<SitePermission, SitePermissionDecision>>()

    init {
        require(maxOrigins in 1..MAX_ALLOWED_ORIGINS) { "Invalid site permission capacity" }
    }

    @Synchronized
    override fun get(origin: SiteOrigin, permission: SitePermission): SitePermissionDecision? =
        entries[origin]?.get(permission)

    @Synchronized
    override fun put(origin: SiteOrigin, permission: SitePermission, decision: SitePermissionDecision) {
        val site = entries[origin]
        if (site == null) {
            while (entries.size >= maxOrigins) entries.remove(entries.keys.first())
            entries[origin] = mutableMapOf(permission to decision)
        } else {
            site[permission] = decision
        }
    }

    @Synchronized
    override fun remove(origin: SiteOrigin) {
        entries.remove(origin)
    }

    @Synchronized
    override fun clear() {
        entries.clear()
    }

    companion object {
        private const val DEFAULT_MAX_ORIGINS = 1_000
        private const val MAX_ALLOWED_ORIGINS = 10_000
    }
}

/**
 * Per-origin permission policy. Unknown origins and malformed/non-HTTPS URLs fail closed.
 * Android runtime permission grants must still be checked separately before allowing access.
 */
class SitePermissionManager(
    private val store: SitePermissionStore = InMemorySitePermissionStore()
) {
    fun decision(url: String, permission: SitePermission): SitePermissionDecision {
        val origin = SiteOrigin.fromUrl(url) ?: return SitePermissionDecision.BLOCK
        return store.get(origin, permission) ?: SitePermissionDecision.ASK
    }

    fun setDecision(url: String, permission: SitePermission, decision: SitePermissionDecision) {
        val origin = requireNotNull(SiteOrigin.fromUrl(url)) { "A valid HTTPS origin is required" }
        store.put(origin, permission, decision)
    }

    fun clearSite(url: String) {
        val origin = requireNotNull(SiteOrigin.fromUrl(url)) { "A valid HTTPS origin is required" }
        store.remove(origin)
    }

    fun clearAll() = store.clear()
}
