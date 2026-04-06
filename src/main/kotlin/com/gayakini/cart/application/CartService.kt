package com.gayakini.cart.application

import com.gayakini.cart.domain.Cart
import com.gayakini.cart.domain.CartItem
import com.gayakini.cart.domain.CartItemRepository
import com.gayakini.cart.domain.CartRepository
import com.gayakini.cart.domain.CartStatus
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.common.api.ForbiddenException
import com.gayakini.common.api.UnauthorizedException
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
        val normalizedCurrency = currency.trim().uppercase()
        require(normalizedCurrency.length == 3) { "Kode mata uang harus terdiri dari 3 huruf." }

        val cartId = UUID.randomUUID()
        var rawToken: String? = null
        var tokenHash: String? = null

        if (customerId == null) {
            rawToken = UUID.randomUUID().toString()
            tokenHash = HashUtils.sha256(rawToken)
        }

        val cart =
            Cart(
                id = cartId,
                customerId = customerId,
                currencyCode = normalizedCurrency,
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
        cartToken: String? = null,
    ): Cart {
        val cart = getValidatedCart(cartId, customerId, cartToken)

        if (cart.status != CartStatus.ACTIVE) {
            throw IllegalStateException("Keranjang sudah tidak aktif.")
        }

        require(quantity in 1..99) { "Jumlah item harus antara 1 dan 99." }

        val variant =
            variantRepository.findById(variantId)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        val existingItem = cart.items.find { it.variant.id == variantId }
        if (existingItem != null) {
            existingItem.quantity += quantity
            if (existingItem.quantity > 99) existingItem.quantity = 99
        } else {
            val newItem =
                CartItem(
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

    @Transactional
    fun updateItem(
        cartId: UUID,
        itemId: UUID,
        quantity: Int,
        customerId: UUID? = null,
        cartToken: String? = null,
    ): Cart {
        require(quantity in 1..99) { "Jumlah item harus antara 1 dan 99." }

        val cart = getValidatedCart(cartId, customerId, cartToken)
        if (cart.status != CartStatus.ACTIVE) {
            throw IllegalStateException("Keranjang sudah tidak aktif.")
        }

        val item =
            cart.items.find { it.id == itemId }
                ?: throw NoSuchElementException("Item keranjang tidak ditemukan.")

        item.quantity = quantity
        item.updatedAt = Instant.now()
        updateTotals(cart)
        return cartRepository.save(cart)
    }

    @Transactional
    fun removeItem(
        cartId: UUID,
        itemId: UUID,
        customerId: UUID? = null,
        cartToken: String? = null,
    ): Cart {
        val cart = getValidatedCart(cartId, customerId, cartToken)
        if (cart.status != CartStatus.ACTIVE) {
            throw IllegalStateException("Keranjang sudah tidak aktif.")
        }

        val item =
            cart.items.find { it.id == itemId }
                ?: throw NoSuchElementException("Item keranjang tidak ditemukan.")

        cart.items.remove(item)
        cartItemRepository.delete(item)
        updateTotals(cart)
        return cartRepository.save(cart)
    }

    fun getValidatedCart(
        cartId: UUID,
        customerId: UUID?,
        cartToken: String?,
    ): Cart {
        val cart =
            cartRepository.findById(cartId)
                .orElseThrow { NoSuchElementException("Keranjang tidak ditemukan.") }

        if (cart.customerId != null && cart.customerId != customerId) {
            if (customerId == null) {
                throw UnauthorizedException("Silakan login untuk mengakses keranjang ini.")
            }
            throw ForbiddenException("Akses keranjang ditolak.")
        }

        if (cart.accessTokenHash != null) {
            if (cartToken == null || HashUtils.sha256(cartToken) != cart.accessTokenHash) {
                throw UnauthorizedException("Token keranjang tidak valid.")
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
