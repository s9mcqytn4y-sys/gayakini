package com.gayakini.payment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.common.api.ForbiddenException
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.common.infrastructure.IdempotencyService
import com.gayakini.common.util.BusinessIdGenerator
import com.gayakini.common.util.HashUtils
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.infrastructure.security.SecurityUtils
import com.gayakini.inventory.application.InventoryService
import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.payment.api.CreatePaymentRequest
import com.gayakini.payment.domain.CustomerPaymentDetails
import com.gayakini.payment.domain.Payment
import com.gayakini.payment.domain.PaymentItemDetail
import com.gayakini.payment.domain.PaymentProvider
import com.gayakini.payment.domain.PaymentReceipt
import com.gayakini.payment.domain.PaymentReceiptRepository
import com.gayakini.payment.domain.PaymentRepository
import com.gayakini.payment.domain.PaymentSettledEvent
import com.gayakini.payment.domain.ReceiptProcessingStatus
import com.gayakini.audit.application.AuditContext
import com.gayakini.audit.domain.AuditEvent
import com.gayakini.finance.application.FinanceService
import com.gayakini.infrastructure.storage.StorageCategory
import com.gayakini.infrastructure.storage.StorageService
import org.springframework.context.ApplicationEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.Instant
import java.util.NoSuchElementException
import java.util.UUID

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentReceiptRepository: PaymentReceiptRepository,
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val inventoryService: InventoryService,
    private val paymentProvider: PaymentProvider,
    private val idempotencyService: IdempotencyService,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: ApplicationEventPublisher,
    private val auditContext: AuditContext,
    private val storageService: StorageService,
    private val financeService: FinanceService,
) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    companion object {
        private const val PAYMENT_EXPIRY_SECONDS = 86400L
        private const val MAX_TITLE_LENGTH = 50
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

            val payment = createNewPaymentSession(order, idempotencyKey, request)

            order.currentPaymentId = payment.id
            order.updatedAt = Instant.now()
            orderRepository.save(order)

            val (actorId, actorRole) = auditContext.getCurrentActor()
            eventPublisher.publishEvent(
                AuditEvent(
                    actorId = actorId,
                    actorRole = actorRole,
                    entityType = "PAYMENT",
                    entityId = payment.transactionNumber,
                    eventType = "PAYMENT_SESSION_CREATED",
                    newState =
                        mapOf(
                            "orderId" to order.id,
                            "paymentId" to payment.id,
                            "amount" to payment.grossAmount,
                            "provider" to payment.provider,
                        ),
                    reason = "Payment session created for order ${order.orderNumber}",
                ),
            )

            payment
        }
    }

    private fun createNewPaymentSession(
        order: Order,
        idempotencyKey: String,
        request: CreatePaymentRequest?,
    ): Payment {
        // Get customer email from order or customer profile
        val customerEmail =
            order.customerId?.let { id ->
                customerRepository.findById(id).map { it.email }.orElse("customer@example.com")
            } ?: "customer@example.com"

        val hash = HashUtils.sha256(idempotencyKey).take(8)
        val providerOrderId = "${order.orderNumber}-$hash"

        val customerDetails =
            CustomerPaymentDetails(
                email = customerEmail,
                fullName = order.shippingAddress?.recipientName ?: "Customer",
                phone = order.shippingAddress?.phone,
            )

        val itemDetails = mutableListOf<PaymentItemDetail>()

        // Products
        order.items.forEach { item ->
            itemDetails.add(
                PaymentItemDetail(
                    id = item.variant.id.toString(),
                    price = item.unitPriceAmount,
                    quantity = item.quantity,
                    name = item.titleSnapshot.take(MAX_TITLE_LENGTH),
                ),
            )
        }

        // Shipping
        if (order.shippingCostAmount > 0) {
            itemDetails.add(
                PaymentItemDetail(
                    id = "SHIPPING",
                    price = order.shippingCostAmount,
                    quantity = 1,
                    name = "Biaya Pengiriman",
                ),
            )
        }

        // Discount (negative price for Midtrans)
        if (order.discountAmount > 0) {
            itemDetails.add(
                PaymentItemDetail(
                    id = "DISCOUNT",
                    price = -order.discountAmount,
                    quantity = 1,
                    name = "Diskon: ${order.promoCode ?: "Promo"}",
                ),
            )
        }

        // Hard requirement: Strict sum validation before calling provider
        val calculatedAmount = itemDetails.sumOf { it.price * it.quantity }
        check(calculatedAmount == order.totalAmount) {
            logger.error(
                "Integritas jumlah pembayaran terganggu: total={}, item_sum={}",
                order.totalAmount,
                calculatedAmount,
            )
            "Ketidaksesuaian perhitungan jumlah pembayaran."
        }

        val enabledMidtransCodes =
            request?.enabledChannels
                ?.flatMap { it.midtransCodes }
                ?.distinct()
        val preferredMidtransCodes = request?.preferredChannel?.midtransCodes

        val session =
            paymentProvider.createPaymentSession(
                orderId = order.id,
                providerOrderId = providerOrderId,
                amount = order.totalAmount,
                customerDetails = customerDetails,
                itemDetails = itemDetails,
                enabledChannels = enabledMidtransCodes,
            )

        val payment =
            Payment(
                transactionNumber = BusinessIdGenerator.generateTransactionNumber(),
                orderId = order.id,
                provider = "MIDTRANS",
                flow = "SNAP",
                status = PaymentStatus.PENDING,
                preferredChannel = preferredMidtransCodes?.firstOrNull(),
                enabledChannels = enabledMidtransCodes?.let { objectMapper.writeValueAsString(it) },
                providerOrderId = providerOrderId,
                grossAmount = order.totalAmount,
                snapToken = session.token,
                snapRedirectUrl = session.redirectUrl,
                expiresAt = Instant.now().plusSeconds(PAYMENT_EXPIRY_SECONDS),
                providerRequestPayload = session.requestPayload,
                providerResponsePayload = session.responsePayload,
            )

        return paymentRepository.save(payment)
    }

    fun validateOrderAccess(
        order: Order,
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
        order: Order,
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
        order: Order,
        orderToken: String?,
    ) {
        val token = orderToken ?: throw UnauthorizedException("Token pesanan diperlukan.")
        if (HashUtils.sha256(token) != order.accessTokenHash) {
            throw UnauthorizedException("Token pesanan tidak valid.")
        }
    }

    @Transactional
    @Suppress("TooGenericExceptionCaught")
    fun processMidtransWebhook(
        payload: Map<String, Any>,
        signature: String,
    ) {
        val providerOrderId = payload["order_id"] as? String
        val transactionStatus = payload["transaction_status"] as? String

        requireNotNull(providerOrderId) { "Missing order_id in payload" }
        requireNotNull(transactionStatus) { "Missing transaction_status in payload" }

        // 1. Audit Trail: Log the incoming webhook immediately
        val receipt =
            PaymentReceipt(
                provider = "MIDTRANS",
                providerOrderId = providerOrderId,
                transactionId = payload["transaction_id"] as? String,
                transactionStatus = transactionStatus,
                fraudStatus = payload["fraud_status"] as? String,
                receivedSignature = signature,
                rawPayload = objectMapper.writeValueAsString(payload),
                processingStatus = ReceiptProcessingStatus.PENDING,
            )
        paymentReceiptRepository.save(receipt)

        try {
            validateAndProcessWebhook(
                receipt = receipt,
                payload = payload,
                signature = signature,
                providerOrderId = providerOrderId,
                transactionStatus = transactionStatus,
            )
        } catch (e: ForbiddenException) {
            // Re-throw ForbiddenException as-is for the controller/advice to handle correctly (403)
            throw e
        } catch (e: Exception) {
            handleWebhookFailure(receipt, providerOrderId, e)
            throw e
        }
    }

    private fun validateAndProcessWebhook(
        receipt: PaymentReceipt,
        payload: Map<String, Any>,
        signature: String,
        providerOrderId: String,
        transactionStatus: String,
    ) {
        // 2. Strict Signature Validation
        if (!paymentProvider.verifyWebhook(payload, signature)) {
            handleInvalidSignature(receipt, providerOrderId)
        }

        // 3. Idempotency Check
        if (isAlreadyProcessed(providerOrderId, transactionStatus)) {
            handleIdempotentSkip(receipt, providerOrderId, transactionStatus)
            return
        }

        // 4. Anti-Spoofing Reconciliation (Authoritative Source of Truth)
        val reconciledStatus = paymentProvider.getPaymentStatus(providerOrderId)
        logger.info(
            "Reconciled status untuk {}: {} (Payload: {})",
            providerOrderId,
            reconciledStatus,
            transactionStatus,
        )

        processAuthoritativeUpdate(providerOrderId, reconciledStatus, payload, transactionStatus)

        // 6. Mark receipt as processed
        receipt.processingStatus = ReceiptProcessingStatus.PROCESSED
        receipt.processedAt = Instant.now()
        paymentReceiptRepository.save(receipt)
    }

    private fun processAuthoritativeUpdate(
        providerOrderId: String,
        reconciledStatus: PaymentStatus,
        payload: Map<String, Any>,
        transactionStatus: String,
    ) {
        val payment =
            paymentRepository.findByProviderOrderId(providerOrderId)
                .orElseThrow {
                    NoSuchElementException("Data pembayaran tidak ditemukan untuk ID: $providerOrderId")
                }

        val order =
            orderRepository.findById(payment.orderId)
                .orElseThrow {
                    NoSuchElementException("Order tidak ditemukan untuk pembayaran: $providerOrderId")
                }

        // 5. Update states if status changed or it's the first authoritative update
        if (payment.status != reconciledStatus || payment.rawProviderStatus != transactionStatus) {
            val previousState =
                mapOf(
                    "status" to payment.status,
                    "rawStatus" to payment.rawProviderStatus,
                )

            updatePaymentAndOrderStates(
                payment = payment,
                order = order,
                reconciledStatus = reconciledStatus,
                payload = payload,
                transactionStatus = transactionStatus,
            )

            eventPublisher.publishEvent(
                AuditEvent(
                    actorId = "SYSTEM",
                    actorRole = "SYSTEM",
                    entityType = "PAYMENT",
                    entityId = payment.transactionNumber,
                    eventType = "PAYMENT_STATUS_UPDATED",
                    previousState = previousState,
                    newState =
                        mapOf(
                            "status" to payment.status,
                            "rawStatus" to payment.rawProviderStatus,
                            "orderStatus" to order.status,
                        ),
                    reason = "Webhook reconciliation: $transactionStatus -> $reconciledStatus",
                ),
            )
        }
    }

    private fun handleInvalidSignature(
        receipt: PaymentReceipt,
        providerOrderId: String,
    ) {
        logger.error("Signature Midtrans tidak valid untuk order: {}", providerOrderId)
        receipt.processingStatus = ReceiptProcessingStatus.FAILED
        receipt.errorMessage = "Signature tidak valid"
        paymentReceiptRepository.save(receipt)
        throw ForbiddenException("Signature tidak valid")
    }

    private fun isAlreadyProcessed(
        providerOrderId: String,
        transactionStatus: String,
    ): Boolean {
        return paymentReceiptRepository
            .findByProviderOrderIdAndTransactionStatusAndProcessingStatus(
                providerOrderId,
                transactionStatus,
                ReceiptProcessingStatus.PROCESSED,
            ).isNotEmpty()
    }

    private fun handleIdempotentSkip(
        receipt: PaymentReceipt,
        providerOrderId: String,
        status: String,
    ) {
        logger.info("Webhook untuk order {} dengan status {} sudah diproses sebelumnya. Skip.", providerOrderId, status)
        receipt.processingStatus = ReceiptProcessingStatus.SKIPPED
        paymentReceiptRepository.save(receipt)
    }

    private fun handleWebhookFailure(
        receipt: PaymentReceipt,
        providerOrderId: String,
        e: Exception,
    ) {
        logger.error("Gagal memproses webhook Midtrans untuk order: {}", providerOrderId, e)
        receipt.processingStatus = ReceiptProcessingStatus.FAILED
        receipt.errorMessage = e.message
        paymentReceiptRepository.save(receipt)
    }

    @Transactional
    fun reconcilePaymentStatus(providerOrderId: String): Payment {
        val reconciledStatus = paymentProvider.getPaymentStatus(providerOrderId)
        val payment =
            paymentRepository.findByProviderOrderId(providerOrderId)
                .orElseThrow {
                    NoSuchElementException("Data pembayaran tidak ditemukan untuk ID: $providerOrderId")
                }

        if (payment.status != reconciledStatus) {
            val order = orderRepository.findById(payment.orderId).orElseThrow()
            updatePaymentAndOrderStates(payment, order, reconciledStatus, emptyMap(), "Manual/Auto Reconciliation")
        }
        return payment
    }

    private fun updatePaymentAndOrderStates(
        payment: Payment,
        order: Order,
        reconciledStatus: PaymentStatus,
        payload: Map<String, Any>,
        transactionStatus: String,
    ) {
        payment.status = reconciledStatus
        payment.providerTransactionId = payload["transaction_id"] as? String
        payment.rawProviderStatus = transactionStatus
        payment.providerResponsePayload = objectMapper.writeValueAsString(payload)
        payment.updatedAt = Instant.now()

        if (reconciledStatus == PaymentStatus.PAID) {
            handlePaidOrder(payment, order)
        } else if (isFailedPaymentStatus(reconciledStatus)) {
            handleFailedOrder(order, reconciledStatus)
        }

        paymentRepository.save(payment)
        order.updatedAt = Instant.now()
        val savedOrder = orderRepository.save(order)

        if (reconciledStatus == PaymentStatus.PAID) {
            eventPublisher.publishEvent(
                PaymentSettledEvent(
                    orderId = savedOrder.id,
                    paymentId = payment.id,
                    transactionNumber = payment.transactionNumber,
                    amount = payment.grossAmount,
                    provider = payment.provider,
                ),
            )

            // Authoritative Finance Posting
            financeService.recordPaymentSettlement(
                transactionId = payment.id,
                orderNumber = savedOrder.orderNumber,
                amount = payment.grossAmount,
                metadata = mapOf("provider" to payment.provider, "providerOrderId" to payment.providerOrderId),
            )
        }
    }

    private fun handlePaidOrder(
        payment: Payment,
        order: Order,
    ) {
        payment.paidAt = Instant.now()
        order.markAsPaid()
    }

    private fun handleFailedOrder(
        order: Order,
        reconciledStatus: PaymentStatus,
    ) {
        order.cancel(
            reason = "Payment failure state: $reconciledStatus",
            paymentStatus = reconciledStatus,
        )

        // Release inventory reservations (Hard Requirement 12)
        inventoryService.releaseReservations(order.id, "Payment failure state: $reconciledStatus")
    }

    private fun isFailedPaymentStatus(status: PaymentStatus): Boolean {
        return status == PaymentStatus.CANCELLED ||
            status == PaymentStatus.EXPIRED ||
            status == PaymentStatus.FAILED
    }

    @Transactional
    fun uploadPaymentProof(
        paymentId: UUID,
        inputStream: InputStream,
        originalFilename: String,
    ): String {
        val payment =
            paymentRepository.findById(paymentId)
                .orElseThrow { NoSuchElementException("Data pembayaran tidak ditemukan.") }

        // Only allow proof upload for pending payments
        check(payment.status == PaymentStatus.PENDING) { "Hanya pembayaran pending yang dapat mengunggah bukti." }

        // Delete old proof if exists
        payment.proofUrl?.let { oldUrl ->
            storageService.delete(oldUrl, StorageCategory.PROOFS)
        }

        val relativePath =
            storageService.store(
                inputStream = inputStream,
                filename = originalFilename,
                category = StorageCategory.PROOFS,
            )

        payment.proofUrl = relativePath
        payment.updatedAt = Instant.now()
        paymentRepository.save(payment)

        val (actorId, actorRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = actorId,
                actorRole = actorRole,
                entityType = "PAYMENT",
                entityId = payment.transactionNumber,
                eventType = "PAYMENT_PROOF_UPLOADED",
                newState = mapOf("proofUrl" to relativePath),
                reason = "Payment proof uploaded for order ${payment.orderId}",
            ),
        )

        return relativePath
    }
}
