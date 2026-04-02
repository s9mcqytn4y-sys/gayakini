package com.gayakini.inventory.application

import com.gayakini.catalog.domain.ProductVariantRepository
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
) {
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

        if (variant.stockAvailable < quantity) {
            throw IllegalStateException(
                "Stok tidak mencukupi untuk varian ${variant.sku}. Tersedia: ${variant.stockAvailable}",
            )
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
        return reservationRepository.save(reservation)
    }

    @Transactional
    fun releaseReservations(
        orderId: UUID,
        reason: String,
    ) {
        val reservations = reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)
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
    }
}
