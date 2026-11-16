package com.fuckass.browser.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.GeneralSecurityException
import java.util.UUID
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeyStoreCipherTest {
    private fun newCipher() = AndroidKeyStoreCipher("test.browser.${UUID.randomUUID()}")

    @Test
    fun encryptDecryptRoundTripUsesAssociatedData() {
        val cipher = newCipher()
        try {
            val plaintext = "sensitive browser secret".toByteArray()
            val context = "password-record:v1".toByteArray()
            val encrypted = cipher.encrypt(plaintext, context)
            assertArrayEquals(plaintext, cipher.decrypt(encrypted, context))
        } finally {
            cipher.deleteKey()
        }
    }

    @Test
    fun encryptionUsesFreshRandomIv() {
        val cipher = newCipher()
        try {
            val plaintext = "same plaintext".toByteArray()
            assertNotEquals(
                cipher.encrypt(plaintext).toList(),
                cipher.encrypt(plaintext).toList()
            )
        } finally {
            cipher.deleteKey()
        }
    }

    @Test
    fun modifiedCiphertextAndWrongAssociatedDataAreRejected() {
        val cipher = newCipher()
        try {
            val encrypted = cipher.encrypt("secret".toByteArray(), "record-A".toByteArray())
            val tampered = encrypted.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(tampered, "record-A".toByteArray()) }
            assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(encrypted, "record-B".toByteArray()) }
        } finally {
            cipher.deleteKey()
        }
    }

    @Test
    fun malformedEnvelopeIsRejected() {
        val cipher = newCipher()
        try {
            assertThrows(IllegalArgumentException::class.java) { cipher.decrypt(byteArrayOf(1, 2, 3)) }
        } finally {
            cipher.deleteKey()
        }
    }

    @Test
    fun deletedKeyCannotDecryptPreviousCiphertext() {
        val cipher = newCipher()
        val encrypted = cipher.encrypt("secret".toByteArray())
        cipher.deleteKey()
        try {
            assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(encrypted) }
        } finally {
            cipher.deleteKey()
        }
    }
}
