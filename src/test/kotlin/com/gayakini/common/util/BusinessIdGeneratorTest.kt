package com.gayakini.common.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BusinessIdGeneratorTest {

    @Test
    fun `generateOrderNumber should start with ORD-`() {
        val orderNumber = BusinessIdGenerator.generateOrderNumber()
        assertTrue(orderNumber.startsWith("ORD-"))
    }

    @Test
    fun `generateTransactionNumber should start with TRX-`() {
        val trxNumber = BusinessIdGenerator.generateTransactionNumber()
        assertTrue(trxNumber.startsWith("TRX-"))
    }

    @Test
    fun `generateOrderNumber should contain current date`() {
        val now = LocalDateTime.now()
        val expectedDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val orderNumber = BusinessIdGenerator.generateOrderNumber()
        assertTrue(orderNumber.contains(expectedDate))
    }

    @Test
    fun `generated numbers should be unique`() {
        val ids = (1..100).map { BusinessIdGenerator.generateOrderNumber() }.toSet()
        assertEquals(100, ids.size)
    }

    @Test
    fun `sequence should increment`() {
        val id1 = BusinessIdGenerator.generateOrderNumber()
        val id2 = BusinessIdGenerator.generateOrderNumber()

        val seq1 = id1.split("-").last().toInt()
        val seq2 = id2.split("-").last().toInt()

        assertTrue(seq2 > seq1)
    }
}
