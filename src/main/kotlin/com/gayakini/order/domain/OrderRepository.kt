package com.gayakini.order.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByOrderNumber(orderNumber: String): Optional<Order>

    fun findByCheckoutId(checkoutId: UUID): Optional<Order>

    fun findAllByCustomerIdOrderByCreatedAtDesc(customerId: UUID): List<Order>

    fun findAllByOrderByCreatedAtDesc(): List<Order>
}
