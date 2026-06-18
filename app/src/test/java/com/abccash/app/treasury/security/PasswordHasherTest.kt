package com.abccash.app.treasury.security

import kotlin.io.encoding.ExperimentalEncodingApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalEncodingApi::class)
class PasswordHasherTest {

    @Test
    fun hashAndVerify_pbkdf2() {
        val password = "secret123"
        val stored = PasswordHasher.hash(password)
        assertTrue(stored.startsWith("pbkdf2$"))
        assertTrue(PasswordHasher.verify(password, stored))
        assertFalse(PasswordHasher.verify("wrong", stored))
    }

    @Test
    fun verify_legacySha256() {
        val password = "legacy"
        val legacy = java.security.MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertTrue(PasswordHasher.verify(password, legacy))
        assertTrue(PasswordHasher.needsUpgrade(legacy))
    }

    @Test
    fun verify_legacyHashedPrefix() {
        val password = "test"
        val stored = "hashed_${password.hashCode()}"
        assertTrue(PasswordHasher.verify(password, stored))
        assertTrue(PasswordHasher.needsUpgrade(stored))
    }

    @Test
    fun needsUpgrade_onlyForPbkdf2() {
        val pbkdf2 = PasswordHasher.hash("x")
        assertFalse(PasswordHasher.needsUpgrade(pbkdf2))
    }
}
