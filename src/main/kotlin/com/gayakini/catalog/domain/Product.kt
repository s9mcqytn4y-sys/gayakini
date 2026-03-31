package com.gayakini.catalog.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "products")
class Product(
    @Id
    val id: UUID,

    @Column(unique = true, nullable = false)
    val slug: String,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "base_price", nullable = false)
    var basePrice: Long,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
