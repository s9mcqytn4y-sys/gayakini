package com.gayakini.operations.api

import com.gayakini.operations.application.WarehouseService
import com.gayakini.order.domain.Order
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/operations")
@Tag(name = "Operations", description = "Warehouse and order fulfillment operations")
class OperationsController(
    private val warehouseService: WarehouseService,
) {
    @GetMapping("/orders/to-pack")
    @Operation(summary = "Get list of orders ready to be packed")
    fun getOrdersToPack(pageable: Pageable): Page<Order> {
        return warehouseService.getOrdersToPack(pageable)
    }

    @PostMapping("/orders/{orderId}/items/{orderItemId}/qc-restock")
    @Operation(summary = "Process QC for a returned item and restock it to STORAGE")
    fun restockAfterQC(
        @PathVariable orderId: UUID,
        @PathVariable orderItemId: UUID,
        @RequestBody request: RestockQCRequest,
    ): RestockQCResponse {
        return warehouseService.processReturnQC(orderId, orderItemId, request)
    }
}
