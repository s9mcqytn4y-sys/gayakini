package com.gayakini.inventory.application

import com.gayakini.audit.application.AuditContext
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.inventory.domain.AdjustmentReason
import com.gayakini.inventory.domain.InventoryAdjustment
import com.gayakini.inventory.domain.InventoryAdjustmentRepository
import com.gayakini.inventory.domain.InventoryReservation
import com.gayakini.inventory.domain.InventoryReservationRepository
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
    private val auditContext: AuditContext,
) {
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

        return adjustmentRepository.save(adjustment)
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
        }
    }
}
