package com.gayakini.common.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class UuidV7GeneratorTest {

    @Test
    fun `generate should return a valid UUID`() {
        val uuid = UuidV7Generator.generate()
        assertNotNull(uuid)
    }

    @Test
    fun `generateString should return a valid UUID string`() {
        val uuidString = UuidV7Generator.generateString()
        assertDoesNotThrow { UUID.fromString(uuidString) }
    }

    @Test
    fun `generated UUIDs should be unique`() {
        val uuid1 = UuidV7Generator.generate()
        val uuid2 = UuidV7Generator.generate()
        assertNotEquals(uuid1, uuid2)
    }

    @Test
    fun `generated UUIDs should be roughly time-ordered`() {
        val uuid1 = UuidV7Generator.generate()
        Thread.sleep(2)
        val uuid2 = UuidV7Generator.generate()

        // UUID v7 is time-ordered
        assertTrue(uuid1 < uuid2, "UUID1 ($uuid1) should be less than UUID2 ($uuid2)")
    }
}
