package com.gayakini.payment.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.payment.domain.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.Base64

@Component
@Suppress("TooGenericExceptionCaught")
class MidtransPaymentProvider(
    private val properties: GayakiniProperties,
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate,
) : PaymentProvider {
    private val logger = LoggerFactory.getLogger(MidtransPaymentProvider::class.java)

    @Retryable(
        value = [PaymentGatewayException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    override fun createPaymentSession(
        orderId: UUID,
        providerOrderId: String,
        amount: Long,
        customerDetails: CustomerPaymentDetails,
        itemDetails: List<PaymentItemDetail>,
        enabledChannels: List<String>?,
    ): PaymentSession {
        // Validation: sum of (price * quantity) must match amount
        val calculatedAmount = itemDetails.sumOf { it.price * it.quantity }
        if (calculatedAmount != amount) {
            logger.error("Ketidaksesuaian jumlah pembayaran: total={}, item_sum={}", amount, calculatedAmount)
            throw PaymentGatewayException("Ketidaksesuaian jumlah pembayaran dalam sistem.")
        }

        val params =
            mutableMapOf<String, Any>(
                "transaction_details" to
                    mapOf(
                        "order_id" to providerOrderId,
                        "gross_amount" to amount,
                    ),
                "item_details" to
                    itemDetails.map {
                        mapOf(
                            "id" to it.id,
                            "price" to it.price,
                            "quantity" to it.quantity,
                            "name" to it.name,
                        )
                    },
                "customer_details" to
                    mapOf(
                        "first_name" to customerDetails.fullName,
                        "email" to customerDetails.email,
                        "phone" to customerDetails.phone,
                    ),
                "usage_limit" to 1,
            )

        enabledChannels?.let {
            if (it.isNotEmpty()) {
                params["enabled_payments"] = it
            }
        }

        return try {
            val headers = createHeaders()
            val response = restTemplate.postForEntity(properties.midtrans.snapUrl, HttpEntity(params, headers), Map::class.java)
            val body = response.body

            if (response.statusCode.is2xxSuccessful && body != null) {
                PaymentSession(
                    token = body["token"].toString(),
                    redirectUrl = body["redirect_url"].toString(),
                    providerOrderId = providerOrderId,
                    requestPayload = objectMapper.writeValueAsString(params),
                    responsePayload = objectMapper.writeValueAsString(body),
                )
            } else {
                throw PaymentGatewayException("Midtrans API Error: ${response.statusCode}")
            }
        } catch (e: Exception) {
            logger.error("Unexpected Error creating Midtrans session", e)
            throw PaymentGatewayException("Terjadi kesalahan teknis saat menghubungi provider pembayaran.", e)
        }
    }

    override fun verifyWebhook(
        payload: Map<String, Any>,
        signature: String,
    ): Boolean {
        val orderId = payload["order_id"] as? String ?: return false
        val statusCode = payload["status_code"] as? String ?: return false
        val grossAmount = payload["gross_amount"] as? String ?: return false
        val serverKey = properties.midtrans.serverKey

        // Midtrans SHA512 signature: SHA512(order_id + status_code + gross_amount + server_key)
        val rawData = orderId + statusCode + grossAmount + serverKey
        val calculatedSignature = sha512(rawData)

        val isValid =
            MessageDigest.isEqual(
                calculatedSignature.lowercase().toByteArray(StandardCharsets.UTF_8),
                signature.lowercase().toByteArray(StandardCharsets.UTF_8),
            )

        if (!isValid) {
            logger.warn("Midtrans signature mismatch for order {}!", orderId)
        }

        return isValid
    }

    override fun getPaymentStatus(providerOrderId: String): PaymentStatus {
        return try {
            val url = "${properties.midtrans.apiUrl}/$providerOrderId/status"
            val headers = createHeaders()
            val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Any>(headers), Map::class.java)
            val body = response.body

            if (response.statusCode.is2xxSuccessful && body != null) {
                mapStatus(body["transaction_status"].toString())
            } else {
                PaymentStatus.PENDING
            }
        } catch (e: Exception) {
            logger.error("Unexpected Error checking Midtrans status", e)
            PaymentStatus.PENDING
        }
    }

    private fun createHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setAccept(listOf(MediaType.APPLICATION_JSON))

        val auth = "${properties.midtrans.serverKey}:"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray(StandardCharsets.UTF_8))
        headers.set("Authorization", "Basic $encodedAuth")

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
