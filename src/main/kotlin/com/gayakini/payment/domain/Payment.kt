package com.gayakini.payment.domain

import com.gayakini.order.domain.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payments", schema = "commerce")
@EntityListeners(AuditingEntityListener::class)
class Payment(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "transaction_number", unique = true, length = 50)
    val transactionNumber: String,
    @Column(name = "order_id", nullable = false)
    val orderId: UUID,
    @Column(nullable = false)
    val provider: String = "MIDTRANS",
    @Column(nullable = false)
    val flow: String = "SNAP",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,
    @Column(name = "proof_url")
    var proofUrl: String? = null,
    @Column(name = "preferred_channel")
    var preferredChannel: String? = null,
    @Column(name = "enabled_channels", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    var enabledChannels: String? = null,
    @Column(name = "provider_order_id", nullable = false, unique = true)
    val providerOrderId: String,
    @Column(name = "provider_transaction_id", unique = true)
    var providerTransactionId: String? = null,
    @Column(name = "gross_amount", nullable = false)
    val grossAmount: Long,
    @Column(name = "raw_provider_status")
    var rawProviderStatus: String? = null,
    @Column(name = "provider_request_payload", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    var providerRequestPayload: String? = null,
    @Column(name = "provider_response_payload", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    var providerResponsePayload: String? = null,
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
) {
    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    var createdBy: UUID? = null

    @LastModifiedBy
    @Column(name = "updated_by")
    var updatedBy: UUID? = null
}
