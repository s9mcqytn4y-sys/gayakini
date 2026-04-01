package com.gayakini.cart.application

import com.gayakini.cart.domain.*
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.common.util.HashUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
    private val variantRepository: ProductVariantRepository
) {
    @Transactional
    fun createCart(customerId: UUID?, currency: String): Cart {
        val cart = Cart(
            id = UUID.randomUUID(),
            customerId = customerId,
            currencyCode = currency,
            status = CartStatus.ACTIVE,
            expiresAt = Instant.now().plusSeconds(86400 * 7) // 7 days
        )
        
        if (customerId == null) {
            val rawToken = UUID.randomUUID().toString()
            cart.accessTokenHash = HashUtils.sha256(rawToken)
            // In a real scenario, we'd return the rawToken to the client once.
            // For now, let's assume the controller handles returning the raw token.
        }
        
        return cartRepository.save(cart)
    }

    @Transactional
    fun addItem(cartId: UUID, variantId: UUID, quantity: Int): Cart {
        val cart = cartRepository.findById(cartId)
            .orElseThrow { NoSuchElementException("Keranjang tidak ditemukan.") }
        
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
                primaryImageUrl = null // TODO: Get from product media
            )
            cart.items.add(newItem)
        }
        
        updateTotals(cart)
        return cartRepository.save(cart)
    }

    private fun updateTotals(cart: Cart) {
        cart.itemCount = cart.items.sumOf { it.quantity }
        // Note: unitPriceAmount is a snapshot, but for active cart we might want to refresh it?
        // Spec says CartItem has unitPrice. Usually it tracks current price until checkout.
        cart.subtotalAmount = cart.items.sumOf { it.unitPriceAmount * it.quantity }
        cart.updatedAt = Instant.now()
    }
    
    fun getCart(cartId: UUID): Cart {
        return cartRepository.findById(cartId)
            .orElseThrow { NoSuchElementException("Keranjang tidak ditemukan.") }
    }
}
