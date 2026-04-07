package com.gayakini.catalog.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_variants", schema = "commerce")
class ProductVariant(
    @Id
    private val id: UUID,
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
    private var _stockAvailable: Int? = 0,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
) : Persistable<UUID> {
    @Transient
    private var isNewRecord = true

    val stockAvailable: Int
        get() = _stockAvailable ?: (stockOnHand - stockReserved).coerceAtLeast(0)

    override fun getId(): UUID = id

    override fun isNew(): Boolean = isNewRecord

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNewRecord = false
    }
}

enum class VariantStatus { ACTIVE, INACTIVE, ARCHIVED }
