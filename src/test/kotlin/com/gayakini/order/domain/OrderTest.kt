package com.gayakini.order.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class OrderTest {
    private fun createTestOrder(
        status: OrderStatus = OrderStatus.PENDING_PAYMENT,
        subtotal: Long = 100000,
        shipping: Long = 10000,
        discount: Long = 0,
    ): Order {
        return Order(
            orderNumber = "ORD-123",
            checkoutId = UUID.randomUUID(),
            cartId = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            accessTokenHash = null,
            status = status,
            subtotalAmount = subtotal,
            shippingCostAmount = shipping,
            discountAmount = discount,
        )
    }

    @Test
    fun `totalAmount should be calculated correctly`() {
        val order = createTestOrder(subtotal = 100000, shipping = 15000, discount = 5000)
        assertEquals(110000L, order.totalAmount)
    }

    @Test
    fun `markAsPaid should transition status correctly`() {
        val order = createTestOrder(status = OrderStatus.PENDING_PAYMENT)
        order.markAsPaid()

        assertEquals(OrderStatus.PAID, order.status)
        assertEquals(PaymentStatus.PAID, order.paymentStatus)
        assertNotNull(order.paidAt)
    }

    @Test
    fun `markAsPaid should throw if current status is not PENDING_PAYMENT`() {
        val order = createTestOrder(status = OrderStatus.SHIPPED)
        val exception =
            assertThrows<IllegalStateException> {
                order.markAsPaid()
            }
        assertTrue(exception.message!!.contains("Transisi status pesanan tidak valid"))
    }

    @Test
    fun `cancel should transition to CANCELLED and set reason`() {
        val order = createTestOrder(status = OrderStatus.PAID)
        order.cancel(reason = "User requested", paymentStatus = PaymentStatus.REFUNDED)

        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(PaymentStatus.REFUNDED, order.paymentStatus)
        assertEquals(FulfillmentStatus.CANCELLED, order.fulfillmentStatus)
        assertEquals("User requested", order.cancellationReason)
        assertNotNull(order.cancelledAt)
    }

    @Test
    fun `OrderStatus canTransitionTo should enforce state machine`() {
        assertTrue(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PAID))
        assertTrue(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.CANCELLED))
        assertFalse(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.SHIPPED))

        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CANCELLED))
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAID))
    }
}
