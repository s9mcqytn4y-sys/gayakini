package com.gayakini.checkout.domain

import com.gayakini.cart.domain.Cart
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import io.mockk.mockk
import java.util.*

class CheckoutTest {

    private fun createTestCheckout(
        subtotal: Long = 100000,
        shipping: Long = 10000,
        discount: Long = 0
    ): Checkout {
        return Checkout(
            id = UUID.randomUUID(),
            cart = mockk<Cart>(),
            subtotalAmount = subtotal,
            shippingCostAmount = shipping,
            discountAmount = discount
        )
    }

    @Test
    fun `totalAmount should be calculated correctly`() {
        val checkout = createTestCheckout(subtotal = 100000, shipping = 15000, discount = 5000)
        assertEquals(110000L, checkout.totalAmount)
    }

    @Test
    fun `isNew should be true for new checkout and false after marking`() {
        val checkout = createTestCheckout()
        assertTrue(checkout.isNew)

        checkout.markNotNew()
        assertFalse(checkout.isNew)
    }
}
