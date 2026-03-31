package com.gayakini.webhook.api

import com.gayakini.payment.application.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Endpoint untuk menerima notifikasi dari pihak ketiga")
class WebhookController(
    private val paymentService: PaymentService
) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    @PostMapping("/midtrans")
    @Operation(summary = "Menerima notifikasi status pembayaran dari Midtrans")
    fun handleMidtransWebhook(
        @RequestBody payload: Map<String, Any>,
        @RequestHeader("X-Callback-Signature", required = false) signature: String? // Midtrans uses Signature Key in payload usually, but headers can vary
    ): ResponseEntity<String> {
        logger.info("Menerima webhook Midtrans: {}", payload)
        
        // Midtrans typically sends signature_key in the JSON body itself
        val signatureKey = payload["signature_key"] as? String ?: signature ?: ""
        
        return try {
            paymentService.processMidtransWebhook(payload, signatureKey)
            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            logger.error("Gagal memproses webhook Midtrans: {}", e.message)
            // Still return 200/OK to provider to stop retries if it's a permanent failure, 
            // but for transient errors, we might want 500. For now, 200 to prevent loops.
            ResponseEntity.ok("PROCESSED_WITH_ERROR")
        }
    }

    @PostMapping("/biteship")
    @Operation(summary = "Menerima notifikasi status pengiriman dari Biteship")
    fun handleBiteshipWebhook(
        @RequestBody payload: Map<String, Any>
    ): ResponseEntity<String> {
        logger.info("Menerima webhook Biteship: {}", payload)
        // Implementation for Biteship status update logic
        return ResponseEntity.ok("OK")
    }
}
