package com.gayakini.catalog.api

import com.gayakini.catalog.domain.*
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.util.UuidV7Generator
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/admin")
class AdminProductController(
    private val productRepository: ProductRepository,
    private val variantRepository: ProductVariantRepository,
) {
    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    fun createProduct(
        @Valid @RequestBody request: AdminCreateProductRequest,
    ): AdminProductResponse {
        val product =
            Product(
                id = UuidV7Generator.generate(),
                slug = request.slug,
                title = request.title,
                subtitle = request.subtitle,
                brandName = request.brandName,
                description = request.description,
                status = request.status,
            )
        // TODO: Actually handle category, collections, media in a real service
        val saved = productRepository.save(product)
        return AdminProductResponse(
            message = "Produk berhasil dibuat.",
            data = mapToAdminData(saved),
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }

    @PatchMapping("/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateProduct(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: AdminUpdateProductRequest,
    ): AdminProductResponse {
        val product =
            productRepository.findById(
                productId,
            ).orElseThrow { NoSuchElementException("Produk tidak ditemukan") }
        request.title?.let { product.title = it }
        request.status?.let { product.status = it }
        request.subtitle?.let { product.subtitle = it }
        request.brandName?.let { product.brandName = it }
        request.description?.let { product.description = it }

        val saved = productRepository.save(product)
        return AdminProductResponse(
            message = "Produk berhasil diperbarui.",
            data = mapToAdminData(saved),
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }

    @PostMapping("/variants/{variantId}/stock-adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    fun adjustStock(
        @PathVariable variantId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: StockAdjustmentRequest,
    ): StockAdjustmentResponse {
        val variant =
            variantRepository.findById(
                variantId,
            ).orElseThrow { NoSuchElementException("Varian tidak ditemukan") }
        variant.stockOnHand += request.quantityDelta
        variantRepository.save(variant)

        return StockAdjustmentResponse(
            message = "Stok berhasil diperbarui. Key: $idempotencyKey",
            data =
                StockAdjustmentData(
                    variantId = variantId,
                    stockOnHand = variant.stockOnHand,
                    stockReserved = variant.stockReserved,
                    stockAvailable = variant.stockAvailable,
                    // TODO: Record in adjustment table
                    lastAdjustmentId = UUID.randomUUID(),
                ),
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }

    private fun mapToAdminData(product: Product): AdminProductData {
        return AdminProductData(
            id = product.id,
            slug = product.slug,
            title = product.title,
            subtitle = product.subtitle,
            brandName = product.brandName,
            categorySlug = product.category?.slug,
            description = product.description,
            // TODO: Map collections
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
            status = product.status,
        )
    }
}
