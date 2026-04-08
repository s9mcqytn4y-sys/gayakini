package com.gayakini.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.payment.domain.CustomerPaymentDetails
import com.gayakini.payment.domain.PaymentItemDetail
import com.gayakini.payment.domain.PaymentGatewayException
import com.gayakini.payment.infrastructure.MidtransPaymentProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.util.*

@RestClientTest(MidtransPaymentProvider::class)
@org.springframework.context.annotation.Import(com.gayakini.infrastructure.config.RestTemplateConfig::class)
@org.springframework.boot.context.properties.EnableConfigurationProperties(GayakiniProperties::class)
@org.springframework.test.context.TestPropertySource(
    properties = [
        "gayakini.midtrans.snap-url=https://app.sandbox.midtrans.com/snap/v1/transactions",
        "gayakini.midtrans.server-key=dummy-server-key",
    ],
)
class PaymentMethodIntegrationTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var provider: MidtransPaymentProvider

    @Autowired
    private lateinit var restTemplateBuilder: org.springframework.boot.web.client.RestTemplateBuilder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var properties: GayakiniProperties

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        val restTemplate = restTemplateBuilder.build()
        server = MockRestServiceServer.createServer(restTemplate)

        // Custom builder that returns the one bound to server
        val mockBuilder = org.mockito.Mockito.mock(org.springframework.boot.web.client.RestTemplateBuilder::class.java)
        org.mockito.Mockito.`when`(mockBuilder.build()).thenReturn(restTemplate)

        provider = MidtransPaymentProvider(properties, mockBuilder, objectMapper)
    }

    private val orderId = UUID.randomUUID()
    private val providerOrderId = "TRX-12345678"
    private val customerDetails =
        CustomerPaymentDetails(
            email = "john@example.com",
            fullName = "John Doe",
            phone = "08123456789",
        )

    @Test
    fun `createPaymentSession - Success with balanced items`() {
        val amount = 100000L
        val items =
            listOf(
                PaymentItemDetail("p1", 50000, 2, "Product 1"),
                PaymentItemDetail("DISCOUNT", -10000, 1, "Discount"),
            )
        // Adjust amount to match items sum: 50000 * 2 - 10000 = 90000
        val correctedAmount = 90000L

        val responseBody =
            mapOf(
                "token" to "mock-token",
                "redirect_url" to "https://mock-url",
            )

        server.expect(requestTo(properties.midtrans.snapUrl))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.transaction_details.order_id").value(providerOrderId))
            .andExpect(jsonPath("$.transaction_details.gross_amount").value(correctedAmount))
            .andExpect(jsonPath("$.item_details[0].price").value(50000))
            .andExpect(jsonPath("$.item_details[1].price").value(-10000))
            .andRespond(withSuccess(objectMapper.writeValueAsString(responseBody), MediaType.APPLICATION_JSON))

        val session = provider.createPaymentSession(orderId, providerOrderId, correctedAmount, customerDetails, items)

        assertEquals("mock-token", session.token)
        assertEquals("https://mock-url", session.redirectUrl)
    }

    @Test
    fun `createPaymentSession - Throws Exception on Math Mismatch`() {
        val amount = 100000L
        val items =
            listOf(
                PaymentItemDetail("p1", 50000, 1, "Product 1"),
            )

        assertThrows<PaymentGatewayException> {
            provider.createPaymentSession(orderId, providerOrderId, amount, customerDetails, items)
        }
    }

    @Test
    fun `createPaymentSession - Handles Midtrans 400 Error`() {
        val amount = 50000L
        val items = listOf(PaymentItemDetail("p1", 50000, 1, "Product 1"))

        server.expect(requestTo(properties.midtrans.snapUrl))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("Invalid payload"))

        val exception =
            assertThrows<PaymentGatewayException> {
                provider.createPaymentSession(orderId, providerOrderId, amount, customerDetails, items)
            }
        assertTrue(exception.message!!.contains("Permintaan pembayaran ditolak"))
    }

    @Test
    fun `createPaymentSession - Handles Midtrans 500 Error`() {
        val amount = 50000L
        val items = listOf(PaymentItemDetail("p1", 50000, 1, "Product 1"))

        server.expect(requestTo(properties.midtrans.snapUrl))
            .andRespond(withServerError())

        val exception =
            assertThrows<PaymentGatewayException> {
                provider.createPaymentSession(orderId, providerOrderId, amount, customerDetails, items)
            }
        assertTrue(exception.message!!.contains("gangguan teknis"))
    }
}
