package com.gayakini.inventory.api

import com.gayakini.inventory.domain.AdjustmentReason
import com.gayakini.inventory.domain.InventoryAdjustment
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

@Schema(description = "Request to adjust inventory levels for a specific variant.")
data class InventoryAdjustmentRequest(
    @field:NotNull
    @Schema(description = "UUID of the product variant", example = "550e8400-e29b-41d4-a716-446655440000")
    val variantId: UUID,
    @Schema(description = "The change in quantity (positive for increase, negative for decrease)", example = "10")
    val quantityDelta: Int,
    @Schema(description = "Reason for the adjustment")
    val reason: AdjustmentReason,
    @Schema(description = "Optional note describing the adjustment", example = "Restock from supplier")
    val note: String? = null,
    @Schema(description = "Optional idempotency key to prevent duplicate adjustments")
    val idempotencyKey: String? = null,
)

@Schema(description = "Response containing details of a recorded inventory adjustment.")
data class InventoryAdjustmentResponse(
    val success: Boolean = true,
    @Schema(description = "Unique ID of the adjustment", example = "550e8400-e29b-41d4-a716-446655440001")
    val id: UUID,
    @Schema(description = "UUID of the product variant")
    val variantId: UUID,
    @Schema(description = "The change in quantity that was applied")
    val quantityDelta: Int,
    @Schema(description = "Reason for the adjustment")
    val reason: AdjustmentReason,
    @Schema(description = "Description or note of the adjustment")
    val note: String?,
    @Schema(description = "The actor (user/system) who performed the adjustment")
    val actorSubject: String?,
    @Schema(description = "New stock on hand after adjustment")
    val stockOnHandAfter: Int,
    @Schema(description = "Stock reserved for pending orders after adjustment")
    val stockReservedAfter: Int,
    @Schema(description = "Timestamp when the adjustment was recorded")
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
