package com.fuckass.browser.engine.net

import java.net.IDN
import java.net.URL
import java.net.InetAddress
import java.net.HttpURLConnection
import java.net.URI
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection

enum class DnsRecordType(val wireValue: Int, val byteLength: Int) {
    A(1, 4),
    AAAA(28, 16)
}

/**
 * Bounded RFC 8484 DNS-message POST client.
 *
 * Resolver endpoint must be HTTPS and uses platform TLS/hostname validation. Resolving the
 * resolver hostname itself still depends on Android's bootstrap DNS; this class is not a
 * complete system-wide DNS replacement and must be explicitly enabled by the caller.
 */
class DnsOverHttpsClient(
    private val endpoint: String = "https://cloudflare-dns.com/dns-query",
    private val connectTimeoutMs: Int = 5_000,
    private val readTimeoutMs: Int = 5_000,
    private val maxResponseBytes: Int = 4_096,
    private val connectionFactory: (URL) -> HttpsURLConnection = {
        (it.openConnection() as? HttpsURLConnection)
            ?: throw IllegalArgumentException("HTTPS connection required")
    }
) {
    init {
        val uri = URI(endpoint)
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.rawUserInfo == null)
        require(connectTimeoutMs in 1..30_000)
        require(readTimeoutMs in 1..30_000)
        require(maxResponseBytes in 12..65_535)
    }

    fun resolve(hostname: String, type: DnsRecordType = DnsRecordType.A): List<String> {
        val normalized = normalizeName(hostname)
        val id = SecureRandom().nextInt(65_536)
        val request = buildQuery(normalized, type, id)
        val connection = connectionFactory(URL(endpoint))
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/dns-message")
            connection.setRequestProperty("Accept", "application/dns-message")
            connection.setFixedLengthStreamingMode(request.size)
            connection.outputStream.use { it.write(request) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("DoH resolver returned HTTP ${connection.responseCode}")
            }
            val contentType = connection.contentType.orEmpty().substringBefore(';').trim()
            if (!contentType.equals("application/dns-message", true)) {
                throw IllegalStateException("DoH resolver returned an unsupported content type")
            }
            val body = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(512)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= maxResponseBytes) { "DoH response exceeds configured limit" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return parseResponse(body, id, normalized, type)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeName(value: String): String {
        val trimmed = value.trim().trimEnd('.')
        require(trimmed.isNotEmpty() && trimmed.length <= 253 && !trimmed.contains('/') && !trimmed.contains('@'))
        val ascii = IDN.toASCII(trimmed, IDN.USE_STD3_ASCII_RULES).lowercase()
        val labels = ascii.split('.')
        require(ascii.length <= 253 && labels.all { it.isNotEmpty() && it.length <= 63 })
        return ascii
    }

    private fun buildQuery(name: String, type: DnsRecordType, id: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        output.write((id ushr 8) and 0xff)
        output.write(id and 0xff)
        output.write(0x01) // recursion desired
        output.write(0x00)
        output.write(0x00); output.write(0x01) // QDCOUNT
        output.write(0x00); output.write(0x00) // ANCOUNT
        output.write(0x00); output.write(0x00) // NSCOUNT
        output.write(0x00); output.write(0x00) // ARCOUNT
        name.split('.').forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            output.write(bytes.size)
            output.write(bytes)
        }
        output.write(0)
        output.write((type.wireValue ushr 8) and 0xff); output.write(type.wireValue and 0xff)
        output.write(0); output.write(1) // IN
        return output.toByteArray()
    }

    private fun parseResponse(message: ByteArray, expectedId: Int, expectedName: String, type: DnsRecordType): List<String> {
        require(message.size in 12..maxResponseBytes) { "Invalid DNS message length" }
        fun u16(offset: Int): Int {
            require(offset >= 0 && offset + 1 < message.size) { "Truncated DNS message" }
            return ((message[offset].toInt() and 0xff) shl 8) or (message[offset + 1].toInt() and 0xff)
        }
        val id = u16(0)
        val flags = u16(2)
        require(id == expectedId && flags and 0x8000 != 0) { "Invalid DNS response identity or direction" }
        require(flags and 0x0200 == 0) { "Truncated DNS response" }
        require(flags and 0x000f == 0) { "DNS resolver returned error code ${flags and 0x000f}" }
        val questions = u16(4)
        val answers = u16(6)
        require(questions == 1 && answers <= 256) { "Unexpected DNS section counts" }
        val questionName = readQuestionName(message, 12)
        var offset = skipName(message, 12)
        require(offset + 4 <= message.size) { "Truncated DNS question" }
        require(questionName.equals(expectedName, ignoreCase = true) &&
            u16(offset) == type.wireValue && u16(offset + 2) == 1
        ) { "DNS response question does not match request" }
        offset += 4
        val result = linkedSetOf<String>()
        repeat(answers) {
            offset = skipName(message, offset)
            require(offset + 10 <= message.size) { "Truncated DNS answer" }
            val answerType = u16(offset)
            val answerClass = u16(offset + 2)
            val dataLength = u16(offset + 8)
            offset += 10
            require(offset + dataLength <= message.size) { "Truncated DNS record data" }
            if (answerClass == 1 && answerType == type.wireValue && dataLength == type.byteLength) {
                val addressBytes = message.copyOfRange(offset, offset + dataLength)
                result += InetAddress.getByAddress(addressBytes).hostAddress
            }
            offset += dataLength
        }
        return result.toList()
    }

    private fun readQuestionName(message: ByteArray, start: Int): String {
        val labels = mutableListOf<String>()
        var offset = start
        while (true) {
            require(offset < message.size) { "Truncated DNS question name" }
            val length = message[offset++].toInt() and 0xff
            if (length == 0) break
            require(length <= 63 && length and 0xc0 == 0 && offset + length <= message.size) {
                "Invalid DNS question label"
            }
            labels += String(message, offset, length, Charsets.US_ASCII)
            offset += length
        }
        return labels.joinToString(".")
    }

    private fun skipName(message: ByteArray, start: Int): Int {
        var offset = start
        var jumps = 0
        while (true) {
            require(offset < message.size) { "Truncated DNS name" }
            val length = message[offset].toInt() and 0xff
            when {
                length == 0 -> return offset + 1
                length and 0xc0 == 0xc0 -> {
                    require(offset + 1 < message.size) { "Truncated DNS compression pointer" }
                    val pointer = ((length and 0x3f) shl 8) or (message[offset + 1].toInt() and 0xff)
                    require(pointer < message.size && ++jumps <= 16) { "Invalid DNS compression pointer" }
                    return offset + 2
                }
                length and 0xc0 != 0 -> throw IllegalArgumentException("Invalid DNS label length")
                else -> {
                    require(length <= 63 && offset + 1 + length <= message.size) { "Truncated DNS label" }
                    offset += 1 + length
                }
            }
        }
    }
}
