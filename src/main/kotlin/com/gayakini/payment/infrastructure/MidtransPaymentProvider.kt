package com.gayakini.payment.infrastructure

import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.payment.domain.CustomerPaymentDetails
import com.gayakini.payment.domain.PaymentProvider
import com.gayakini.payment.domain.PaymentSession
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

@Component
class MidtransPaymentProvider(
    private val properties: GayakiniProperties,
    private val restTemplate: RestTemplate,
) : PaymentProvider {
    private val logger = LoggerFactory.getLogger(MidtransPaymentProvider::class.java)

    override fun createPaymentSession(
        orderId: UUID,
        providerOrderId: String,
        amount: Long,
        customerDetails: CustomerPaymentDetails,
    ): PaymentSession {
        val requestBody =
            mapOf(
                "transaction_details" to
                    mapOf(
                        "order_id" to providerOrderId,
                        "gross_amount" to amount,
                    ),
                "customer_details" to
                    mapOf(
                        "first_name" to customerDetails.fullName,
                        "email" to customerDetails.email,
                        "phone" to customerDetails.phone,
                    ),
            )

        val headers = createHeaders()
        val response =
            restTemplate.postForEntity(
                properties.midtrans.snapUrl,
                HttpEntity(requestBody, headers),
                Map::class.java,
            )

        if (response.statusCode.is2xxSuccessful) {
            val body = response.body as Map<*, *>
            return PaymentSession(
                token = body["token"] as String,
                redirectUrl = body["redirect_url"] as String,
                providerOrderId = providerOrderId,
            )
        } else {
            logger.error("Gagal membuat sesi pembayaran Midtrans: {} - {}", response.statusCode, response.body)
            error("Gagal membuat sesi pembayaran. Silakan coba lagi.")
        }
    }

    override fun verifyWebhook(
        payload: Map<String, Any>,
        signature: String,
    ): Boolean {
        val orderId = payload["order_id"] as? String ?: return false
        val statusCode = payload["status_code"] as? String ?: return false
        val grossAmount = payload["gross_amount"] as? String ?: return false
        val rawData = orderId + statusCode + grossAmount + properties.midtrans.serverKey

        val calculatedSignature = sha512(rawData)
        return calculatedSignature.equals(signature, ignoreCase = true)
    }

    override fun getPaymentStatus(providerOrderId: String): PaymentStatus {
        val statusUrl = "${properties.midtrans.apiUrl}/$providerOrderId/status"

        val headers = createHeaders()
        return try {
            val response = restTemplate.exchange(statusUrl, HttpMethod.GET, HttpEntity<Any>(headers), Map::class.java)
            if (response.statusCode.is2xxSuccessful) {
                val body = response.body as Map<*, *>
                mapStatus(body["transaction_status"] as String)
            } else {
                PaymentStatus.PENDING
            }
        } catch (e: RestClientException) {
            logger.error("Gagal mengambil status pembayaran dari Midtrans: {}", e.message)
            PaymentStatus.PENDING
        }
    }

    private fun createHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBasicAuth(properties.midtrans.serverKey, "", StandardCharsets.UTF_8)
        return headers
    }

    private fun mapStatus(midtransStatus: String): PaymentStatus {
        return when (midtransStatus) {
            "capture", "settlement" -> PaymentStatus.PAID
            "pending" -> PaymentStatus.PENDING
            "deny", "cancel" -> PaymentStatus.CANCELLED
            "expire" -> PaymentStatus.EXPIRED
            "refund" -> PaymentStatus.REFUNDED
            else -> PaymentStatus.FAILED
        }
    }

    private fun sha512(input: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val bytes = md.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
