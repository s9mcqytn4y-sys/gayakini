package com.gayakini.payment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payment_receipts")
class PaymentReceipt(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "provider", nullable = false)
    val provider: String,
    @Column(name = "provider_order_id", nullable = false)
    val providerOrderId: String,
    @Column(name = "transaction_id")
    var transactionId: String? = null,
    @Column(name = "transaction_status", nullable = false)
    val transactionStatus: String,
    @Column(name = "fraud_status")
    var fraudStatus: String? = null,
    @Column(name = "received_signature", nullable = false)
    val receivedSignature: String,
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    val rawPayload: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    var processingStatus: ReceiptProcessingStatus = ReceiptProcessingStatus.PENDING,
    @Column(name = "received_at", nullable = false)
    val receivedAt: Instant = Instant.now(),
    @Column(name = "processed_at")
    var processedAt: Instant? = null,
    @Column(name = "error_message")
    var errorMessage: String? = null,
)

enum class ReceiptProcessingStatus {
    PENDING,
    PROCESSED,
    FAILED,
    SKIPPED,
}

@Repository
interface PaymentReceiptRepository : CrudRepository<PaymentReceipt, UUID> {
    fun findByProviderOrderIdAndTransactionStatusAndProcessingStatus(
        providerOrderId: String,
        transactionStatus: String,
        processingStatus: ReceiptProcessingStatus,
    ): List<PaymentReceipt>

    fun findByProviderOrderId(providerOrderId: String): List<PaymentReceipt>
}
