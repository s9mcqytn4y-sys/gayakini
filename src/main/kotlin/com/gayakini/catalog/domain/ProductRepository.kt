package com.gayakini.catalog.domain

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.Optional
import java.util.UUID

interface ProductRepository : JpaRepository<Product, UUID> {
    fun findBySlug(slug: String): Optional<Product>
}

interface ProductVariantRepository : JpaRepository<ProductVariant, UUID> {
    fun findBySku(sku: String): Optional<ProductVariant>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: UUID): Optional<ProductVariant>

    fun findAllByIdIn(ids: List<UUID>): List<ProductVariant>
}
