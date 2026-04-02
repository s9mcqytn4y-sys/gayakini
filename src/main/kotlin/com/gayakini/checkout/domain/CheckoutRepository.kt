package com.gayakini.checkout.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CheckoutRepository : JpaRepository<Checkout, UUID> {
    fun findByAccessTokenHash(accessTokenHash: String): Optional<Checkout>

    fun findByCartId(cartId: UUID): Optional<Checkout>
}

interface CheckoutItemRepository : JpaRepository<CheckoutItem, UUID> {
    fun findByCheckoutId(checkoutId: UUID): List<CheckoutItem>
}

interface CheckoutShippingQuoteRepository : JpaRepository<CheckoutShippingQuote, UUID> {
    fun deleteAllByCheckoutId(checkoutId: UUID)
}
