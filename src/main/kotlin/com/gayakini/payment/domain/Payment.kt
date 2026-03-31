package com.gayakini.payment.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payments")
class Payment(
    @Id
    val id: UUID,

    @Column(name = "order_id", nullable = false)
    val orderId: UUID,

    @Column(name = "external_id")
    var externalId: String?,

    @Column(nullable = false)
    val provider: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus,

    @Column(nullable = false)
    val amount: Long,

    @Column(name = "payment_type")
    var paymentType: String?,

    @Column(name = "raw_response", columnDefinition = "JSONB")
    var rawResponse: String?,

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
