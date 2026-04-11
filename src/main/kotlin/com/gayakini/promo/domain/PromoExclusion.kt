package com.gayakini.promo.domain

import com.gayakini.common.util.UuidV7Generator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "promo_exclusions", schema = "commerce")
class PromoExclusion(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UuidV7Generator.generate(),

    @Column(name = "promo_id", nullable = false)
    val promoId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "exclusion_type", nullable = false, length = 20)
    val exclusionType: ExclusionType,

    @Column(name = "excluded_entity_id", nullable = false)
    val excludedEntityId: UUID,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now(),
)

enum class ExclusionType {
    CATEGORY,
    COLLECTION,
    PRODUCT,
}
