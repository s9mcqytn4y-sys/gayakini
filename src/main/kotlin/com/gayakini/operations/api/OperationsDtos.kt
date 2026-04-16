package com.gayakini.operations.api

import java.time.Instant
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
    val packedAt: Instant,
)

/**
 * Request to process QC for a returned item and restock it.
 */
data class RestockQCRequest(
    val note: String? = null,
)

/**
 * Response summarizing the QC restock result.
 */
data class RestockQCResponse(
    val orderId: UUID,
    val orderItemId: UUID,
    val status: String = "RESTOCKED",
)
