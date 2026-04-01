package com.gayakini.payment.domain

import com.gayakini.order.domain.PaymentStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payments", schema = "commerce")
class Payment(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "order_id", nullable = false)
    val orderId: UUID,
    @Column(nullable = false)
    val provider: String = "MIDTRANS",
    @Column(nullable = false)
    val flow: String = "SNAP",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,
    @Column(name = "preferred_channel")
    var preferredChannel: String? = null,
    @Column(name = "enabled_channels", columnDefinition = "JSONB")
    var enabledChannels: String? = null,
    @Column(name = "provider_order_id", nullable = false, unique = true)
    val providerOrderId: String,
    @Column(name = "provider_transaction_id", unique = true)
    var providerTransactionId: String? = null,
    @Column(name = "gross_amount", nullable = false)
    val grossAmount: Long,
    @Column(name = "raw_provider_status")
    var rawProviderStatus: String? = null,
    @Column(name = "snap_token")
    var snapToken: String? = null,
    @Column(name = "snap_redirect_url")
    var snapRedirectUrl: String? = null,
    @Column(name = "expires_at")
    var expiresAt: Instant? = null,
    @Column(name = "paid_at")
    var paidAt: Instant? = null,
    @Column(name = "failed_at")
    var failedAt: Instant? = null,
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,
    @Column(name = "expired_at")
    var expiredAt: Instant? = null,
    @Column(name = "refunded_at")
    var refundedAt: Instant? = null,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
