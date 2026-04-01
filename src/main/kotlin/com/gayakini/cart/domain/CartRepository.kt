package com.gayakini.cart.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface CartRepository : JpaRepository<Cart, UUID> {
    @Query("SELECT c FROM Cart c WHERE c.customerId = :customerId AND c.status = 'ACTIVE'")
    fun findByCustomerIdAndStatusActive(customerId: UUID): Optional<Cart>

    @Query("SELECT c FROM Cart c WHERE c.accessTokenHash = :accessTokenHash AND c.status = 'ACTIVE'")
    fun findByAccessTokenHashAndStatusActive(accessTokenHash: String): Optional<Cart>
}

interface CartItemRepository : JpaRepository<CartItem, UUID>
