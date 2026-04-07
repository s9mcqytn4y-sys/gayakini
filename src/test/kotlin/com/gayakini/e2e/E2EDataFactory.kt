package com.gayakini.e2e

import com.gayakini.cart.api.AddCartItemRequest
import com.gayakini.cart.api.UpdateCartItemRequest
import com.gayakini.catalog.domain.Category
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.checkout.api.CheckoutShippingAddressRequest
import com.gayakini.checkout.api.CreateCheckoutRequest
import com.gayakini.checkout.api.GuestAddressRequest
import com.gayakini.checkout.api.SelectShippingQuoteRequest
import com.gayakini.customer.api.LoginRequest
import com.gayakini.customer.api.RegisterRequest
import com.gayakini.order.api.PlaceOrderRequest
import java.util.UUID

object E2EDataFactory {
    // AUTH
    fun createRegisterRequest(
        email: String = "e2e.budi.santoso@gayakini.local",
        fullName: String = "Budi Santoso",
        phone: String = "081234567890",
    ) = RegisterRequest(
        email = email,
        password = "Password123!",
        fullName = fullName,
        phone = phone,
    )

    fun createLoginRequest(email: String = "e2e.budi.santoso@gayakini.local") =
        LoginRequest(
            email = email,
            password = "Password123!",
        )

    // CATALOG SEED HELPERS
    fun createCategory(
        slug: String = "kaos-pria",
        name: String = "Kaos Pria",
    ) = Category(
        id = UUID.randomUUID(),
        slug = slug,
        name = name,
        description = "Koleksi kaos pria terbaik.",
    )

    fun createProduct(
        category: Category,
        slug: String = "kaos-polos-hitam",
        title: String = "Kaos Polos Hitam",
    ) = Product(
        id = UUID.randomUUID(),
        slug = slug,
        title = title,
        subtitle = "Basic Essential",
        brandName = "GAYAKINI",
        category = category,
        description = "Kaos polos hitam berkualitas tinggi dari bahan katun prima.",
        status = ProductStatus.PUBLISHED,
    )

    fun createVariant(
        product: Product,
        sku: String = "SKU-E2E-KAOS-HITAM-M",
        size: String = "M",
        color: String = "Hitam",
        price: Long = 150000,
    ) = ProductVariant(
        id = UUID.randomUUID(),
        product = product,
        sku = sku,
        sizeCode = size,
        color = color,
        priceAmount = price,
        stockOnHand = 100,
        status = VariantStatus.ACTIVE,
    )

    // CART REQUESTS
    fun createAddCartItemRequest(
        variantId: UUID,
        quantity: Int = 1,
    ) = AddCartItemRequest(variantId = variantId, quantity = quantity)

    fun createUpdateCartItemRequest(quantity: Int) = UpdateCartItemRequest(quantity = quantity)

    // CHECKOUT & ORDER REQUESTS
    fun createCheckoutRequest(cartId: UUID) = CreateCheckoutRequest(cartId = cartId)

    fun createShippingAddressRequest() = CheckoutShippingAddressRequest(
        guestAddress = GuestAddressRequest(
            recipientName = "Budi Santoso",
            phone = "081234567890",
            line1 = "Jl. Merdeka No. 10",
            district = "Coblong",
            city = "Bandung",
            province = "Jawa Barat",
            postalCode = "40132",
            areaId = "3273010",
        ),
    )

    fun createSelectShippingQuoteRequest(quoteId: UUID) = SelectShippingQuoteRequest(quoteId = quoteId)

    fun createPlaceOrderRequest(notes: String = "Harap segera diproses.") = PlaceOrderRequest(customerNotes = notes)
}
