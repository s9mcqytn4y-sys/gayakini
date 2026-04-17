package com.gayakini.order.api

import com.gayakini.BaseWebMvcTest
import com.gayakini.order.application.OrderService
import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderStatus
import com.gayakini.shipping.application.ShippingService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant
import java.util.*

@WebMvcTest(AdminOrderController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class AdminOrderControllerTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var shippingService: ShippingService

    @org.springframework.boot.test.context.TestConfiguration
    class ControllerTestConfig {
        @org.springframework.context.annotation.Bean
        fun orderService(): OrderService = mockk()

        @org.springframework.context.annotation.Bean
        fun shippingService(): ShippingService = mockk()
    }

    @Test
    fun `listOrders should return 403 for customers`() {
        mockMvc.get("/v1/admin/orders") {
            header("Authorization", "Bearer valid-customer-token")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `listOrders should return 200 for admins`() {
        every { orderService.listOrdersForAdmin(any(), any(), any(), any(), any()) } returns PageImpl(mutableListOf())

        mockMvc.get("/v1/admin/orders") {
            header("Authorization", "Bearer valid-admin-token")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `getOrder should return 200 for admins`() {
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

        every { orderService.getOrder(orderId) } returns order
        every { shippingService.findShipmentByOrderId(orderId) } returns null

        mockMvc.get("/v1/admin/orders/$orderId") {
            header("Authorization", "Bearer valid-admin-token")
        }.andExpectStandardResponse(200)
            .andExpect {
                jsonPath("$.data.id") { value(orderId.toString()) }
            }
    }
}
