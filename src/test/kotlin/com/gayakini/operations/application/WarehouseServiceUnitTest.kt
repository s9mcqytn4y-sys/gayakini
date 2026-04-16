package com.gayakini.operations.application

import com.gayakini.audit.application.AuditContext
import com.gayakini.infrastructure.monitoring.OrderMetrics
import com.gayakini.inventory.application.InventoryService
import com.gayakini.operations.api.PackOrderRequest
import com.gayakini.operations.api.RestockQCRequest
import com.gayakini.order.domain.Order
import com.gayakini.order.domain.OrderRepository
import com.gayakini.order.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional
import java.util.UUID

class WarehouseServiceUnitTest {
    private val orderRepository = mockk<OrderRepository>()
    private val inventoryService = mockk<InventoryService>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val auditContext = mockk<AuditContext>()
    private val orderMetrics = mockk<OrderMetrics>(relaxed = true)

    private val service =
        WarehouseService(
            orderRepository,
            inventoryService,
            eventPublisher,
            auditContext,
            orderMetrics,
        )

    @Test
    fun `packOrder should transition status to READY_TO_SHIP`() {
        // Given
        val orderId = UUID.randomUUID()
        val order =
            mockk<Order>(relaxed = true) {
                every { id } returns orderId
                every { orderNumber } returns "ORD-123"
                every { status } returns OrderStatus.PAID
            }
        val request =
            PackOrderRequest(
                weightInGrams = 1000,
                dimensionCm = "10x10x10",
                notes = "Handle with care",
            )

        every { orderRepository.findById(orderId) } returns Optional.of(order)
        every { orderRepository.save(any()) } returns order
        every { auditContext.getCurrentActor() } returns ("warehouse-user" to "OPERATOR")

        // When
        val response = service.packOrder(orderId, request)

        // Then
        verify { order.markAsReadyToShip() }
        verify { orderRepository.save(order) }
        verify { orderMetrics.recordOrderReadyToShip() }
        assertEquals(orderId, response.orderId)
    }

    @Test
    fun `packOrder should throw exception if order is not PAID`() {
        // Given
        val orderId = UUID.randomUUID()
        val order =
            mockk<Order>(relaxed = true) {
                every { status } returns OrderStatus.PENDING_PAYMENT
            }
        every { orderRepository.findById(orderId) } returns Optional.of(order)

        // When & Then
        assertThrows(IllegalStateException::class.java) {
            service.packOrder(
                orderId,
                PackOrderRequest(notes = "Invalid State Test", weightInGrams = 100, dimensionCm = "1x1x1"),
            )
        }
    }

    @Test
    fun `processReturnQC should delegate to InventoryService`() {
        // Given
        val orderId = UUID.randomUUID()
        val orderItemId = UUID.randomUUID()
        val request = RestockQCRequest(note = "Restocking after minor repair")

        every { inventoryService.restockOrderItemAfterQC(any(), any(), any()) } returns Unit

        // When
        val response = service.processReturnQC(orderId, orderItemId, request)

        // Then
        verify {
            inventoryService.restockOrderItemAfterQC(orderId, orderItemId, request.note)
        }
        assertEquals(orderId, response.orderId)
    }
}
