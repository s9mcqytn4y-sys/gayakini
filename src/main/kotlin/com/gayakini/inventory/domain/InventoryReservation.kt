package com.gayakini.inventory.domain

import com.gayakini.catalog.domain.ProductVariant
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "inventory_reservations", schema = "commerce")
class InventoryReservation(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "order_id", nullable = false)
    val orderId: UUID,
    @Column(name = "order_item_id", nullable = false, unique = true)
    val orderItemId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    val variant: ProductVariant,
    @Column(nullable = false)
    val quantity: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReservationStatus = ReservationStatus.ACTIVE,
    @Column(name = "reserved_at", nullable = false)
    val reservedAt: Instant = Instant.now(),
    @Column(name = "released_at")
    var releasedAt: Instant? = null,
    @Column(name = "consumed_at")
    var consumedAt: Instant? = null,
    @Column(name = "release_reason")
    var releaseReason: String? = null,
)

enum class ReservationStatus { ACTIVE, RELEASED, CONSUMED }
