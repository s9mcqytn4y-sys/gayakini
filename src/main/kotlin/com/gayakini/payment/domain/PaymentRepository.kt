package com.gayakini.payment.domain

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PaymentRepository : JpaRepository<Payment, UUID> {
    fun findByOrderId(orderId: UUID): Optional<Payment>
    fun findByExternalId(externalId: String): Optional<Payment>
}
