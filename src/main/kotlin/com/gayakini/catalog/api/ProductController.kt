package com.gayakini.catalog.api

import com.gayakini.catalog.application.ProductService
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.PublicProductSummary
import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.common.api.*
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/products")
class ProductController(private val productService: ProductService) {
    @GetMapping
    fun listProducts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "newest") sort: String,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) categorySlug: String?,
        @RequestParam(required = false) collectionSlug: String?,
        @RequestParam(required = false) color: String?,
        @RequestParam(required = false) sizeCode: String?,
        @RequestParam(required = false) minPrice: Long?,
        @RequestParam(required = false) maxPrice: Long?,
        @RequestParam(required = false) inStock: Boolean?,
    ): PaginatedResponse<ProductSummaryDto> {
        val resultPage =
            productService.searchProducts(
                page, size, sort, q, categorySlug, collectionSlug, color, sizeCode, minPrice, maxPrice, inStock,
            )

        return PaginatedResponse(
            message = "Daftar produk berhasil diambil.",
            data = resultPage.content.map { mapToSummary(it) },
            meta =
                PageMeta(
                    page = page,
                    size = size,
                    totalElements = resultPage.totalElements,
                    totalPages = resultPage.totalPages,
                    requestId = UUID.randomUUID().toString(),
                ),
        )
    }

    @GetMapping("/{productId}")
    fun getProductById(
        @PathVariable productId: UUID,
    ): StandardResponse<ProductDetailDto> {
        val product = productService.getProduct(productId)

        return StandardResponse(
            message = "Detail produk berhasil diambil.",
            data = mapToDetail(product),
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }

    @GetMapping("/{productId}/variants")
    fun listProductVariants(
        @PathVariable productId: UUID,
    ): StandardResponse<List<ProductVariantDto>> {
        val product = productService.getProduct(productId)

        return StandardResponse(
            message = "Variasi produk berhasil diambil.",
            data = product.variants.map { mapToVariantDto(it) },
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }

    private fun mapToSummary(summary: PublicProductSummary): ProductSummaryDto {
        return ProductSummaryDto(
            id = summary.id,
            slug = summary.slug,
            title = summary.title,
            subtitle = summary.subtitle,
            brandName = summary.brandName,
            categorySlug = summary.categorySlug,
            primaryImageUrl = summary.primaryImageUrl,
            priceRange =
                PriceRangeDto(
                    min = MoneyDto(amount = summary.minPriceAmount),
                    max = MoneyDto(amount = summary.maxPriceAmount),
                ),
            inStock = summary.inStock,
        )
    }

    private fun mapToDetail(product: Product): ProductDetailDto {
        val activeVariants = product.variants.filter { it.status == VariantStatus.ACTIVE }
        return ProductDetailDto(
            id = product.id,
            slug = product.slug,
            title = product.title,
            subtitle = product.subtitle,
            brandName = product.brandName,
            categorySlug = product.category?.slug ?: "unknown",
            primaryImageUrl = product.media.firstOrNull { it.isPrimary }?.url ?: product.media.firstOrNull()?.url,
            priceRange =
                PriceRangeDto(
                    min = MoneyDto(amount = activeVariants.minOfOrNull { it.priceAmount } ?: 0),
                    max = MoneyDto(amount = activeVariants.maxOfOrNull { it.priceAmount } ?: 0),
                ),
            inStock = activeVariants.any { it.stockAvailable > 0 },
            description = product.description,
            // TODO: Map collections relationship if available in Product entity
            collections = listOf(),
            media =
                product.media.map {
                    ProductMediaDto(
                        id = it.id,
                        url = it.url,
                        altText = it.altText,
                        sortOrder = it.sortOrder,
                        isPrimary = it.isPrimary,
                    )
                },
            variants = product.variants.map { mapToVariantDto(it) },
        )
    }

    private fun mapToVariantDto(variant: ProductVariant): ProductVariantDto {
        return ProductVariantDto(
            id = variant.id,
            sku = variant.sku,
            status = variant.status,
            price = MoneyDto(amount = variant.priceAmount),
            compareAtPrice = variant.compareAtAmount?.let { MoneyDto(amount = it) },
            inventory =
                InventorySummaryDto(
                    stockOnHand = variant.stockOnHand,
                    stockReserved = variant.stockReserved,
                    stockAvailable = variant.stockAvailable,
                ),
            attributes =
                listOf(
                    ProductVariantAttributeDto(name = "color", value = variant.color),
                    ProductVariantAttributeDto(name = "size", value = variant.sizeCode),
                ),
            weightGrams = variant.weightGrams,
            primaryImageUrl = null,
        )
    }
}
