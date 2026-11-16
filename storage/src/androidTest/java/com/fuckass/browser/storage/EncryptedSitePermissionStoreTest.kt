package com.fuckass.browser.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fuckass.browser.privacy.SiteOrigin
import com.fuckass.browser.privacy.SitePermission
import com.fuckass.browser.privacy.SitePermissionDecision
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedSitePermissionStoreTest {
    private fun newCipher() = AndroidKeyStoreCipher("test.permission.${UUID.randomUUID()}")

    @Test
    fun decisionsPersistAcrossStoreRecreationAndRemainOriginScoped() {
        val directory = Files.createTempDirectory("site-permissions").toFile()
        val cipher = newCipher()
        try {
            val store = EncryptedSitePermissionStore(directory, cipher)
            val origin = requireNotNull(SiteOrigin.fromUrl("https://example.test/path"))
            store.put(origin, SitePermission.CAMERA, SitePermissionDecision.ALLOW)
            store.put(origin, SitePermission.MICROPHONE, SitePermissionDecision.BLOCK)

            val reopened = EncryptedSitePermissionStore(directory, cipher)
            assertEquals(SitePermissionDecision.ALLOW, reopened.get(origin, SitePermission.CAMERA))
            assertEquals(SitePermissionDecision.BLOCK, reopened.get(origin, SitePermission.MICROPHONE))
            assertEquals(
                null,
                reopened.get(requireNotNull(SiteOrigin.fromUrl("https://example.test:444/")), SitePermission.CAMERA)
            )
        } finally {
            cipher.deleteKey()
            directory.deleteRecursively()
        }
    }

    @Test
    fun persistedFileContainsCiphertextNotReadablePermissionNames() {
        val directory = Files.createTempDirectory("site-permissions").toFile()
        val cipher = newCipher()
        try {
            val store = EncryptedSitePermissionStore(directory, cipher)
            store.put(
                requireNotNull(SiteOrigin.fromUrl("https://private.example/")),
                SitePermission.CAMERA,
                SitePermissionDecision.ALLOW
            )
            val bytes = File(directory, "site-permissions.enc").readBytes()
            val text = bytes.toString(Charsets.ISO_8859_1)
            assertFalse(text.contains("private.example"))
            assertFalse(text.contains("CAMERA"))
            assertFalse(text.contains("ALLOW"))
        } finally {
            cipher.deleteKey()
            directory.deleteRecursively()
        }
    }

    @Test
    fun tamperedEncryptedFileIsRejectedInsteadOfReset() {
        val directory = Files.createTempDirectory("site-permissions").toFile()
        val cipher = newCipher()
        try {
            val store = EncryptedSitePermissionStore(directory, cipher)
            store.put(
                requireNotNull(SiteOrigin.fromUrl("https://example.test/")),
                SitePermission.LOCATION,
                SitePermissionDecision.BLOCK
            )
            val file = File(directory, "site-permissions.enc")
            val bytes = file.readBytes()
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
            file.writeBytes(bytes)
            assertThrows(IOException::class.java) { EncryptedSitePermissionStore(directory, cipher) }
        } finally {
            cipher.deleteKey()
            directory.deleteRecursively()
        }
    }

    @Test
    fun removeAndClearDeletePersistedDecisions() {
        val directory = Files.createTempDirectory("site-permissions").toFile()
        val cipher = newCipher()
        try {
            val store = EncryptedSitePermissionStore(directory, cipher)
            val a = requireNotNull(SiteOrigin.fromUrl("https://a.example/"))
            val b = requireNotNull(SiteOrigin.fromUrl("https://b.example/"))
            store.put(a, SitePermission.CAMERA, SitePermissionDecision.ALLOW)
            store.put(b, SitePermission.CAMERA, SitePermissionDecision.BLOCK)
            store.remove(a)
            assertEquals(null, EncryptedSitePermissionStore(directory, cipher).get(a, SitePermission.CAMERA))
            store.clear()
            assertFalse(File(directory, "site-permissions.enc").exists())
            assertTrue(directory.listFiles()?.none { it.name.endsWith(".tmp") } ?: true)
        } finally {
            cipher.deleteKey()
            directory.deleteRecursively()
        }
    }
}
