package com.gayakini.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.catalog.domain.CategoryRepository
import com.gayakini.catalog.domain.ProductRepository
import com.gayakini.catalog.domain.ProductVariant
import com.gayakini.customer.domain.Customer
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.customer.domain.CustomerRole
import com.gayakini.finance.application.FinanceService
import com.gayakini.finance.domain.AccountType
import com.gayakini.finance.domain.LedgerAccount
import com.gayakini.finance.domain.LedgerAccountRepository
import com.gayakini.finance.domain.PayoutDestination
import com.gayakini.finance.domain.PayoutDestinationRepository
import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.shipping.domain.MerchantShippingOrigin
import com.gayakini.shipping.domain.MerchantShippingOriginRepository
import com.gayakini.shipping.domain.ShippingProvider
import com.gayakini.shipping.domain.ShippingRate
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID

class FinanceE2ETest : BaseE2ETest() {
    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var customerRepository: CustomerRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var accountRepository: LedgerAccountRepository

    @Autowired
    lateinit var destinationRepository: PayoutDestinationRepository

    @Autowired
    lateinit var merchantOriginRepository: MerchantShippingOriginRepository

    @Autowired
    lateinit var gayakiniProperties: GayakiniProperties

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var shippingProvider: ShippingProvider

    private lateinit var testVariant: ProductVariant
    private lateinit var adminToken: String
    private lateinit var customerToken: String

    @BeforeEach
    fun setup() {
        cleanupDatabase()
        seedCatalog()
        seedFinance()
        setupUsers()
    }

    private fun seedCatalog() {
        val category = categoryRepository.save(E2EDataFactory.createCategory())
        val product = productRepository.save(E2EDataFactory.createProduct(category))
        testVariant = E2EDataFactory.createVariant(product)
        product.variants.add(testVariant)
        productRepository.save(product)
    }

    private fun seedFinance() {
        every { shippingProvider.getRates(any(), any(), any()) } returns
            listOf(
                ShippingRate(
                    id = "jne_reg",
                    courierCode = "jne",
                    courierName = "JNE",
                    serviceCode = "reg",
                    serviceName = "Regular",
                    description = "Reguler Service",
                    price = 10000,
                    minDuration = 1,
                    maxDuration = 3,
                ),
            )

        if (merchantOriginRepository.findAll().isEmpty()) {
            merchantOriginRepository.save(
                MerchantShippingOrigin(
                    code = "MAIN_WH",
                    name = "Main Warehouse",
                    contactName = "Warehouse Manager",
                    contactPhone = "08123456789",
                    contactEmail = "warehouse@gayakini.local",
                    line1 = "Jl. Industri No. 1",
                    line2 = null,
                    notes = null,
                    areaId = "3273010",
                    district = "Coblong",
                    city = "Bandung",
                    province = "Jawa Barat",
                    postalCode = "40132",
                    isDefault = true,
                    isActive = true,
                ),
            )
        }

        if (accountRepository.findByCode(FinanceService.ACC_GATEWAY).isEmpty) {
            accountRepository.saveAll(
                listOf(
                    LedgerAccount(UUID.randomUUID(), FinanceService.ACC_GATEWAY, "Gateway", AccountType.ASSET, null),
                    LedgerAccount(UUID.randomUUID(), FinanceService.ACC_REVENUE, "Revenue", AccountType.REVENUE, null),
                    LedgerAccount(
                        UUID.randomUUID(),
                        FinanceService.ACC_LIABILITY,
                        "Liability",
                        AccountType.LIABILITY,
                        null,
                    ),
                ),
            )
        }

        if (destinationRepository.findAll().isEmpty()) {
            destinationRepository.save(
                PayoutDestination(
                    bankName = "BCA",
                    accountName = "Gayakini Admin",
                    accountNumber = "1234567890",
                    branch = "Jakarta",
                ),
            )
        }
    }

    private fun setupUsers() {
        // Create Admin
        val adminEmail = "admin@gayakini.local"
        customerRepository.findByEmail(adminEmail).ifPresent { customerRepository.delete(it) }
        val admin =
            Customer(
                id = UUID.randomUUID(),
                email = adminEmail,
                passwordHash = passwordEncoder.encode("Admin123!"),
                fullName = "Admin Gayakini",
                phone = "081111111111",
                role = CustomerRole.ADMIN,
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        customerRepository.save(admin)
        adminToken = loginAndGetToken(adminEmail, "Admin123!")

        // Create Customer
        val customerEmail = "customer@gayakini.local"
        customerRepository.findByEmail(customerEmail).ifPresent { customerRepository.delete(it) }
        val registerRequest = E2EDataFactory.createRegisterRequest(email = customerEmail)
        restTemplate.postForEntity("/v1/auth/register", registerRequest, Map::class.java)
        customerToken = loginAndGetToken(customerEmail, "Password123!")
    }

    private fun loginAndGetToken(
        email: String,
        pass: String,
    ): String {
        val loginRequest = mapOf("email" to email, "password" to pass)
        val response = restTemplate.postForEntity("/v1/auth/login", loginRequest, Map::class.java)
        val body = response.body as Map<*, *>
        val data = body["data"] as Map<*, *>
        val tokens = data["tokens"] as Map<*, *>
        return tokens["accessToken"] as String
    }

    @Test
    fun `full flow - checkout to payment to ledger to withdrawal`() {
        // 1. Create Cart & Add Item
        val cartResponse =
            restTemplate.exchange(
                "/v1/carts",
                HttpMethod.POST,
                HttpEntity<Any>(HttpHeaders().apply { set("X-Cart-Token", "") }),
                Map::class.java,
            )
        val cartId = (cartResponse.body!!["data"] as Map<*, *>)["id"] as String
        val cartToken = (cartResponse.body!!["data"] as Map<*, *>)["accessToken"] as String
        val cartHeaders = HttpHeaders().apply { set("X-Cart-Token", cartToken) }

        val addRequest = E2EDataFactory.createAddCartItemRequest(testVariant.id, 1)
        restTemplate.exchange(
            "/v1/carts/$cartId/items",
            HttpMethod.POST,
            HttpEntity(addRequest, cartHeaders),
            Map::class.java,
        )

        // 2. Checkout
        val checkoutRequest = E2EDataFactory.createCheckoutRequest(UUID.fromString(cartId))
        val checkoutResp =
            restTemplate.exchange(
                "/v1/checkouts",
                HttpMethod.POST,
                HttpEntity(checkoutRequest, cartHeaders),
                Map::class.java,
            )
        val checkoutId = (checkoutResp.body!!["data"] as Map<*, *>)["id"] as String
        val checkoutToken = (checkoutResp.body!!["data"] as Map<*, *>)["accessToken"] as String
        val checkoutHeaders = HttpHeaders().apply { set("X-Checkout-Token", checkoutToken) }

        // 3. Set Address
        val addressRequest = E2EDataFactory.createShippingAddressRequest()
        restTemplate.exchange(
            "/v1/checkouts/$checkoutId/shipping-address",
            HttpMethod.PUT,
            HttpEntity(addressRequest, checkoutHeaders),
            Map::class.java,
        )

        // 4. Get Quotes & Select
        val quotesResp =
            restTemplate.exchange(
                "/v1/checkouts/$checkoutId/shipping-quotes",
                HttpMethod.POST,
                HttpEntity<Any>(checkoutHeaders),
                Map::class.java,
            )
        println("Quotes Response Body: ${objectMapper.writeValueAsString(quotesResp.body)}")

        val quotesBody = quotesResp.body ?: throw AssertionError("Quotes response body is null")
        val checkoutData = quotesBody["data"] as? Map<*, *>
            ?: throw AssertionError(
                "Checkout data in quotes response is null",
            )
        val quotesList = checkoutData["availableShippingQuotes"] as? List<Map<*, *>>
            ?: throw AssertionError(
                "availableShippingQuotes is null",
            )

        if (quotesList.isEmpty()) throw AssertionError("availableShippingQuotes is empty")

        val quoteId = quotesList.first()["id"] as String
        restTemplate.exchange(
            "/v1/checkouts/$checkoutId/shipping-selection",
            HttpMethod.PUT,
            HttpEntity(E2EDataFactory.createSelectShippingQuoteRequest(UUID.fromString(quoteId)), checkoutHeaders),
            Map::class.java,
        )

        // 5. Place Order
        val authHeaders = HttpHeaders().apply { setBearerAuth(customerToken) }
        val placeOrderResp =
            restTemplate.exchange(
                "/v1/orders?checkoutId=$checkoutId",
                HttpMethod.POST,
                HttpEntity(E2EDataFactory.createPlaceOrderRequest(), authHeaders),
                Map::class.java,
            )
        val orderId = (placeOrderResp.body!!["data"] as Map<*, *>)["id"] as String
        val orderNumber = (placeOrderResp.body!!["data"] as Map<*, *>)["orderNumber"] as String
        val totalAmount = ((placeOrderResp.body!!["data"] as Map<*, *>)["totalAmount"] as Number).toLong()

        // 6. Create Payment Session
        val paymentSessionResp =
            restTemplate.exchange(
                "/v1/payments/orders/$orderId",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders),
                Map::class.java,
            )
        assertEquals(HttpStatus.OK, paymentSessionResp.statusCode)

        // 7. Mock Midtrans Webhook (Settlement)
        // Signature: SHA512(order_id + status_code + gross_amount + server_key)
        // We need to match the signature logic in MidtransPaymentProvider
        // In E2E/Test, we should ideally use a fixed server key from application-test.yml
        // For simplicity here, let's assume signature validation passes or we bypass it if possible.
        // Actually, the PaymentService.processMidtransWebhook calls verifyWebhook.

        val webhookPayload =
            mapOf(
                "order_id" to orderNumber,
                "status_code" to "200",
                "gross_amount" to totalAmount.toString(),
                "transaction_status" to "settlement",
                "transaction_id" to "midtrans-tx-123",
            )

        // We can't easily calculate SHA512 here without knowing the exact server-key used in the test context
        // But we can check application-test.yml or just use the same logic.
        // Assuming server-key is from properties
        val signature = sha512(orderNumber + "200" + totalAmount.toString() + gayakiniProperties.midtrans.serverKey)

        val webhookHeaders =
            HttpHeaders().apply {
                set("X-Signature-Key", signature)
            }

        val webhookResp =
            restTemplate.postForEntity(
                "/v1/webhooks/midtrans",
                HttpEntity(webhookPayload, webhookHeaders),
                Map::class.java,
            )
        assertEquals(HttpStatus.OK, webhookResp.statusCode)

        // 8. Verify Ledger & Balance (Admin)
        // Wait a bit for async processing if any, though current implementation is likely sync
        val adminHeaders = HttpHeaders().apply { setBearerAuth(adminToken) }
        val balanceResp =
            restTemplate.exchange(
                "/v1/admin/finance/balance",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders),
                Map::class.java,
            )
        println("Balance Response Body: ${balanceResp.body}")
        val balanceData = balanceResp.body!!["data"] as Map<*, *>
        val balance = (balanceData["availableBalance"] as Number).toLong()
        assertEquals(totalAmount, balance)

        // 9. Withdrawal Request
        val destinationId = destinationRepository.findAll().first().id
        val withdrawalRequest =
            mapOf(
                "amount" to totalAmount,
                "payoutDestinationId" to destinationId,
            )
        val withdrawalResp =
            restTemplate.exchange(
                "/v1/admin/finance/withdrawals",
                HttpMethod.POST,
                HttpEntity(withdrawalRequest, adminHeaders),
                Map::class.java,
            )
        val withdrawalId = (withdrawalResp.body!!["data"] as Map<*, *>)["id"] as String

        // 10. Approve & Process
        restTemplate.exchange(
            "/v1/admin/finance/withdrawals/$withdrawalId/approve",
            HttpMethod.POST,
            HttpEntity(mapOf("notes" to "Approved E2E"), adminHeaders),
            Map::class.java,
        )
        restTemplate.exchange(
            "/v1/admin/finance/withdrawals/$withdrawalId/process",
            HttpMethod.POST,
            HttpEntity<Any>(adminHeaders),
            Map::class.java,
        )

        // 11. Final Balance Check
        val finalBalanceResp =
            restTemplate.exchange(
                "/v1/admin/finance/balance",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders),
                Map::class.java,
            )
        val finalBalance = ((finalBalanceResp.body!!["data"] as Map<*, *>)["availableBalance"] as Number).toLong()
        assertEquals(0L, finalBalance)
    }

    private fun sha512(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-512")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
