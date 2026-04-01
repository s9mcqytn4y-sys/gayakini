package com.gayakini.catalog.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_media", schema = "commerce")
class ProductMedia(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,
    @Column(nullable = false, columnDefinition = "TEXT")
    var url: String,
    @Column(name = "alt_text", nullable = false, length = 200)
    var altText: String,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
