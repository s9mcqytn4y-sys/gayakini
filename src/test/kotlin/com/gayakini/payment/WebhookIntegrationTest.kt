package com.gayakini.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.inventory.application.InventoryService
import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.payment.domain.Payment
import com.gayakini.payment.domain.PaymentProvider
import com.gayakini.payment.domain.PaymentRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.Optional
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var paymentRepository: PaymentRepository

    @MockkBean
    private lateinit var orderRepository: OrderRepository

    @MockkBean
    private lateinit var paymentProvider: PaymentProvider

    @MockkBean
    private lateinit var inventoryService: InventoryService

    private val providerOrderId = "TRX-123"
    private val orderId = UUID.randomUUID()
    private val checkoutId = UUID.randomUUID()
    private val cartId = UUID.randomUUID()

    private fun createDummyOrder(): Order {
        return Order(
            id = orderId,
            orderNumber = "ORD-001",
            checkoutId = checkoutId,
            cartId = cartId,
            customerId = null,
            accessTokenHash = null,
            status = OrderStatus.PENDING_PAYMENT,
            paymentStatus = PaymentStatus.PENDING,
            subtotalAmount = 10000,
            shippingCostAmount = 0,
            discountAmount = 0,
            placedAt = Instant.now(),
        )
    }

    private fun createDummyPayment(): Payment {
        return Payment(
            id = UUID.randomUUID(),
            transactionNumber = "PAY-001",
            orderId = orderId,
            provider = "MIDTRANS",
            flow = "SNAP",
            status = PaymentStatus.PENDING,
            providerOrderId = providerOrderId,
            grossAmount = 10000,
        )
    }

    @Test
    fun `receiveMidtransWebhook - Success Settlement`() {
        val payload =
            mapOf(
                "order_id" to providerOrderId,
                "status_code" to "200",
                "transaction_status" to "settlement",
                "gross_amount" to "10000.00",
                "signature_key" to "valid-signature",
                "transaction_id" to "midtrans-tx-123",
            )

        val payment = createDummyPayment()
        val order = createDummyOrder()

        every { paymentProvider.verifyWebhook(any(), any()) } returns true
        every { paymentRepository.findByProviderOrderId(providerOrderId) } returns Optional.of(payment)
        every { orderRepository.findById(orderId) } returns Optional.of(order)
        every { paymentProvider.getPaymentStatus(providerOrderId) } returns PaymentStatus.PAID
        every { paymentRepository.save(any()) } returns payment
        every { orderRepository.save(any()) } returns order

        mockMvc
            .post("/v1/webhooks/midtrans") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(payload)
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.accepted") { value(true) }
            }

        verify { orderRepository.save(match { it.status == OrderStatus.PAID }) }
        verify { paymentRepository.save(match { it.status == PaymentStatus.PAID }) }
    }

    @Test
    fun `receiveMidtransWebhook - Invalid Signature`() {
        val payload =
            mapOf(
                "order_id" to providerOrderId,
                "status_code" to "200",
                "transaction_status" to "settlement",
                "gross_amount" to "10000.00",
                "signature_key" to "invalid-signature",
            )

        every { paymentProvider.verifyWebhook(any(), any()) } returns false

        mockMvc
            .post("/v1/webhooks/midtrans") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(payload)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `receiveMidtransWebhook - Payment Failure Triggers Inventory Release`() {
        val payload =
            mapOf(
                "order_id" to providerOrderId,
                "status_code" to "202",
                "transaction_status" to "cancel",
                "gross_amount" to "10000.00",
                "signature_key" to "valid-signature",
            )

        val payment = createDummyPayment()
        val order = createDummyOrder()

        every { paymentProvider.verifyWebhook(any(), any()) } returns true
        every { paymentRepository.findByProviderOrderId(providerOrderId) } returns Optional.of(payment)
        every { orderRepository.findById(orderId) } returns Optional.of(order)
        every { paymentProvider.getPaymentStatus(providerOrderId) } returns PaymentStatus.CANCELLED
        every { inventoryService.releaseReservations(any(), any()) } returns Unit
        every { paymentRepository.save(any()) } returns payment
        every { orderRepository.save(any()) } returns order

        mockMvc
            .post("/v1/webhooks/midtrans") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(payload)
            }.andExpect {
                status { isOk() }
            }

        verify { inventoryService.releaseReservations(orderId, any()) }
        verify { orderRepository.save(match { it.status == OrderStatus.CANCELLED }) }
    }
}
