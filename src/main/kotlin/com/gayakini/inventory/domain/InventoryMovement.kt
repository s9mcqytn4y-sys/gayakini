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
@Table(name = "inventory_movements", schema = "commerce")
class InventoryMovement(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UuidV7Generator.generate(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    val variant: ProductVariant,
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    @Column(name = "source_location", nullable = false)
    val sourceLocation: String,
    @Column(name = "destination_location", nullable = false)
    val destinationLocation: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    val movementType: MovementType,
    @Column(name = "reference_id")
    val referenceId: String? = null,
    @Column(name = "notes")
    val notes: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)

enum class MovementType {
    INTERNAL_TRANSFER,
    WAREHOUSE_PICKING,
    RESTOCKING,
    RETURNS,
}
