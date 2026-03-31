package com.gayakini.catalog.domain

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import jakarta.persistence.LockModeType
import java.util.Optional

interface ProductRepository : JpaRepository<Product, UUID> {
    fun findBySlug(slug: String): Optional<Product>
}

interface ProductVariantRepository : JpaRepository<ProductVariant, UUID> {
    fun findBySku(sku: String): Optional<ProductVariant>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: UUID): Optional<ProductVariant>
}
