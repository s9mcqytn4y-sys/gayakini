package com.gayakini.promo.application

import com.gayakini.promo.api.CreatePromoRequest
import com.gayakini.promo.api.PromoResponse
import com.gayakini.promo.api.UpdatePromoRequest
import com.gayakini.promo.domain.Promo
import com.gayakini.promo.domain.PromoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.NoSuchElementException
import java.util.UUID

@Service
class PromoService(
    private val promoRepository: PromoRepository,
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
        return PromoResponse.fromEntity(promoRepository.save(promo))
    }

    @Transactional
    fun updatePromo(
        id: UUID,
        request: UpdatePromoRequest,
    ): PromoResponse {
        val promo =
            promoRepository.findById(id)
                .orElseThrow { NoSuchElementException("Promo tidak ditemukan.") }

        request.code?.let { promo.code = it.uppercase() }
        request.type?.let { promo.type = it }
        request.value?.let { promo.value = it }
        request.maxDiscountAmount?.let { promo.maxDiscountAmount = it }
        request.minOrderValue?.let { promo.minOrderValue = it }
        request.usageLimit?.let { promo.usageLimit = it }
        request.startDate?.let { promo.startDate = it }
        request.endDate?.let { promo.endDate = it }
        request.isActive?.let { promo.isActive = it }

        return PromoResponse.fromEntity(promoRepository.save(promo))
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
