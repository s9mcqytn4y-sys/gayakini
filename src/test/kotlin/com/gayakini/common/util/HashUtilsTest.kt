package com.gayakini.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HashUtilsTest {
    @Test
    fun `sha256 should return valid hex string`() {
        val input = "gayakini"
        // echo -n "gayakini" | sha256sum -> ecb5895e824240a84917916808d39117c84d43bb070b54128e63038e472b8077
        val expected = "ecb5895e824240a84917916808d39117c84d43bb070b54128e63038e472b8077"
        assertEquals(expected, HashUtils.sha256(input))
    }

    @Test
    fun `sha256 should handle empty string`() {
        val input = ""
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assertEquals(expected, HashUtils.sha256(input))
    }
}
