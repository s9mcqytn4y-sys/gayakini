package com.gayakini.cart.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CartRepository : JpaRepository<Cart, UUID> {
    fun findByCustomerIdAndIsActiveTrue(customerId: UUID): Optional<Cart>

    fun findByGuestTokenHashAndIsActiveTrue(guestTokenHash: String): Optional<Cart>
}

interface CartItemRepository : JpaRepository<CartItem, UUID>
