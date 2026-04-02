package com.gayakini.webhook.api

import com.gayakini.payment.application.PaymentService
import com.gayakini.shipping.application.ShippingService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

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
        @RequestHeader("X-Callback-Signature", required = false) signature: String?,
    ): WebhookAckResponse {
        logger.info("Menerima webhook Midtrans untuk Order: {}", payload.orderId)

        // Midtrans typically uses signature_key in payload
        val signatureKey = payload.signatureKey

        // Convert to map for existing service compatibility
        val payloadMap = mutableMapOf<String, Any>(
            "order_id" to payload.orderId,
            "status_code" to payload.statusCode,
            "transaction_status" to payload.transactionStatus,
            "gross_amount" to payload.grossAmount,
            "signature_key" to payload.signatureKey,
            "transaction_id" to (payload.transactionId ?: "")
        )

        paymentService.processMidtransWebhook(payloadMap, signatureKey)

        return WebhookAckResponse()
    }

    @PostMapping("/biteship")
    fun handleBiteshipWebhook(
        @Valid @RequestBody payload: BiteshipWebhookPayload,
    ): WebhookAckResponse {
        logger.info("Menerima webhook Biteship Event: {} untuk Order: {}", payload.event, payload.orderId)

        val payloadMap = mutableMapOf<String, Any>(
            "event" to payload.event,
            "id" to payload.id
        )
        payload.orderId?.let { payloadMap["order_id"] = it }
        payload.status?.let { payloadMap["status"] = it }

        shippingService.processBiteshipWebhook(payloadMap)

        return WebhookAckResponse()
    }
}
