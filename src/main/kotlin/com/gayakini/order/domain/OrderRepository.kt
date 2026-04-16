package com.gayakini.order.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import jakarta.persistence.LockModeType
import java.util.Optional
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByOrderNumber(orderNumber: String): Optional<Order>

    fun findByCheckoutId(checkoutId: UUID): Optional<Order>

    fun findAllByCustomerIdOrderByCreatedAtDesc(customerId: UUID): List<Order>

    fun findAllByOrderByCreatedAtDesc(): List<Order>

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    fun findWithLockById(id: UUID): Optional<Order>

    fun findAllByStatus(
        status: OrderStatus,
        pageable: Pageable,
    ): Page<Order>
}
