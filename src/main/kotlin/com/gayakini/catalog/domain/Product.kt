package com.gayakini.catalog.domain

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "products", schema = "commerce")
class Product(
    @Id
    private val id: UUID,
    @Column(unique = true, nullable = false)
    var slug: String,
    @Column(nullable = false)
    var title: String,
    @Column(length = 180)
    var subtitle: String?,
    @Column(name = "brand_name", nullable = false, length = 120)
    var brandName: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,
    @Column(columnDefinition = "TEXT", nullable = false)
    var description: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProductStatus = ProductStatus.DRAFT,
    @Column(name = "image_url")
    var imageUrl: String? = null,
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
    @Column(name = "archived_at")
    var archivedAt: Instant? = null,
    @JsonManagedReference
    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    var variants: MutableList<ProductVariant> = mutableListOf(),
    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    var media: MutableList<ProductMedia> = mutableListOf(),
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
) : Persistable<UUID> {
    @Transient
    private var isNewRecord = true

    override fun getId(): UUID = id

    override fun isNew(): Boolean = isNewRecord

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNewRecord = false
    }
}

enum class ProductStatus { DRAFT, PUBLISHED, ARCHIVED }
