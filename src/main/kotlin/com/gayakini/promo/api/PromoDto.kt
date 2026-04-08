package com.gayakini.promo.api

import com.gayakini.promo.domain.Promo
import com.gayakini.promo.domain.PromoType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PromoResponse(
    val id: UUID,
    val code: String,
    val type: PromoType,
    val value: BigDecimal,
    val maxDiscountAmount: BigDecimal?,
    val minOrderValue: BigDecimal,
    val usageLimit: Int?,
    val currentUsage: Int,
    val startDate: Instant,
    val endDate: Instant,
    val isActive: Boolean,
) {
    companion object {
        fun fromEntity(entity: Promo) =
            PromoResponse(
                id = entity.id,
                code = entity.code,
                type = entity.type,
                value = entity.value,
                maxDiscountAmount = entity.maxDiscountAmount,
                minOrderValue = entity.minOrderValue,
                usageLimit = entity.usageLimit,
                currentUsage = entity.currentUsage,
                startDate = entity.startDate,
                endDate = entity.endDate,
                isActive = entity.isActive,
            )
    }
}

data class CreatePromoRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val code: String,
    @field:NotNull
    val type: PromoType,
    @field:NotNull
    @field:DecimalMin("0.0")
    val value: BigDecimal,
    @field:DecimalMin("0.0")
    val maxDiscountAmount: BigDecimal? = null,
    @field:NotNull
    @field:DecimalMin("0.0")
    val minOrderValue: BigDecimal = BigDecimal.ZERO,
    val usageLimit: Int? = null,
    @field:NotNull
    val startDate: Instant,
    @field:NotNull
    val endDate: Instant,
    val isActive: Boolean = true,
)

data class UpdatePromoRequest(
    @field:Size(max = 50)
    val code: String? = null,
    val type: PromoType? = null,
    @field:DecimalMin("0.0")
    val value: BigDecimal? = null,
    @field:DecimalMin("0.0")
    val maxDiscountAmount: BigDecimal? = null,
    @field:DecimalMin("0.0")
    val minOrderValue: BigDecimal? = null,
    val usageLimit: Int? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val isActive: Boolean? = null,
)
