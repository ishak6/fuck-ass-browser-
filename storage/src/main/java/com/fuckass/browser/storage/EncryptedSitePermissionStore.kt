package com.fuckass.browser.storage

import com.fuckass.browser.privacy.SiteOrigin
import com.fuckass.browser.privacy.SitePermission
import com.fuckass.browser.privacy.SitePermissionDecision
import com.fuckass.browser.privacy.SitePermissionStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.security.GeneralSecurityException

/**
 * Durable per-origin permission decisions encrypted with an Android Keystore AES-GCM key.
 * The backing directory must be app-private. Corrupt/authentication-failed data is rejected,
 * never silently replaced with an empty permission database.
 */
class EncryptedSitePermissionStore(
    private val directory: File,
    private val cipher: AndroidKeyStoreCipher = AndroidKeyStoreCipher(KEY_ALIAS)
) : SitePermissionStore {
    private val file = File(directory, FILE_NAME)
    private val temp = File(directory, "$FILE_NAME.tmp")
    private val entries = linkedMapOf<SiteOrigin, MutableMap<SitePermission, SitePermissionDecision>>()

    init {
        require(directory.exists() || directory.mkdirs()) { "Could not create permission store directory" }
        require(directory.isDirectory) { "Permission store path is not a directory" }
        load()
    }

    @Synchronized
    override fun get(origin: SiteOrigin, permission: SitePermission): SitePermissionDecision? =
        entries[origin]?.get(permission)

    @Synchronized
    override fun put(origin: SiteOrigin, permission: SitePermission, decision: SitePermissionDecision) {
        val site = entries[origin]
        if (site == null) {
            while (entries.size >= MAX_ORIGINS) entries.remove(entries.keys.first())
            entries[origin] = mutableMapOf(permission to decision)
        } else {
            site[permission] = decision
        }
        persist()
    }

    @Synchronized
    override fun remove(origin: SiteOrigin) {
        if (entries.remove(origin) != null) persist()
    }

    @Synchronized
    override fun clear() {
        entries.clear()
        if (file.exists() && !file.delete()) throw IOException("Could not clear encrypted site permissions")
        temp.delete()
    }

    private fun load() {
        if (!file.exists()) return
        if (file.length() !in MIN_ENVELOPE_SIZE.toLong()..MAX_ENCRYPTED_BYTES.toLong()) {
            throw IOException("Encrypted site permission file size is invalid")
        }
        val encrypted = file.readBytes()
        val plaintext = try {
            cipher.decrypt(encrypted, ASSOCIATED_DATA)
        } catch (failure: GeneralSecurityException) {
            throw IOException("Encrypted site permission data failed authentication", failure)
        }
        try {
            DataInputStream(ByteArrayInputStream(plaintext)).use { input ->
                if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                    throw IOException("Unsupported site permission data format")
                }
                val count = input.readInt()
                if (count !in 0..MAX_ORIGINS) throw IOException("Invalid site permission origin count")
                repeat(count) {
                    val scheme = input.readUTF()
                    val host = input.readUTF()
                    val port = input.readInt()
                    val origin = runCatching { SiteOrigin(scheme, host, port) }.getOrElse {
                        throw IOException("Invalid stored site origin", it)
                    }
                    val permissionCount = input.readInt()
                    if (permissionCount !in 1..SitePermission.entries.size) {
                        throw IOException("Invalid site permission count")
                    }
                    val decisions = mutableMapOf<SitePermission, SitePermissionDecision>()
                    repeat(permissionCount) {
                        val permission = runCatching { SitePermission.valueOf(input.readUTF()) }
                            .getOrElse { throw IOException("Unknown stored site permission", it) }
                        val decision = runCatching { SitePermissionDecision.valueOf(input.readUTF()) }
                            .getOrElse { throw IOException("Unknown stored permission decision", it) }
                        if (decisions.put(permission, decision) != null) {
                            throw IOException("Duplicate stored site permission")
                        }
                    }
                    if (entries.put(origin, decisions) != null) throw IOException("Duplicate stored site origin")
                }
                if (input.read() != -1) throw IOException("Unexpected trailing site permission data")
            }
        } catch (failure: IOException) {
            throw failure
        } catch (failure: Exception) {
            throw IOException("Could not decode site permission data", failure)
        } finally {
            plaintext.fill(0)
        }
    }

    private fun persist() {
        val plaintextBytes = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(VERSION)
                output.writeInt(entries.size)
                entries.forEach { (origin, permissions) ->
                    output.writeUTF(origin.scheme)
                    output.writeUTF(origin.host)
                    output.writeInt(origin.port)
                    output.writeInt(permissions.size)
                    permissions.forEach { (permission, decision) ->
                        output.writeUTF(permission.name)
                        output.writeUTF(decision.name)
                    }
                }
            }
            bytes.toByteArray()
        }
        if (plaintextBytes.size > MAX_PLAINTEXT_BYTES) {
            plaintextBytes.fill(0)
            throw IOException("Site permission data exceeds the storage limit")
        }
        try {
            val encrypted = cipher.encrypt(plaintextBytes, ASSOCIATED_DATA)
            if (encrypted.size > MAX_ENCRYPTED_BYTES) throw IOException("Encrypted permission data exceeds the storage limit")
            temp.outputStream().use { it.write(encrypted); it.fd.sync() }
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (failure: Exception) {
            temp.delete()
            if (failure is IOException) throw failure
            throw IOException("Could not persist encrypted site permissions", failure)
        } finally {
            plaintextBytes.fill(0)
        }
    }

    companion object {
        private const val KEY_ALIAS = "custom-browser.site-permissions.v1"
        private const val FILE_NAME = "site-permissions.enc"
        private const val MAGIC = 0x4353504D
        private const val VERSION = 1
        private const val MAX_ORIGINS = 1_000
        private const val MAX_PLAINTEXT_BYTES = 128 * 1024
        private const val MAX_ENCRYPTED_BYTES = MAX_PLAINTEXT_BYTES + 64
        private const val MIN_ENVELOPE_SIZE = 6 + 12 + 16
        private val ASSOCIATED_DATA = "custom-browser/site-permissions/v1".toByteArray(Charsets.UTF_8)
    }
}
