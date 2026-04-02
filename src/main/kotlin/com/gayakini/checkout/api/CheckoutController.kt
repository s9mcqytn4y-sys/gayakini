package com.gayakini.checkout.api

import com.gayakini.cart.api.CartItemDto
import com.gayakini.checkout.application.CheckoutService
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/checkouts")
class CheckoutController(private val checkoutService: CheckoutService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCheckout(
        @Valid @RequestBody request: CreateCheckoutRequest,
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?,
    ): CheckoutResponse {
        // Extract customerId from SecurityContext if authenticated, otherwise use guest flow
        val checkout = checkoutService.createCheckout(request.cartId, null, cartToken)

        return mapToResponse(checkout, "Checkout berhasil dibuat.")
    }

    @GetMapping("/{checkoutId}")
    fun getCheckout(
        @PathVariable checkoutId: UUID,
        @RequestHeader(value = "X-Checkout-Token", required = false) checkoutToken: String?,
    ): CheckoutResponse {
        val checkout = checkoutService.getCheckout(checkoutId)
        return mapToResponse(checkout, "Checkout berhasil diambil.")
    }

    @PutMapping("/{checkoutId}/shipping-address")
    fun upsertShippingAddress(
        @PathVariable checkoutId: UUID,
        @Valid @RequestBody request: CheckoutShippingAddressRequest
    ): CheckoutResponse {
        val checkout = checkoutService.updateShippingAddress(checkoutId, request)
        return mapToResponse(checkout, "Alamat pengiriman berhasil dipilih.")
    }

    @PostMapping("/{checkoutId}/shipping-quotes")
    fun createShippingQuotes(
        @PathVariable checkoutId: UUID,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?
    ): CheckoutResponse {
        val checkout = checkoutService.calculateShippingQuotes(checkoutId)
        return mapToResponse(checkout, "Pilihan pengiriman berhasil dihitung.")
    }

    @PutMapping("/{checkoutId}/shipping-selection")
    fun setShippingSelection(
        @PathVariable checkoutId: UUID,
        @Valid @RequestBody request: SelectShippingQuoteRequest
    ): CheckoutResponse {
        val checkout = checkoutService.selectShippingQuote(checkoutId, request.quoteId)
        return mapToResponse(checkout, "Pilihan pengiriman berhasil dipilih.")
    }

    private fun mapToResponse(checkout: com.gayakini.checkout.domain.Checkout, message: String): CheckoutResponse {
        return CheckoutResponse(
            message = message,
            data = CheckoutDto(
                id = checkout.id,
                cartId = checkout.cart.id,
                customerId = checkout.customerId,
                status = checkout.status,
                currency = checkout.currencyCode,
                items = checkout.items.map { item ->
                    CartItemDto(
                        id = item.id,
                        productId = item.product.id,
                        productTitle = item.productTitleSnapshot,
                        variantId = item.variant.id,
                        sku = item.skuSnapshot,
                        attributes = listOf(), // TODO: Map attributes
                        quantity = item.quantity,
                        unitPrice = MoneyDto(amount = item.unitPriceAmount),
                        compareAtPrice = item.compareAtAmount?.let { MoneyDto(amount = it) },
                        lineTotal = MoneyDto(amount = item.lineTotalAmount),
                        primaryImageUrl = item.primaryImageUrl
                    )
                },
                subtotal = MoneyDto(amount = checkout.subtotalAmount),
                shippingCost = MoneyDto(amount = checkout.shippingCostAmount),
                total = MoneyDto(amount = checkout.totalAmount),
                expiresAt = checkout.expiresAt,
                shippingAddress = null, // TODO: Map address
                selectedShippingQuote = null, // TODO: Map selected quote
                availableShippingQuotes = listOf() // TODO: Map quotes
            ),
            meta = ApiMeta(requestId = UUID.randomUUID().toString())
        )
    }
}
