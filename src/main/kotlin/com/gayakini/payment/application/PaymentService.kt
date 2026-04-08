package com.gayakini.payment.application

import com.gayakini.common.api.ForbiddenException
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.common.infrastructure.IdempotencyService
import com.gayakini.common.util.HashUtils
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.infrastructure.security.SecurityUtils
import com.gayakini.inventory.application.InventoryService
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
    private val customerRepository: CustomerRepository,
    private val inventoryService: InventoryService,
    private val paymentProvider: PaymentProvider,
    private val idempotencyService: IdempotencyService,
) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    companion object {
        private const val PAYMENT_EXPIRY_SECONDS = 86400L
    }

    @Transactional
    fun createPaymentSession(
        orderId: UUID,
        idempotencyKey: String,
        orderToken: String?,
        request: CreatePaymentRequest?,
    ): Payment {
        val currentUserId = SecurityUtils.getCurrentUserId()
        val order =
            orderRepository.findById(orderId)
                .orElseThrow { NoSuchElementException("Order tidak ditemukan.") }

        return idempotencyService.handle(
            scope = "create_payment",
            key = idempotencyKey,
            requestPayload = request ?: emptyMap<String, String>(),
            requesterType = if (currentUserId != null) "CUSTOMER" else "GUEST",
            requesterId = currentUserId,
        ) {
            validateOrderAccess(order, orderToken, currentUserId)

            check(order.status == OrderStatus.PENDING_PAYMENT) { "Order tidak dalam status menunggu pembayaran." }

            // Return existing pending payment if any
            val existingPayment =
                paymentRepository.findByOrderId(order.id)
                    .filter { it.status == PaymentStatus.PENDING && it.expiresAt?.isAfter(Instant.now()) == true }

            if (existingPayment.isPresent) {
                return@handle existingPayment.get()
            }

            val payment = createNewPaymentSession(order)

            order.currentPaymentId = payment.id
            order.updatedAt = Instant.now()
            orderRepository.save(order)

            payment
        }
    }

    private fun createNewPaymentSession(order: com.gayakini.order.domain.Order): Payment {
        // Get customer email from order or customer profile
        val customerEmail =
            order.customerId?.let { id ->
                customerRepository.findById(id).map { it.email }.orElse("customer@example.com")
            } ?: "customer@example.com"

        val providerOrderId = "${order.orderNumber}-${Instant.now().toEpochMilli()}"

        val customerDetails =
            CustomerPaymentDetails(
                email = customerEmail,
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
                expiresAt = Instant.now().plusSeconds(PAYMENT_EXPIRY_SECONDS),
            )

        return paymentRepository.save(payment)
    }

    fun validateOrderAccess(
        order: com.gayakini.order.domain.Order,
        orderToken: String?,
        currentUserId: UUID?,
    ) {
        if (order.customerId != null) {
            validateCustomerOrderAccess(order, currentUserId)
            return
        }

        if (order.accessTokenHash != null) {
            validateGuestOrderAccess(order, orderToken)
        }
    }

    private fun validateCustomerOrderAccess(
        order: com.gayakini.order.domain.Order,
        currentUserId: UUID?,
    ) {
        if (order.customerId != currentUserId) {
            if (currentUserId == null) {
                throw UnauthorizedException("Silakan login untuk mengakses pesanan ini.")
            }
            throw ForbiddenException("Akses pesanan ditolak.")
        }
    }

    private fun validateGuestOrderAccess(
        order: com.gayakini.order.domain.Order,
        orderToken: String?,
    ) {
        val token = orderToken ?: throw UnauthorizedException("Token pesanan diperlukan.")
        if (HashUtils.sha256(token) != order.accessTokenHash) {
            throw UnauthorizedException("Token pesanan tidak valid.")
        }
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

        val reconciledStatus = paymentProvider.getPaymentStatus(providerOrderId)

        val payment =
            paymentRepository.findByProviderOrderId(providerOrderId)
                .orElseThrow { NoSuchElementException("Data pembayaran tidak ditemukan untuk ID: $providerOrderId") }

        val order =
            orderRepository.findById(payment.orderId)
                .orElseThrow { NoSuchElementException("Order tidak ditemukan untuk pembayaran: $providerOrderId") }

        if (payment.status != reconciledStatus) {
            updatePaymentAndOrderStates(payment, order, reconciledStatus, payload, transactionStatus)
        }
    }

    private fun updatePaymentAndOrderStates(
        payment: Payment,
        order: com.gayakini.order.domain.Order,
        reconciledStatus: PaymentStatus,
        payload: Map<String, Any>,
        transactionStatus: String,
    ) {
        payment.status = reconciledStatus
        payment.providerTransactionId = payload["transaction_id"] as? String
        payment.rawProviderStatus = transactionStatus
        payment.updatedAt = Instant.now()

        if (reconciledStatus == PaymentStatus.PAID) {
            handlePaidOrder(payment, order)
        } else if (isFailedPaymentStatus(reconciledStatus)) {
            handleFailedOrder(order, reconciledStatus)
        }

        paymentRepository.save(payment)
        order.updatedAt = Instant.now()
        orderRepository.save(order)
    }

    private fun handlePaidOrder(
        payment: Payment,
        order: com.gayakini.order.domain.Order,
    ) {
        payment.paidAt = Instant.now()
        order.status = OrderStatus.PAID
        order.paymentStatus = PaymentStatus.PAID
        order.paidAt = Instant.now()
    }

    private fun handleFailedOrder(
        order: com.gayakini.order.domain.Order,
        reconciledStatus: PaymentStatus,
    ) {
        order.status = OrderStatus.CANCELLED
        order.paymentStatus = reconciledStatus
        order.cancelledAt = Instant.now()

        // Release inventory reservations (Hard Requirement 12)
        inventoryService.releaseReservations(order.id, "Payment failure state: $reconciledStatus")
    }

    private fun isFailedPaymentStatus(status: PaymentStatus): Boolean {
        return status == PaymentStatus.CANCELLED ||
            status == PaymentStatus.EXPIRED ||
            status == PaymentStatus.FAILED
    }
}
