package com.gayakini.catalog.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_variants", schema = "commerce")
class ProductVariant(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,
    @Column(unique = true, nullable = false, length = 64)
    val sku: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VariantStatus = VariantStatus.ACTIVE,
    @Column(nullable = false, length = 50)
    var color: String,
    @Column(name = "size_code", nullable = false, length = 20)
    var sizeCode: String,
    @Column(name = "price_amount", nullable = false)
    var priceAmount: Long,
    @Column(name = "compare_at_amount")
    var compareAtAmount: Long? = null,
    @Column(name = "weight_grams", nullable = false)
    var weightGrams: Int = 0,
    @Column(name = "stock_on_hand", nullable = false)
    var stockOnHand: Int = 0,
    @Column(name = "stock_reserved", nullable = false)
    var stockReserved: Int = 0,
    @Column(name = "stock_available", insertable = false, updatable = false)
    val stockAvailable: Int = 0, // Generated column in DB
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)

enum class VariantStatus { ACTIVE, INACTIVE, ARCHIVED }
