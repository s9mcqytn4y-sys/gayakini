package com.gayakini.promo.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import jakarta.persistence.LockModeType
import java.util.Optional
import java.util.UUID

interface PromoRepository : JpaRepository<Promo, UUID> {
    fun findByCode(code: String): Optional<Promo>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByCode(code: String): Optional<Promo>
}
