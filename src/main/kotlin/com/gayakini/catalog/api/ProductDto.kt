package com.gayakini.catalog.api

import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import com.gayakini.common.api.PageMeta
import java.util.UUID

data class ProductPageResponse(
    val message: String,
    val data: List<ProductSummaryDto>,
    val meta: PageMeta,
)

data class ProductSummaryDto(
    val id: UUID,
    val slug: String,
    val title: String,
    val subtitle: String?,
    val brandName: String,
    val categorySlug: String,
    val primaryImageUrl: String?,
    val priceRange: PriceRangeDto,
    val inStock: Boolean,
)

data class PriceRangeDto(
    val min: MoneyDto,
    val max: MoneyDto,
)

data class ProductDetailResponse(
    val message: String,
    val data: ProductDetailDto,
    val meta: ApiMeta? = null,
)

data class ProductDetailDto(
    val id: UUID,
    val slug: String,
    val title: String,
    val subtitle: String?,
    val brandName: String,
    val categorySlug: String,
    val primaryImageUrl: String?,
    val priceRange: PriceRangeDto,
    val inStock: Boolean,
    val description: String,
    val collections: List<String>,
    val media: List<ProductMediaDto>,
    val variants: List<ProductVariantDto>,
)

data class ProductMediaDto(
    val id: UUID,
    val url: String,
    val altText: String,
    val sortOrder: Int,
    val isPrimary: Boolean = false,
)

data class ProductVariantDto(
    val id: UUID,
    val sku: String,
    val status: VariantStatus,
    val price: MoneyDto,
    val compareAtPrice: MoneyDto?,
    val inventory: InventorySummaryDto,
    val attributes: List<ProductVariantAttributeDto>,
    val weightGrams: Int,
    val primaryImageUrl: String?,
)

data class InventorySummaryDto(
    val stockOnHand: Int,
    val stockReserved: Int,
    val stockAvailable: Int,
)

data class ProductVariantAttributeDto(
    val name: String,
    val value: String,
)

data class ProductVariantListResponse(
    val message: String,
    val data: List<ProductVariantDto>,
    val meta: ApiMeta? = null,
)
