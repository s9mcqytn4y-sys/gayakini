package com.gayakini.webhook.api

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * Payload for Midtrans Payment Webhook
 */
@Schema(description = "Webhook notification payload from Midtrans.")
data class MidtransWebhookPayload(
    @field:NotBlank
    @Schema(description = "Unique order ID", example = "ORD-20231027-001")
    @JsonProperty("order_id") val orderId: String,
    @field:NotBlank
    @Schema(description = "HTTP status code from Midtrans", example = "200")
    @JsonProperty("status_code") val statusCode: String,
    @field:NotBlank
    @Schema(description = "Transaction status (e.g., settlement, pending, deny)", example = "settlement")
    @JsonProperty("transaction_status") val transactionStatus: String,
    @field:NotBlank
    @Schema(description = "Total transaction amount as string", example = "150000.00")
    @JsonProperty("gross_amount") val grossAmount: String,
    @Schema(description = "Signature key for security validation", example = "abcdef123456...")
    @JsonProperty("signature_key") val signatureKey: String? = null,
    @Schema(description = "Unique transaction ID from Midtrans", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("transaction_id") val transactionId: String? = null,
    @Schema(description = "Payment type used", example = "credit_card")
    @JsonProperty("payment_type") val paymentType: String? = null,
    @Schema(description = "Fraud detection status", example = "accept")
    @JsonProperty("fraud_status") val fraudStatus: String? = null,
    @Schema(description = "Transaction settlement time", example = "2023-10-27 10:00:00")
    @JsonProperty("settlement_time") val settlementTime: String? = null,
    @Schema(description = "Transaction creation time", example = "2023-10-27 09:55:00")
    @JsonProperty("transaction_time") val transactionTime: String? = null,
    @Schema(description = "Transaction expiry time", example = "2023-10-28 09:55:00")
    @JsonProperty("expiry_time") val expiryTime: String? = null,
    @Schema(description = "Transaction currency", example = "IDR")
    @JsonProperty("currency") val currency: String? = null,
)

/**
 * Payload for Biteship Shipment Webhook
 */
@Schema(description = "Webhook notification payload from Biteship for shipment tracking.")
data class BiteshipWebhookPayload(
    @field:NotBlank
    @Schema(description = "Event type (e.g., order.status_updated)", example = "order.status_updated")
    val event: String,
    @field:NotBlank
    @Schema(description = "Unique notification ID from Biteship", example = "evt_12345")
    val id: String,
    @Schema(description = "Internal order ID", example = "ORD-20231027-001")
    @JsonProperty("order_id") val orderId: String? = null,
    @Schema(description = "Latest shipment status", example = "delivered")
    val status: String? = null,
    @Schema(description = "Courier code", example = "jne")
    @JsonProperty("courier_code") val courierCode: String? = null,
    @Schema(description = "Shipping tracking number (waybill)", example = "WAYBILL123456")
    @JsonProperty("waybill_id") val waybillId: String? = null,
    @Schema(description = "Additional note or status reason", example = "Received by Budi")
    val note: String? = null,
)
