package com.abccash.app.treasury.repository

import com.abccash.app.treasury.security.PasswordHasher
import org.junit.Assert.assertTrue
import org.junit.Test

class TreasuryAuthTest {

    @Test
    fun passwordHash_survivesRegisterAndLoginRoundTrip() {
        val plainPassword = "MonMotDePasse123"
        val stored = PasswordHasher.hash(plainPassword)
        assertTrue(PasswordHasher.verify(plainPassword, stored))
    }
}
