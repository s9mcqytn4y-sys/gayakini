package com.gayakini.order.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByOrderNumber(orderNumber: String): Optional<Order>

    fun findByCustomerId(customerId: UUID): List<Order>

    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.checkoutId = :checkoutId")
    fun findByCheckoutId(checkoutId: UUID): Optional<Order>
}

interface OrderItemRepository : JpaRepository<OrderItem, UUID> {
    fun findByOrderId(orderId: UUID): List<OrderItem>
}
