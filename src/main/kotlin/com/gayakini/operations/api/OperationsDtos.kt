package com.gayakini.operations.api

import java.util.UUID

/**
 * Request to mark an order as picked and packed, ready for shipment.
 */
data class PackOrderRequest(
    val notes: String? = null,
    val weightInGrams: Int? = null,
    val dimensionCm: String? = null,
)

/**
 * Response summarizing the packing result.
 */
data class PackingResponse(
    val orderId: UUID,
    val orderNumber: String,
    val status: String,
    val packedAt: java.time.Instant,
)
