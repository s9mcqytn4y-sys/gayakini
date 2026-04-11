package com.gayakini.catalog.domain

import com.gayakini.common.util.UuidV7Generator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "collections", schema = "commerce")
class Collection(
    @Id
    val id: UUID = UuidV7Generator.generate(),
    @Column(unique = true, nullable = false, length = 100)
    val slug: String,
    @Column(nullable = false, length = 120)
    var name: String,
    @Column(columnDefinition = "TEXT")
    var description: String?,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)

interface CollectionRepository : org.springframework.data.jpa.repository.JpaRepository<Collection, UUID> {
    fun findBySlug(slug: String): java.util.Optional<Collection>
}
