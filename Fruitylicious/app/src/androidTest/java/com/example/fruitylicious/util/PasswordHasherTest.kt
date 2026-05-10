package com.example.fruitylicious.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordHasherTest {

    private val passwordHasher = PasswordHasher()

    @Test
    fun hashPassword_generatesValidHash() {
        val password = "password123"
        val hash = passwordHasher.hashPassword(password)

        assertTrue(hash.startsWith("pbkdf2:10000:"))
        val parts = hash.split(":")
        assertEquals(4, parts.size)
    }

    @Test
    fun verifyPassword_correctPassword_returnsTrue() {
        val password = "password123"
        val hash = passwordHasher.hashPassword(password)

        assertTrue(passwordHasher.verifyPassword(password, hash))
    }

    @Test
    fun verifyPassword_wrongPassword_returnsFalse() {
        val password = "password123"
        val hash = passwordHasher.hashPassword(password)

        assertFalse(passwordHasher.verifyPassword("wrongpassword", hash))
    }

    @Test
    fun verifyPassword_legacyPlainText_returnsTrue() {
        val password = "password123"
        // Simulate legacy plain text password in DB
        assertTrue(passwordHasher.verifyPassword(password, password))
    }

    @Test
    fun verifyPassword_legacyPlainText_wrongPassword_returnsFalse() {
        val password = "password123"
        assertFalse(passwordHasher.verifyPassword("wrongpassword", password))
    }

    @Test
    fun hashPassword_isUniqueForEachCall() {
        val password = "password123"
        val hash1 = passwordHasher.hashPassword(password)
        val hash2 = passwordHasher.hashPassword(password)

        assertNotEquals(hash1, hash2)
    }

    private fun assertEquals(expected: Any, actual: Any) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}