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
    companion object {
        private const val DEFAULT_EXPIRY_DAYS = 7L
        private const val MAX_ITEM_QUANTITY = 99
        private const val SECONDS_IN_DAY = 86400L
        private const val CURRENCY_CODE_LENGTH = 3
    }

    @Transactional
    fun createCart(
        customerId: UUID?,
        currency: String,
    ): Pair<Cart, String?> {
        val normalizedCurrency = currency.trim().uppercase()
        require(normalizedCurrency.length == CURRENCY_CODE_LENGTH) {
            "Kode mata uang harus terdiri dari $CURRENCY_CODE_LENGTH huruf."
        }

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
                expiresAt = Instant.now().plusSeconds(SECONDS_IN_DAY * DEFAULT_EXPIRY_DAYS),
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

        check(cart.status == CartStatus.ACTIVE) { "Keranjang sudah tidak aktif." }

        require(quantity in 1..MAX_ITEM_QUANTITY) { "Jumlah item harus antara 1 dan $MAX_ITEM_QUANTITY." }

        val variant =
            variantRepository.findById(variantId)
                .orElseThrow { NoSuchElementException("Varian produk tidak ditemukan.") }

        val existingItem = cart.items.find { it.variant.id == variantId }
        if (existingItem != null) {
            existingItem.quantity += quantity
            if (existingItem.quantity > MAX_ITEM_QUANTITY) existingItem.quantity = MAX_ITEM_QUANTITY
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
        require(quantity in 1..MAX_ITEM_QUANTITY) { "Jumlah item harus antara 1 dan $MAX_ITEM_QUANTITY." }

        val cart = getValidatedCart(cartId, customerId, cartToken)
        check(cart.status == CartStatus.ACTIVE) { "Keranjang sudah tidak aktif." }

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
        check(cart.status == CartStatus.ACTIVE) { "Keranjang sudah tidak aktif." }

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

        validateCartOwnership(cart, customerId, cartToken)

        return cart
    }

    private fun validateCartOwnership(
        cart: Cart,
        customerId: UUID?,
        cartToken: String?,
    ) {
        if (cart.customerId != null && cart.customerId != customerId) {
            handleCustomerOwnershipMismatch(customerId)
        }

        if (cart.accessTokenHash != null) {
            validateGuestToken(cartToken, cart.accessTokenHash)
        }
    }

    private fun handleCustomerOwnershipMismatch(customerId: UUID?) {
        if (customerId == null) {
            throw UnauthorizedException("Silakan login untuk mengakses keranjang ini.")
        }
        throw ForbiddenException("Akses keranjang ditolak.")
    }

    private fun validateGuestToken(
        cartToken: String?,
        accessTokenHash: String?,
    ) {
        if (cartToken == null || HashUtils.sha256(cartToken) != accessTokenHash) {
            throw UnauthorizedException("Token keranjang tidak valid.")
        }
    }

    @Transactional
    fun refreshCartPrices(cart: Cart) {
        val variantIds = cart.items.map { it.variant.id }
        val variants = variantRepository.findAllByIdIn(variantIds).associateBy { it.id }

        cart.items.forEach { item ->
            val variant = variants[item.variant.id]
            if (variant != null) {
                item.unitPriceAmount = variant.priceAmount
                item.compareAtAmount = variant.compareAtAmount
                item.productTitleSnapshot = variant.product.title
                item.skuSnapshot = variant.sku
                item.updatedAt = Instant.now()
            }
        }
        updateTotals(cart)
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
