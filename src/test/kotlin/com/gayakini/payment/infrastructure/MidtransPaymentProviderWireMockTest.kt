package com.gayakini.payment.infrastructure

import com.gayakini.BaseWireMockTest
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.payment.domain.CustomerPaymentDetails
import com.gayakini.payment.domain.PaymentItemDetail
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

@WireMockTest(httpPort = 8089)
class MidtransPaymentProviderWireMockTest : BaseWireMockTest() {
    @Autowired
    private lateinit var paymentProvider: MidtransPaymentProvider

    @BeforeEach
    fun setup() {
        WireMock.configureFor("localhost", 8089)
    }

    @Test
    fun `should create payment session successfully (wiremocked)`() {
        // Given
        val orderId = UUID.randomUUID()
        val providerOrderId = "TEST-${System.currentTimeMillis()}"

        WireMock.stubFor(
            post(urlEqualTo("/snap/v1/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "token": "test-token-123",
                              "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/test-token-123"
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val amount = 10000L
        val customerDetails =
            CustomerPaymentDetails(
                email = "test@example.com",
                fullName = "Test User",
                phone = "08123456789",
            )
        val itemDetails =
            listOf(
                PaymentItemDetail(id = "item1", price = 10000L, quantity = 1, name = "Test Item"),
            )

        // When
        val session =
            paymentProvider.createPaymentSession(
                orderId = orderId,
                providerOrderId = providerOrderId,
                amount = amount,
                customerDetails = customerDetails,
                itemDetails = itemDetails,
            )

        // Then
        assertNotNull(session.token)
        assertEquals("test-token-123", session.token)
        assertNotNull(session.redirectUrl)
    }

    @Test
    fun `should get payment status successfully (wiremocked)`() {
        // Given
        val providerOrderId = "ORDER-123"
        WireMock.stubFor(
            get(urlEqualTo("/v2/$providerOrderId/status"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "transaction_status": "settlement",
                              "status_code": "200"
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        // When
        val status = paymentProvider.getPaymentStatus(providerOrderId)

        // Then
        assertEquals(PaymentStatus.PAID, status)
    }

    @Test
    fun `should return PENDING when Midtrans returns 404 (wiremocked)`() {
        // Given
        val providerOrderId = "NON-EXISTENT"
        WireMock.stubFor(
            get(urlEqualTo("/v2/$providerOrderId/status"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "status_code": "404",
                              "status_message": "Transaction not found"
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        // When
        val status = paymentProvider.getPaymentStatus(providerOrderId)

        // Then
        assertEquals(PaymentStatus.PENDING, status)
    }
}
