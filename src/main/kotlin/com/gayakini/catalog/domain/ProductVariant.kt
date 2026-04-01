package com.gayakini.catalog.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_variants")
class ProductVariant(
    @Id
    val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,
    @Column(unique = true, nullable = false)
    val sku: String,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var price: Long,
    @Column(nullable = false)
    var stock: Int = 0,
    @Column(name = "weight_grams", nullable = false)
    var weightGrams: Int = 0,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
