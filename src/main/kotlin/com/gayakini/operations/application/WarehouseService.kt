package com.gayakini.operations.application

import com.gayakini.audit.application.AuditContext
import com.gayakini.audit.domain.AuditEvent
import com.gayakini.infrastructure.monitoring.OrderMetrics
import com.gayakini.operations.api.PackOrderRequest
import com.gayakini.operations.api.PackingResponse
import com.gayakini.operations.api.RestockQCRequest
import com.gayakini.operations.api.RestockQCResponse
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import com.gayakini.inventory.application.InventoryService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class WarehouseService(
    private val orderRepository: OrderRepository,
    private val inventoryService: InventoryService,
    private val eventPublisher: ApplicationEventPublisher,
    private val auditContext: AuditContext,
    private val orderMetrics: OrderMetrics,
) {
    private val logger = LoggerFactory.getLogger(WarehouseService::class.java)

    /**
     * Lists orders that are paid and ready to be picked/packed.
     */
    fun getOrdersToPack(pageable: Pageable): Page<com.gayakini.order.domain.Order> {
        return orderRepository.findAllByStatus(OrderStatus.PAID, pageable)
    }

    /**
     * Transitions an order from PAID to READY_TO_SHIP after physical packing is done.
     */
    @Transactional
    fun packOrder(
        orderId: UUID,
        request: PackOrderRequest,
    ): PackingResponse {
        val order =
            orderRepository.findById(orderId)
                .orElseThrow { NoSuchElementException("Order tidak ditemukan.") }

        check(order.status == OrderStatus.PAID) {
            "Order harus dalam status PAID untuk diproses di gudang. Status saat ini: ${order.status}"
        }

        // Physical validation
        request.weightInGrams?.let {
            require(it > 0) { "Berat paket harus lebih besar dari 0 gram." }
        }
        request.dimensionCm?.let {
            val dimensions = it.split("x")
            val expectedDimensionParts = 3
            require(dimensions.size == expectedDimensionParts) { "Format dimensi harus PxLxT (contoh: 10x10x10)." }
            dimensions.forEach { dim ->
                require(dim.trim().toInt() > 0) { "Setiap dimensi harus lebih besar dari 0 cm." }
            }
        }

        val previousState = mapOf("status" to order.status)

        // Update order state
        order.markAsReadyToShip()

        val savedOrder = orderRepository.save(order)
        orderMetrics.recordOrderReadyToShip()

        val (actorId, actorRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = actorId,
                actorRole = actorRole,
                entityType = "ORDER",
                entityId = order.id.toString(),
                eventType = "ORDER_PACKED",
                previousState = previousState,
                newState =
                    mapOf(
                        "status" to order.status,
                        "notes" to request.notes,
                        "weight" to request.weightInGrams,
                        "dimensions" to request.dimensionCm,
                    ),
                reason = "Order picking and packing completed by warehouse staff.",
            ),
        )

        logger.info("Order {} packed and ready for shipping by {}", order.orderNumber, actorId)

        return PackingResponse(
            orderId = savedOrder.id,
            orderNumber = savedOrder.orderNumber,
            status = savedOrder.status.name,
            packedAt = Instant.now(),
        )
    }

    /**
     * Processes an item that has been returned and passed QC, moving it back to STORAGE.
     */
    @Transactional
    fun processReturnQC(
        orderId: UUID,
        orderItemId: UUID,
        request: RestockQCRequest,
    ): RestockQCResponse {
        logger.info("Processing QC restock for order {} item {}", orderId, orderItemId)

        // Delegate to InventoryService which handles the movement and stock update
        inventoryService.restockOrderItemAfterQC(
            orderId = orderId,
            orderItemId = orderItemId,
            note = request.note,
        )

        return RestockQCResponse(
            orderId = orderId,
            orderItemId = orderItemId,
        )
    }
}
