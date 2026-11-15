package com.fuckass.browser.adblock

import java.net.URI
import java.util.Locale

/**
 * Deliberately bounded Adblock Plus subset.
 *
 * Supported: ||host^, ||host/path, @@ exceptions, resource-type options,
 * third-party/~third-party, and domain= include/exclude options.
 * Unsupported syntax is rejected rather than silently misinterpreted.
 */
class FilterList private constructor(private val rules: List<NetworkRule>) {
    val size: Int get() = rules.size

    fun shouldBlock(url: String, resourceType: String, topLevelUrl: String?): Boolean {
        val target = runCatching { URI(url) }.getOrNull() ?: return false
        val host = target.host?.lowercase(Locale.ROOT) ?: return false
        val topHost = topLevelUrl?.let { runCatching { URI(it).host?.lowercase(Locale.ROOT) }.getOrNull() }
        var blocked = false
        for (rule in rules) {
            if (!rule.matches(host, target.rawPath.orEmpty(), resourceType, topHost)) continue
            if (rule.exception) return false
            blocked = true
        }
        return blocked
    }

    companion object {
        fun parse(text: String): FilterList {
            val parsed = mutableListOf<NetworkRule>()
            text.lineSequence().take(MAX_LINES).forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[") || line.startsWith("#")) return@forEach
                parseRule(line)?.let(parsed::add)
            }
            return FilterList(parsed)
        }

        private fun parseRule(line: String): NetworkRule? {
            val exception = line.startsWith("@@")
            val body = if (exception) line.drop(2) else line
            val split = body.split('$', limit = 2)
            val pattern = split[0]
            if (!pattern.startsWith("||")) return null
            val remainder = pattern.drop(2)
            val hostEnd = remainder.indexOfFirst { it == '^' || it == '/' }
            val host = (if (hostEnd < 0) remainder else remainder.substring(0, hostEnd))
                .lowercase(Locale.ROOT)
            if (host.isBlank() || host.length > 253 || host.any { !(it.isLetterOrDigit() || it == '.' || it == '-') }) return null
            val path = if (hostEnd >= 0 && remainder[hostEnd] == '/') remainder.substring(hostEnd) else ""
            if (path.length > 2048 || path.contains("..")) return null
            val options = split.getOrNull(1)?.split(',').orEmpty()
            val types = mutableSetOf<String>()
            var thirdParty: Boolean? = null
            val includeDomains = mutableSetOf<String>()
            val excludeDomains = mutableSetOf<String>()
            for (optionRaw in options) {
                val option = optionRaw.trim()
                when {
                    option in RESOURCE_TYPES -> types += option
                    option == "third-party" -> thirdParty = true
                    option == "~third-party" -> thirdParty = false
                    option.startsWith("domain=") -> option.removePrefix("domain=").split('|').forEach { domain ->
                        val d = domain.trim().lowercase(Locale.ROOT)
                        if (d.startsWith("~")) excludeDomains += d.drop(1) else if (d.isNotBlank()) includeDomains += d
                    }
                    option.isEmpty() -> Unit
                    else -> return null
                }
            }
            return NetworkRule(exception, host, path, types, thirdParty, includeDomains, excludeDomains)
        }

        private const val MAX_LINES = 100_000
        private val RESOURCE_TYPES = setOf("document", "subdocument", "script", "image", "stylesheet", "object", "xmlhttprequest", "media", "font", "other")
    }
}

private data class NetworkRule(
    val exception: Boolean,
    val host: String,
    val path: String,
    val types: Set<String>,
    val thirdParty: Boolean?,
    val includeDomains: Set<String>,
    val excludeDomains: Set<String>
) {
    fun matches(targetHost: String, targetPath: String, resourceType: String, topHost: String?): Boolean {
        if (targetHost != host && !targetHost.endsWith(".$host")) return false
        if (path.isNotEmpty() && !pathMatches(path, targetPath)) return false
        if (types.isNotEmpty() && resourceType.lowercase(Locale.ROOT) !in types) return false
        if (thirdParty != null) {
            if (topHost == null) return false
            val same = targetHost == topHost || targetHost.endsWith(".$topHost") || topHost.endsWith(".$targetHost")
            if (same == thirdParty) return false
        }
        if (includeDomains.isNotEmpty() && (topHost == null || includeDomains.none { topHost == it || topHost.endsWith(".$it") })) return false
        if (excludeDomains.any { topHost == it || topHost?.endsWith(".$it") == true }) return false
        return true
    }

    private fun pathMatches(pattern: String, value: String): Boolean {
        val prefix = pattern.substringBefore('*')
        return value.startsWith(prefix)
    }
}
