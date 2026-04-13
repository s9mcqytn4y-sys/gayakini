package com.gayakini.checkout.api

import com.gayakini.cart.api.CartItemDto
import com.gayakini.cart.api.ProductVariantAttributeDto
import com.gayakini.checkout.application.CheckoutService
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import com.gayakini.infrastructure.security.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/checkouts")
@Tag(name = "Checkout", description = "Proses konversi keranjang menjadi pesanan.")
class CheckoutController(private val checkoutService: CheckoutService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Buat checkout baru",
        description = "Menginisialisasi sesi checkout dari keranjang yang ada.",
    )
    @SecurityRequirements
    fun createCheckout(
        @Valid @RequestBody request: CreateCheckoutRequest,
        @Parameter(description = "Token akses keranjang (jika tamu)")
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?,
    ): CheckoutResponse {
        val checkout =
            checkoutService.createCheckout(
                request.cartId,
                SecurityUtils.getCurrentUserId(),
                cartToken,
            )
        return mapToResponse(checkout, "Checkout berhasil dibuat.", cartToken)
    }

    @GetMapping("/{checkoutId}")
    @Operation(
        summary = "Ambil data checkout",
        description = "Mendapatkan status dan rincian sesi checkout saat ini.",
    )
    @SecurityRequirements
    fun getCheckout(
        @Parameter(description = "ID Checkout (UUID)")
        @PathVariable checkoutId: UUID,
        @Parameter(description = "Token akses checkout (jika tamu)")
        @RequestHeader(value = "X-Checkout-Token", required = false) checkoutToken: String?,
    ): CheckoutResponse {
        val checkout = checkoutService.getValidatedCheckout(checkoutId, SecurityUtils.getCurrentUserId(), checkoutToken)
        return mapToResponse(checkout, "Checkout berhasil diambil.", checkoutToken)
    }

    @PutMapping("/{checkoutId}/shipping-address")
    @Operation(
        summary = "Set alamat pengiriman",
        description = "Menentukan alamat pengiriman untuk sesi checkout ini.",
    )
    @SecurityRequirements
    fun upsertShippingAddress(
        @Parameter(description = "ID Checkout (UUID)")
        @PathVariable checkoutId: UUID,
        @Parameter(description = "Token akses checkout (jika tamu)")
        @RequestHeader(value = "X-Checkout-Token", required = false) checkoutToken: String?,
        @Valid @RequestBody request: CheckoutShippingAddressRequest,
    ): CheckoutResponse {
        val checkout =
            checkoutService.updateShippingAddress(
                checkoutId,
                SecurityUtils.getCurrentUserId(),
                checkoutToken,
                request,
            )
        return mapToResponse(checkout, "Alamat pengiriman berhasil dipilih.", checkoutToken)
    }

    @PostMapping("/{checkoutId}/shipping-quotes")
    @Operation(
        summary = "Hitung opsi pengiriman",
        description = "Mengambil daftar pilihan kurir dan biaya kirim yang tersedia berdasarkan alamat.",
    )
    @SecurityRequirements
    fun createShippingQuotes(
        @Parameter(description = "ID Checkout (UUID)")
        @PathVariable checkoutId: UUID,
        @Parameter(description = "Token akses checkout (jika tamu)")
        @RequestHeader(value = "X-Cart-Token", required = false) checkoutToken: String?,
    ): CheckoutResponse {
        val checkout =
            checkoutService.calculateShippingQuotes(
                checkoutId,
                SecurityUtils.getCurrentUserId(),
                checkoutToken,
            )
        return mapToResponse(checkout, "Pilihan pengiriman berhasil dihitung.", checkoutToken)
    }

    @PostMapping("/{checkoutId}/promo")
    @Operation(
        summary = "Gunakan kode promo",
        description = "Menerapkan potongan harga ke sesi checkout menggunakan kode voucher.",
    )
    @SecurityRequirements
    fun applyPromo(
        @Parameter(description = "ID Checkout (UUID)")
        @PathVariable checkoutId: UUID,
        @Parameter(description = "Token akses checkout (jika tamu)")
        @RequestHeader(value = "X-Checkout-Token", required = false) checkoutToken: String?,
        @Valid @RequestBody request: ApplyPromoRequest,
    ): CheckoutResponse {
        val checkout =
            checkoutService.applyPromo(
                checkoutId,
                SecurityUtils.getCurrentUserId(),
                checkoutToken,
                request.promoCode,
            )
        return mapToResponse(checkout, "Promo berhasil digunakan.", checkoutToken)
    }

    @DeleteMapping("/{checkoutId}/promo")
    @Operation(
        summary = "Hapus kode promo",
        description = "Menghapus potongan harga yang sebelumnya telah diterapkan.",
    )
    @SecurityRequirements
    fun removePromo(
        @Parameter(description = "ID Checkout (UUID)")
        @PathVariable checkoutId: UUID,
        @Parameter(description = "Token akses checkout (jika tamu)")
        @RequestHeader(value = "X-Checkout-Token", required = false) checkoutToken: String?,
    ): CheckoutResponse {
        val checkout =
            checkoutService.removePromo(
                checkoutId,
                SecurityUtils.getCurrentUserId(),
                checkoutToken,
            )
        return mapToResponse(checkout, "Promo berhasil dihapus.", checkoutToken)
    }

    @PutMapping("/{checkoutId}/shipping-selection")
    @Operation(
        summary = "Pilih opsi pengiriman",
        description = "Mengunci pilihan kurir tertentu untuk sesi checkout.",
    )
    @SecurityRequirements
    fun setShippingSelection(
        @Parameter(description = "ID Checkout (UUID)")
        @PathVariable checkoutId: UUID,
        @Parameter(description = "Token akses checkout (jika tamu)")
        @RequestHeader(value = "X-Checkout-Token", required = false) checkoutToken: String?,
        @Valid @RequestBody request: SelectShippingQuoteRequest,
    ): CheckoutResponse {
        val checkout =
            checkoutService.selectShippingQuote(
                checkoutId,
                SecurityUtils.getCurrentUserId(),
                checkoutToken,
                request.quoteId,
            )
        return mapToResponse(checkout, "Pilihan pengiriman berhasil dipilih.", checkoutToken)
    }

    private fun mapToResponse(
        checkout: com.gayakini.checkout.domain.Checkout,
        message: String,
        accessToken: String? = null,
    ): CheckoutResponse {
        return CheckoutResponse(
            message = message,
            data =
                CheckoutDto(
                    id = checkout.id,
                    cartId = checkout.cart.id,
                    customerId = checkout.customerId,
                    accessToken = accessToken,
                    status = checkout.status,
                    currency = checkout.currencyCode,
                    items =
                        checkout.items.map { item ->
                            CartItemDto(
                                id = item.id,
                                productId = item.product.id,
                                productTitle = item.productTitleSnapshot,
                                variantId = item.variant.id,
                                sku = item.skuSnapshot,
                                attributes =
                                    listOf(
                                        ProductVariantAttributeDto(name = "color", value = item.color),
                                        ProductVariantAttributeDto(name = "size", value = item.sizeCode),
                                    ),
                                quantity = item.quantity,
                                unitPrice = MoneyDto(amount = item.unitPriceAmount),
                                compareAtPrice = item.compareAtAmount?.let { MoneyDto(amount = it) },
                                lineTotal = MoneyDto(amount = item.lineTotalAmount),
                                primaryImageUrl = item.primaryImageUrl,
                            )
                        },
                    subtotal = MoneyDto(amount = checkout.subtotalAmount),
                    shippingCost = MoneyDto(amount = checkout.shippingCostAmount),
                    discount = MoneyDto(amount = checkout.discountAmount),
                    total = MoneyDto(amount = checkout.totalAmount),
                    promoCode = checkout.promoCode,
                    expiresAt = checkout.expiresAt,
                    shippingAddress =
                        checkout.shippingAddress?.let { addr ->
                            CheckoutAddressDto(
                                id = addr.customerAddressId,
                                recipientName = addr.recipientName,
                                phone = addr.phone,
                                line1 = addr.line1,
                                line2 = addr.line2,
                                notes = addr.notes,
                                areaId = addr.areaId,
                                district = addr.district,
                                city = addr.city,
                                province = addr.province,
                                postalCode = addr.postalCode,
                                countryCode = addr.countryCode,
                            )
                        },
                    selectedShippingQuote =
                        checkout.availableShippingQuotes.find { it.id == checkout.selectedShippingQuoteId }?.let {
                                quote ->
                            ShippingQuoteDto(
                                quoteId = quote.id,
                                provider = quote.provider,
                                providerReference = quote.providerReference,
                                courierCode = quote.courierCode,
                                courierName = quote.courierName,
                                serviceCode = quote.serviceCode,
                                serviceName = quote.serviceName,
                                description = quote.description,
                                cost = MoneyDto(amount = quote.costAmount),
                                estimatedDaysMin = quote.estimatedDaysMin,
                                estimatedDaysMax = quote.estimatedDaysMax,
                                isRecommended = quote.isRecommended,
                            )
                        },
                    availableShippingQuotes =
                        checkout.availableShippingQuotes.map { quote ->
                            ShippingQuoteDto(
                                quoteId = quote.id,
                                provider = quote.provider,
                                providerReference = quote.providerReference,
                                courierCode = quote.courierCode,
                                courierName = quote.courierName,
                                serviceCode = quote.serviceCode,
                                serviceName = quote.serviceName,
                                description = quote.description,
                                cost = MoneyDto(amount = quote.costAmount),
                                estimatedDaysMin = quote.estimatedDaysMin,
                                estimatedDaysMax = quote.estimatedDaysMax,
                                isRecommended = quote.isRecommended,
                            )
                        },
                ),
            meta = ApiMeta(requestId = UUID.randomUUID().toString()),
        )
    }
}
