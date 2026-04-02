package com.gayakini.shipping.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface MerchantShippingOriginRepository : JpaRepository<MerchantShippingOrigin, UUID> {
    @Query("SELECT m FROM MerchantShippingOrigin m WHERE m.isDefault = true AND m.isActive = true")
    fun findDefaultActive(): Optional<MerchantShippingOrigin>
}
