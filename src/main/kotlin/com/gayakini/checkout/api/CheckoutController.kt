package com.gayakini.checkout.api

import com.gayakini.cart.api.CartItemResponse
import com.gayakini.cart.api.MoneyResponse
import com.gayakini.checkout.application.CheckoutService
import com.gayakini.common.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/checkouts")
class CheckoutController(private val checkoutService: CheckoutService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCheckout(
        @Valid @RequestBody request: CreateCheckoutRequest,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        @RequestHeader(value = "X-Cart-Token", required = false) cartToken: String?,
    ): ApiResponse<CheckoutResponse> {
        // TODO: Extract customerId from authHeader
        val customerId: UUID? = null

        val checkout = checkoutService.createCheckout(request.cartId, customerId, cartToken)

        return ApiResponse.success(
            message = "Checkout berhasil dibuat.",
            data = mapToResponse(checkout),
        )
    }

    @GetMapping("/{checkoutId}")
    fun getCheckout(
        @PathVariable checkoutId: UUID,
        @RequestHeader(value = "X-Checkout-Token", required = false) checkoutToken: String?,
    ): ApiResponse<CheckoutResponse> {
        val checkout = checkoutService.getCheckout(checkoutId)
        // TODO: Validate token

        return ApiResponse.success(
            message = "Checkout berhasil diambil.",
            data = mapToResponse(checkout),
        )
    }

    private fun mapToResponse(checkout: com.gayakini.checkout.domain.Checkout): CheckoutResponse {
        return CheckoutResponse(
            id = checkout.id,
            cartId = checkout.cart.id,
            customerId = checkout.customerId,
            status = checkout.status,
            currency = checkout.currencyCode,
            items =
                checkout.items.map { item ->
                    CartItemResponse(
                        id = item.id,
                        productId = item.product.id,
                        productTitle = item.productTitleSnapshot,
                        variantId = item.variant.id,
                        sku = item.skuSnapshot,
                        attributes = listOf(),
                        quantity = item.quantity,
                        unitPrice = MoneyResponse(amount = item.unitPriceAmount),
                        compareAtPrice = item.compareAtAmount?.let { MoneyResponse(amount = it) },
                        lineTotal = MoneyResponse(amount = item.lineTotalAmount),
                        primaryImageUrl = item.primaryImageUrl,
                    )
                },
            subtotal = MoneyResponse(amount = checkout.subtotalAmount),
            shippingCost = MoneyResponse(amount = checkout.shippingCostAmount),
            total = MoneyResponse(amount = checkout.totalAmount),
            expiresAt = checkout.expiresAt,
        )
    }
}
