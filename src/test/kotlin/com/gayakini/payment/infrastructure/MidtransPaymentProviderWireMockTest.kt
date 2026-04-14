package com.gayakini.payment.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.payment.domain.CustomerPaymentDetails
import com.gayakini.payment.domain.PaymentItemDetail
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.mockito.Mockito.`when`
import com.midtrans.service.MidtransSnapApi
import java.util.*
import org.springframework.test.util.ReflectionTestUtils
import org.mockito.ArgumentMatchers.anyMap

@SpringBootTest(classes = [MidtransPaymentProvider::class, ObjectMapper::class])
@ActiveProfiles("test")
@EnableConfigurationProperties(GayakiniProperties::class)
class MidtransPaymentProviderWireMockTest {
    @Autowired
    private lateinit var paymentProvider: MidtransPaymentProvider

    @MockitoBean
    private lateinit var snapApi: MidtransSnapApi

    @Test
    fun `should create payment session successfully (mocked SDK)`() {
        // We use Map to mock the response if we can't use JSONObject directly due to classloader/dependency issues
        // However, Midtrans SDK returns a JSONObject (from org.json).
        // Let's try to mock it without explicit import if it's transitive.

        val mockResponse = org.json.JSONObject()
        mockResponse.put("token", "test-token-123")
        mockResponse.put("redirect_url", "https://app.sandbox.midtrans.com/snap/v2/vtweb/test-token-123")

        ReflectionTestUtils.setField(paymentProvider, "snapApi", snapApi)

        `when`(snapApi.createTransaction(anyMap())).thenReturn(mockResponse)

        val orderId = UUID.randomUUID()
        val providerOrderId = "TEST-${System.currentTimeMillis()}"
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

        val session =
            paymentProvider.createPaymentSession(
                orderId = orderId,
                providerOrderId = providerOrderId,
                amount = amount,
                customerDetails = customerDetails,
                itemDetails = itemDetails,
            )

        assertNotNull(session.token)
        assertNotNull(session.redirectUrl)
    }
}
