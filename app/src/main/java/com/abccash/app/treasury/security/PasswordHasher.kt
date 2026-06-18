package com.abccash.app.treasury.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object PasswordHasher {
    private const val PBKDF2_PREFIX = "pbkdf2"
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH = 16

    fun hash(password: String): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val derived = deriveKey(password, salt)
        return "$PBKDF2_PREFIX\$$ITERATIONS\$${Base64.encode(salt)}\$${Base64.encode(derived)}"
    }

    fun verify(password: String, storedHash: String): Boolean {
        return when {
            storedHash.startsWith("$PBKDF2_PREFIX$") -> verifyPbkdf2(password, storedHash)
            storedHash.startsWith("hashed_") -> storedHash == "hashed_${password.hashCode()}"
            storedHash.length == 64 && storedHash.all { it in '0'..'9' || it in 'a'..'f' } ->
                legacySha256(password) == storedHash
            else -> false
        }
    }

    fun needsUpgrade(storedHash: String): Boolean =
        !storedHash.startsWith("$PBKDF2_PREFIX$")

    private fun verifyPbkdf2(password: String, storedHash: String): Boolean {
        val parts = storedHash.split('$', limit = 4)
        if (parts.size != 4 || parts[0] != PBKDF2_PREFIX) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = runCatching { Base64.decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(parts[3]) }.getOrNull() ?: return false
        val actual = deriveKey(password, salt, iterations)
        return constantTimeEquals(expected, actual)
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int = ITERATIONS
    ): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun legacySha256(password: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
