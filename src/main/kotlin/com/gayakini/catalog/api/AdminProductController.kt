package com.gayakini.catalog.api

import com.gayakini.catalog.application.ProductService
import com.gayakini.catalog.domain.*
import com.gayakini.common.api.ApiMeta
import com.gayakini.inventory.application.InventoryService
import com.gayakini.inventory.domain.AdjustmentReason
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/v1/admin")
class AdminProductController(
    private val productService: ProductService,
    private val inventoryService: InventoryService,
) {
    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    fun createProduct(
        @Valid @RequestBody request: AdminCreateProductRequest,
    ): AdminProductResponse {
        val saved = productService.createProduct(request)
        return AdminProductResponse(
            message = "Produk berhasil dibuat.",
            data = mapToAdminData(saved),
            meta = ApiMeta(),
        )
    }

    @PatchMapping("/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateProduct(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: AdminUpdateProductRequest,
    ): AdminProductResponse {
        val saved = productService.updateProduct(productId, request)
        return AdminProductResponse(
            message = "Produk berhasil diperbarui.",
            data = mapToAdminData(saved),
            meta = ApiMeta(),
        )
    }

    @PostMapping("/variants/{variantId}/stock-adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    fun adjustStock(
        @PathVariable variantId: UUID,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
        @Valid @RequestBody request: StockAdjustmentRequest,
    ): StockAdjustmentResponse {
        val reason =
            try {
                AdjustmentReason.valueOf(request.reasonCode)
            } catch (e: IllegalArgumentException) {
                AdjustmentReason.MANUAL_CORRECTION
            }

        val adjustment =
            inventoryService.adjustStock(
                variantId = variantId,
                quantityDelta = request.quantityDelta,
                reason = reason,
                note = request.note,
                idempotencyKey = idempotencyKey,
            )

        return StockAdjustmentResponse(
            message = "Stok berhasil diperbarui.",
            data =
                StockAdjustmentData(
                    variantId = variantId,
                    stockOnHand = adjustment.stockOnHandAfter,
                    stockReserved = adjustment.stockReservedAfter,
                    stockAvailable = adjustment.stockOnHandAfter - adjustment.stockReservedAfter,
                    lastAdjustmentId = adjustment.id,
                ),
            meta = ApiMeta(),
        )
    }

    @PostMapping("/products/{productId}/image")
    @PreAuthorize("hasRole('ADMIN')")
    fun uploadProductImage(
        @PathVariable productId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(defaultValue = "Product image") altText: String,
        @RequestParam(defaultValue = "false") isPrimary: Boolean,
    ): AdminProductResponse {
        val media =
            productService.uploadProductMedia(
                productId = productId,
                inputStream = file.inputStream,
                originalFilename = file.originalFilename ?: "image.jpg",
                altText = altText,
                isPrimary = isPrimary,
            )

        return AdminProductResponse(
            message = "Gambar produk berhasil diunggah.",
            data = mapToAdminData(media.product),
            meta = ApiMeta(),
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
