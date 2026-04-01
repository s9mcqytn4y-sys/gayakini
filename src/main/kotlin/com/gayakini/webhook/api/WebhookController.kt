package com.gayakini.webhook.api

import com.gayakini.common.api.ApiResponse
import com.gayakini.payment.application.PaymentService
import com.gayakini.shipping.application.ShippingService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/webhooks")
class WebhookController(
    private val paymentService: PaymentService,
    private val shippingService: ShippingService,
) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    @PostMapping("/midtrans")
    fun handleMidtransWebhook(
        @RequestBody payload: Map<String, Any>,
        @RequestHeader("X-Callback-Signature", required = false) signature: String?,
    ): ApiResponse<WebhookAckData> {
        logger.info("Menerima webhook Midtrans: {}", payload)

        val signatureKey = payload["signature_key"] as? String ?: signature ?: ""

        paymentService.processMidtransWebhook(payload, signatureKey)

        return ApiResponse.success(
            message = "Webhook berhasil diterima.",
            data = WebhookAckData(accepted = true),
        )
    }

    @PostMapping("/biteship")
    fun handleBiteshipWebhook(
        @RequestBody payload: Map<String, Any>,
    ): ApiResponse<WebhookAckData> {
        logger.info("Menerima webhook Biteship: {}", payload)

        shippingService.processBiteshipWebhook(payload)

        return ApiResponse.success(
            message = "Webhook berhasil diterima.",
            data = WebhookAckData(accepted = true),
        )
    }
}
