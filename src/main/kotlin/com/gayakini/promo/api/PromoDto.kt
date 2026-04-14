package com.gayakini.promo.api

import com.gayakini.promo.domain.Promo
import com.gayakini.promo.domain.PromoType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Schema(description = "Detailed information about a promotion.")
data class PromoResponse(
    val success: Boolean = true,
    @Schema(description = "Unique promo ID", example = "550e8400-e29b-41d4-a716-446655440200")
    val id: UUID,
    @Schema(description = "Unique promo code", example = "LEBARAN_SALE")
    val code: String,
    @Schema(description = "Discount type (PERCENTAGE or FIXED)")
    val type: PromoType,
    @Schema(description = "Discount value (percentage or absolute amount)", example = "10.0")
    val value: BigDecimal,
    @Schema(description = "Maximum discount amount for percentage-based promos", example = "50000")
    val maxDiscountAmount: BigDecimal?,
    @Schema(description = "Minimum order value required to apply the promo", example = "100000")
    val minOrderValue: BigDecimal,
    @Schema(description = "Total usage limit for the promo", example = "100")
    val usageLimit: Int?,
    @Schema(description = "Current number of times this promo has been used", example = "45")
    val currentUsage: Int,
    @Schema(description = "Promo validity start date", example = "2024-04-01T00:00:00Z")
    val startDate: Instant,
    @Schema(description = "Promo validity end date", example = "2024-04-30T23:59:59Z")
    val endDate: Instant,
    @Schema(description = "Whether the promo is active", example = "true")
    val isActive: Boolean,
    @Schema(description = "List of exclusions for specific products or categories")
    val exclusions: List<ExclusionResponse> = emptyList(),
) {
    companion object {
        fun fromEntity(
            entity: Promo,
            exclusions: List<com.gayakini.promo.domain.PromoExclusion> = emptyList(),
        ) = PromoResponse(
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
            exclusions = exclusions.map { ExclusionResponse.fromEntity(it) },
        )
    }
}

@Schema(description = "Exclusion details for a specific product or category from a promo.")
data class ExclusionResponse(
    @Schema(description = "Type of exclusion (PRODUCT or CATEGORY)", example = "PRODUCT")
    val type: com.gayakini.promo.domain.ExclusionType,
    @Schema(description = "ID of the excluded entity", example = "550e8400-e29b-41d4-a716-446655440201")
    val excludedEntityId: UUID,
) {
    companion object {
        fun fromEntity(entity: com.gayakini.promo.domain.PromoExclusion) =
            ExclusionResponse(
                type = entity.exclusionType,
                excludedEntityId = entity.excludedEntityId,
            )
    }
}

@Schema(description = "Request to add an exclusion to a promo.")
data class AddExclusionRequest(
    @Schema(description = "Type of exclusion", example = "CATEGORY")
    @field:NotNull
    val type: com.gayakini.promo.domain.ExclusionType,
    @Schema(description = "ID of the entity (Product/Category) to exclude")
    @field:NotNull
    val excludedEntityId: UUID,
)

data class PromoItemContext(
    val productId: UUID,
    val variantId: UUID,
    val categoryId: UUID,
    val collectionIds: Set<UUID> = emptySet(),
    val unitPriceAmount: Long,
    val quantity: Int,
)

@Schema(description = "Request to create a new promo.")
data class CreatePromoRequest(
    @Schema(description = "Unique promo code", example = "NEW_PROMO_2024")
    @field:NotBlank
    @field:Size(max = 50)
    val code: String,
    @Schema(description = "Promo type (PERCENTAGE or FIXED)", example = "PERCENTAGE")
    @field:NotNull
    val type: PromoType,
    @Schema(description = "Discount value (percentage or absolute amount)", example = "20.0")
    @field:NotNull
    @field:DecimalMin("0.0")
    val value: BigDecimal,
    @Schema(description = "Maximum discount amount (only for PERCENTAGE type)", example = "10000")
    @field:DecimalMin("0.0")
    val maxDiscountAmount: BigDecimal? = null,
    @Schema(description = "Minimum order value required", example = "50000")
    @field:NotNull
    @field:DecimalMin("0.0")
    val minOrderValue: BigDecimal = BigDecimal.ZERO,
    @Schema(description = "Total usage limit for this promo", example = "50")
    val usageLimit: Int? = null,
    @Schema(description = "Promo validity start date", example = "2024-05-01T00:00:00Z")
    @field:NotNull
    val startDate: Instant,
    @Schema(description = "Promo validity end date", example = "2024-05-31T23:59:59Z")
    @field:NotNull
    val endDate: Instant,
    @Schema(description = "Initial active status", example = "true")
    val isActive: Boolean = true,
)

@Schema(description = "Request to update an existing promo.")
data class UpdatePromoRequest(
    @Schema(description = "Unique promo code", example = "UPDATED_PROMO")
    @field:Size(max = 50)
    val code: String? = null,
    @Schema(description = "Promo type", example = "FIXED")
    val type: PromoType? = null,
    @Schema(description = "Discount value", example = "15000")
    @field:DecimalMin("0.0")
    val value: BigDecimal? = null,
    @Schema(description = "Maximum discount amount", example = "20000")
    @field:DecimalMin("0.0")
    val maxDiscountAmount: BigDecimal? = null,
    @Schema(description = "Minimum order value required", example = "75000")
    @field:DecimalMin("0.0")
    val minOrderValue: BigDecimal? = null,
    @Schema(description = "Total usage limit", example = "100")
    val usageLimit: Int? = null,
    @Schema(description = "Promo validity start date", example = "2024-06-01T00:00:00Z")
    val startDate: Instant? = null,
    @Schema(description = "Promo validity end date", example = "2024-06-30T23:59:59Z")
    val endDate: Instant? = null,
    @Schema(description = "Active status", example = "false")
    val isActive: Boolean? = null,
)
