package com.gayakini.payment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.audit.application.AuditContext
import com.gayakini.common.infrastructure.IdempotencyService
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.finance.application.FinanceService
import com.gayakini.infrastructure.security.SecurityUtils
import com.gayakini.infrastructure.storage.StorageService
import com.gayakini.inventory.application.InventoryService
import com.gayakini.order.domain.*
import com.gayakini.audit.domain.AuditEvent
import com.gayakini.payment.domain.*
import com.gayakini.payment.domain.PaymentSettledEvent
import com.gayakini.promo.application.PromoService
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.*

class PaymentServiceUnitTest {

    private val paymentRepository = mockk<PaymentRepository>()
    private val paymentReceiptRepository = mockk<PaymentReceiptRepository>()
    private val orderRepository = mockk<OrderRepository>()
    private val customerRepository = mockk<CustomerRepository>()
    private val inventoryService = mockk<InventoryService>()
    private val promoService = mockk<PromoService>()
    private val paymentProvider = mockk<PaymentProvider>()
    private val idempotencyService = mockk<IdempotencyService>()
    private val objectMapper = ObjectMapper()
    private val eventPublisher = mockk<ApplicationEventPublisher>()
    private val auditContext = mockk<AuditContext>()
    private val storageService = mockk<StorageService>()
    private val financeService = mockk<FinanceService>()
    private val meterRegistry = mockk<MeterRegistry>(relaxed = true)

    private val paymentService = PaymentService(
        paymentRepository,
        paymentReceiptRepository,
        orderRepository,
        customerRepository,
        inventoryService,
        promoService,
        paymentProvider,
        idempotencyService,
        objectMapper,
        eventPublisher,
        auditContext,
        storageService,
        financeService,
        meterRegistry
    )

    @BeforeEach
    fun setup() {
        mockkObject(SecurityUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SecurityUtils)
    }

    @Test
    fun `validateOrderAccess should allow owner of customer order`() {
        val customerId = UUID.randomUUID()
        val order = mockk<Order>()
        every { order.customerId } returns customerId
        every { SecurityUtils.getCurrentUserId() } returns customerId

        paymentService.validateOrderAccess(order, null, customerId)
        // No exception thrown
    }

    @Test
    fun `validateOrderAccess should allow guest with valid token`() {
        val orderToken = "valid-token"
        val tokenHash = com.gayakini.common.util.HashUtils.sha256(orderToken)
        val order = mockk<Order>()
        every { order.customerId } returns null
        every { order.accessTokenHash } returns tokenHash

        paymentService.validateOrderAccess(order, orderToken, null)
        // No exception thrown
    }

    @Test
    fun `createNewPaymentSession should sum items correctly and call provider`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()

        val order = mockk<Order>()
        val item = mockk<OrderItem>()

        every { order.id } returns orderId
        every { order.orderNumber } returns "ORD-123"
        every { order.customerId } returns customerId
        every { order.status } returns OrderStatus.PENDING_PAYMENT
        every { order.totalAmount } returns 150000L
        every { order.shippingCostAmount } returns 50000L
        every { order.discountAmount } returns 0L
        every { order.promoCode } returns null
        every { order.items } returns mutableListOf(item)
        every { order.shippingAddress } returns null

        every { item.variant.id } returns UUID.randomUUID()
        every { item.unitPriceAmount } returns 100000L
        every { item.quantity } returns 1
        every { item.titleSnapshot } returns "Product A"

        every { orderRepository.findById(orderId) } returns Optional.of(order)
        every { paymentRepository.findByOrderId(orderId) } returns Optional.empty()
        every { SecurityUtils.getCurrentUserId() } returns customerId
        every { customerRepository.findById(customerId) } returns Optional.empty()

        val mockSession = mockk<PaymentSession>()
        every { mockSession.token } returns "snap-123"
        every { mockSession.redirectUrl } returns "http://snap"
        every { mockSession.requestPayload } returns "{}"
        every { mockSession.responsePayload } returns "{}"

        every { paymentProvider.createPaymentSession(any(), any(), any(), any(), any(), any()) } returns mockSession
        every { paymentRepository.save(any()) } answers { it.invocation.args[0] as Payment }

        // Mocking behavior for Order object that is being updated
        every { order.currentPaymentId = any() } just runs
        every { order.updatedAt = any() } just runs
        every { orderRepository.save(order) } returns order

        every { auditContext.getCurrentActor() } returns ("SYSTEM" to "SYSTEM")
        every { eventPublisher.publishEvent(any<com.gayakini.audit.domain.AuditEvent>()) } returns Unit

        every { idempotencyService.handle<Payment>(any(), any(), any(), any(), any(), any(), any()) } answers {
            val block = invocation.args[6] as () -> Payment
            block()
        }

        val payment = paymentService.createPaymentSession(orderId, "key", null, null)

        assertEquals(150000L, payment.grossAmount)
        assertEquals("snap-123", payment.snapToken)
        verify { paymentProvider.createPaymentSession(orderId, any(), 150000L, any(), any(), any()) }
    }

    @Test
    fun `processMidtransWebhook should handle invalid signature`() {
        val payload = mapOf("order_id" to "ORD-123", "transaction_status" to "settlement")
        val signature = "invalid"

        every { paymentReceiptRepository.save(any()) } answers { it.invocation.args[0] as PaymentReceipt }
        every { paymentProvider.verifyWebhook(payload, signature) } returns false

        org.junit.jupiter.api.assertThrows<com.gayakini.common.api.ForbiddenException> {
            paymentService.processMidtransWebhook(payload, signature)
        }

        verify { paymentReceiptRepository.save(match { it.processingStatus == ReceiptProcessingStatus.FAILED }) }
    }

    @Test
    fun `processMidtransWebhook should return early if payment already processed`() {
        val providerOrderId = "ORD-123-hash"
        val payload = mapOf(
            "order_id" to providerOrderId,
            "transaction_status" to "settlement",
            "transaction_id" to "mid-123"
        )
        val signature = "valid"

        val payment = Payment(
            transactionNumber = "PAY-OLD",
            orderId = UUID.randomUUID(),
            providerOrderId = providerOrderId,
            grossAmount = 100000L,
            status = PaymentStatus.PAID // Already paid
        )

        every { paymentReceiptRepository.save(any()) } answers { it.invocation.args[0] as PaymentReceipt }
        every { paymentProvider.verifyWebhook(payload, signature) } returns true
        every { paymentProvider.getPaymentStatus(providerOrderId) } returns PaymentStatus.PAID
        every { paymentReceiptRepository.findByProviderOrderIdAndTransactionStatusAndProcessingStatus(any(), any(), any()) } returns listOf(mockk())
        every { paymentRepository.findByProviderOrderId(providerOrderId) } returns Optional.of(payment)

        paymentService.processMidtransWebhook(payload, signature)

        verify(exactly = 0) { orderRepository.findById(any()) }
        verify { paymentReceiptRepository.save(match { it.processingStatus == ReceiptProcessingStatus.SKIPPED }) }
    }

    @Test
    fun `processMidtransWebhook should reconcile with provider and update order to paid`() {
        val providerOrderId = "ORD-123-hash"
        val payload = mapOf(
            "order_id" to providerOrderId,
            "transaction_status" to "settlement",
            "transaction_id" to "mid-123"
        )
        val signature = "valid"
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val payment = Payment(
            id = paymentId,
            orderId = orderId,
            transactionNumber = "PAY-123",
            providerOrderId = providerOrderId,
            grossAmount = 100000L,
            status = PaymentStatus.PENDING
        )
        val order = Order(
            id = orderId,
            orderNumber = "ORD-123",
            checkoutId = UUID.randomUUID(),
            cartId = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            accessTokenHash = null,
            status = OrderStatus.PENDING_PAYMENT,
            subtotalAmount = 100000L,
            shippingCostAmount = 0L,
            discountAmount = 0L
        )
        val item = mockk<OrderItem>()
        order.items.add(item)

        every { paymentReceiptRepository.save(any()) } answers { it.invocation.args[0] as PaymentReceipt }
        every { paymentProvider.verifyWebhook(payload, signature) } returns true
        every { paymentReceiptRepository.findByProviderOrderIdAndTransactionStatusAndProcessingStatus(any(), any(), any()) } returns emptyList()
        every { paymentProvider.getPaymentStatus(providerOrderId) } returns PaymentStatus.PAID
        every { paymentRepository.findByProviderOrderId(providerOrderId) } returns Optional.of(payment)
        every { orderRepository.findById(orderId) } returns Optional.of(order)

        every { item.id } returns UUID.randomUUID()
        every { inventoryService.consumeReservation(any()) } just runs

        every { paymentRepository.save(any()) } answers { it.invocation.args[0] as Payment }
        every { orderRepository.save(order) } returns order

        every { auditContext.getCurrentActor() } returns ("SYSTEM" to "SYSTEM")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit
        every { eventPublisher.publishEvent(any<PaymentSettledEvent>()) } returns Unit
        every { financeService.recordPaymentSettlement(any(), any(), any(), any()) } just runs

        paymentService.processMidtransWebhook(payload, signature)

        assertEquals(PaymentStatus.PAID, payment.status)
        assertEquals(OrderStatus.PAID, order.status)
        verify { inventoryService.consumeReservation(any()) }
        verify { financeService.recordPaymentSettlement(paymentId, "ORD-123", 100000L, any()) }
    }
}
