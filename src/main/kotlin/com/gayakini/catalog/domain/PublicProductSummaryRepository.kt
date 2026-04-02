package com.gayakini.catalog.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface PublicProductSummaryRepository : JpaRepository<PublicProductSummary, UUID> {

    @Query("""
        SELECT p FROM PublicProductSummary p
        WHERE (:q IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.brandName) LIKE LOWER(CONCAT('%', :q, '%')))
        AND (:categorySlug IS NULL OR p.categorySlug = :categorySlug)
        AND (:collectionSlug IS NULL OR p.collectionSlug = :collectionSlug)
        AND (:color IS NULL OR p.color = :color)
        AND (:sizeCode IS NULL OR p.sizeCode = :sizeCode)
        AND (:minPrice IS NULL OR p.minPriceAmount >= :minPrice)
        AND (:maxPrice IS NULL OR p.maxPriceAmount <= :maxPrice)
        AND (:inStock IS NULL OR p.inStock = :inStock)
    """)
    fun search(
        q: String?,
        categorySlug: String?,
        collectionSlug: String?,
        color: String?,
        sizeCode: String?,
        minPrice: Long?,
        maxPrice: Long?,
        inStock: Boolean?,
        pageable: Pageable
    ): Page<PublicProductSummary>
}
