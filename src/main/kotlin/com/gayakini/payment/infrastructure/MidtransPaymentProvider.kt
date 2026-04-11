package com.gayakini.payment.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.payment.domain.CustomerPaymentDetails
import com.gayakini.payment.domain.PaymentGatewayException
import com.gayakini.payment.domain.PaymentItemDetail
import com.gayakini.payment.domain.PaymentProvider
import com.gayakini.payment.domain.PaymentSession
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClientException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

@Component
class MidtransPaymentProvider(
    private val properties: GayakiniProperties,
    restTemplateBuilder: org.springframework.boot.web.client.RestTemplateBuilder,
    private val objectMapper: ObjectMapper,
) : PaymentProvider {
    private val logger = LoggerFactory.getLogger(MidtransPaymentProvider::class.java)
    private val restTemplate = restTemplateBuilder.build()

    @Retryable(
        value = [RestClientException::class, PaymentGatewayException::class],
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

        val requestMap =
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
                requestMap["enabled_payments"] = it
            }
        }

        val requestPayload = objectMapper.writeValueAsString(requestMap)
        val headers = createHeaders()

        return try {
            val response =
                restTemplate.postForEntity(
                    properties.midtrans.snapUrl,
                    HttpEntity(requestMap, headers),
                    Map::class.java,
                )

            val responsePayload = objectMapper.writeValueAsString(response.body)

            if (response.statusCode.is2xxSuccessful) {
                val body = response.body as Map<*, *>
                val token = body["token"] as String
                val redirectUrl = body["redirect_url"] as String
                PaymentSession(
                    token = token,
                    redirectUrl = redirectUrl,
                    providerOrderId = providerOrderId,
                    requestPayload = requestPayload,
                    responsePayload = responsePayload,
                )
            } else {
                logger.error(
                    "Gagal membuat sesi pembayaran Midtrans: {} - {}",
                    response.statusCode,
                    responsePayload,
                )
                throw PaymentGatewayException(
                    "Gagal membuat sesi pembayaran. Provider merespon dengan status ${response.statusCode}.",
                )
            }
        } catch (e: HttpClientErrorException) {
            val errorBody = e.responseBodyAsString
            logger.error("Midtrans Client Error (4xx): {} - {}", e.statusCode, errorBody)
            throw PaymentGatewayException("Permintaan pembayaran ditolak oleh provider: $errorBody")
        } catch (e: HttpServerErrorException) {
            logger.error("Midtrans Server Error (5xx): {} - {}", e.statusCode, e.responseBodyAsString)
            throw PaymentGatewayException("Provider pembayaran sedang mengalami gangguan teknis.")
        } catch (e: RestClientException) {
            logger.error("Midtrans Connection Error: {}", e.message)
            throw PaymentGatewayException("Tidak dapat terhubung ke provider pembayaran.")
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

        // Use constant-time comparison to prevent timing attacks
        val isValid =
            MessageDigest.isEqual(
                calculatedSignature.lowercase().toByteArray(StandardCharsets.UTF_8),
                signature.lowercase().toByteArray(StandardCharsets.UTF_8),
            )

        if (!isValid) {
            logger.warn(
                "Midtrans signature mismatch for order {}! Calculated: {}, Received: {}",
                orderId,
                calculatedSignature,
                signature,
            )
        }

        return isValid
    }

    @Retryable(
        value = [RestClientException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    override fun getPaymentStatus(providerOrderId: String): PaymentStatus {
        val statusUrl = "${properties.midtrans.apiUrl}/$providerOrderId/status"

        val headers = createHeaders()
        return try {
            val response =
                restTemplate.exchange(
                    statusUrl,
                    HttpMethod.GET,
                    HttpEntity<Any>(headers),
                    Map::class.java,
                )
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
