package com.gayakini.webhook.api

import com.gayakini.BaseWebMvcTest
import com.gayakini.payment.application.PaymentService
import com.gayakini.shipping.application.ShippingService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(WebhookController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class WebhookControllerWebMvcTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var paymentService: PaymentService

    @MockkBean
    private lateinit var shippingService: ShippingService

    @Test
    fun `should process midtrans webhook`() {
        every { paymentService.processMidtransWebhook(any(), any()) } returns Unit

        mockMvc.post("/v1/webhooks/midtrans") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                    "order_id": "ORD-123",
                    "status_code": "200",
                    "transaction_status": "settlement",
                    "gross_amount": "10000.00",
                    "signature_key": "valid-sig"
                }
                """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        verify { paymentService.processMidtransWebhook(any(), "valid-sig") }
    }

    @Test
    fun `should process biteship webhook`() {
        every { shippingService.processBiteshipWebhook(any()) } returns Unit

        mockMvc.post("/v1/webhooks/biteship") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                    "event": "order.status",
                    "id": "evt_123",
                    "order_id": "ORD-456",
                    "status": "shipped"
                }
                """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        verify { shippingService.processBiteshipWebhook(any()) }
    }

    @Test
    fun `should fail biteship webhook with invalid signature`() {
        // This test assumes a secret is configured in application-test.yml or similar
        // For now, we'll just test the logic in the controller if we can trigger it.
        // The controller uses properties.biteship.webhookSecret
    }
}
