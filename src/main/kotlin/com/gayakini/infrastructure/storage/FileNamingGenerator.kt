package com.gayakini.infrastructure.storage

import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Generates cryptographically randomized filenames to prevent enumeration attacks and directory traversal.
 */
object FileNamingGenerator {
    private val secureRandom = SecureRandom()
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()

    private const val RANDOM_SUFFIX_BYTES = 6
    private const val RANDOM_SUFFIX_LENGTH = 8

    fun generateSecureInvoiceName(orderNumber: String): String {
        val hash = generateRandomSuffix()
        val sanitized = sanitize(orderNumber)
        return "INV-$sanitized-$hash.pdf"
    }

    fun generateProductName(extension: String): String {
        val hash = generateRandomSuffix()
        return "PRD-${UUID.randomUUID()}-$hash.$extension"
    }

    fun generateProfileName(
        userId: String,
        extension: String,
    ): String {
        val hash = generateRandomSuffix()
        return "USR-${sanitize(userId)}-$hash.$extension"
    }

    fun generateProofName(
        transactionId: String,
        extension: String,
    ): String {
        val hash = generateRandomSuffix()
        return "PRF-${sanitize(transactionId)}-$hash.$extension"
    }

    private fun generateRandomSuffix(): String {
        val randomBytes = ByteArray(RANDOM_SUFFIX_BYTES)
        secureRandom.nextBytes(randomBytes)
        val rawHash = base64Encoder.encodeToString(randomBytes)
        return rawHash
            .replace("-", "")
            .replace("_", "")
            .take(RANDOM_SUFFIX_LENGTH)
            .lowercase()
    }

    private fun sanitize(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9-]"), "")
    }
}
