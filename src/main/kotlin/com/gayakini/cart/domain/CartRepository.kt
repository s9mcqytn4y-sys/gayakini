package com.gayakini.cart.domain

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CartRepository : JpaRepository<Cart, UUID> {
    fun findByCustomerIdAndIsActiveTrue(customerId: UUID): Optional<Cart>
    fun findByGuestTokenHashAndIsActiveTrue(guestTokenHash: String): Optional<Cart>
}

interface CartItemRepository : JpaRepository<CartItem, UUID>
