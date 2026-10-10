package com.sentinel.ai.core.story

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.google.gson.Gson
import com.sentinel.ai.core.feature.PrivacyPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class StoryListing(val cases: List<SavedStory>, val unreadable: Int = 0)
interface StoryStore {
    suspend fun list(): StoryListing
    suspend fun save(case: StoryCase): StoryCase
    suspend fun open(id: String): StoryCase
    suspend fun delete(id: String)
    suspend fun deleteAll()
}

/** File identity is authenticated as well as ciphertext; an encrypted file cannot be swapped. */
object StoryCipher {
    private val header = byteArrayOf(83, 88, 67, 1)
    fun encrypt(bytes: ByteArray, key: SecretKey, id: String): ByteArray {
        require(bytes.size <= StoryLimits.FILE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(("SafeXStory:1:" + id).toByteArray(Charsets.UTF_8))
        require(cipher.iv.size == 12)
        return header + cipher.iv + cipher.doFinal(bytes)
    }
    fun decrypt(bytes: ByteArray, key: SecretKey, id: String): ByteArray {
        require(bytes.size in 32..StoryLimits.FILE_BYTES + 32)
        require(bytes.take(4).toByteArray().contentEquals(header))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(4, 16)))
        cipher.updateAAD(("SafeXStory:1:" + id).toByteArray(Charsets.UTF_8))
        return cipher.doFinal(bytes, 16, bytes.size - 16)
    }
}

@Singleton
class StoryVault @Inject constructor(@ApplicationContext private val context: Context) : StoryStore {
    private val mutex = Mutex()
    private val gson = Gson()
    private val directory get() = File(context.noBackupFilesDir, "story-v1").apply { check(exists() || mkdirs()) }
    private data class Envelope(val schema: Int = 1, val case: StoryCase)
    private fun file(id: String): File {
        require(UUID.fromString(id).toString() == id)
        return File(directory, "$id.sxc")
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey("safex.story.v1", null)
        if (existing is SecretKey) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("safex.story.v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    private fun read(id: String): StoryCase {
        val atomic = AtomicFile(file(id))
        val bytes = atomic.openRead().use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= StoryLimits.FILE_BYTES + 32)
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val clear = StoryCipher.decrypt(bytes, key(), id)
        try {
            val envelope = gson.fromJson(clear.toString(Charsets.UTF_8), Envelope::class.java)
            require(envelope.schema == 1 && envelope.case.id == id && envelope.case.savedAt != null)
            StoryLimits.validate(envelope.case)
            return envelope.case
        } finally { clear.fill(0) }
    }
    private fun ids(): List<String> = directory.listFiles().orEmpty()
        .map { it.name.removeSuffix(".bak").removeSuffix(".new") }.filter { it.endsWith(".sxc") }
        .map { it.removeSuffix(".sxc") }.filter { runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false) }.distinct()
    override suspend fun list(): StoryListing = withContext(Dispatchers.IO) { mutex.withLock {
        val valid = mutableListOf<SavedStory>(); var unreadable = 0
        val deadline = System.currentTimeMillis() - PrivacyPreferences.retentionDays.coerceIn(1,365) * 86400000L
        ids().take(StoryLimits.SAVED_CASES + 1).forEach { id ->
            try {
                val case = read(id)
                if (case.savedAt!! < deadline) AtomicFile(file(id)).delete()
                else valid += SavedStory(id, case.title, case.items.size, case.savedAt, case.synthetic)
            } catch (_: Exception) { unreadable++ }
        }
        StoryListing(valid.sortedByDescending { it.savedAt }, unreadable)
    } }
    override suspend fun save(case: StoryCase): StoryCase = withContext(Dispatchers.IO) { mutex.withLock {
        StoryLimits.validate(case)
        require(case.items.isNotEmpty())
        require(file(case.id).exists() || ids().size < StoryLimits.SAVED_CASES)
        val saved = case.copy(savedAt = System.currentTimeMillis())
        val plain = gson.toJson(Envelope(case = saved)).toByteArray(Charsets.UTF_8)
        val encrypted = try { StoryCipher.encrypt(plain, key(), saved.id) } finally { plain.fill(0) }
        val atomic = AtomicFile(file(saved.id))
        val output = atomic.startWrite()
        try { output.write(encrypted); atomic.finishWrite(output) }
        catch (error: Throwable) { atomic.failWrite(output); throw error }
        saved
    } }
    override suspend fun open(id: String): StoryCase = withContext(Dispatchers.IO) { mutex.withLock { read(id) } }
    override suspend fun delete(id: String) = withContext(Dispatchers.IO) { mutex.withLock { AtomicFile(file(id)).delete() } }
    override suspend fun deleteAll() = withContext(Dispatchers.IO) { mutex.withLock { ids().forEach { AtomicFile(file(it)).delete() } } }
}
