package com.gayakini.promo.domain

import com.gayakini.common.util.UuidV7Generator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.persistence.Transient
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.domain.Persistable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "promos", schema = "commerce")
@EntityListeners(AuditingEntityListener::class)
class Promo(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private val id: UUID = UuidV7Generator.generate(),
    @Column(name = "code", unique = true, nullable = false, length = 50)
    var code: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    var type: PromoType,
    @Column(name = "\"value\"", nullable = false, precision = 19, scale = 4)
    var value: BigDecimal,
    @Column(name = "max_discount_amount", precision = 19, scale = 4)
    var maxDiscountAmount: BigDecimal? = null,
    @Column(name = "min_order_value", nullable = false, precision = 19, scale = 4)
    var minOrderValue: BigDecimal = BigDecimal.ZERO,
    @Column(name = "usage_limit")
    var usageLimit: Int? = null,
    @Column(name = "current_usage", nullable = false)
    var currentUsage: Int = 0,
    @Column(name = "start_date", nullable = false)
    var startDate: Instant,
    @Column(name = "end_date", nullable = false)
    var endDate: Instant,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0,
) : Persistable<UUID> {
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    var createdBy: UUID? = null

    @LastModifiedBy
    @Column(name = "updated_by")
    var updatedBy: UUID? = null

    @Transient
    private var isNewRecord = true

    override fun getId(): UUID = id

    override fun isNew(): Boolean = isNewRecord

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNewRecord = false
    }

    fun isValid(
        orderSubtotal: BigDecimal,
        now: Instant = Instant.now(),
    ): Boolean {
        return when {
            !isActive -> false
            now.isBefore(startDate) || now.isAfter(endDate) -> false
            usageLimit != null && currentUsage >= (usageLimit ?: Int.MAX_VALUE) -> false
            orderSubtotal < minOrderValue -> false
            else -> true
        }
    }

    fun calculateDiscount(orderSubtotal: BigDecimal): BigDecimal {
        val discount =
            when (type) {
                PromoType.FIXED_AMOUNT -> value
                PromoType.PERCENTAGE -> {
                    val percentage = value.divide(BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)
                    orderSubtotal.multiply(percentage).setScale(0, java.math.RoundingMode.HALF_UP)
                }
            }

        val finalDiscount =
            if (type == PromoType.PERCENTAGE && maxDiscountAmount != null) {
                discount.min(maxDiscountAmount ?: discount)
            } else {
                discount.min(orderSubtotal)
            }

        return finalDiscount.setScale(0, java.math.RoundingMode.HALF_UP)
    }
}

enum class PromoType { FIXED_AMOUNT, PERCENTAGE }
