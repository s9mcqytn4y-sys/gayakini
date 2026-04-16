package com.gayakini.operations.application

import com.gayakini.audit.application.AuditContext
import com.gayakini.audit.domain.AuditEvent
import com.gayakini.infrastructure.monitoring.OrderMetrics
import com.gayakini.operations.api.PackOrderRequest
import com.gayakini.operations.api.PackingResponse
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
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
}
