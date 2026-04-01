package com.gayakini.catalog.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "products", schema = "commerce")
class Product(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(unique = true, nullable = false)
    val slug: String,
    @Column(nullable = false)
    var title: String,
    @Column(length = 180)
    var subtitle: String?,
    @Column(name = "brand_name", nullable = false, length = 120)
    var brandName: String,
    @Column(name = "category_id")
    var categoryId: UUID?,
    @Column(columnDefinition = "TEXT", nullable = false)
    var description: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProductStatus = ProductStatus.DRAFT,
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
    @Column(name = "archived_at")
    var archivedAt: Instant? = null,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)

enum class ProductStatus { DRAFT, PUBLISHED, ARCHIVED }
