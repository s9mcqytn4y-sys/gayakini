package com.gayakini.catalog.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.UUID

@Entity
@Immutable
@Table(name = "v_public_product_summaries", schema = "commerce")
class PublicProductSummary(
    @Id
    val id: UUID,
    val slug: String,
    val title: String,
    val subtitle: String?,
    @Column(name = "brand_name")
    val brandName: String,
    @Column(name = "category_slug")
    val categorySlug: String,
    // Added to support filtering in view
    @Column(name = "collection_slug")
    val collectionSlug: String?,
    // Added to support filtering in view
    @Column(name = "color")
    val color: String?,
    // Added to support filtering in view
    @Column(name = "size_code")
    val sizeCode: String?,
    @Column(name = "primary_image_url")
    val primaryImageUrl: String?,
    @Column(name = "min_price_amount")
    val minPriceAmount: Long,
    @Column(name = "max_price_amount")
    val maxPriceAmount: Long,
    @Column(name = "in_stock")
    val inStock: Boolean,
    @Column(name = "created_at")
    val createdAt: Instant,
)
