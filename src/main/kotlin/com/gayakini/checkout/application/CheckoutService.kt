package com.gayakini.checkout.application

import com.gayakini.cart.application.CartService
import com.gayakini.cart.domain.Cart
import com.gayakini.cart.domain.CartRepository
import com.gayakini.cart.domain.CartStatus
import com.gayakini.catalog.domain.ProductStatus
import com.gayakini.catalog.domain.VariantStatus
import com.gayakini.checkout.api.CheckoutShippingAddressRequest
import com.gayakini.checkout.api.GuestAddressRequest
import com.gayakini.checkout.domain.*
import com.gayakini.common.api.ForbiddenException
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.common.util.HashUtils
import com.gayakini.common.util.UuidV7Generator
import com.gayakini.customer.domain.CustomerAddress
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
    private val cartService: CartService,
    private val shippingProvider: ShippingProvider,
    private val shippingQuoteRepository: CheckoutShippingQuoteRepository,
    private val customerAddressRepository: CustomerAddressRepository,
    private val merchantOriginRepository: MerchantShippingOriginRepository,
) {
    companion object {
        private const val CHECKOUT_EXPIRY_SECONDS = 3600L
        private const val QUOTE_EXPIRY_SECONDS = 3600L
    }

    @Transactional
    fun createCheckout(
        cartId: UUID,
        customerId: UUID?,
        cartToken: String?,
    ): Checkout {
        val cart =
            cartRepository.findById(cartId)
                .orElseThrow { NoSuchElementException("Keranjang tidak ditemukan.") }

        validateCartForCheckout(cart, customerId, cartToken)

        // Refresh prices and snapshots from DB to ensure integrity
        cartService.refreshCartPrices(cart)

        // Validate stock and status for all items
        cart.items.forEach { item ->
            val variant = item.variant
            check(variant.status == VariantStatus.ACTIVE) { "Produk ${item.productTitleSnapshot} tidak tersedia." }
            check(variant.product.status == ProductStatus.PUBLISHED) { "Produk ${item.productTitleSnapshot} tidak tersedia." }
            check(variant.stockAvailable >= item.quantity) {
                "Stok tidak mencukupi untuk ${item.productTitleSnapshot}. Tersedia: ${variant.stockAvailable}"
            }
        }

        expireExistingCheckouts(cartId)

        val checkout =
            Checkout(
                id = UuidV7Generator.generate(),
                cart = cart,
                customerId = customerId,
                status = CheckoutStatus.ACTIVE,
                currencyCode = cart.currencyCode,
                subtotalAmount = cart.subtotalAmount,
                expiresAt = Instant.now().plusSeconds(CHECKOUT_EXPIRY_SECONDS),
            )

        if (customerId == null && cartToken != null) {
            checkout.accessTokenHash = HashUtils.sha256(cartToken)
        }

        val savedCheckout = checkoutRepository.save(checkout)
        snapshotCartItems(cart, savedCheckout)

        cart.status = CartStatus.CHECKOUT_IN_PROGRESS
        cart.updatedAt = Instant.now()
        cartRepository.save(cart)

        return checkoutRepository.save(savedCheckout)
    }

    private fun expireExistingCheckouts(cartId: UUID) {
        checkoutRepository.findByCartId(cartId).ifPresent {
            if (it.status == CheckoutStatus.ACTIVE) {
                it.status = CheckoutStatus.EXPIRED
                it.updatedAt = Instant.now()
                checkoutRepository.save(it)
            }
        }
    }

    private fun validateCartForCheckout(
        cart: Cart,
        customerId: UUID?,
        cartToken: String?,
    ) {
        check(cart.status == CartStatus.ACTIVE) {
            "Keranjang tidak dapat digunakan untuk checkout (status: ${cart.status})."
        }

        check(cart.items.isNotEmpty()) { "Keranjang kosong." }

        validateCartOwnership(cart, customerId, cartToken)
    }

    private fun validateCartOwnership(
        cart: Cart,
        customerId: UUID?,
        cartToken: String?,
    ) {
        if (cart.customerId != null && cart.customerId != customerId) {
            handleCustomerCartOwnershipMismatch(customerId)
        }
        if (cart.accessTokenHash != null) {
            validateGuestCartToken(cartToken, cart.accessTokenHash!!)
        }
    }

    private fun handleCustomerCartOwnershipMismatch(customerId: UUID?) {
        if (customerId == null) {
            throw UnauthorizedException("Silakan login untuk mengakses keranjang ini.")
        }
        throw ForbiddenException("Akses keranjang ditolak.")
    }

    private fun validateGuestCartToken(
        cartToken: String?,
        accessTokenHash: String,
    ) {
        val token = cartToken ?: throw UnauthorizedException("Token keranjang diperlukan.")
        if (HashUtils.sha256(token) != accessTokenHash) {
            throw UnauthorizedException("Token keranjang tidak valid.")
        }
    }

    private fun snapshotCartItems(
        cart: Cart,
        checkout: Checkout,
    ) {
        cart.items.forEach { cartItem ->
            val checkoutItem =
                CheckoutItem(
                    id = UuidV7Generator.generate(),
                    checkout = checkout,
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
            checkout.items.add(checkoutItem)
        }
    }

    @Transactional
    fun updateShippingAddress(
        checkoutId: UUID,
        customerId: UUID?,
        checkoutToken: String?,
        request: CheckoutShippingAddressRequest,
    ): Checkout {
        val checkout = getValidatedCheckout(checkoutId, customerId, checkoutToken)

        when {
            request.addressId != null -> updateFromStoredAddress(checkout, request.addressId)
            request.guestAddress != null -> updateFromGuestAddress(checkout, request.guestAddress)
            else -> requireNotNull(null) { "Alamat harus diisi." }
        }

        checkout.selectedShippingQuoteId = null
        checkout.shippingCostAmount = 0
        checkout.updatedAt = Instant.now()
        return checkoutRepository.save(checkout)
    }

    private fun updateFromStoredAddress(
        checkout: Checkout,
        addressId: UUID,
    ) {
        val custId = checkout.customerId
        checkNotNull(custId) { "Harus login untuk menggunakan alamat tersimpan." }

        val customerAddress =
            customerAddressRepository.findById(addressId)
                .filter { it.customer.id == custId }
                .orElseThrow { NoSuchElementException("Alamat tidak ditemukan.") }

        val addr = checkout.shippingAddress ?: createNewShippingAddress(checkout, customerAddress)
        mapAddressFields(addr, customerAddress)
        addr.customerAddressId = customerAddress.id
        addr.updatedAt = Instant.now()
        checkout.shippingAddress = addr
    }

    private fun createNewShippingAddress(
        checkout: Checkout,
        source: CustomerAddress,
    ): CheckoutShippingAddress {
        return CheckoutShippingAddress(
            checkoutId = checkout.id,
            checkout = checkout,
            recipientName = source.recipientName,
            phone = source.phone,
            line1 = source.line1,
            line2 = source.line2,
            notes = source.notes,
            areaId = source.areaId,
            district = source.district,
            city = source.city,
            province = source.province,
            postalCode = source.postalCode,
            countryCode = source.countryCode,
        )
    }

    private fun mapAddressFields(
        target: CheckoutShippingAddress,
        source: CustomerAddress,
    ) {
        target.recipientName = source.recipientName
        target.phone = source.phone
        target.line1 = source.line1
        target.line2 = source.line2
        target.notes = source.notes
        target.areaId = source.areaId
        target.district = source.district
        target.city = source.city
        target.province = source.province
        target.postalCode = source.postalCode
        target.countryCode = source.countryCode
    }

    private fun updateFromGuestAddress(
        checkout: Checkout,
        guest: GuestAddressRequest,
    ) {
        val addr = checkout.shippingAddress ?: createGuestShippingAddress(checkout, guest)

        mapGuestAddressFields(addr, guest)
        addr.email = guest.email
        addr.updatedAt = Instant.now()
        checkout.shippingAddress = addr
    }

    private fun createGuestShippingAddress(
        checkout: Checkout,
        guest: GuestAddressRequest,
    ): CheckoutShippingAddress {
        return CheckoutShippingAddress(
            checkoutId = checkout.id,
            checkout = checkout,
            recipientName = guest.recipientName,
            phone = guest.phone,
            email = guest.email,
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
    }

    private fun mapGuestAddressFields(
        target: CheckoutShippingAddress,
        guest: GuestAddressRequest,
    ) {
        target.recipientName = guest.recipientName
        target.phone = guest.phone
        target.line1 = guest.line1
        target.line2 = guest.line2
        target.notes = guest.notes
        target.areaId = guest.areaId
        target.district = guest.district
        target.city = guest.city
        target.province = guest.province
        target.postalCode = guest.postalCode
        target.countryCode = guest.countryCode
    }

    @Transactional
    fun calculateShippingQuotes(
        checkoutId: UUID,
        customerId: UUID?,
        checkoutToken: String?,
    ): Checkout {
        val checkout = getValidatedCheckout(checkoutId, customerId, checkoutToken)
        val address = checkout.shippingAddress
        checkNotNull(address) { "Alamat pengiriman belum diset." }

        val origin =
            merchantOriginRepository.findDefaultActive()
                .orElseThrow { IllegalStateException("Origin pengiriman merchant belum dikonfigurasi.") }

        val items = createShippingItems(checkout)

        val rates =
            shippingProvider.getRates(
                origin = origin.areaId ?: "TODO_DEFAULT_ORIGIN",
                destination = address.areaId,
                items = items,
            )

        shippingQuoteRepository.deleteAllByCheckoutId(checkout.id)
        checkout.availableShippingQuotes.clear()

        rates.forEach { rate ->
            val quote = createShippingQuote(checkout, rate)
            checkout.availableShippingQuotes.add(quote)
        }

        checkout.updatedAt = Instant.now()
        return checkoutRepository.save(checkout)
    }

    private fun createShippingItems(checkout: Checkout): List<ShippingItem> {
        return checkout.items.map {
            ShippingItem(
                name = it.productTitleSnapshot,
                weightGrams = it.variant.weightGrams,
                quantity = it.quantity,
                valueIdr = it.unitPriceAmount,
            )
        }
    }

    private fun createShippingQuote(
        checkout: Checkout,
        rate: com.gayakini.shipping.domain.ShippingRate,
    ): CheckoutShippingQuote {
        return CheckoutShippingQuote(
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
            expiresAt = Instant.now().plusSeconds(QUOTE_EXPIRY_SECONDS),
        )
    }

    @Transactional
    fun selectShippingQuote(
        checkoutId: UUID,
        customerId: UUID?,
        checkoutToken: String?,
        quoteId: UUID,
    ): Checkout {
        val checkout = getValidatedCheckout(checkoutId, customerId, checkoutToken)
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

    fun getValidatedCheckout(
        checkoutId: UUID,
        customerId: UUID?,
        checkoutToken: String?,
    ): Checkout {
        val checkout = getCheckout(checkoutId)
        validateCheckoutOwnership(checkout, customerId, checkoutToken)
        return checkout
    }

    private fun validateCheckoutOwnership(
        checkout: Checkout,
        customerId: UUID?,
        checkoutToken: String?,
    ) {
        if (checkout.customerId != null && checkout.customerId != customerId) {
            handleCustomerCheckoutOwnershipMismatch(customerId)
        }

        if (checkout.accessTokenHash != null) {
            validateGuestCheckoutToken(checkoutToken, checkout.accessTokenHash!!)
        }
    }

    private fun handleCustomerCheckoutOwnershipMismatch(customerId: UUID?) {
        if (customerId == null) {
            throw UnauthorizedException("Silakan login untuk mengakses checkout ini.")
        }
        throw ForbiddenException("Akses checkout ditolak.")
    }

    private fun validateGuestCheckoutToken(
        checkoutToken: String?,
        accessTokenHash: String,
    ) {
        val token = checkoutToken ?: throw UnauthorizedException("Token checkout diperlukan.")
        if (HashUtils.sha256(token) != accessTokenHash) {
            throw UnauthorizedException("Token checkout tidak valid.")
        }
    }
}
