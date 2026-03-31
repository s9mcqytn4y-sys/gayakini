package com.gayakini.payment.infrastructure

import com.gayakini.payment.domain.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.*

@Component
class MidtransPaymentProvider(
    @Value("\${midtrans.server-key}") private val serverKey: String,
    @Value("\${midtrans.base-url}") private val baseUrl: String,
    private val restTemplate: RestTemplate = RestTemplate()
) : PaymentProvider {

    override fun createPaymentSession(orderId: UUID, amount: Long, customerDetails: CustomerPaymentDetails): PaymentSession {
        // Implementation logic for Midtrans Snap API
        // This is a skeleton for the starter pack
        return PaymentSession(
            token = "snap-token-placeholder-\${UUID.randomUUID()}",
            redirectUrl = "https://app.sandbox.midtrans.com/snap/v2/vtweb/\${UUID.randomUUID()}",
            externalId = orderId.toString()
        )
    }

    override fun verifyWebhook(payload: Map<String, Any>, signature: String): Boolean {
        // Implementation logic to verify Midtrans signature key
        return true 
    }

    override fun getPaymentStatus(externalId: String): PaymentStatus {
        // Implementation logic to call Midtrans Status API
        return PaymentStatus.PENDING
    }
}
