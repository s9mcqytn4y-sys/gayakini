package com.gayakini.catalog.api

import com.gayakini.catalog.application.ProductService
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.PublicProductSummary
import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.common.api.ApiResponse
import com.gayakini.common.api.MoneyDto
import com.gayakini.common.api.PageMeta
import com.gayakini.common.api.PaginatedResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/products")
@Tag(name = "Products", description = "Katalog produk publik untuk browsing dan pencarian.")
class ProductController(private val productService: ProductService) {
    @GetMapping
    @Operation(summary = "Mencari dan menampilkan daftar produk")
    @SecurityRequirements
    fun listProducts(
        @Parameter(description = "Halaman ke-n (mulai dari 1)")
        @RequestParam(defaultValue = "1")
        page: Int,
        @Parameter(description = "Jumlah item per halaman")
        @RequestParam(defaultValue = "20")
        size: Int,
        @Parameter(description = "Kriteria pengurutan (newest, price_asc, price_desc)")
        @RequestParam(defaultValue = "newest")
        sort: String,
        @Parameter(description = "Kata kunci pencarian")
        @RequestParam(required = false)
        q: String?,
        @Parameter(description = "Slug kategori")
        @RequestParam(required = false)
        categorySlug: String?,
        @Parameter(description = "Slug koleksi")
        @RequestParam(required = false)
        collectionSlug: String?,
        @Parameter(description = "Filter warna")
        @RequestParam(required = false)
        color: String?,
        @Parameter(description = "Filter ukuran")
        @RequestParam(required = false)
        sizeCode: String?,
        @Parameter(description = "Harga minimum")
        @RequestParam(required = false)
        minPrice: Long?,
        @Parameter(description = "Harga maksimum")
        @RequestParam(required = false)
        maxPrice: Long?,
        @Parameter(description = "Hanya tampilkan produk yang tersedia stoknya")
        @RequestParam(required = false)
        inStock: Boolean?,
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
                ),
        )
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Mendapatkan detail produk lengkap berdasarkan ID")
    @SecurityRequirements
    @org.springframework.cache.annotation.Cacheable(value = ["products"], key = "#productId")
    fun getProductById(
        @Parameter(description = "UUID unik produk") @PathVariable productId: UUID,
    ): ApiResponse<ProductDetailDto> {
        val product = productService.getProduct(productId)

        return ApiResponse(
            message = "Detail produk berhasil diambil.",
            data = mapToDetail(product),
        )
    }

    @GetMapping("/{productId}/variants")
    @Operation(summary = "Daftar semua varian (sku, warna, ukuran) dari sebuah produk")
    @SecurityRequirements
    fun listProductVariants(
        @Parameter(description = "UUID unik produk") @PathVariable productId: UUID,
    ): ApiResponse<List<ProductVariantDto>> {
        val product = productService.getProduct(productId)

        return ApiResponse(
            message = "Variasi produk berhasil diambil.",
            data = product.variants.map { mapToVariantDto(it) },
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
            collections =
                product.collections.map {
                    CollectionDto(
                        id = it.id,
                        slug = it.slug,
                        name = it.name,
                    )
                },
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
