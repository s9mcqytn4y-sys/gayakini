package com.gayakini.payment.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface PaymentRepository : JpaRepository<Payment, UUID> {
    fun findByOrderId(orderId: UUID): Optional<Payment>

    fun findByExternalId(externalId: String): Optional<Payment>
}
