package com.gayakini.location.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "shipping_areas", schema = "commerce")
class LocationArea(
    @Id
    @Column(name = "area_id", length = 100)
    val areaId: String,
    @Column(nullable = false, length = 20)
    val provider: String = "BITESHIP",
    @Column(nullable = false, length = 250)
    val label: String,
    @Column(length = 120)
    val district: String?,
    @Column(length = 120)
    val city: String?,
    @Column(length = 120)
    val province: String?,
    @Column(name = "postal_code", length = 20)
    val postalCode: String?,
    @Column(name = "country_code", length = 2, nullable = false)
    val countryCode: String = "ID",
    @Column(name = "raw_payload", columnDefinition = "JSONB")
    val rawPayload: String? = null,
    @Column(name = "refreshed_at", nullable = false)
    var refreshedAt: Instant = Instant.now(),
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)

interface LocationAreaRepository : org.springframework.data.jpa.repository.JpaRepository<LocationArea, String> {
    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM LocationArea a WHERE lower(a.label) LIKE lower(concat('%', :input, '%'))",
    )
    fun searchByLabel(
        input: String,
        pageable: org.springframework.data.domain.Pageable,
    ): List<LocationArea>
}
