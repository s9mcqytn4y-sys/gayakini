package com.gayakini.checkout.application

import com.gayakini.cart.domain.CartRepository
import com.gayakini.cart.domain.CartStatus
import com.gayakini.checkout.api.CheckoutShippingAddressRequest
import com.gayakini.checkout.domain.*
import com.gayakini.common.util.HashUtils
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.customer.domain.CustomerAddressRepository
import com.gayakini.shipping.domain.MerchantShippingOriginRepository
import com.gayakini.shipping.domain.ShippingItem
import com.gayakini.shipping.domain.ShippingProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.NoSuchElementException
import java.util.UUID

@Service
class CheckoutService(
    private val checkoutRepository: CheckoutRepository,
    private val cartRepository: CartRepository,
    private val shippingProvider: ShippingProvider,
    private val shippingQuoteRepository: CheckoutShippingQuoteRepository,
    private val customerAddressRepository: CustomerAddressRepository,
    private val merchantOriginRepository: MerchantShippingOriginRepository,
) {
    @Transactional
    fun createCheckout(
        cartId: UUID,
        customerId: UUID?,
        cartToken: String?,
    ): Checkout {
        val cart =
            cartRepository.findById(cartId)
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
                it.updatedAt = Instant.now()
                checkoutRepository.save(it)
            }
        }

        val checkout =
            Checkout(
                id = UuidV7Generator.generate(),
                cart = cart,
                customerId = customerId,
                status = CheckoutStatus.ACTIVE,
                currencyCode = cart.currencyCode,
                subtotalAmount = cart.subtotalAmount,
                expiresAt = Instant.now().plusSeconds(3600), // 1 hour
            )

        if (customerId == null && cartToken != null) {
            checkout.accessTokenHash = HashUtils.sha256(cartToken)
        }

        val savedCheckout = checkoutRepository.save(checkout)

        // Snapshot items
        cart.items.forEach { cartItem ->
            val checkoutItem =
                CheckoutItem(
                    id = UuidV7Generator.generate(),
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
                    primaryImageUrl = cartItem.primaryImageUrl,
                )
            savedCheckout.items.add(checkoutItem)
        }

        cart.status = CartStatus.CHECKOUT_IN_PROGRESS
        cart.updatedAt = Instant.now()
        cartRepository.save(cart)

        return checkoutRepository.save(savedCheckout)
    }

    @Transactional
    fun updateShippingAddress(
        checkoutId: UUID,
        request: CheckoutShippingAddressRequest,
    ): Checkout {
        val checkout = getCheckout(checkoutId)

        if (request.addressId != null) {
            val customerId = checkout.customerId ?: throw IllegalStateException("Harus login untuk menggunakan alamat tersimpan.")
            val customerAddress =
                customerAddressRepository.findById(request.addressId)
                    .filter { it.customer.id == customerId }
                    .orElseThrow { NoSuchElementException("Alamat tidak ditemukan.") }

            val addr =
                checkout.shippingAddress ?: CheckoutShippingAddress(
                    checkoutId = checkout.id,
                    checkout = checkout,
                    recipientName = customerAddress.recipientName,
                    phone = customerAddress.phone,
                    line1 = customerAddress.line1,
                    line2 = customerAddress.line2,
                    notes = customerAddress.notes,
                    areaId = customerAddress.areaId,
                    district = customerAddress.district,
                    city = customerAddress.city,
                    province = customerAddress.province,
                    postalCode = customerAddress.postalCode,
                    countryCode = customerAddress.countryCode,
                )
            addr.customerAddressId = customerAddress.id
            addr.recipientName = customerAddress.recipientName
            addr.phone = customerAddress.phone
            addr.line1 = customerAddress.line1
            addr.line2 = customerAddress.line2
            addr.notes = customerAddress.notes
            addr.areaId = customerAddress.areaId
            addr.district = customerAddress.district
            addr.city = customerAddress.city
            addr.province = customerAddress.province
            addr.postalCode = customerAddress.postalCode
            addr.countryCode = customerAddress.countryCode
            addr.updatedAt = Instant.now()

            checkout.shippingAddress = addr
        } else if (request.guestAddress != null) {
            val guest = request.guestAddress
            val addr =
                checkout.shippingAddress ?: CheckoutShippingAddress(
                    checkoutId = checkout.id,
                    checkout = checkout,
                    recipientName = guest.recipientName,
                    phone = guest.phone,
                    line1 = guest.line1,
                    line2 = guest.line2,
                    notes = guest.notes,
                    areaId = guest.areaId,
                    district = guest.district,
                    city = guest.city,
                    province = guest.province,
                    postalCode = guest.postalCode,
                    countryCode = guest.countryCode,
                )

            addr.recipientName = guest.recipientName
            addr.phone = guest.phone
            addr.line1 = guest.line1
            addr.line2 = guest.line2
            addr.notes = guest.notes
            addr.areaId = guest.areaId
            addr.district = guest.district
            addr.city = guest.city
            addr.province = guest.province
            addr.postalCode = guest.postalCode
            addr.countryCode = guest.countryCode
            addr.updatedAt = Instant.now()

            checkout.shippingAddress = addr
        } else {
            throw IllegalArgumentException("Alamat harus diisi.")
        }

        checkout.selectedShippingQuoteId = null
        checkout.shippingCostAmount = 0
        checkout.updatedAt = Instant.now()
        return checkoutRepository.save(checkout)
    }

    @Transactional
    fun calculateShippingQuotes(checkoutId: UUID): Checkout {
        val checkout = getCheckout(checkoutId)
        val address = checkout.shippingAddress ?: throw IllegalStateException("Alamat pengiriman belum diset.")

        val origin =
            merchantOriginRepository.findDefaultActive()
                .orElseThrow { IllegalStateException("Origin pengiriman merchant belum dikonfigurasi.") }

        val items =
            checkout.items.map {
                ShippingItem(
                    name = it.productTitleSnapshot,
                    weightGrams = it.variant.weightGrams,
                    quantity = it.quantity,
                    valueIdr = it.unitPriceAmount,
                )
            }

        val rates =
            shippingProvider.getRates(
                origin = origin.areaId ?: "TODO_DEFAULT_ORIGIN",
                destination = address.areaId,
                items = items,
            )

        shippingQuoteRepository.deleteAllByCheckoutId(checkout.id)
        checkout.availableShippingQuotes.clear()

        rates.forEach { rate ->
            val quote =
                CheckoutShippingQuote(
                    id = UuidV7Generator.generate(),
                    checkout = checkout,
                    provider = "BITESHIP",
                    providerReference = rate.id,
                    courierCode = rate.courierCode,
                    courierName = rate.courierName,
                    serviceCode = rate.serviceCode,
                    serviceName = rate.serviceName,
                    description = rate.description,
                    costAmount = rate.price,
                    estimatedDaysMin = rate.minDuration,
                    estimatedDaysMax = rate.maxDuration,
                    rawPayload = null,
                    expiresAt = Instant.now().plusSeconds(3600),
                )
            checkout.availableShippingQuotes.add(quote)
        }

        checkout.updatedAt = Instant.now()
        return checkoutRepository.save(checkout)
    }

    @Transactional
    fun selectShippingQuote(
        checkoutId: UUID,
        quoteId: UUID,
    ): Checkout {
        val checkout = getCheckout(checkoutId)
        val quote =
            checkout.availableShippingQuotes.find { it.id == quoteId }
                ?: throw NoSuchElementException("Pilihan pengiriman tidak ditemukan.")

        checkout.selectedShippingQuoteId = quote.id
        checkout.shippingCostAmount = quote.costAmount
        checkout.status = CheckoutStatus.READY_FOR_ORDER
        checkout.updatedAt = Instant.now()

        return checkoutRepository.save(checkout)
    }

    fun getCheckout(checkoutId: UUID): Checkout {
        return checkoutRepository.findById(checkoutId)
            .orElseThrow { NoSuchElementException("Checkout tidak ditemukan.") }
    }
}
