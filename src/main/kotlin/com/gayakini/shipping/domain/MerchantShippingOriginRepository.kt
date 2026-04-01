package com.gayakini.shipping.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface MerchantShippingOriginRepository : JpaRepository<MerchantShippingOrigin, UUID> {
    fun findByIsDefaultTrueAndIsActiveTrue(): Optional<MerchantShippingOrigin>
}
