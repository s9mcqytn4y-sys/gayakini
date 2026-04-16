package com.gayakini.inventory.application

import com.gayakini.audit.application.AuditContext
import com.gayakini.audit.domain.AuditEvent
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.inventory.domain.AdjustmentReason
import com.gayakini.inventory.domain.InventoryAdjustment
import com.gayakini.inventory.domain.InventoryAdjustmentRepository
import com.gayakini.inventory.domain.InventoryMovement
import com.gayakini.inventory.domain.InventoryMovementRepository
import com.gayakini.inventory.domain.InventoryReservation
import com.gayakini.inventory.domain.InventoryReservationRepository
import com.gayakini.inventory.domain.MovementType
import com.gayakini.inventory.domain.ReservationStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional
import java.util.UUID

class InventoryServiceUnitTest {
    private val variantRepository = mockk<ProductVariantRepository>()
    private val reservationRepository = mockk<InventoryReservationRepository>()
    private val adjustmentRepository = mockk<InventoryAdjustmentRepository>()
    private val movementRepository = mockk<InventoryMovementRepository>()
    private val auditContext = mockk<AuditContext>()
    private val eventPublisher = mockk<ApplicationEventPublisher>()

    private val inventoryService =
        InventoryService(
            variantRepository,
            reservationRepository,
            adjustmentRepository,
            movementRepository,
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
        every { reservationRepository.save(any()) } answers { firstArg() }
        every { adjustmentRepository.save(any()) } answers { firstArg() }
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
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { reservationRepository.save(any()) } answers { firstArg() }
        every { adjustmentRepository.save(any()) } answers { firstArg() }
        every { movementRepository.save(any()) } answers { firstArg() }
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
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { reservationRepository.save(any()) } answers { firstArg() }
        every { adjustmentRepository.save(any()) } answers { firstArg() }
        every { movementRepository.save(any()) } answers { firstArg() }
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
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { adjustmentRepository.save(any()) } returns mockk()
        every { movementRepository.save(any()) } answers { firstArg() }
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

    @Test
    fun `recordMovement should create and save movement`() {
        val variant = createVariant(onHand = 10, reserved = 0)
        val variantId = variant.id
        every { variantRepository.findById(variantId) } returns Optional.of(variant)
        every {
            movementRepository.save(any())
        } answers { it.invocation.args[0] as com.gayakini.inventory.domain.InventoryMovement }
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { auditContext.getCurrentActor() } returns ("actor-1" to "ADMIN")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        val result =
            inventoryService.recordMovement(
                variantId = variantId,
                quantity = 5,
                source = "A",
                destination = "B",
                type = MovementType.INTERNAL_TRANSFER,
                referenceId = "REF-1",
                notes = "Test movement",
            )

        assertEquals(variantId, result.variant.id)
        assertEquals(5, result.quantity)
        assertEquals("A", result.sourceLocation)
        assertEquals("B", result.destinationLocation)
        assertEquals(MovementType.INTERNAL_TRANSFER, result.movementType)
        assertEquals("REF-1", result.referenceId)
        assertEquals("Test movement", result.notes)

        verify { movementRepository.save(any()) }
    }

    @Test
    fun `adjustStock should update onHand and record adjustment`() {
        val variant = createVariant(onHand = 10, reserved = 2)
        val variantId = variant.id
        every { variantRepository.findWithLockById(variantId) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { adjustmentRepository.save(any()) } answers { firstArg() }
        every { adjustmentRepository.existsByIdempotencyKey(any()) } returns false
        every {
            adjustmentRepository.save(any())
        } answers { it.invocation.args[0] as com.gayakini.inventory.domain.InventoryAdjustment }
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { auditContext.getCurrentActor() } returns ("actor-1" to "ADMIN")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        val result =
            inventoryService.adjustStock(
                variantId = variantId,
                quantityDelta = 5,
                reason = AdjustmentReason.MANUAL_CORRECTION,
                note = "Manual correction",
            )

        assertEquals(15, variant.stockOnHand)
        assertEquals(5, result.quantityDelta)
        assertEquals(AdjustmentReason.MANUAL_CORRECTION, result.reasonCode)
        assertEquals(15, result.stockOnHandAfter)

        verify { variantRepository.save(variant) }
        verify { adjustmentRepository.save(any()) }
    }

    @Test
    fun `adjustStock should throw exception if stock becomes negative`() {
        val variant = createVariant(onHand = 5, reserved = 0)
        every { variantRepository.findWithLockById(variant.id) } returns Optional.of(variant)

        assertThrows<IllegalArgumentException> {
            inventoryService.adjustStock(variant.id, -10, AdjustmentReason.MANUAL_CORRECTION)
        }
    }

    @Test
    fun `releaseReservations should release all active reservations for order`() {
        val variant = createVariant(onHand = 10, reserved = 5)
        val orderId = UUID.randomUUID()
        val reservation =
            InventoryReservation(
                orderId = orderId,
                orderItemId = UUID.randomUUID(),
                variant = variant,
                quantity = 5,
                status = ReservationStatus.ACTIVE,
            )

        every {
            reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)
        } returns listOf(reservation)
        every { variantRepository.findWithLockById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { reservationRepository.save(any()) } answers { firstArg() }
        every { adjustmentRepository.save(any()) } answers { firstArg() }
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { auditContext.getCurrentActor() } returns ("actor-1" to "ADMIN")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        inventoryService.releaseReservations(orderId, "Order Cancelled")

        assertEquals(0, variant.stockReserved)
        assertEquals(ReservationStatus.RELEASED, reservation.status)
        verify { variantRepository.save(variant) }
    }

    @Test
    fun `releaseReservation should release specific active reservation`() {
        val variant = createVariant(onHand = 10, reserved = 5)
        val orderItemId = UUID.randomUUID()
        val reservation =
            InventoryReservation(
                orderId = UUID.randomUUID(),
                orderItemId = orderItemId,
                variant = variant,
                quantity = 5,
                status = ReservationStatus.ACTIVE,
            )

        every { reservationRepository.findByOrderItemId(orderItemId) } returns Optional.of(reservation)
        every { variantRepository.findWithLockById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { reservationRepository.save(any()) } answers { firstArg() }
        every { adjustmentRepository.save(any()) } answers { firstArg() }
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { auditContext.getCurrentActor() } returns ("actor-1" to "ADMIN")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        inventoryService.releaseReservation(orderItemId, "User Cancelled Item")

        assertEquals(0, variant.stockReserved)
        assertEquals(ReservationStatus.RELEASED, reservation.status)
        verify { variantRepository.save(variant) }
        verify { reservationRepository.save(reservation) }
    }

    @Test
    fun `restockOrder should restore onHand stock for consumed reservations`() {
        val variant = createVariant(onHand = 10, reserved = 0)
        val orderId = UUID.randomUUID()
        val reservation =
            InventoryReservation(
                orderId = orderId,
                orderItemId = UUID.randomUUID(),
                variant = variant,
                quantity = 2,
                status = ReservationStatus.CONSUMED,
            )

        every {
            reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.CONSUMED)
        } returns listOf(reservation)
        every { variantRepository.findWithLockById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } returns variant
        every { adjustmentRepository.save(any()) } answers { firstArg() }
        every { movementRepository.save(any()) } answers { firstArg() }
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { auditContext.getCurrentActor() } returns ("actor-1" to "ADMIN")
        every { eventPublisher.publishEvent(any<AuditEvent>()) } returns Unit

        inventoryService.restockOrder(orderId, AdjustmentReason.CANCELLATION_RESTOCK)

        assertEquals(12, variant.stockOnHand)
        verify { variantRepository.save(variant) }
        verify { movementRepository.save(any()) }
    }
}
