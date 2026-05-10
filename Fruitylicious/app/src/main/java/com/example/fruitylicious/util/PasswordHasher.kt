package com.example.fruitylicious.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordHasher @Inject constructor() {

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 10000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
        private const val DELIMITER = ":"
        private const val PREFIX = "pbkdf2"
    }

    /**
     * Checks if a string is already hashed using this hasher.
     */
    fun isHashed(password: String): Boolean {
        return password.startsWith("$PREFIX$DELIMITER")
    }

    /**
     * Hashes a plain-text password using PBKDF2 with HmacSHA256.
     * Returns a string in the format: pbkdf2:iterations:salt:hash
     */
    fun hashPassword(password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)

        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded

        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        return "$PREFIX$DELIMITER$ITERATIONS$DELIMITER$saltBase64$DELIMITER$hashBase64"
    }

    /**
     * Verifies a plain-text password against a hashed password string.
     * Supports both hashed passwords and legacy plain-text passwords.
     */
    fun verifyPassword(password: String, storedPasswordHash: String): Boolean {
        if (!storedPasswordHash.startsWith("$PREFIX$DELIMITER")) {
            // Legacy support: compare as plain text
            return password == storedPasswordHash
        }

        return try {
            val parts = storedPasswordHash.split(DELIMITER)
            if (parts.size != 4) return false

            val iterations = parts[1].toInt()
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val storedHash = Base64.decode(parts[3], Base64.NO_WRAP)

            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val testHash = factory.generateSecret(spec).encoded

            testHash.contentEquals(storedHash)
        } catch (e: Exception) {
            false
        }
    }
}