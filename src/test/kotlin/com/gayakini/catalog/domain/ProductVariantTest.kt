package com.gayakini.catalog.domain

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ProductVariantTest {

    @Test
    fun `stockAvailable should be difference between onHand and reserved`() {
        val variant = ProductVariant(
            id = UUID.randomUUID(),
            product = mockk(),
            sku = "TEST-SKU",
            color = "Red",
            sizeCode = "M",
            priceAmount = 100000L,
            stockOnHand = 100,
            stockReserved = 20
        )

        assertEquals(80, variant.stockAvailable)
    }

    @Test
    fun `stockAvailable should not be negative`() {
        val variant = ProductVariant(
            id = UUID.randomUUID(),
            product = mockk(),
            sku = "TEST-SKU",
            color = "Red",
            sizeCode = "M",
            priceAmount = 100000L,
            stockOnHand = 10,
            stockReserved = 20
        )

        assertEquals(0, variant.stockAvailable)
    }
}
