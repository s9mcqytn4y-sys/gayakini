package com.gayakini.promo.application

import com.gayakini.promo.api.CreatePromoRequest
import com.gayakini.promo.api.PromoResponse
import com.gayakini.promo.api.UpdatePromoRequest
import com.gayakini.promo.domain.Promo
import com.gayakini.promo.domain.PromoRepository
import com.gayakini.audit.application.AuditContext
import com.gayakini.audit.domain.AuditEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.NoSuchElementException
import java.util.UUID

@Service
class PromoService(
    private val promoRepository: PromoRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val auditContext: AuditContext,
) {
    @Transactional(readOnly = true)
    fun getAllPromos(): List<PromoResponse> {
        return promoRepository.findAll().map { PromoResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getPromoById(id: UUID): PromoResponse {
        val promo =
            promoRepository.findById(id)
                .orElseThrow { NoSuchElementException("Promo tidak ditemukan.") }
        return PromoResponse.fromEntity(promo)
    }

    @Transactional
    fun createPromo(request: CreatePromoRequest): PromoResponse {
        val promo =
            Promo(
                code = request.code.uppercase(),
                type = request.type,
                value = request.value,
                maxDiscountAmount = request.maxDiscountAmount,
                minOrderValue = request.minOrderValue,
                usageLimit = request.usageLimit,
                startDate = request.startDate,
                endDate = request.endDate,
                isActive = request.isActive,
            )
        val savedPromo = promoRepository.save(promo)
        val (actorId, actorRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = actorId,
                actorRole = actorRole,
                entityType = "PROMO",
                entityId = savedPromo.code,
                eventType = "PROMO_CREATED",
                newState =
                    mapOf(
                        "id" to savedPromo.id,
                        "code" to savedPromo.code,
                        "type" to savedPromo.type,
                        "value" to savedPromo.value,
                        "isActive" to savedPromo.isActive,
                    ),
                reason = "Admin created new promo",
            ),
        )
        return PromoResponse.fromEntity(savedPromo)
    }

    @Transactional
    fun updatePromo(
        id: UUID,
        request: UpdatePromoRequest,
    ): PromoResponse {
        val promo =
            promoRepository.findById(id)
                .orElseThrow { NoSuchElementException("Promo tidak ditemukan.") }

        val previousState =
            mapOf(
                "code" to promo.code,
                "type" to promo.type,
                "value" to promo.value,
                "isActive" to promo.isActive,
            )

        request.code?.let { promo.code = it.uppercase() }
        request.type?.let { promo.type = it }
        request.value?.let { promo.value = it }
        request.maxDiscountAmount?.let { promo.maxDiscountAmount = it }
        request.minOrderValue?.let { promo.minOrderValue = it }
        request.usageLimit?.let { promo.usageLimit = it }
        request.startDate?.let { promo.startDate = it }
        request.endDate?.let { promo.endDate = it }
        request.isActive?.let { promo.isActive = it }

        val savedPromo = promoRepository.save(promo)
        val (actorId, actorRole) = auditContext.getCurrentActor()
        eventPublisher.publishEvent(
            AuditEvent(
                actorId = actorId,
                actorRole = actorRole,
                entityType = "PROMO",
                entityId = savedPromo.code,
                eventType = "PROMO_UPDATED",
                previousState = previousState,
                newState =
                    mapOf(
                        "id" to savedPromo.id,
                        "code" to savedPromo.code,
                        "type" to savedPromo.type,
                        "value" to savedPromo.value,
                        "isActive" to savedPromo.isActive,
                    ),
                reason = "Admin updated promo",
            ),
        )
        return PromoResponse.fromEntity(savedPromo)
    }

    @Transactional
    fun deletePromo(id: UUID) {
        if (!promoRepository.existsById(id)) {
            throw NoSuchElementException("Promo tidak ditemukan.")
        }
        promoRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun validateAndCalculateDiscount(
        code: String,
        orderSubtotal: Long,
    ): Pair<Promo, Long> {
        val promo =
            promoRepository.findByCode(code.uppercase())
                .orElseThrow { IllegalArgumentException("Kode promo tidak valid.") }

        val subtotalBd = BigDecimal(orderSubtotal)
        require(promo.isValid(subtotalBd)) { "Promo tidak dapat digunakan." }

        val discountBd = promo.calculateDiscount(subtotalBd)
        return Pair(promo, discountBd.toLong())
    }

    @Transactional
    fun incrementUsage(code: String) {
        val promo =
            promoRepository.findWithLockByCode(code.uppercase())
                .orElseThrow { IllegalArgumentException("Kode promo tidak valid.") }

        val usageLimit = promo.usageLimit
        check(usageLimit == null || promo.currentUsage < usageLimit) {
            "Batas penggunaan promo telah tercapai."
        }

        promo.currentUsage += 1
        promoRepository.save(promo)
    }
}
