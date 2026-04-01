package com.gayakini.order.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByOrderNumber(orderNumber: String): Optional<Order>

    fun findByCustomerId(customerId: UUID): List<Order>

    fun findByIdempotencyKey(idempotencyKey: String): Optional<Order>
}

interface OrderItemRepository : JpaRepository<OrderItem, UUID> {
    fun findByOrderId(orderId: UUID): List<OrderItem>
}

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "order_items")
class OrderItem(
    @jakarta.persistence.Id
    val id: UUID,
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "order_id", nullable = false)
    val order: Order,
    @jakarta.persistence.Column(name = "variant_id", nullable = false)
    val variantId: UUID,
    @jakarta.persistence.Column(name = "sku_snapshot", nullable = false)
    val skuSnapshot: String,
    @jakarta.persistence.Column(name = "name_snapshot", nullable = false)
    val nameSnapshot: String,
    @jakarta.persistence.Column(name = "price_snapshot", nullable = false)
    val priceSnapshot: Long,
    @jakarta.persistence.Column(nullable = false)
    val quantity: Int,
    @jakarta.persistence.Column(nullable = false)
    val subtotal: Long,
    @jakarta.persistence.Column(name = "created_at", updatable = false)
    val createdAt: java.time.Instant = java.time.Instant.now(),
)
