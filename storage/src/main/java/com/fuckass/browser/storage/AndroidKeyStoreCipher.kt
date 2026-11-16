package com.fuckass.browser.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * App-private AES-256-GCM encryption backed by the Android Keystore.
 *
 * The key is non-exportable. Callers must persist only the returned ciphertext, never plaintext.
 * This primitive does not itself choose what data should be encrypted or where it is stored.
 */
class AndroidKeyStoreCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) {
    init {
        require(keyAlias.matches(Regex("[A-Za-z0-9._-]{1,80}"))) { "Invalid Keystore alias" }
    }

    @Synchronized
    @Throws(GeneralSecurityException::class)
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        require(plaintext.size <= MAX_PLAINTEXT_BYTES) { "Plaintext exceeds the 1 MiB limit" }
        require(associatedData.size <= MAX_AAD_BYTES) { "Associated data exceeds the 4 KiB limit" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        if (associatedData.isNotEmpty()) cipher.updateAAD(associatedData)
        val encrypted = cipher.doFinal(plaintext)
        val iv = cipher.iv
        check(iv.size == GCM_IV_BYTES) { "Unexpected AES-GCM IV length" }

        return ByteBuffer.allocate(HEADER_BYTES + iv.size + encrypted.size)
            .putInt(MAGIC)
            .put(VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()
    }

    @Synchronized
    @Throws(GeneralSecurityException::class)
    fun decrypt(envelope: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        require(associatedData.size <= MAX_AAD_BYTES) { "Associated data exceeds the 4 KiB limit" }
        require(envelope.size in MIN_ENVELOPE_BYTES..MAX_ENVELOPE_BYTES) { "Invalid encrypted payload size" }

        val input = ByteBuffer.wrap(envelope)
        if (input.int != MAGIC) throw GeneralSecurityException("Invalid encrypted payload header")
        if (input.get() != VERSION) throw GeneralSecurityException("Unsupported encrypted payload version")
        val ivLength = input.get().toInt() and 0xff
        if (ivLength != GCM_IV_BYTES || input.remaining() < ivLength + GCM_TAG_BYTES) {
            throw GeneralSecurityException("Invalid encrypted payload structure")
        }
        val iv = ByteArray(ivLength)
        input.get(iv)
        val encrypted = ByteArray(input.remaining())
        input.get(encrypted)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        if (associatedData.isNotEmpty()) cipher.updateAAD(associatedData)
        return cipher.doFinal(encrypted)
    }

    @Synchronized
    @Throws(GeneralSecurityException::class)
    fun deleteKey() {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(keyAlias)) keyStore.deleteEntry(keyAlias)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = loadKeyStore()
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    companion object {
        private const val DEFAULT_KEY_ALIAS = "custom-browser.data-encryption.v1"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MAGIC = 0x43424B53
        private const val VERSION: Byte = 1
        private const val KEY_SIZE_BITS = 256
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = 16
        private const val HEADER_BYTES = 6
        private const val MAX_PLAINTEXT_BYTES = 1024 * 1024
        private const val MAX_AAD_BYTES = 4 * 1024
        private const val MIN_ENVELOPE_BYTES = HEADER_BYTES + GCM_IV_BYTES + GCM_TAG_BYTES
        private const val MAX_ENVELOPE_BYTES = HEADER_BYTES + GCM_IV_BYTES + MAX_PLAINTEXT_BYTES + GCM_TAG_BYTES
    }
}
