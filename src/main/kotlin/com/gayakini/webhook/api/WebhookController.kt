package com.gayakini.webhook.api

import com.gayakini.common.api.ForbiddenException
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.WebhookAckResponse
import com.gayakini.payment.application.PaymentService
import com.gayakini.shipping.application.ShippingService
import com.gayakini.webhook.api.MidtransWebhookPayload
import com.gayakini.webhook.api.BiteshipWebhookPayload
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
        logger.info("Webhook received for Midtrans Order: {}, status: {}", payload.orderId, payload.transactionStatus)

        // The webhook MUST be authoritative. PaymentService will handle verification and idempotency.
        val effectiveSignature = signature ?: payload.signatureKey
            ?: throw ForbiddenException("Missing signature key")

        // Convert data class to map for PaymentService compatibility
        val payloadMap = mapOf(
            "order_id" to payload.orderId,
            "status_code" to payload.statusCode,
            "transaction_status" to payload.transactionStatus,
            "gross_amount" to payload.grossAmount,
            "transaction_id" to (payload.transactionId ?: ""),
            "payment_type" to (payload.paymentType ?: ""),
            "fraud_status" to (payload.fraudStatus ?: ""),
            "signature_key" to (payload.signatureKey ?: "")
        )

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
