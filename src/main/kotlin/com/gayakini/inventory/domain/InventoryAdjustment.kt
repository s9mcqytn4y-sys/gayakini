package com.gayakini.inventory.domain

import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.common.util.UuidV7Generator
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
@Table(name = "inventory_adjustments", schema = "commerce")
class InventoryAdjustment(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UuidV7Generator.generate(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    val variant: ProductVariant,
    @Column(name = "quantity_delta", nullable = false)
    val quantityDelta: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false)
    val reasonCode: AdjustmentReason,
    @Column(name = "note")
    val note: String? = null,
    @Column(name = "actor_subject")
    val actorSubject: String? = null,
    @Column(name = "idempotency_key")
    val idempotencyKey: String? = null,
    @Column(name = "stock_on_hand_after", nullable = false)
    val stockOnHandAfter: Int,
    @Column(name = "stock_reserved_after", nullable = false)
    val stockReservedAfter: Int,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)

enum class AdjustmentReason {
    INITIAL_STOCK,
    MANUAL_CORRECTION,
    DAMAGE,
    LOST,
    FOUND,
    RETURN_RESTOCK,
    RESERVATION,
    RESERVATION_RELEASE,
    SALE,
    CANCELLATION_RESTOCK,
    RETURN_RESTOCK_QC,
}
