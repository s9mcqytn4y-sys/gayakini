package com.gayakini.inventory.application

import com.gayakini.audit.application.AuditContext
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.inventory.domain.AdjustmentReason
import com.gayakini.inventory.domain.InventoryAdjustmentRepository
import com.gayakini.inventory.domain.InventoryReservation
import com.gayakini.inventory.domain.InventoryReservationRepository
import com.gayakini.inventory.domain.ReservationStatus
import com.gayakini.audit.domain.AuditEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.util.*

class InventoryServiceUnitTest {
    private val variantRepository = mockk<ProductVariantRepository>()
    private val reservationRepository = mockk<InventoryReservationRepository>()
    private val adjustmentRepository = mockk<InventoryAdjustmentRepository>()
    private val auditContext = mockk<AuditContext>()
    private val eventPublisher = mockk<ApplicationEventPublisher>()

    private val inventoryService =
        InventoryService(
            variantRepository,
            reservationRepository,
            adjustmentRepository,
            auditContext,
            eventPublisher,
        )

    private fun createVariant(
        onHand: Int,
        reserved: Int,
    ): ProductVariant {
        return ProductVariant(
            id = UUID.randomUUID(),
            product = mockk<Product>(),
            sku = "TEST-SKU",
            color = "Black",
            sizeCode = "L",
            priceAmount = 50000L,
            stockOnHand = onHand,
            stockReserved = reserved,
        )
    }

    @Test
    fun `reserveStock should increase stockReserved and create reservation`() {
        val variant = createVariant(onHand = 10, reserved = 2)
        val variantId = variant.id
        val orderId = UUID.randomUUID()
        val orderItemId = UUID.randomUUID()

        every { variantRepository.findWithLockById(variantId) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { reservationRepository.save(any()) } returns mockk()
        every { adjustmentRepository.save(any()) } returns mockk()
        every { auditContext.getCurrentActor() } returns (UUID.randomUUID().toString() to "USER")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        val reservation = inventoryService.reserveStock(orderId, orderItemId, variantId, 3)

        assertEquals(5, variant.stockReserved)
        assertEquals(10, variant.stockOnHand)
        assertEquals(ReservationStatus.ACTIVE, reservation.status)
        assertEquals(3, reservation.quantity)

        verify { variantRepository.save(variant) }
        verify { reservationRepository.save(any()) }
    }

    @Test
    fun `reserveStock should throw exception if stock not available`() {
        val variant = createVariant(onHand = 10, reserved = 8) // available: 2
        val variantId = variant.id

        every { variantRepository.findWithLockById(variantId) } returns Optional.of(variant)

        assertThrows<IllegalStateException> {
            inventoryService.reserveStock(UUID.randomUUID(), UUID.randomUUID(), variantId, 5)
        }
    }

    @Test
    fun `consumeReservation should decrease onHand and reserved stock`() {
        val variant = createVariant(onHand = 10, reserved = 3)
        val orderItemId = UUID.randomUUID()
        val reservation =
            InventoryReservation(
                orderId = UUID.randomUUID(),
                orderItemId = orderItemId,
                variant = variant,
                quantity = 3,
                status = ReservationStatus.ACTIVE,
            )

        every { reservationRepository.findByOrderItemId(orderItemId) } returns Optional.of(reservation)
        every { variantRepository.findWithLockById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { reservationRepository.save(any()) } returns reservation
        every { adjustmentRepository.save(any()) } returns mockk()
        every { auditContext.getCurrentActor() } returns (UUID.randomUUID().toString() to "USER")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        inventoryService.consumeReservation(orderItemId)

        assertEquals(0, variant.stockReserved)
        assertEquals(7, variant.stockOnHand)
        assertEquals(ReservationStatus.CONSUMED, reservation.status)

        verify { variantRepository.save(variant) }
    }

    @Test
    fun `releaseReservation should decrease reserved stock and keep onHand`() {
        val variant = createVariant(onHand = 10, reserved = 3)
        val orderItemId = UUID.randomUUID()
        val reservation =
            InventoryReservation(
                orderId = UUID.randomUUID(),
                orderItemId = orderItemId,
                variant = variant,
                quantity = 3,
                status = ReservationStatus.ACTIVE,
            )

        every { reservationRepository.findByOrderItemId(orderItemId) } returns Optional.of(reservation)
        every { variantRepository.findWithLockById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { reservationRepository.save(any()) } returns reservation
        every { adjustmentRepository.save(any()) } returns mockk()
        every { auditContext.getCurrentActor() } returns (UUID.randomUUID().toString() to "USER")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        inventoryService.releaseReservation(orderItemId, "Cancelled")

        assertEquals(0, variant.stockReserved)
        assertEquals(10, variant.stockOnHand)
        assertEquals(ReservationStatus.RELEASED, reservation.status)
        assertEquals("Cancelled", reservation.releaseReason)
    }

    @Test
    fun `restockOrderItemAfterQC should increase onHand and create QC adjustment`() {
        val variant = createVariant(onHand = 10, reserved = 0)
        val orderId = UUID.randomUUID()
        val orderItemId = UUID.randomUUID()
        val reservation =
            InventoryReservation(
                orderId = orderId,
                orderItemId = orderItemId,
                variant = variant,
                quantity = 3,
                status = ReservationStatus.CONSUMED,
            )

        every { reservationRepository.findByOrderItemId(orderItemId) } returns Optional.of(reservation)
        every { variantRepository.findWithLockById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { adjustmentRepository.save(any()) } returns mockk()
        every { auditContext.getCurrentActor() } returns (UUID.randomUUID().toString() to "ADMIN")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        inventoryService.restockOrderItemAfterQC(orderId, orderItemId, "Item passed inspection")

        assertEquals(13, variant.stockOnHand)
        verify {
            adjustmentRepository.save(
                match {
                    it.reasonCode == AdjustmentReason.RETURN_RESTOCK_QC && it.quantityDelta == 3
                },
            )
        }
    }
}
