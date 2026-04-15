package com.gayakini.inventory.api

import com.gayakini.common.api.ApiResponse
import com.gayakini.inventory.application.InventoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/admin/inventory")
@Tag(name = "Admin Inventory", description = "Inventory and stock management for administrators (Internal/English).")
class AdminInventoryController(
    private val inventoryService: InventoryService,
) {
    @PostMapping("/adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Record stock adjustment",
        description = "Adjust the stock level for a specific product variant.",
    )
    fun adjustStock(
        @Parameter(description = "Idempotency key for preventing duplicates")
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
        @Valid @RequestBody
        request: InventoryAdjustmentRequest,
    ): ApiResponse<InventoryAdjustmentResponse> {
        val adjustment =
            inventoryService.adjustStock(
                variantId = request.variantId,
                quantityDelta = request.quantityDelta,
                reason = request.reason,
                note = request.note,
                idempotencyKey = idempotencyKey ?: request.idempotencyKey,
            )

        return ApiResponse(
            message = "Stock adjustment recorded successfully.",
            data = InventoryAdjustmentResponse.fromEntity(adjustment),
        )
    }
}
