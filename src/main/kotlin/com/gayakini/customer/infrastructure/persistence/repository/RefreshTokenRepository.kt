package com.gayakini.customer.infrastructure.persistence.repository

import com.gayakini.customer.infrastructure.persistence.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByToken(token: String): Optional<RefreshToken>

    fun findAllByFamilyId(familyId: UUID): List<RefreshToken>

    fun deleteByCustomerId(customerId: UUID)
}
