package com.gayakini.catalog.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductMediaRepository : JpaRepository<ProductMedia, UUID> {
    fun findByProductIdOrderBySortOrderAsc(productId: UUID): List<ProductMedia>
}
