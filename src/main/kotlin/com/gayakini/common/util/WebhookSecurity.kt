package com.gayakini.common.util

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Security utilities for webhook signature verification.
 */
object WebhookSecurity {
    /**
     * Verifies two strings in constant time to prevent timing attacks.
     */
    fun safeEqual(
        a: String,
        b: String,
    ): Boolean {
        return MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    }

    /**
     * Calculates HMAC-SHA256 hex digest of the input.
     */
    fun hmacSha256(
        secret: String,
        input: String,
    ): String {
        val keySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        val bytes = mac.doFinal(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
