package com.gayakini.promo.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PromoExclusionRepository : JpaRepository<PromoExclusion, UUID> {
    fun findByPromoId(promoId: UUID): List<PromoExclusion>

    fun existsByPromoIdAndExclusionTypeAndExcludedEntityId(
        promoId: UUID,
        exclusionType: ExclusionType,
        excludedEntityId: UUID,
    ): Boolean

    fun deleteByPromoIdAndExclusionTypeAndExcludedEntityId(
        promoId: UUID,
        exclusionType: ExclusionType,
        excludedEntityId: UUID,
    )
}
