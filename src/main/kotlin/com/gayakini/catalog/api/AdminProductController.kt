package com.gayakini.catalog.api

import com.gayakini.catalog.application.ProductService
import com.gayakini.catalog.domain.Product
import com.gayakini.common.api.ApiMeta
import com.gayakini.inventory.application.InventoryService
import com.gayakini.inventory.domain.AdjustmentReason
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/v1/admin")
@Tag(
    name = "Admin Products",
    description = "Product catalog and inventory management for administrators (Internal/English).",
)
class AdminProductController(
    private val productService: ProductService,
    private val inventoryService: InventoryService,
) {
    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new product", description = "Add a new product to the catalog.")
    fun createProduct(
        @Valid @RequestBody request: AdminCreateProductRequest,
    ): AdminProductResponse {
        val saved = productService.createProduct(request)
        return AdminProductResponse(
            message = "Product created successfully.",
            data = mapToAdminData(saved),
            meta = ApiMeta(),
        )
    }

    @PatchMapping("/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product", description = "Update details of an existing product.")
    fun updateProduct(
        @Parameter(description = "Product UUID") @PathVariable productId: UUID,
        @Valid @RequestBody request: AdminUpdateProductRequest,
    ): AdminProductResponse {
        val saved = productService.updateProduct(productId, request)
        return AdminProductResponse(
            message = "Product updated successfully.",
            data = mapToAdminData(saved),
            meta = ApiMeta(),
        )
    }

    @PostMapping("/variants/{variantId}/stock-adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Stock adjustment",
        description = "Increase or decrease stock for a specific product variant.",
    )
    fun adjustStock(
        @Parameter(description = "Variant UUID") @PathVariable variantId: UUID,
        @Parameter(description = "Idempotency token")
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
            message = "Stock updated successfully.",
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

    @PostMapping("/products/{productId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Upload product image",
        description = "Upload an image file and link it to a product.",
    )
    fun uploadProductImage(
        @Parameter(description = "Product UUID") @PathVariable productId: UUID,
        @Parameter(
            description = "Image file (JPEG/PNG)",
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = Schema(type = "string", format = "binary"),
                ),
            ],
        )
        @RequestParam("file") file: MultipartFile,
        @Parameter(description = "Alt text for accessibility")
        @RequestParam(defaultValue = "Product image") altText: String,
        @Parameter(description = "Set as primary image")
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
            message = "Product image uploaded successfully.",
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
            collections = product.collections.map { it.slug },
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
