package com.gayakini.infrastructure.storage

import java.security.SecureRandom
import java.util.Base64

object FileNamingGenerator {
    private val secureRandom = SecureRandom()
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()

    fun generateSecureInvoiceName(orderNumber: String): String {
        val randomBytes = ByteArray(6)
        secureRandom.nextBytes(randomBytes)
        val hash =
            base64Encoder.encodeToString(randomBytes)
                .replace("-", "")
                .replace("_", "")
                .take(8)

        // Sanitize order number to prevent directory traversal just in case
        val sanitizedOrderNumber = orderNumber.replace(Regex("[^a-zA-Z0-9-]"), "")

        return "INV-$sanitizedOrderNumber-$hash.pdf"
    }
}
