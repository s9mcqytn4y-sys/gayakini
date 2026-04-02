package com.gayakini.webhook.api

import com.fasterxml.jackson.annotation.JsonProperty

data class MidtransWebhookPayload(
    @JsonProperty("order_id") val orderId: String,
    @JsonProperty("status_code") val statusCode: String,
    @JsonProperty("transaction_status") val transactionStatus: String,
    @JsonProperty("gross_amount") val grossAmount: String,
    @JsonProperty("signature_key") val signatureKey: String,
    @JsonProperty("transaction_id") val transactionId: String? = null,
    @JsonProperty("payment_type") val paymentType: String? = null,
    @JsonProperty("fraud_status") val fraudStatus: String? = null,
    @JsonProperty("settlement_time") val settlementTime: String? = null,
    @JsonProperty("transaction_time") val transactionTime: String? = null,
    @JsonProperty("expiry_time") val expiryTime: String? = null,
    @JsonProperty("currency") val currency: String? = null,
)

data class BiteshipWebhookPayload(
    val event: String,
    val id: String,
    @JsonProperty("order_id") val orderId: String? = null,
    val status: String? = null,
    @JsonProperty("courier_code") val courierCode: String? = null,
    @JsonProperty("waybill_id") val waybillId: String? = null,
    val note: String? = null,
)
