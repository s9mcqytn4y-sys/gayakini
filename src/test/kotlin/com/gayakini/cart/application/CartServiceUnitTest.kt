package com.gayakini.cart.application

import com.gayakini.cart.domain.*
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.ProductVariantRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class CartServiceUnitTest {

    private val cartRepository = mockk<CartRepository>(relaxed = true)
    private val cartItemRepository = mockk<CartItemRepository>(relaxed = true)
    private val variantRepository = mockk<ProductVariantRepository>()

    private val cartService = CartService(
        cartRepository,
        cartItemRepository,
        variantRepository
    )

    @Test
    fun `addItem should create new item if variant not in cart`() {
        val cartId = UUID.randomUUID()
        val variantId = UUID.randomUUID()
        val cart = Cart(id = cartId, status = CartStatus.ACTIVE)

        val product = mockk<Product> {
            every { title } returns "Test Product"
            every { media } returns mutableListOf()
        }
        val variant = mockk<ProductVariant> {
            every { id } returns variantId
            every { sku } returns "SKU-1"
            every { color } returns "Red"
            every { sizeCode } returns "XL"
            every { priceAmount } returns 100000L
            every { compareAtAmount } returns 120000L
            every { this@mockk.product } returns product
        }

        every { cartRepository.findById(cartId) } returns Optional.of(cart)
        every { variantRepository.findById(variantId) } returns Optional.of(variant)
        every { cartRepository.save(any()) } returns cart

        cartService.addItem(cartId, variantId, 2)

        assertEquals(1, cart.items.size)
        assertEquals(2, cart.items[0].quantity)
        assertEquals(100000L, cart.items[0].unitPriceAmount)
        assertEquals(2, cart.itemCount)
        assertEquals(200000L, cart.subtotalAmount)
    }

    @Test
    fun `addItem should increment quantity if variant already in cart`() {
        val cartId = UUID.randomUUID()
        val variantId = UUID.randomUUID()
        val cart = Cart(id = cartId, status = CartStatus.ACTIVE)
        val variant = mockk<ProductVariant> {
            every { id } returns variantId
            every { priceAmount } returns 100000L
        }

        val existingItem = CartItem(
            cart = cart,
            product = mockk(),
            variant = variant,
            productTitleSnapshot = "Title",
            skuSnapshot = "SKU",
            color = "Red",
            sizeCode = "XL",
            quantity = 1,
            unitPriceAmount = 100000L
        )
        cart.items.add(existingItem)

        every { cartRepository.findById(cartId) } returns Optional.of(cart)
        every { variantRepository.findById(variantId) } returns Optional.of(variant)
        every { cartRepository.save(any()) } returns cart

        cartService.addItem(cartId, variantId, 2)

        assertEquals(1, cart.items.size)
        assertEquals(3, cart.items[0].quantity)
        assertEquals(3, cart.itemCount)
        assertEquals(300000L, cart.subtotalAmount)
    }
}
