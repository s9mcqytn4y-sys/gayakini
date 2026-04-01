package com.gayakini.webhook.api

data class WebhookAckResponse(
    val message: String = "Webhook berhasil diterima.",
    val data: WebhookAckData = WebhookAckData()
)

data class WebhookAckData(
    val accepted: Boolean = true
)
