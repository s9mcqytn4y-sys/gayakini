package com.gayakini.cart.domain

import com.gayakini.catalog.domain.ProductVariant
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class CartTest {
    @Test
    fun `CartItem lineTotalAmount should be calculated correctly`() {
        val cart = mockk<Cart>()
        val variant = mockk<ProductVariant>()

        val item =
            CartItem(
                cart = cart,
                product = mockk(),
                variant = variant,
                productTitleSnapshot = "Test Product",
                skuSnapshot = "SKU-123",
                color = "Red",
                sizeCode = "XL",
                quantity = 3,
                unitPriceAmount = 50000,
            )

        assertEquals(150000L, item.lineTotalAmount)
    }

    @Test
    fun `Cart isNew should work correctly`() {
        val cart = Cart(id = UUID.randomUUID())
        assertTrue(cart.isNew)

        cart.markNotNew()
        assertFalse(cart.isNew)
    }
}
