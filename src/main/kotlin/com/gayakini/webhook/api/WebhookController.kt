package com.gayakini.webhook.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.WebhookAckData
import com.gayakini.common.api.WebhookAckResponse
import com.gayakini.payment.application.PaymentService
import com.gayakini.shipping.application.ShippingService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/webhooks", "/v1/webhooks")
class WebhookController(
    private val paymentService: PaymentService,
    private val shippingService: ShippingService,
) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    @PostMapping("/midtrans")
    fun handleMidtransWebhook(
        @RequestBody payload: Map<String, Any>,
        @RequestHeader("X-Callback-Signature", required = false) signature: String?,
    ): WebhookAckResponse {
        logger.info("Menerima webhook Midtrans: {}", payload)

        val signatureKey = payload["signature_key"] as? String ?: signature ?: ""
        paymentService.processMidtransWebhook(payload, signatureKey)

        return WebhookAckResponse(
            message = "Webhook berhasil diterima.",
            data = WebhookAckData(accepted = true),
            meta = ApiMeta(requestId = UUID.randomUUID().toString())
        )
    }

    @PostMapping("/biteship")
    fun handleBiteshipWebhook(
        @RequestBody payload: Map<String, Any>,
    ): WebhookAckResponse {
        logger.info("Menerima webhook Biteship: {}", payload)

        shippingService.processBiteshipWebhook(payload)

        return WebhookAckResponse(
            message = "Webhook berhasil diterima.",
            data = WebhookAckData(accepted = true),
            meta = ApiMeta(requestId = UUID.randomUUID().toString())
        )
    }
}
