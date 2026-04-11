package com.gayakini.inventory.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.inventory.application.InventoryService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/admin/inventory")
class AdminInventoryController(
    private val inventoryService: InventoryService,
) {
    @PostMapping("/adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    fun adjustStock(
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
        @Valid @RequestBody
        request: InventoryAdjustmentRequest,
    ): InventoryAdjustmentResponseWrapper {
        val adjustment =
            inventoryService.adjustStock(
                variantId = request.variantId,
                quantityDelta = request.quantityDelta,
                reason = request.reason,
                note = request.note,
                idempotencyKey = idempotencyKey ?: request.idempotencyKey,
            )

        return InventoryAdjustmentResponseWrapper(
            message = "Penyesuaian stok berhasil dicatat.",
            data = InventoryAdjustmentResponse.fromEntity(adjustment),
            meta = ApiMeta(),
        )
    }
}

data class InventoryAdjustmentResponseWrapper(
    val message: String,
    val data: InventoryAdjustmentResponse,
    val meta: ApiMeta,
)
