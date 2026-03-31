package com.gayakini.order.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "orders")
class Order(
    @Id
    val id: UUID,

    @Column(name = "order_number", unique = true, nullable = false)
    val orderNumber: String,

    @Column(name = "customer_id")
    val customerId: UUID?,

    @Column(name = "guest_token_hash")
    val guestTokenHash: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus,

    @Column(name = "total_amount", nullable = false)
    val totalAmount: Long,

    @Column(name = "shipping_cost", nullable = false)
    val shippingCost: Long,

    @Column(name = "service_fee", nullable = false)
    val serviceFee: Long = 0,

    @Column(name = "grand_total", nullable = false)
    val grandTotal: Long,

    @Column(name = "shipping_address_snapshot", columnDefinition = "JSONB", nullable = false)
    val shippingAddressSnapshot: String,

    @Column(name = "payment_method")
    var paymentMethod: String?,

    @Column(name = "idempotency_key", unique = true)
    val idempotencyKey: String?,

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)

enum class OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    EXPIRED
}
