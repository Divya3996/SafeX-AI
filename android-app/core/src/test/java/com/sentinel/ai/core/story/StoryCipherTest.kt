package com.sentinel.ai.core.story

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.KeyGenerator

class StoryCipherTest {
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    @Test fun encryptedRoundTripDoesNotContainPlaintext() {
        val clear = "Private OTP 123456 account name".toByteArray()
        val key = key(); val encrypted = StoryCipher.encrypt(clear, key, "case-a")
        assertFalse(encrypted.toString(Charsets.UTF_8).contains("Private OTP"))
        assertArrayEquals(clear, StoryCipher.decrypt(encrypted, key, "case-a"))
    }
    @Test fun freshWritesUseDifferentNonces() {
        val key = key()
        assertFalse(StoryCipher.encrypt("same".toByteArray(),key,"a").contentEquals(StoryCipher.encrypt("same".toByteArray(),key,"a")))
    }
    @Test fun changingCiphertextFailsAuthentication() {
        val key = key(); val bytes = StoryCipher.encrypt("private".toByteArray(),key,"a")
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { StoryCipher.decrypt(bytes,key,"a") }
    }
    @Test fun movingEncryptedContentToAnotherCaseFails() {
        val key = key(); val bytes = StoryCipher.encrypt("private".toByteArray(),key,"a")
        assertThrows(Exception::class.java) { StoryCipher.decrypt(bytes,key,"b") }
    }
    @Test fun wrongKeyCannotReadCase() {
        val bytes = StoryCipher.encrypt("private".toByteArray(),key(),"a")
        assertThrows(Exception::class.java) { StoryCipher.decrypt(bytes,key(),"a") }
    }
    @Test fun corruptHeadersAndOversizedPayloadsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { StoryCipher.decrypt(ByteArray(33),key(),"a") }
        assertThrows(IllegalArgumentException::class.java) { StoryCipher.encrypt(ByteArray(StoryLimits.FILE_BYTES+1),key(),"a") }
    }
}
