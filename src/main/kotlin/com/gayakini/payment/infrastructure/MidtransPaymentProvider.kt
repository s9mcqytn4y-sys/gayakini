package com.gayakini.payment.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.order.domain.PaymentStatus
import com.gayakini.payment.domain.CustomerPaymentDetails
import com.gayakini.payment.domain.PaymentGatewayException
import com.gayakini.payment.domain.PaymentItemDetail
import com.gayakini.payment.domain.PaymentProvider
import com.gayakini.payment.domain.PaymentSession
import com.midtrans.Config
import com.midtrans.ConfigFactory
import com.midtrans.httpclient.error.MidtransError
import com.midtrans.service.MidtransSnapApi
import com.midtrans.service.MidtransCoreApi
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import jakarta.annotation.PostConstruct

@Component
@Suppress("TooGenericExceptionCaught")
class MidtransPaymentProvider(
    private val properties: GayakiniProperties,
    private val objectMapper: ObjectMapper,
) : PaymentProvider {
    private val logger = LoggerFactory.getLogger(MidtransPaymentProvider::class.java)

    private lateinit var snapApi: MidtransSnapApi
    private lateinit var coreApi: MidtransCoreApi

    @PostConstruct
    fun init() {
        val config =
            Config.builder()
                .setServerKey(properties.midtrans.serverKey)
                .setClientKey(properties.midtrans.clientKey)
                .setIsProduction(properties.midtrans.isProduction)
                .build()

        val factory = ConfigFactory(config)
        snapApi = factory.getSnapApi()
        coreApi = factory.getCoreApi()
    }

    @Retryable(
        value = [MidtransError::class, PaymentGatewayException::class],
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
            // Using SDK to create transaction
            val response = snapApi.createTransaction(params)

            PaymentSession(
                token = response.getString("token"),
                redirectUrl = response.getString("redirect_url"),
                providerOrderId = providerOrderId,
                requestPayload = objectMapper.writeValueAsString(params),
                responsePayload = response.toString(),
            )
        } catch (e: MidtransError) {
            logger.error("Midtrans SDK Error: {} (Status: {})", e.message, e.statusCode)
            throw PaymentGatewayException("Gagal membuat sesi pembayaran: ${e.message}", e)
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
            // Using SDK CoreApi equivalent for status check
            val response = coreApi.checkTransaction(providerOrderId)
            mapStatus(response.getString("transaction_status"))
        } catch (e: MidtransError) {
            logger.error("Gagal mengambil status pembayaran dari Midtrans SDK: {}", e.message)
            PaymentStatus.PENDING
        } catch (e: Exception) {
            logger.error("Unexpected Error checking Midtrans status", e)
            PaymentStatus.PENDING
        }
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
