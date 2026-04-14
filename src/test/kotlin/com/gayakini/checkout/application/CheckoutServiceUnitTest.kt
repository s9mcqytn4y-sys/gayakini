package com.gayakini.checkout.application

import com.gayakini.cart.application.CartService
import com.gayakini.cart.domain.Cart
import com.gayakini.cart.domain.CartItem
import com.gayakini.cart.domain.CartRepository
import com.gayakini.cart.domain.CartStatus
import com.gayakini.catalog.domain.Product
import com.gayakini.catalog.domain.ProductCollectionRepository
import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.checkout.domain.*
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.common.util.HashUtils
import com.gayakini.customer.domain.CustomerAddressRepository
import com.gayakini.promo.application.PromoService
import com.gayakini.shipping.domain.MerchantShippingOriginRepository
import com.gayakini.shipping.domain.ShippingProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class CheckoutServiceUnitTest {

    private val checkoutRepository = mockk<CheckoutRepository>()
    private val cartRepository = mockk<CartRepository>()
    private val cartService = mockk<CartService>(relaxed = true)
    private val shippingProvider = mockk<ShippingProvider>()
    private val shippingQuoteRepository = mockk<CheckoutShippingQuoteRepository>(relaxed = true)
    private val customerAddressRepository = mockk<CustomerAddressRepository>()
    private val merchantOriginRepository = mockk<MerchantShippingOriginRepository>()
    private val promoService = mockk<PromoService>()
    private val productCollectionRepository = mockk<ProductCollectionRepository>()

    private val checkoutService = CheckoutService(
        checkoutRepository,
        cartRepository,
        cartService,
        shippingProvider,
        shippingQuoteRepository,
        customerAddressRepository,
        merchantOriginRepository,
        promoService,
        productCollectionRepository
    )

    private fun createPopulatedCart(cartId: UUID): Cart {
        val product = mockk<Product> {
            every { status } returns ProductStatus.PUBLISHED
            every { title } returns "Test Product"
            every { id } returns UUID.randomUUID()
        }
        val variant = mockk<ProductVariant> {
            every { status } returns VariantStatus.ACTIVE
            every { stockAvailable } returns 10
            every { this@mockk.product } returns product
            every { id } returns UUID.randomUUID()
            every { sku } returns "SKU-1"
            every { color } returns "Red"
            every { sizeCode } returns "XL"
            every { priceAmount } returns 50000L
            every { compareAtAmount } returns null
        }

        val cart = Cart(id = cartId, status = CartStatus.ACTIVE)
        val cartItem = CartItem(
            cart = cart,
            product = product,
            variant = variant,
            productTitleSnapshot = "Test Product",
            skuSnapshot = "SKU-1",
            color = "Red",
            sizeCode = "XL",
            quantity = 2,
            unitPriceAmount = 50000L
        )
        cart.items.add(cartItem)
        cart.subtotalAmount = 100000L
        return cart
    }

    @Test
    fun `createCheckout should create a valid checkout from cart`() {
        val cartId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val cart = createPopulatedCart(cartId).apply { this.customerId = customerId }

        every { cartRepository.findById(cartId) } returns Optional.of(cart)
        every { checkoutRepository.findByCartId(cartId) } returns Optional.empty()
        every { checkoutRepository.save(any()) } returnsArgument 0
        every { cartRepository.save(any()) } returnsArgument 0

        val checkout = checkoutService.createCheckout(cartId, customerId, null)

        assertEquals(cart.subtotalAmount, checkout.subtotalAmount)
        assertEquals(customerId, checkout.customerId)
        assertEquals(CheckoutStatus.ACTIVE, checkout.status)
        assertEquals(1, checkout.items.size)
        assertEquals(CartStatus.CHECKOUT_IN_PROGRESS, cart.status)

        verify { cartRepository.save(cart) }
        verify { checkoutRepository.save(any()) }
    }

    @Test
    fun `createCheckout should throw exception if guest token is missing`() {
        val cartId = UUID.randomUUID()
        val cart = createPopulatedCart(cartId).apply {
            accessTokenHash = HashUtils.sha256("secret")
        }

        every { cartRepository.findById(cartId) } returns Optional.of(cart)

        assertThrows<UnauthorizedException> {
            checkoutService.createCheckout(cartId, null, null)
        }
    }

    @Test
    fun `createCheckout should throw exception if guest token is invalid`() {
        val cartId = UUID.randomUUID()
        val cart = createPopulatedCart(cartId).apply {
            accessTokenHash = HashUtils.sha256("correct-token")
        }

        every { cartRepository.findById(cartId) } returns Optional.of(cart)

        assertThrows<UnauthorizedException> {
            checkoutService.createCheckout(cartId, null, "wrong-token")
        }
    }
}
