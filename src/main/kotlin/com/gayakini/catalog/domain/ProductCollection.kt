package com.gayakini.catalog.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_collections", schema = "commerce")
class ProductCollection(
    @EmbeddedId
    val id: ProductCollectionId,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now(),
)

@Embeddable
data class ProductCollectionId(
    @Column(name = "product_id", nullable = false)
    val productId: UUID,
    @Column(name = "collection_id", nullable = false)
    val collectionId: UUID,
) : Serializable

interface ProductCollectionRepository : org.springframework.data.jpa.repository.JpaRepository<ProductCollection, ProductCollectionId> {
    fun findAllByIdProductId(productId: UUID): List<ProductCollection>
}
