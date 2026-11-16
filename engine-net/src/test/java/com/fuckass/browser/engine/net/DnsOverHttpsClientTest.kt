package com.fuckass.browser.engine.net

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.Principal
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsOverHttpsClientTest {
    @Test
    fun buildsRfc8484QueryAndParsesCompressedAAnswer() {
        lateinit var fake: FakeHttpsConnection
        val client = DnsOverHttpsClient(connectionFactory = { url ->
            FakeHttpsConnection(url).also { fake = it }
        })

        val result = client.resolve("Example.Test.", DnsRecordType.A)

        assertEquals(listOf("1.1.1.1"), result)
        assertTrue(fake.requestBody.size > 12)
        assertEquals("POST", fake.requestMethod)
        assertEquals("application/dns-message", fake.requestedContentType)
        assertTrue(fake.disconnected)
    }

    @Test
    fun rejectsUnexpectedContentType() {
        val client = DnsOverHttpsClient(connectionFactory = {
            FakeHttpsConnection(it, mimeType = "text/plain")
        })
        assertThrows(IllegalStateException::class.java) {
            client.resolve("example.test")
        }
    }

    @Test
    fun enforcesResponseByteLimit() {
        val client = DnsOverHttpsClient(
            maxResponseBytes = 12,
            connectionFactory = { FakeHttpsConnection(it) }
        )
        assertThrows(IllegalArgumentException::class.java) {
            client.resolve("example.test")
        }
    }

    @Test
    fun rejectsNonHttpsResolverEndpoint() {
        assertThrows(IllegalArgumentException::class.java) {
            DnsOverHttpsClient(endpoint = "http://resolver.example/dns-query")
        }
    }

    @Test
    fun rejectsInvalidHostnamesBeforeNetworkAccess() {
        var opened = false
        val client = DnsOverHttpsClient(connectionFactory = {
            opened = true
            FakeHttpsConnection(it)
        })
        assertThrows(IllegalArgumentException::class.java) {
            client.resolve("bad/name")
        }
        assertTrue(!opened)
    }

    private class FakeHttpsConnection(
        url: URL,
        private val mimeType: String = "application/dns-message"
    ) : HttpsURLConnection(url) {
        private val output = ByteArrayOutputStream()
        val requestBody: ByteArray get() = output.toByteArray()
        var requestedContentType: String? = null
        var disconnected = false

        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getOutputStream() = output
        override fun getInputStream(): java.io.InputStream =
            ByteArrayInputStream(responseFor(requestBody))
        override fun getResponseCode() = 200
        override fun getContentType() = mimeType
        override fun setRequestProperty(key: String?, value: String?) {
            if (key.equals("Content-Type", true)) requestedContentType = value
        }
        override fun getCipherSuite(): String = "TLS_FAKE"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = throw SSLPeerUnverifiedException("test")
        override fun getPeerPrincipal(): Principal = throw SSLPeerUnverifiedException("test")
        override fun getLocalPrincipal(): Principal? = null

        private fun responseFor(query: ByteArray): ByteArray {
            require(query.size >= 12)
            val questionEnd = query.size
            val result = ByteArrayOutputStream()
            result.write(query[0].toInt()); result.write(query[1].toInt())
            result.write(0x81); result.write(0x80)
            result.write(0); result.write(1) // one question
            result.write(0); result.write(1) // one answer
            result.write(0); result.write(0)
            result.write(0); result.write(0)
            result.write(query, 12, questionEnd - 12)
            result.write(0xc0); result.write(0x0c) // compressed owner name
            result.write(0); result.write(1) // A
            result.write(0); result.write(1) // IN
            result.write(0); result.write(0); result.write(0); result.write(60)
            result.write(0); result.write(4)
            result.write(byteArrayOf(1, 1, 1, 1))
            return result.toByteArray()
        }
    }
}
