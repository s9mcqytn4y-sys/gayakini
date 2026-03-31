package com.gayakini.payment.infrastructure

import com.gayakini.payment.domain.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

@Component
class MidtransPaymentProvider(
    @Value("\${midtrans.server-key}") private val serverKey: String,
    @Value("\${midtrans.base-url}") private val baseUrl: String,
    private val restTemplate: RestTemplate
) : PaymentProvider {

    private val logger = LoggerFactory.getLogger(MidtransPaymentProvider::class.java)

    override fun createPaymentSession(orderId: UUID, amount: Long, customerDetails: CustomerPaymentDetails): PaymentSession {
        val url = "\$baseUrl/snap/v1/transactions"

        val requestBody = mapOf(
            "transaction_details" to mapOf(
                "order_id" to orderId.toString(),
                "gross_amount" to amount
            ),
            "customer_details" to mapOf(
                "first_name" to customerDetails.fullName,
                "email" to customerDetails.email,
                "phone" to customerDetails.phone
            ),
            "usage_limit" to 1
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBasicAuth(serverKey, "", StandardCharsets.UTF_8)
        }

        val response = restTemplate.postForEntity(url, HttpEntity(requestBody, headers), Map::class.java)

        if (response.statusCode.is2xxSuccessful) {
            val body = response.body as Map<*, *>
            return PaymentSession(
                token = body["token"] as String,
                redirectUrl = body["redirect_url"] as String,
                externalId = orderId.toString()
            )
        } else {
            logger.error("Gagal membuat sesi pembayaran Midtrans: \${response.statusCode}")
            throw IllegalStateException("Gagal membuat sesi pembayaran. Silakan coba lagi.")
        }
    }

    override fun verifyWebhook(payload: Map<String, Any>, signature: String): Boolean {
        val orderId = payload["order_id"] as String
        val statusCode = payload["status_code"] as String
        val grossAmount = payload["gross_amount"] as String
        val rawData = orderId + statusCode + grossAmount + serverKey
        
        val calculatedSignature = sha512(rawData)
        return calculatedSignature.equals(signature, ignoreCase = true)
    }

    override fun getPaymentStatus(externalId: String): PaymentStatus {
        val url = "\$baseUrl/v2/\$externalId/status"
        
        val headers = HttpHeaders().apply {
            setBasicAuth(serverKey, "", StandardCharsets.UTF_8)
        }

        val response = restTemplate.getForEntity(url, Map::class.java, headers)
        
        return if (response.statusCode.is2xxSuccessful) {
            val body = response.body as Map<*, *>
            mapStatus(body["transaction_status"] as String)
        } else {
            PaymentStatus.PENDING
        }
    }

    private fun mapStatus(midtransStatus: String): PaymentStatus {
        return when (midtransStatus) {
            "capture", "settlement" -> PaymentStatus.SETTLED
            "pending" -> PaymentStatus.PENDING
            "deny", "cancel" -> PaymentStatus.CANCELLED
            "expire" -> PaymentStatus.EXPIRED
            "refund" -> PaymentStatus.REFUNDED
            else -> PaymentStatus.PENDING
        }
    }

    private fun sha512(input: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val bytes = md.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
