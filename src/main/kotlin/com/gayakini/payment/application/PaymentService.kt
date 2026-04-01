package com.gayakini.payment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.common.util.HashUtils
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.payment.api.CreatePaymentRequest
import com.gayakini.payment.domain.CustomerPaymentDetails
import com.gayakini.payment.domain.Payment
import com.gayakini.payment.domain.PaymentProvider
import com.gayakini.payment.domain.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.NoSuchElementException
import java.util.UUID

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val paymentProvider: PaymentProvider,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    @Transactional
    fun createPaymentSession(
        orderId: UUID,
        idempotencyKey: String,
        orderToken: String?,
        request: CreatePaymentRequest?,
    ): Payment {
        val order =
            orderRepository.findById(orderId)
                .orElseThrow { NoSuchElementException("Order tidak ditemukan.") }

        if (order.status != OrderStatus.PENDING_PAYMENT) {
            throw IllegalStateException("Order tidak dalam status menunggu pembayaran.")
        }

        // Validate token if guest
        if (order.accessTokenHash != null) {
            if (HashUtils.sha256(orderToken ?: "") != order.accessTokenHash) {
                throw IllegalStateException("Akses order ditolak.")
            }
        }

        // Return existing pending payment if any
        val existingPayment =
            paymentRepository.findByOrderId(order.id)
                .filter { it.status == PaymentStatus.PENDING && it.expiresAt?.isAfter(Instant.now()) == true }

        if (existingPayment.isPresent) {
            return existingPayment.get()
        }

        // Create new payment session with provider
        val providerOrderId = "${order.orderNumber}-${System.currentTimeMillis()}"

        val customerDetails =
            CustomerPaymentDetails(
                email = "customer@example.com", // TODO: Get from order/customer
                fullName = order.shippingAddress?.recipientName ?: "Customer",
                phone = order.shippingAddress?.phone,
            )

        val session =
            paymentProvider.createPaymentSession(
                orderId = order.id,
                providerOrderId = providerOrderId,
                amount = order.totalAmount,
                customerDetails = customerDetails,
            )

        val payment =
            Payment(
                orderId = order.id,
                providerOrderId = providerOrderId,
                grossAmount = order.totalAmount,
                status = PaymentStatus.PENDING,
                snapToken = session.token,
                snapRedirectUrl = session.redirectUrl,
                expiresAt = Instant.now().plusSeconds(86400), // 24 hours
            )

        order.currentPaymentId = payment.id
        orderRepository.save(order)

        return paymentRepository.save(payment)
    }

    @Transactional
    fun processMidtransWebhook(
        payload: Map<String, Any>,
        signature: String,
    ) {
        if (!paymentProvider.verifyWebhook(payload, signature)) {
            logger.error("Signature Midtrans tidak valid untuk payload: {}", payload)
            throw IllegalArgumentException("Signature tidak valid")
        }

        val providerOrderId = payload["order_id"] as String
        val transactionStatus = payload["transaction_status"] as String

        val payment =
            paymentRepository.findByProviderOrderId(providerOrderId)
                .orElseThrow { NoSuchElementException("Data pembayaran tidak ditemukan untuk ID: $providerOrderId") }

        val order =
            orderRepository.findById(payment.orderId)
                .orElseThrow { NoSuchElementException("Order tidak ditemukan untuk pembayaran: $providerOrderId") }

        val newStatus = mapMidtransStatus(transactionStatus)

        if (payment.status != newStatus) {
            payment.status = newStatus
            payment.providerTransactionId = payload["transaction_id"] as? String
            payment.rawProviderStatus = transactionStatus
            payment.updatedAt = Instant.now()

            if (newStatus == PaymentStatus.PAID) {
                payment.paidAt = Instant.now()
                order.status = OrderStatus.PAID
                order.paymentStatus = PaymentStatus.PAID
                order.paidAt = Instant.now()
            } else if (newStatus == PaymentStatus.CANCELLED || newStatus == PaymentStatus.EXPIRED || newStatus == PaymentStatus.FAILED) {
                order.status = OrderStatus.CANCELLED
                order.paymentStatus = newStatus
                order.cancelledAt = Instant.now()
            }

            paymentRepository.save(payment)
            orderRepository.save(order)
        }
    }

    private fun mapMidtransStatus(status: String): PaymentStatus {
        return when (status) {
            "capture", "settlement" -> PaymentStatus.PAID
            "pending" -> PaymentStatus.PENDING
            "deny", "cancel" -> PaymentStatus.CANCELLED
            "expire" -> PaymentStatus.EXPIRED
            "refund" -> PaymentStatus.REFUNDED
            else -> PaymentStatus.FAILED
        }
    }
}
