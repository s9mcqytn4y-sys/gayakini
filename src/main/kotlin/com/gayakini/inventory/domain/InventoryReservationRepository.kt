package com.gayakini.inventory.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface InventoryReservationRepository : JpaRepository<InventoryReservation, UUID> {
    fun findByOrderId(orderId: UUID): List<InventoryReservation>

    fun findByOrderItemId(orderItemId: UUID): Optional<InventoryReservation>

    fun findAllByOrderIdAndStatus(orderId: UUID, status: ReservationStatus): List<InventoryReservation>
}
