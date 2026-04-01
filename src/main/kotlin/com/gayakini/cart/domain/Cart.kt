package com.gayakini.cart.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "carts")
class Cart(
    @Id
    val id: UUID,
    @Column(name = "customer_id")
    val customerId: UUID?,
    @Column(name = "guest_token_hash")
    val guestTokenHash: String?,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @OneToMany(mappedBy = "cart", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<CartItem> = mutableListOf(),
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "cart_items")
class CartItem(
    @Id
    val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    val cart: Cart,
    @Column(name = "variant_id", nullable = false)
    val variantId: UUID,
    @Column(nullable = false)
    var quantity: Int,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
