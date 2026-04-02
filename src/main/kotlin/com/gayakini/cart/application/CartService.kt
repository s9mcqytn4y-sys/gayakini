package com.gayakini.cart.application

import com.gayakini.cart.domain.Cart
import com.gayakini.cart.domain.CartItem
import com.gayakini.cart.domain.CartItemRepository
import com.gayakini.cart.domain.CartRepository
import com.gayakini.cart.domain.CartStatus
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.common.util.HashUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.NoSuchElementException
import java.util.UUID

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
    private val variantRepository: ProductVariantRepository,
) {
    @Transactional
    fun createCart(
        customerId: UUID?,
        currency: String,
    ): Pair<Cart, String?> {
        val cartId = UUID.randomUUID()
        var rawToken: String? = null
        var tokenHash: String? = null

        if (customerId == null) {
            rawToken = UUID.randomUUID().toString()
            tokenHash = HashUtils.sha256(rawToken)
        }

        val cart = Cart(
            id = cartId,
            customerId = customerId,
            currencyCode = currency,
            status = CartStatus.ACTIVE,
            accessTokenHash = tokenHash,
            expiresAt = Instant.now().plusSeconds(86400 * 7), // 7 days
        )

        return cartRepository.save(cart) to rawToken
    }

    @Transactional
    fun addItem(
        cartId: UUID,
        variantId: UUID,
        quantity: Int,
        customerId: UUID? = null,
        cartToken: String? = null
    ): Cart {
        val cart = getValidatedCart(cartId, customerId, cartToken)

        if (cart.status != CartStatus.ACTIVE) {
            throw IllegalStateException("Keranjang sudah tidak aktif.")
        }

        val variant = variantRepository.findById(variantId)
            .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        val existingItem = cart.items.find { it.variant.id == variantId }
        if (existingItem != null) {
            existingItem.quantity += quantity
            if (existingItem.quantity > 99) existingItem.quantity = 99
        } else {
            val newItem = CartItem(
                cart = cart,
                product = variant.product,
                variant = variant,
                productTitleSnapshot = variant.product.title,
                skuSnapshot = variant.sku,
                color = variant.color,
                sizeCode = variant.sizeCode,
                quantity = quantity,
                unitPriceAmount = variant.priceAmount,
                compareAtAmount = variant.compareAtAmount,
                primaryImageUrl = variant.product.media.firstOrNull { it.isPrimary }?.url,
            )
            cart.items.add(newItem)
        }

        updateTotals(cart)
        return cartRepository.save(cart)
    }

    fun getValidatedCart(cartId: UUID, customerId: UUID?, cartToken: String?): Cart {
        val cart = cartRepository.findById(cartId)
            .orElseThrow { NoSuchElementException("Keranjang tidak ditemukan.") }

        if (cart.customerId != null && cart.customerId != customerId) {
            throw IllegalStateException("Akses keranjang ditolak.")
        }

        if (cart.accessTokenHash != null) {
            if (cartToken == null || HashUtils.sha256(cartToken) != cart.accessTokenHash) {
                throw IllegalStateException("Akses keranjang ditolak.")
            }
        }

        return cart
    }

    private fun updateTotals(cart: Cart) {
        cart.itemCount = cart.items.sumOf { it.quantity }
        cart.subtotalAmount = cart.items.sumOf { it.unitPriceAmount * it.quantity }
        cart.updatedAt = Instant.now()
    }

    fun getCart(cartId: UUID): Cart {
        return cartRepository.findById(cartId)
            .orElseThrow { NoSuchElementException("Keranjang tidak ditemukan.") }
    }
}
