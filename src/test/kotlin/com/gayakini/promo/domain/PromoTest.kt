package com.gayakini.promo.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

class PromoTest {
    private fun createTestPromo(
        type: PromoType = PromoType.PERCENTAGE,
        value: BigDecimal = BigDecimal("10"),
        minOrder: BigDecimal = BigDecimal.ZERO,
        maxDiscount: BigDecimal? = null,
        usageLimit: Int? = null,
        currentUsage: Int = 0,
        active: Boolean = true,
    ): Promo {
        return Promo(
            code = "TESTPROMO",
            type = type,
            value = value,
            minOrderValue = minOrder,
            maxDiscountAmount = maxDiscount,
            usageLimit = usageLimit,
            currentUsage = currentUsage,
            isActive = active,
            startDate = Instant.now().minus(1, ChronoUnit.DAYS),
            endDate = Instant.now().plus(1, ChronoUnit.DAYS),
        )
    }

    @Test
    fun `calculateDiscount should handle PERCENTAGE correctly`() {
        val promo = createTestPromo(type = PromoType.PERCENTAGE, value = BigDecimal("10"))
        val subtotal = BigDecimal("100000")

        val discount = promo.calculateDiscount(subtotal)
        assertEquals(BigDecimal("10000"), discount)
    }

    @Test
    fun `calculateDiscount should respect maxDiscountAmount for PERCENTAGE`() {
        val promo =
            createTestPromo(
                type = PromoType.PERCENTAGE,
                value = BigDecimal("10"),
                maxDiscount = BigDecimal("5000"),
            )
        val subtotal = BigDecimal("100000")

        val discount = promo.calculateDiscount(subtotal)
        assertEquals(BigDecimal("5000"), discount)
    }

    @Test
    fun `calculateDiscount should handle FIXED_AMOUNT correctly`() {
        val promo = createTestPromo(type = PromoType.FIXED_AMOUNT, value = BigDecimal("20000"))
        val subtotal = BigDecimal("100000")

        val discount = promo.calculateDiscount(subtotal)
        assertEquals(BigDecimal("20000"), discount)
    }

    @Test
    fun `calculateDiscount should not exceed subtotal for FIXED_AMOUNT`() {
        val promo = createTestPromo(type = PromoType.FIXED_AMOUNT, value = BigDecimal("150000"))
        val subtotal = BigDecimal("100000")

        val discount = promo.calculateDiscount(subtotal)
        assertEquals(BigDecimal("100000"), discount)
    }

    @Test
    fun `isValid should return false if inactive`() {
        val promo = createTestPromo(active = false)
        assertFalse(promo.isValid(BigDecimal("100000")))
    }

    @Test
    fun `isValid should return false if usage limit reached`() {
        val promo = createTestPromo(usageLimit = 10, currentUsage = 10)
        assertFalse(promo.isValid(BigDecimal("100000")))
    }

    @Test
    fun `isValid should return false if subtotal less than minOrderValue`() {
        val promo = createTestPromo(minOrder = BigDecimal("200000"))
        assertFalse(promo.isValid(BigDecimal("150000")))
    }
}
