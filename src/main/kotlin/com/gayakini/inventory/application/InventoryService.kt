package com.gayakini.inventory.application

import com.gayakini.audit.application.AuditContext
import com.gayakini.audit.domain.AuditEvent
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.NoSuchElementException
import java.util.UUID

@Service
class InventoryService(
    private val variantRepository: ProductVariantRepository,
    private val reservationRepository: InventoryReservationRepository,
    private val adjustmentRepository: InventoryAdjustmentRepository,
    private val movementRepository: InventoryMovementRepository,
    private val auditContext: AuditContext,
    private val eventPublisher: org.springframework.context.ApplicationEventPublisher,
) {
    @Transactional
    fun recordMovement(
        variantId: UUID,
        quantity: Int,
        source: String,
        destination: String,
        type: MovementType,
        referenceId: String? = null,
        notes: String? = null,
    ): InventoryMovement {
        val variant =
            variantRepository.findById(variantId)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        val movement =
            InventoryMovement(
                variant = variant,
                quantity = quantity,
                sourceLocation = source,
                destinationLocation = destination,
                movementType = type,
                referenceId = referenceId,
                notes = notes,
            )

        val savedMovement = movementRepository.save(movement)

        val (actorId, actorRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = actorId,
                actorRole = actorRole,
                entityType = "INVENTORY_MOVEMENT",
                entityId = savedMovement.id.toString(),
                eventType = "MOVEMENT_RECORDED",
                newState =
                    mapOf(
                        "variantSku" to variant.sku,
                        "quantity" to quantity,
                        "source" to source,
                        "destination" to destination,
                        "type" to type,
                    ),
                reason = notes,
            ),
        )

        return savedMovement
    }

    @Transactional
    fun adjustStock(
        variantId: UUID,
        quantityDelta: Int,
        reason: AdjustmentReason,
        note: String? = null,
        idempotencyKey: String? = null,
    ): InventoryAdjustment {
        if (idempotencyKey != null && adjustmentRepository.existsByIdempotencyKey(idempotencyKey)) {
            check(false) { "Adjustment with idempotency key $idempotencyKey already exists" }
        }

        val variant =
            variantRepository.findWithLockById(variantId)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        variant.stockOnHand += quantityDelta
        require(variant.stockOnHand >= 0) { "Stok on hand tidak boleh negatif setelah penyesuaian." }
        variant.updatedAt = Instant.now()
        variantRepository.save(variant)

        val (actorId, _) = auditContext.getCurrentActor()

        val adjustment =
            InventoryAdjustment(
                variant = variant,
                quantityDelta = quantityDelta,
                reasonCode = reason,
                note = note,
                actorSubject = actorId,
                idempotencyKey = idempotencyKey,
                stockOnHandAfter = variant.stockOnHand,
                stockReservedAfter = variant.stockReserved,
            )

        val savedAdjustment = adjustmentRepository.save(adjustment)

        val (auditId, auditRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = auditId,
                actorRole = auditRole,
                entityType = "INVENTORY",
                entityId = variant.sku,
                eventType = "STOCK_ADJUSTED",
                newState =
                    mapOf(
                        "delta" to quantityDelta,
                        "reason" to reason,
                        "onHand" to variant.stockOnHand,
                        "reserved" to variant.stockReserved,
                    ),
                reason = note,
            ),
        )

        return savedAdjustment
    }

    @Transactional
    fun reserveStock(
        orderId: UUID,
        orderItemId: UUID,
        variantId: UUID,
        quantity: Int,
    ): InventoryReservation {
        val variant =
            variantRepository.findWithLockById(variantId)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        check(variant.stockAvailable >= quantity) {
            "Stok tidak mencukupi untuk varian ${variant.sku}. Tersedia: ${variant.stockAvailable}"
        }

        // Update variant stock
        variant.stockReserved += quantity
        variant.updatedAt = Instant.now()
        variantRepository.save(variant)

        // Create reservation record
        val reservation =
            InventoryReservation(
                orderId = orderId,
                orderItemId = orderItemId,
                variant = variant,
                quantity = quantity,
                status = ReservationStatus.ACTIVE,
            )
        reservationRepository.save(reservation)

        // Record adjustment for audit/ledger
        val (actorId, _) = auditContext.getCurrentActor()
        val adjustment =
            InventoryAdjustment(
                variant = variant,
                quantityDelta = 0,
                reasonCode = AdjustmentReason.RESERVATION,
                note = "Reservation for order $orderId item $orderItemId",
                actorSubject = actorId,
                stockOnHandAfter = variant.stockOnHand,
                stockReservedAfter = variant.stockReserved,
            )
        adjustmentRepository.save(adjustment)

        val (auditId, auditRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = auditId,
                actorRole = auditRole,
                entityType = "INVENTORY",
                entityId = variant.sku,
                eventType = "STOCK_RESERVED",
                newState =
                    mapOf(
                        "orderId" to orderId,
                        "orderItemId" to orderItemId,
                        "quantity" to quantity,
                        "onHand" to variant.stockOnHand,
                        "reserved" to variant.stockReserved,
                    ),
                reason = "Reservation for order $orderId",
            ),
        )

        return reservation
    }

    @Transactional
    fun releaseReservations(
        orderId: UUID,
        reason: String,
    ) {
        val reservations =
            reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)
                .sortedBy { it.variant.id }

        reservations.forEach { reservation ->
            val variant =
                variantRepository.findWithLockById(reservation.variant.id)
                    .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

            variant.stockReserved -= reservation.quantity
            variant.updatedAt = Instant.now()
            variantRepository.save(variant)

            reservation.status = ReservationStatus.RELEASED
            reservation.releasedAt = Instant.now()
            reservation.releaseReason = reason
            reservationRepository.save(reservation)

            // Record adjustment for ledger
            val (actorId, _) = auditContext.getCurrentActor()
            val adjustment =
                InventoryAdjustment(
                    variant = variant,
                    quantityDelta = 0,
                    reasonCode = AdjustmentReason.RESERVATION_RELEASE,
                    note = "Release: $reason (Order $orderId)",
                    actorSubject = actorId,
                    stockOnHandAfter = variant.stockOnHand,
                    stockReservedAfter = variant.stockReserved,
                )
            adjustmentRepository.save(adjustment)

            val (auditId, auditRole) = auditContext.getCurrentActor()
            eventPublisher.publishEvent(
                AuditEvent(
                    actorId = auditId,
                    actorRole = auditRole,
                    entityType = "INVENTORY",
                    entityId = variant.sku,
                    eventType = "STOCK_RELEASED",
                    newState =
                        mapOf(
                            "orderId" to orderId,
                            "quantity" to reservation.quantity,
                            "onHand" to variant.stockOnHand,
                            "reserved" to variant.stockReserved,
                        ),
                    reason = reason,
                ),
            )
        }
    }

    @Transactional
    fun releaseReservation(
        orderItemId: UUID,
        reason: String,
    ) {
        val reservation =
            reservationRepository.findByOrderItemId(orderItemId)
                .orElseThrow { NoSuchElementException("Reservasi tidak ditemukan.") }

        if (reservation.status != ReservationStatus.ACTIVE) return

        val variant =
            variantRepository.findWithLockById(reservation.variant.id)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        variant.stockReserved -= reservation.quantity
        variant.updatedAt = Instant.now()
        variantRepository.save(variant)

        reservation.status = ReservationStatus.RELEASED
        reservation.releasedAt = Instant.now()
        reservation.releaseReason = reason
        reservationRepository.save(reservation)

        // Record adjustment for ledger
        val (actorId, _) = auditContext.getCurrentActor()
        val adjustment =
            InventoryAdjustment(
                variant = variant,
                quantityDelta = 0,
                reasonCode = AdjustmentReason.RESERVATION_RELEASE,
                note = "Release: $reason (Item $orderItemId)",
                actorSubject = actorId,
                stockOnHandAfter = variant.stockOnHand,
                stockReservedAfter = variant.stockReserved,
            )
        adjustmentRepository.save(adjustment)

        val (auditId, auditRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = auditId,
                actorRole = auditRole,
                entityType = "INVENTORY",
                entityId = variant.sku,
                eventType = "STOCK_RELEASED",
                newState =
                    mapOf(
                        "orderItemId" to orderItemId,
                        "quantity" to reservation.quantity,
                        "onHand" to variant.stockOnHand,
                        "reserved" to variant.stockReserved,
                    ),
                reason = reason,
            ),
        )
    }

    @Transactional
    fun consumeReservation(orderItemId: UUID) {
        val reservation =
            reservationRepository.findByOrderItemId(orderItemId)
                .orElseThrow { NoSuchElementException("Reservasi tidak ditemukan.") }

        if (reservation.status != ReservationStatus.ACTIVE) return

        val variant =
            variantRepository.findWithLockById(reservation.variant.id)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        // stockOnHand decreases, stockReserved decreases (because it becomes a real sale)
        variant.stockOnHand -= reservation.quantity
        variant.stockReserved -= reservation.quantity
        variant.updatedAt = Instant.now()
        variantRepository.save(variant)

        reservation.status = ReservationStatus.CONSUMED
        reservation.consumedAt = Instant.now()
        reservationRepository.save(reservation)

        // Record adjustment for ledger (SALE)
        val (actorId, _) = auditContext.getCurrentActor()
        val adjustment =
            InventoryAdjustment(
                variant = variant,
                quantityDelta = -reservation.quantity,
                reasonCode = AdjustmentReason.SALE,
                note = "Sale/Consumption for item $orderItemId",
                actorSubject = actorId,
                stockOnHandAfter = variant.stockOnHand,
                stockReservedAfter = variant.stockReserved,
            )
        adjustmentRepository.save(adjustment)

        // Record movement from storage to packing area
        recordMovement(
            variantId = variant.id,
            quantity = reservation.quantity,
            source = "STORAGE",
            destination = "PACKING",
            type = MovementType.WAREHOUSE_PICKING,
            referenceId = orderItemId.toString(),
            notes = "Picked for order item $orderItemId",
        )

        val (auditId, auditRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = auditId,
                actorRole = auditRole,
                entityType = "INVENTORY",
                entityId = variant.sku,
                eventType = "STOCK_CONSUMED",
                newState =
                    mapOf(
                        "orderItemId" to orderItemId,
                        "quantity" to reservation.quantity,
                        "onHand" to variant.stockOnHand,
                        "reserved" to variant.stockReserved,
                    ),
                reason = "Sale/Consumption for item $orderItemId",
            ),
        )
    }

    @Transactional
    fun restockOrder(
        orderId: UUID,
        reason: AdjustmentReason,
        note: String? = null,
    ) {
        val consumedReservations =
            reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.CONSUMED)
                .sortedBy { it.variant.id }

        consumedReservations.forEach { reservation ->
            val variant =
                variantRepository.findWithLockById(reservation.variant.id)
                    .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

            variant.stockOnHand += reservation.quantity
            variant.updatedAt = Instant.now()
            variantRepository.save(variant)

            val (actorId, _) = auditContext.getCurrentActor()
            val adjustment =
                InventoryAdjustment(
                    variant = variant,
                    quantityDelta = reservation.quantity,
                    reasonCode = reason,
                    note = note ?: "Restock for order $orderId",
                    actorSubject = actorId,
                    stockOnHandAfter = variant.stockOnHand,
                    stockReservedAfter = variant.stockReserved,
                )
            adjustmentRepository.save(adjustment)

            // Record movement from customer/unknown back to storage
            recordMovement(
                variantId = variant.id,
                quantity = reservation.quantity,
                source = "EXTERNAL",
                destination = "STORAGE",
                type = MovementType.RESTOCKING,
                referenceId = orderId.toString(),
                notes = "Restock: $note (Order $orderId)",
            )

            val (auditId, auditRole) = auditContext.getCurrentActor()
            eventPublisher.publishEvent(
                AuditEvent(
                    actorId = auditId,
                    actorRole = auditRole,
                    entityType = "INVENTORY",
                    entityId = variant.sku,
                    eventType = "STOCK_RESTOCKED",
                    newState =
                        mapOf(
                            "orderId" to orderId,
                            "quantity" to reservation.quantity,
                            "onHand" to variant.stockOnHand,
                            "reserved" to variant.stockReserved,
                        ),
                    reason = note ?: "Restock for order $orderId",
                ),
            )
        }
    }

    @Transactional
    fun restockOrderItemAfterQC(
        orderId: UUID,
        orderItemId: UUID,
        note: String? = null,
    ) {
        val reservation =
            reservationRepository.findByOrderItemId(orderItemId)
                .orElseThrow { NoSuchElementException("Reservasi tidak ditemukan.") }

        check(reservation.orderId == orderId) { "Item tidak cocok dengan pesanan." }
        check(reservation.status == ReservationStatus.CONSUMED) {
            "Hanya item yang sudah dikonsumsi (paid/shipped) yang dapat di-restock melalui QC."
        }

        val variant =
            variantRepository.findWithLockById(reservation.variant.id)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        variant.stockOnHand += reservation.quantity
        variant.updatedAt = Instant.now()
        variantRepository.save(variant)

        val (actorId, _) = auditContext.getCurrentActor()
        val adjustment =
            InventoryAdjustment(
                variant = variant,
                quantityDelta = reservation.quantity,
                reasonCode = AdjustmentReason.RETURN_RESTOCK_QC,
                note = note ?: "QC Restock for order $orderId item $orderItemId",
                actorSubject = actorId,
                stockOnHandAfter = variant.stockOnHand,
                stockReservedAfter = variant.stockReserved,
            )
        adjustmentRepository.save(adjustment)

        // Record movement from returns area to storage
        recordMovement(
            variantId = variant.id,
            quantity = reservation.quantity,
            source = "RETURNS_QC",
            destination = "STORAGE",
            type = MovementType.RETURNS,
            referenceId = orderItemId.toString(),
            notes = "QC Pass Restock: $note",
        )

        val (auditId, auditRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = auditId,
                actorRole = auditRole,
                entityType = "INVENTORY",
                entityId = variant.sku,
                eventType = "STOCK_RESTOCKED_QC",
                newState =
                    mapOf(
                        "orderId" to orderId,
                        "orderItemId" to orderItemId,
                        "quantity" to reservation.quantity,
                        "onHand" to variant.stockOnHand,
                        "reserved" to variant.stockReserved,
                    ),
                reason = note ?: "QC Restock for order $orderId item $orderItemId",
            ),
        )
    }
}
