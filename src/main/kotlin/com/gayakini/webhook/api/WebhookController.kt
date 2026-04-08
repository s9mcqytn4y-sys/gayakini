package com.gayakini.webhook.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.WebhookAckResponse
import com.gayakini.payment.application.PaymentService
import com.gayakini.shipping.application.ShippingService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/webhooks")
class WebhookController(
    private val paymentService: PaymentService,
    private val shippingService: ShippingService,
) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    @PostMapping("/midtrans")
    fun handleMidtransWebhook(
        @Valid @RequestBody payload: MidtransWebhookPayload,
        @RequestHeader("X-Signature-Key", required = false) signature: String?,
    ): WebhookAckResponse {
        logger.info("Menerima webhook Midtrans untuk Order: {}", payload.orderId)

        // Midtrans signature key validation is handled by PaymentService/Provider
        // We pass the payload as a map to maintain consistency with PaymentService's expectation
        val payloadMap =
            mutableMapOf<String, Any>(
                "order_id" to payload.orderId,
                "status_code" to payload.statusCode,
                "transaction_status" to payload.transactionStatus,
                "gross_amount" to payload.grossAmount,
                "signature_key" to payload.signatureKey,
            )

        payload.transactionId?.let { payloadMap["transaction_id"] = it }
        payload.fraudStatus?.let { payloadMap["fraud_status"] = it }

        val effectiveSignature = signature ?: payload.signatureKey

        paymentService.processMidtransWebhook(payloadMap, effectiveSignature)

        return WebhookAckResponse(
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }

    @PostMapping("/biteship")
    fun handleBiteshipWebhook(
        @Valid @RequestBody payload: BiteshipWebhookPayload,
    ): WebhookAckResponse {
        logger.info("Menerima webhook Biteship Event: {} untuk Order: {}", payload.event, payload.orderId)

        val payloadMap =
            mutableMapOf<String, Any>(
                "event" to payload.event,
                "id" to payload.id,
            )
        payload.orderId?.let { payloadMap["order_id"] = it }
        payload.status?.let { payloadMap["status"] = it }

        shippingService.processBiteshipWebhook(payloadMap)

        return WebhookAckResponse(
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }
}
