package com.gayakini.common.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object HashUtils {
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
