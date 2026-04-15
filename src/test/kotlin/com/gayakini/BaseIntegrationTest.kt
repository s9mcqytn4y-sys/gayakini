package com.gayakini

import com.gayakini.audit.infrastructure.AuditRepository
import com.gayakini.cart.domain.CartItemRepository
import com.gayakini.cart.domain.CartRepository
import com.gayakini.catalog.domain.CategoryRepository
import com.gayakini.catalog.domain.CollectionRepository
import com.gayakini.catalog.domain.ProductCollectionRepository
import com.gayakini.catalog.domain.ProductMediaRepository
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.catalog.domain.ProductVariantRepository
import com.gayakini.catalog.domain.PublicProductSummaryRepository
import com.gayakini.checkout.domain.CheckoutItemRepository
import com.gayakini.checkout.domain.CheckoutRepository
import com.gayakini.checkout.domain.CheckoutShippingQuoteRepository
import com.gayakini.common.idempotency.IdempotencyRecordRepository
import com.gayakini.common.infrastructure.IdempotencyKeyRepository
import com.gayakini.customer.domain.CustomerAddressRepository
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.customer.infrastructure.persistence.repository.RefreshTokenRepository
import com.gayakini.finance.domain.LedgerAccountRepository
import com.gayakini.finance.domain.LedgerEntryRepository
import com.gayakini.finance.domain.PayoutDestinationRepository
import com.gayakini.finance.domain.WithdrawalRequestRepository
import com.gayakini.inventory.domain.InventoryAdjustmentRepository
import com.gayakini.inventory.domain.InventoryReservationRepository
import com.gayakini.location.domain.LocationAreaRepository
import com.gayakini.order.domain.OrderRepository
import com.gayakini.payment.domain.PaymentReceiptRepository
import com.gayakini.payment.domain.PaymentRepository
import com.gayakini.promo.domain.PromoExclusionRepository
import com.gayakini.promo.domain.PromoRepository
import com.gayakini.reporting.infrastructure.ReportingOrderRepository
import com.gayakini.shipping.application.ShipmentRepository
import com.gayakini.shipping.domain.MerchantShippingOriginRepository
import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseIntegrationTest {
    @BeforeEach
    fun resetWireMock() {
        WireMock.reset()
    }
}

/**
 * Base class for tests that require external provider mocks (WireMock)
 * but DO NOT require a database connection.
 *
 * Disables database auto-configuration via the 'test-no-db' profile.
 */
@ActiveProfiles("test", "test-no-db")
@Suppress("UnusedPrivateMember")
abstract class BaseWireMockTest : BaseIntegrationTest() {
    @MockBean
    private lateinit var auditRepository: AuditRepository

    @MockBean
    private lateinit var orderRepository: OrderRepository

    @MockBean
    private lateinit var cartRepository: CartRepository

    @MockBean
    private lateinit var cartItemRepository: CartItemRepository

    @MockBean
    private lateinit var checkoutRepository: CheckoutRepository

    @MockBean
    private lateinit var customerRepository: CustomerRepository

    @MockBean
    private lateinit var productVariantRepository: ProductVariantRepository

    @MockBean
    private lateinit var productRepository: ProductRepository

    @MockBean
    private lateinit var productMediaRepository: ProductMediaRepository

    @MockBean
    private lateinit var inventoryReservationRepository: InventoryReservationRepository

    @MockBean
    private lateinit var inventoryAdjustmentRepository: InventoryAdjustmentRepository

    @MockBean
    private lateinit var categoryRepository: CategoryRepository

    @MockBean
    private lateinit var publicProductSummaryRepository: PublicProductSummaryRepository

    @MockBean
    private lateinit var collectionRepository: CollectionRepository

    @MockBean
    private lateinit var productCollectionRepository: ProductCollectionRepository

    @MockBean
    private lateinit var checkoutItemRepository: CheckoutItemRepository

    @MockBean
    private lateinit var checkoutShippingQuoteRepository: CheckoutShippingQuoteRepository

    @MockBean
    private lateinit var customerAddressRepository: CustomerAddressRepository

    @MockBean
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockBean
    private lateinit var merchantShippingOriginRepository: MerchantShippingOriginRepository

    @MockBean
    private lateinit var locationAreaRepository: LocationAreaRepository

    @MockBean
    private lateinit var shipmentRepository: ShipmentRepository

    @MockBean
    private lateinit var promoRepository: PromoRepository

    @MockBean
    private lateinit var promoExclusionRepository: PromoExclusionRepository

    @MockBean
    private lateinit var idempotencyKeyRepository: IdempotencyKeyRepository

    @MockBean
    private lateinit var idempotencyRecordRepository: IdempotencyRecordRepository

    @MockBean
    private lateinit var paymentRepository: PaymentRepository

    @MockBean
    private lateinit var paymentReceiptRepository: PaymentReceiptRepository

    @MockBean
    private lateinit var ledgerAccountRepository: LedgerAccountRepository

    @MockBean
    private lateinit var ledgerEntryRepository: LedgerEntryRepository

    @MockBean
    private lateinit var payoutDestinationRepository: PayoutDestinationRepository

    @MockBean
    private lateinit var withdrawalRequestRepository: WithdrawalRequestRepository

    @MockBean
    private lateinit var reportingOrderRepository: ReportingOrderRepository
}

/**
 * Base class for full integration tests that require a real PostgreSQL database.
 * Uses Testcontainers and @ServiceConnection for configuration.
 */
@Testcontainers
abstract class BaseDbIntegrationTest : BaseIntegrationTest() {
    companion object {
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }
    }
}
