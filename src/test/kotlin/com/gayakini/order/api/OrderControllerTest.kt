package com.gayakini.order.api

import com.gayakini.BaseWebMvcTest
import com.gayakini.order.application.OrderService
import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.*

@WebMvcTest(OrderController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class OrderControllerTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var orderService: OrderService

    @org.springframework.boot.test.context.TestConfiguration
    class ControllerTestConfig {
        @org.springframework.context.annotation.Bean
        fun orderService(): OrderService = mockk()
    }

    @Test
    fun `placeOrder should return 201`() {
        val checkoutId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val order =
            mockk<Order> {
                every { id } returns orderId
                every { orderNumber } returns "ORD-123"
                every { status } returns OrderStatus.PENDING_PAYMENT
                every { currencyCode } returns "IDR"
                every { subtotalAmount } returns 100000L
                every { shippingCostAmount } returns 10000L
                every { discountAmount } returns 0L
                every { totalAmount } returns 110000L
                every { placedAt } returns Instant.now()
                every { createdAt } returns Instant.now()
                every { customerId } returns UUID.randomUUID()
                every { items } returns mutableListOf()
                every { shippingAddress } returns
                    mockk {
                        every { recipientName } returns "John Doe"
                        every { phone } returns "08123456789"
                        every { line1 } returns "Jl. Sudirman"
                        every { line2 } returns null
                        every { notes } returns null
                        every { areaId } returns "area-1"
                        every { district } returns "District"
                        every { city } returns "City"
                        every { province } returns "Province"
                        every { postalCode } returns "12345"
                        every { countryCode } returns "ID"
                    }
                every { shippingSelection } returns null
                every { paymentStatus } returns com.gayakini.order.domain.PaymentStatus.PENDING
                every { fulfillmentStatus } returns com.gayakini.order.domain.FulfillmentStatus.UNFULFILLED
                every { paidAt } returns null
                every { cancelledAt } returns null
                every { customerNotes } returns null
            }

        every { orderService.placeOrderFromCheckout(checkoutId, any(), any(), any()) } returns order

        mockMvc.post("/v1/checkouts/$checkoutId/orders") {
            header("Idempotency-Key", UUID.randomUUID().toString())
            contentType = MediaType.APPLICATION_JSON
            content = """{ "customerNotes": "Please handle with care" }"""
        }.andExpectStandardResponse(201)
            .andExpect {
                jsonPath("$.data.id") { value(orderId.toString()) }
                jsonPath("$.data.orderNumber") { value("ORD-123") }
            }
    }

    @Test
    fun `getOrderById should return 200`() {
        val orderId = UUID.randomUUID()
        val order =
            mockk<Order> {
                every { id } returns orderId
                every { orderNumber } returns "ORD-123"
                every { status } returns OrderStatus.PENDING_PAYMENT
                every { currencyCode } returns "IDR"
                every { subtotalAmount } returns 100000L
                every { shippingCostAmount } returns 10000L
                every { discountAmount } returns 0L
                every { totalAmount } returns 110000L
                every { placedAt } returns Instant.now()
                every { createdAt } returns Instant.now()
                every { customerId } returns UUID.randomUUID()
                every { items } returns mutableListOf()
                every { shippingAddress } returns
                    mockk {
                        every { recipientName } returns "John Doe"
                        every { phone } returns "08123456789"
                        every { line1 } returns "Jl. Sudirman"
                        every { line2 } returns null
                        every { notes } returns null
                        every { areaId } returns "area-1"
                        every { district } returns "District"
                        every { city } returns "City"
                        every { province } returns "Province"
                        every { postalCode } returns "12345"
                        every { countryCode } returns "ID"
                    }
                every { shippingSelection } returns null
                every { paymentStatus } returns com.gayakini.order.domain.PaymentStatus.PENDING
                every { fulfillmentStatus } returns com.gayakini.order.domain.FulfillmentStatus.UNFULFILLED
                every { paidAt } returns null
                every { cancelledAt } returns null
                every { customerNotes } returns null
            }

        every { orderService.getAuthorizedOrder(orderId, any()) } returns order

        mockMvc.get("/v1/orders/$orderId")
            .andExpectStandardResponse(200)
            .andExpect {
                jsonPath("$.data.id") { value(orderId.toString()) }
            }
    }

    @Test
    fun `listMyOrders should return 401 when not authenticated`() {
        mockMvc.get("/v1/me/orders")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `listMyOrders should return 200 when authenticated`() {
        every { orderService.listOrders(any()) } returns emptyList()

        mockMvc.get("/v1/me/orders") {
            header("Authorization", "Bearer valid-customer-token")
        }.andExpectStandardResponse(200)
            .andExpect {
                jsonPath("$.data") { isArray() }
            }
    }
}
