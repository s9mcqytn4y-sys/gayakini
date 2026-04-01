package com.gayakini.checkout.application

import com.gayakini.cart.domain.CartRepository
import com.gayakini.cart.domain.CartStatus
import com.gayakini.checkout.api.GuestAddressRequest
import com.gayakini.checkout.domain.*
import com.gayakini.common.util.HashUtils
import com.gayakini.customer.domain.CustomerAddress
import com.gayakini.shipping.domain.ShippingProvider
import com.gayakini.shipping.domain.ShippingItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class CheckoutService(
    private val checkoutRepository: CheckoutRepository,
    private val cartRepository: CartRepository,
    private val checkoutItemRepository: CheckoutItemRepository,
    private val shippingProvider: ShippingProvider
) {
    @Transactional
    fun createCheckout(cartId: UUID, customerId: UUID?, cartToken: String?): Checkout {
        val cart = cartRepository.findById(cartId)
            .orElseThrow { NoSuchElementException("Keranjang tidak ditemukan.") }

        if (cart.status != CartStatus.ACTIVE) {
            throw IllegalStateException("Keranjang tidak dapat digunakan untuk checkout (status: ${cart.status}).")
        }
        
        if (cart.items.isEmpty()) {
            throw IllegalStateException("Keranjang kosong.")
        }

        // Validate cart ownership
        if (cart.customerId != null && cart.customerId != customerId) {
            throw IllegalStateException("Akses keranjang ditolak.")
        }
        if (cart.accessTokenHash != null && HashUtils.sha256(cartToken ?: "") != cart.accessTokenHash) {
             throw IllegalStateException("Akses keranjang ditolak.")
        }

        // Close existing checkout for this cart if any
        checkoutRepository.findByCartId(cartId).ifPresent {
            if (it.status == CheckoutStatus.ACTIVE) {
                it.status = CheckoutStatus.EXPIRED
                checkoutRepository.save(it)
            }
        }

        val checkout = Checkout(
            cart = cart,
            customerId = customerId,
            status = CheckoutStatus.ACTIVE,
            currencyCode = cart.currencyCode,
            subtotalAmount = cart.subtotalAmount,
            expiresAt = Instant.now().plusSeconds(3600) // 1 hour
        )
        
        if (customerId == null && cartToken != null) {
            checkout.accessTokenHash = HashUtils.sha256(cartToken)
        }

        val savedCheckout = checkoutRepository.save(checkout)

        // Snapshot items
        cart.items.forEach { cartItem ->
            val checkoutItem = CheckoutItem(
                checkout = savedCheckout,
                product = cartItem.product!!,
                variant = cartItem.variant,
                productTitleSnapshot = cartItem.productTitleSnapshot ?: "",
                skuSnapshot = cartItem.skuSnapshot ?: "",
                color = cartItem.color ?: "",
                sizeCode = cartItem.sizeCode ?: "",
                quantity = cartItem.quantity,
                unitPriceAmount = cartItem.unitPriceAmount,
                compareAtAmount = cartItem.compareAtAmount,
                primaryImageUrl = cartItem.primaryImageUrl
            )
            savedCheckout.items.add(checkoutItem)
        }
        
        cart.status = CartStatus.CHECKOUT_IN_PROGRESS
        cartRepository.save(cart)

        return checkoutRepository.save(savedCheckout)
    }

    @Transactional
    fun setShippingAddress(checkoutId: UUID, addressId: UUID?, guestAddress: GuestAddressRequest?): Checkout {
        val checkout = getCheckout(checkoutId)
        
        if (addressId != null) {
            // TODO: Load from customer addresses
            // This is just a placeholder logic
        } else if (guestAddress != null) {
            val addr = checkout.shippingAddress ?: CheckoutShippingAddress(
                checkoutId = checkout.id,
                checkout = checkout,
                recipientName = guestAddress.recipientName,
                phone = guestAddress.phone,
                line1 = guestAddress.line1,
                line2 = null,
                notes = null,
                areaId = guestAddress.areaId,
                district = guestAddress.district,
                city = guestAddress.city,
                province = guestAddress.province,
                postalCode = guestAddress.postalCode,
                countryCode = guestAddress.countryCode
            )
            // Update fields
            addr.recipientName = guestAddress.recipientName
            addr.phone = guestAddress.phone
            addr.line1 = guestAddress.line1
            addr.areaId = guestAddress.areaId
            addr.district = guestAddress.district
            addr.city = guestAddress.city
            addr.province = guestAddress.province
            addr.postalCode = guestAddress.postalCode
            
            checkout.shippingAddress = addr
        } else {
            throw IllegalArgumentException("Alamat harus diisi.")
        }
        
        // Reset selected quote when address changes
        checkout.selectedShippingQuoteId = null
        checkout.shippingCostAmount = 0
        
        return checkoutRepository.save(checkout)
    }

    @Transactional
    fun calculateShippingQuotes(checkoutId: UUID): Checkout {
        val checkout = getCheckout(checkoutId)
        val address = checkout.shippingAddress ?: throw IllegalStateException("Alamat pengiriman belum diset.")
        
        // TODO: Origin should come from Merchant Origin table
        val originAreaId = "IDNP01" 
        
        val items = checkout.items.map {
            ShippingItem(
                name = it.productTitleSnapshot,
                weightGrams = it.variant.weightGrams,
                quantity = it.quantity,
                valueIdr = it.unitPriceAmount
            )
        }
        
        val rates = shippingProvider.getRates(originAreaId, address.areaId, items)
        
        // Clear old quotes
        // JPA will handle orphan removal if configured, or we do it manually
        // For simplicity, let's assume we refresh them.
        
        // map rates to CheckoutShippingQuote and save
        // ... (omitted for brevity in this step, but crucial in full implementation)
        
        return checkoutRepository.save(checkout)
    }

    fun getCheckout(checkoutId: UUID): Checkout {
        return checkoutRepository.findById(checkoutId)
            .orElseThrow { NoSuchElementException("Checkout tidak ditemukan.") }
    }
}
