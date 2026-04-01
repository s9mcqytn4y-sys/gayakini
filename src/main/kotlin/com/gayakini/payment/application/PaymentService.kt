package com.gayakini.payment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.payment.domain.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val paymentProvider: PaymentProvider,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    @Transactional
    fun processMidtransWebhook(
        payload: Map<String, Any>,
        signature: String,
    ) {
        // 1. Verify Signature (Midtrans SHA-512 Signature Key)
        if (!paymentProvider.verifyWebhook(payload, signature)) {
            logger.error("Signature Midtrans tidak valid untuk payload: {}", payload)
            throw IllegalArgumentException("Signature tidak valid")
        }

        // Midtrans order_id can be UUID or custom string
        val orderIdStr = payload["order_id"] as String
        val transactionStatus = payload["transaction_status"] as String

        // 2. Find Order and Payment (lookup by our internal UUID orderId)
        val orderId =
            try {
                UUID.fromString(orderIdStr)
            } catch (e: Exception) {
                null
            }

        val order =
            if (orderId != null) {
                orderRepository.findById(orderId).orElse(null)
            } else {
                // Fallback: in case order_id in Midtrans is orderNumber
                orderRepository.findByOrderNumber(orderIdStr).orElse(null)
            }

        if (order == null) {
            logger.error("Order tidak ditemukan untuk order_id: {}", orderIdStr)
            return
        }

        val payment =
            paymentRepository.findByOrderId(order.id)
                .orElseGet {
                    Payment(
                        id = UUID.randomUUID(),
                        orderId = order.id,
                        externalId = payload["transaction_id"] as String?,
                        provider = "MIDTRANS",
                        status = PaymentStatus.PENDING,
                        amount = order.grandTotal,
                        paymentType = payload["payment_type"] as String?,
                        rawResponse = null,
                    )
                }

        // 3. Update Status Idempotently
        val newStatus = mapMidtransStatus(transactionStatus)

        if (payment.status != newStatus) {
            payment.status = newStatus
            payment.externalId = payload["transaction_id"] as String?
            payment.paymentType = payload["payment_type"] as String?
            payment.rawResponse = objectMapper.writeValueAsString(payload)
            payment.updatedAt = Instant.now()
            paymentRepository.save(payment)

            if (newStatus == PaymentStatus.SETTLED) {
                order.status = OrderStatus.PAID
                order.updatedAt = Instant.now()
                orderRepository.save(order)
            } else if (newStatus == PaymentStatus.CANCELLED || newStatus == PaymentStatus.EXPIRED) {
                order.status = OrderStatus.CANCELLED
                order.updatedAt = Instant.now()
                orderRepository.save(order)
            }
        }
    }

    private fun mapMidtransStatus(status: String): PaymentStatus {
        return when (status) {
            "capture", "settlement" -> PaymentStatus.SETTLED
            "pending" -> PaymentStatus.PENDING
            "deny", "cancel" -> PaymentStatus.CANCELLED
            "expire" -> PaymentStatus.EXPIRED
            "refund" -> PaymentStatus.REFUNDED
            else -> PaymentStatus.PENDING
        }
    }
}
