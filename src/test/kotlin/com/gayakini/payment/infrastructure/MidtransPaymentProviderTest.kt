package com.gayakini.payment.infrastructure

import com.gayakini.payment.domain.CustomerPaymentDetails
import com.gayakini.payment.domain.PaymentItemDetail
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.*

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class MidtransPaymentProviderTest {
    @Autowired
    private lateinit var paymentProvider: MidtransPaymentProvider

    @Test
    @Disabled("Only run manually to verify real Midtrans connectivity")
    fun `test create payment session with real midtrans sandbox`() {
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

        println("Session Token: ${session.token}")
        println("Redirect URL: ${session.redirectUrl}")
        assert(session.token.isNotEmpty())
    }
}
