package com.gayakini.promo.application

import com.gayakini.audit.application.AuditContext
import com.gayakini.catalog.domain.CategoryRepository
import com.gayakini.catalog.domain.CollectionRepository
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.promo.api.PromoItemContext
import com.gayakini.promo.domain.ExclusionType
import com.gayakini.promo.domain.Promo
import com.gayakini.promo.domain.PromoExclusion
import com.gayakini.promo.domain.PromoExclusionRepository
import com.gayakini.promo.domain.PromoRepository
import com.gayakini.promo.domain.PromoType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class PromoServiceUnitTest {
    private val promoRepository = mockk<PromoRepository>()
    private val promoExclusionRepository = mockk<PromoExclusionRepository>()
    private val productRepository = mockk<ProductRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val collectionRepository = mockk<CollectionRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher>()
    private val auditContext = mockk<AuditContext>()

    private val promoService =
        PromoService(
            promoRepository,
            promoExclusionRepository,
            productRepository,
            categoryRepository,
            collectionRepository,
            eventPublisher,
            auditContext,
        )

    private fun createPromo(
        type: PromoType = PromoType.PERCENTAGE,
        value: BigDecimal = BigDecimal("10"),
    ): Promo {
        return Promo(
            code = "SAVE10",
            type = type,
            value = value,
            startDate = Instant.now().minus(1, ChronoUnit.DAYS),
            endDate = Instant.now().plus(1, ChronoUnit.DAYS),
        )
    }

    @Test
    fun `validateAndCalculateDiscount should calculate discount correctly for valid promo`() {
        val promo = createPromo()
        every { promoRepository.findByCode("SAVE10") } returns Optional.of(promo)
        every { promoExclusionRepository.findByPromoId(promo.id) } returns emptyList()

        val (returnedPromo, discount) = promoService.validateAndCalculateDiscount("SAVE10", 100000L)

        assertEquals("SAVE10", returnedPromo.code)
        assertEquals(10000L, discount)
    }

    @Test
    fun `validateAndCalculateDiscount should respect exclusions`() {
        val promo = createPromo()
        val excludedProductId = UUID.randomUUID()
        val otherProductId = UUID.randomUUID()

        val exclusion =
            PromoExclusion(
                promoId = promo.id,
                exclusionType = ExclusionType.PRODUCT,
                excludedEntityId = excludedProductId,
            )

        every { promoRepository.findByCode("SAVE10") } returns Optional.of(promo)
        every { promoExclusionRepository.findByPromoId(promo.id) } returns listOf(exclusion)

        val items =
            listOf(
                PromoItemContext(
                    productId = excludedProductId,
                    variantId = UUID.randomUUID(),
                    categoryId = UUID.randomUUID(),
                    collectionIds = emptySet(),
                    unitPriceAmount = 100000L,
                    quantity = 1,
                ),
                PromoItemContext(
                    productId = otherProductId,
                    variantId = UUID.randomUUID(),
                    categoryId = UUID.randomUUID(),
                    collectionIds = emptySet(),
                    unitPriceAmount = 50000L,
                    quantity = 1,
                ),
            )

        // Only the 50k item should be discounted (10% of 50k = 5k)
        val (_, discount) = promoService.validateAndCalculateDiscount("SAVE10", 150000L, items)

        assertEquals(5000L, discount)
    }

    @Test
    fun `validateAndCalculateDiscount should throw if all items are excluded`() {
        val promo = createPromo()
        val excludedProductId = UUID.randomUUID()
        val exclusion =
            PromoExclusion(
                promoId = promo.id,
                exclusionType = ExclusionType.PRODUCT,
                excludedEntityId = excludedProductId,
            )

        every { promoRepository.findByCode("SAVE10") } returns Optional.of(promo)
        every { promoExclusionRepository.findByPromoId(promo.id) } returns listOf(exclusion)

        val items =
            listOf(
                PromoItemContext(
                    productId = excludedProductId,
                    variantId = UUID.randomUUID(),
                    categoryId = UUID.randomUUID(),
                    collectionIds = emptySet(),
                    unitPriceAmount = 100000L,
                    quantity = 1,
                ),
            )

        assertThrows<IllegalArgumentException> {
            promoService.validateAndCalculateDiscount("SAVE10", 100000L, items)
        }
    }
}
