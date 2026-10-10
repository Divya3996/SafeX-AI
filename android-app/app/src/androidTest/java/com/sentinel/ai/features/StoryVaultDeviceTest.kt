package com.sentinel.ai.features

import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.sentinel.ai.core.feature.PrivacyPreferences
import com.sentinel.ai.core.model.*
import com.sentinel.ai.core.story.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.security.KeyStore
import javax.crypto.SecretKey

class StoryVaultDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val vault = StoryVault(context)
    private val directory get() = File(context.noBackupFilesDir, "story-v1")
    private fun example() = StoryCase(title = "PrivateName", items = listOf(StoryItem(source = StorySource.MESSAGE, text = "SecretXYZ 9999988888",
        result = ScanResult(id = "authored", source = "Authored", riskLevel = RiskLevel.GREEN, riskScore = 0f, explanation = "Fixture", timestamp = 0, target = "SecretXYZ 9999988888"))))
    @Before fun reset() = runBlocking { vault.deleteAll(); PrivacyPreferences.retentionDays = 30 }
    @After fun clear() = runBlocking { vault.deleteAll(); PrivacyPreferences.retentionDays = 30 }
    @Test fun androidKeystoreRoundTripExcludesPlaintextAndDetectsTampering() = runBlocking {
        val saved = vault.save(example()); val file = File(directory, "${saved.id}.sxc")
        val bytes = file.readBytes(); val serialized = bytes.toString(Charsets.UTF_8)
        for (value in listOf("PrivateName", "SecretXYZ", "9999988888")) assertFalse(serialized.contains(value))
        assertEquals(saved, vault.open(saved.id)); assertEquals(1, vault.list().cases.size)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte(); file.writeBytes(bytes)
        assertTrue(runCatching { vault.open(saved.id) }.isFailure)
        assertEquals(1, vault.list().unreadable); assertTrue(vault.list().cases.isEmpty())
        vault.delete(saved.id); assertTrue(vault.list().cases.isEmpty()); assertEquals(0, vault.list().unreadable)
    }
    @Test fun savedFileIdentityAndUuidPathAreAuthenticated() = runBlocking {
        val a = vault.save(example()); val b = vault.save(example())
        File(directory, "${b.id}.sxc").writeBytes(File(directory, "${a.id}.sxc").readBytes())
        assertTrue(runCatching { vault.open(b.id) }.isFailure)
        assertTrue(runCatching { vault.open("../outside") }.isFailure)
        assertEquals(a, vault.open(a.id))
    }
    @Test fun retentionDeletesAnExpiredEncryptedSnapshot() = runBlocking {
        val saved = vault.save(example()); val file = File(directory, "${saved.id}.sxc")
        val key = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey("safex.story.v1", null) as SecretKey
        val json = com.google.gson.JsonParser.parseString(StoryCipher.decrypt(file.readBytes(), key, saved.id).toString(Charsets.UTF_8)).asJsonObject
        json.getAsJsonObject("case").addProperty("savedAt", System.currentTimeMillis() - 31 * 86400000L)
        file.writeBytes(StoryCipher.encrypt(Gson().toJson(json).toByteArray(), key, saved.id))
        assertTrue(vault.list().cases.isEmpty()); assertFalse(file.exists())
    }
    @Test fun capacityIsBoundedAndExistingSnapshotCanStillBeUpdated() = runBlocking {
        val saved = (1..StoryLimits.SAVED_CASES).map { vault.save(example()) }
        assertTrue(runCatching { vault.save(example()) }.isFailure)
        vault.save(saved.first().copy(title = "Updated title"))
        assertEquals("Updated title", vault.open(saved.first().id).title)
        vault.deleteAll(); assertTrue(vault.list().cases.isEmpty())
    }
}
