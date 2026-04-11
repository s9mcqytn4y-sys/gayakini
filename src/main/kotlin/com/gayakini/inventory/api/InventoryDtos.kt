package com.gayakini.inventory.api

import com.gayakini.inventory.domain.AdjustmentReason
import com.gayakini.inventory.domain.InventoryAdjustment
import java.time.Instant
import java.util.UUID

data class InventoryAdjustmentRequest(
    val variantId: UUID,
    val quantityDelta: Int,
    val reason: AdjustmentReason,
    val note: String? = null,
    val idempotencyKey: String? = null,
)

data class InventoryAdjustmentResponse(
    val id: UUID,
    val variantId: UUID,
    val quantityDelta: Int,
    val reason: AdjustmentReason,
    val note: String?,
    val actorSubject: String?,
    val stockOnHandAfter: Int,
    val stockReservedAfter: Int,
    val createdAt: Instant,
) {
    companion object {
        fun fromEntity(entity: InventoryAdjustment): InventoryAdjustmentResponse {
            return InventoryAdjustmentResponse(
                id = entity.id,
                variantId = entity.variant.id,
                quantityDelta = entity.quantityDelta,
                reason = entity.reasonCode,
                note = entity.note,
                actorSubject = entity.actorSubject,
                stockOnHandAfter = entity.stockOnHandAfter,
                stockReservedAfter = entity.stockReservedAfter,
                createdAt = entity.createdAt,
            )
        }
    }
}
