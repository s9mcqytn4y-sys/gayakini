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
                    id = UUID.randomUUID(),
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
        customerRepository.findByEmail(adminEmail).ifPresent {
            jdbcTemplate.execute("DELETE FROM commerce.refresh_tokens WHERE customer_id = '${it.id}'")
            customerRepository.delete(it)
        }
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
        customerRepository.findByEmail(customerEmail).ifPresent {
            jdbcTemplate.execute("DELETE FROM commerce.refresh_tokens WHERE customer_id = '${it.id}'")
            customerRepository.delete(it)
        }
        val registerRequest = E2EDataFactory.createRegisterRequest(email = customerEmail)
        val registerResp = restTemplate.postForEntity("/v1/auth/register", registerRequest, Map::class.java)
        if (registerResp.statusCode != HttpStatus.CREATED && registerResp.statusCode != HttpStatus.OK) {
            throw AssertionError("Registration failed for $customerEmail: ${registerResp.statusCode} - ${registerResp.body}")
        }

        // Wait briefly for DB consistency if needed, though usually not for H2/Postgres in same thread
        customerToken = loginAndGetToken(customerEmail, "Password123!")
    }

    private fun loginAndGetToken(
        email: String,
        pass: String,
    ): String {
        val loginRequest = mapOf("email" to email, "password" to pass)
        // Add a retry mechanism for login to handle potential race conditions in async environments
        var lastResponse: org.springframework.http.ResponseEntity<Map<*, *>>? = null
        for (i in 1..3) {
            val response = restTemplate.postForEntity("/v1/auth/login", loginRequest, Map::class.java)
            if (response.statusCode == HttpStatus.OK) {
                val body = response.body ?: throw AssertionError("Login response body is null")
                val data = body["data"] as? Map<*, *> ?: throw AssertionError("Login data is null: $body")
                val tokens = data["tokens"] as? Map<*, *> ?: throw AssertionError("tokens is null or not a map: $data")
                return tokens["accessToken"] as? String ?: throw AssertionError("accessToken is missing or not a string")
            }
            lastResponse = response
            Thread.sleep(500)
        }

        throw AssertionError("Login failed for $email after retries: ${lastResponse?.statusCode} - ${lastResponse?.body}")
    }

    @Test
    fun `full flow - checkout to payment to ledger to withdrawal`() {
        val cartData = createAndPopulateCart()
        val checkoutData = performCheckout(cartData)
        setAddressAndSelectShipping(checkoutData)

        val orderData = placeOrder(checkoutData)
        val orderNumber = orderData["orderNumber"] as String
        val totalAmount = (orderData["totalAmount"] as Number).toLong()

        initiatePaymentSession(orderData["id"] as String)
        mockMidtransWebhook(orderNumber, totalAmount)

        verifyAdminBalance(totalAmount)
        val withdrawalId = requestWithdrawal(totalAmount)
        processWithdrawalWorkflow(withdrawalId)
        verifyAdminBalance(0L)
    }

    private fun createAndPopulateCart(): Map<String, String> {
        val cartResponse =
            restTemplate.exchange(
                "/v1/carts",
                HttpMethod.POST,
                HttpEntity<Any>(HttpHeaders().apply { set("X-Cart-Token", "") }),
                Map::class.java,
            )
        val data = cartResponse.body!!["data"] as Map<*, *>
        val cartId = data["id"] as String
        val cartToken = data["accessToken"] as String
        val cartHeaders = HttpHeaders().apply { set("X-Cart-Token", cartToken) }

        val addRequest = E2EDataFactory.createAddCartItemRequest(testVariant.id, 1)
        restTemplate.exchange(
            "/v1/carts/$cartId/items",
            HttpMethod.POST,
            HttpEntity(addRequest, cartHeaders),
            Map::class.java,
        )
        return mapOf("id" to cartId, "token" to cartToken)
    }

    private fun performCheckout(cartData: Map<String, String>): Map<String, String> {
        val cartHeaders = HttpHeaders().apply { set("X-Cart-Token", cartData["token"]!!) }
        val checkoutRequest = E2EDataFactory.createCheckoutRequest(UUID.fromString(cartData["id"]!!))
        val checkoutResp =
            restTemplate.exchange(
                "/v1/checkouts",
                HttpMethod.POST,
                HttpEntity(checkoutRequest, cartHeaders),
                Map::class.java,
            )
        val data = checkoutResp.body!!["data"] as Map<*, *>
        return mapOf(
            "id" to data["id"] as String,
            "token" to data["accessToken"] as String,
        )
    }

    private fun setAddressAndSelectShipping(checkoutData: Map<String, String>) {
        val checkoutId = checkoutData["id"]!!
        val checkoutHeaders = HttpHeaders().apply { set("X-Checkout-Token", checkoutData["token"]!!) }

        val addressRequest = E2EDataFactory.createShippingAddressRequest()
        val addressResp = restTemplate.exchange(
            "/v1/checkouts/$checkoutId/shipping-address",
            HttpMethod.PUT,
            HttpEntity(addressRequest, checkoutHeaders),
            String::class.java,
        )
        if (addressResp.statusCode != HttpStatus.OK) {
            throw AssertionError("Failed to set shipping address: ${addressResp.statusCode} - ${addressResp.body}")
        }

        val quotesResp =
            restTemplate.exchange(
                "/v1/checkouts/$checkoutId/shipping-quotes",
                HttpMethod.POST,
                HttpEntity<Any>(HttpHeaders().apply {
                    set("X-Checkout-Token", checkoutData["token"]!!)
                    set("Idempotency-Key", UUID.randomUUID().toString())
                }),
                Map::class.java,
            )

        if (quotesResp.statusCode != HttpStatus.OK) {
            throw AssertionError("Failed to get shipping quotes: ${quotesResp.body}")
        }

        val quotesBody = quotesResp.body ?: throw AssertionError("Quotes response body is null")
        val quotesData = quotesBody["data"] as? Map<*, *> ?: throw AssertionError("Quotes data is null: $quotesBody")
        val quotesList = quotesData["availableShippingQuotes"] as? List<Map<*, *>>
            ?: throw AssertionError("availableShippingQuotes is null or not a list: $quotesData")

        if (quotesList.isEmpty()) {
            throw AssertionError("No shipping quotes available. Check MerchantShippingOrigin and areaId: ${addressRequest.guestAddress?.areaId}")
        }

        val quoteId = quotesList.first()["quoteId"] as? String ?: throw AssertionError("quoteId is missing in first quote")

        val selectionResp = restTemplate.exchange(
            "/v1/checkouts/$checkoutId/shipping-selection",
            HttpMethod.PUT,
            HttpEntity(E2EDataFactory.createSelectShippingQuoteRequest(UUID.fromString(quoteId)), checkoutHeaders),
            Map::class.java,
        )
        if (selectionResp.statusCode != HttpStatus.OK) {
            throw AssertionError("Failed to select shipping: ${selectionResp.body}")
        }
    }

    private fun placeOrder(checkoutData: Map<String, String>): Map<*, *> {
        val checkoutHeaders = HttpHeaders().apply {
            set("X-Checkout-Token", checkoutData["token"]!!)
            set("Idempotency-Key", UUID.randomUUID().toString())
        }
        val placeOrderResp =
            restTemplate.exchange(
                "/v1/checkouts/${checkoutData["id"]}/orders",
                HttpMethod.POST,
                HttpEntity(E2EDataFactory.createPlaceOrderRequest(), checkoutHeaders),
                Map::class.java,
            )
        return placeOrderResp.body!!["data"] as Map<*, *>
    }

    private fun initiatePaymentSession(orderId: String) {
        val orderHeaders = HttpHeaders().apply {
            set("Idempotency-Key", UUID.randomUUID().toString())
        }
        val paymentSessionResp =
            restTemplate.exchange(
                "/v1/payments/orders/$orderId",
                HttpMethod.POST,
                HttpEntity<Any>(orderHeaders),
                Map::class.java,
            )
        assertEquals(HttpStatus.CREATED, paymentSessionResp.statusCode)
    }

    private fun mockMidtransWebhook(
        orderNumber: String,
        totalAmount: Long,
    ) {
        val webhookPayload =
            mapOf(
                "order_id" to orderNumber,
                "status_code" to "200",
                "gross_amount" to totalAmount.toString(),
                "transaction_status" to "settlement",
                "transaction_id" to "midtrans-tx-123",
            )

        val signature = sha512(orderNumber + "200" + totalAmount.toString() + gayakiniProperties.midtrans.serverKey)
        val webhookHeaders = HttpHeaders().apply { set("X-Signature-Key", signature) }

        val webhookResp =
            restTemplate.postForEntity(
                "/v1/webhooks/midtrans",
                HttpEntity(webhookPayload, webhookHeaders),
                Map::class.java,
            )
        assertEquals(HttpStatus.OK, webhookResp.statusCode)
    }

    private fun verifyAdminBalance(expectedBalance: Long) {
        val adminHeaders = HttpHeaders().apply { setBearerAuth(adminToken) }
        val balanceResp =
            restTemplate.exchange(
                "/v1/admin/finance/balance",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders),
                Map::class.java,
            )
        if (balanceResp.statusCode != HttpStatus.OK) {
            throw AssertionError("Failed to get balance: ${balanceResp.statusCode} - ${balanceResp.body}")
        }
        val balanceData = balanceResp.body!!["data"] as Map<*, *>
        val balance = (balanceData["availableBalance"] as Number).toLong()
        assertEquals(expectedBalance, balance)
    }

    private fun requestWithdrawal(amount: Long): String {
        val adminHeaders = HttpHeaders().apply { setBearerAuth(adminToken) }
        val destinations = destinationRepository.findAll()
        if (destinations.isEmpty()) {
            throw AssertionError("No payout destinations found in database")
        }
        val destinationId = destinations.first().id
        val request = mapOf("amount" to amount, "payoutDestinationId" to destinationId)
        val resp =
            restTemplate.exchange(
                "/v1/admin/finance/withdrawals",
                HttpMethod.POST,
                HttpEntity(request, adminHeaders),
                Map::class.java,
            )
        if (resp.statusCode != HttpStatus.OK) {
            throw AssertionError("Withdrawal request failed: ${resp.statusCode} - ${resp.body}")
        }
        return (resp.body!!["data"] as Map<*, *>)["id"] as String
    }

    private fun processWithdrawalWorkflow(withdrawalId: String) {
        val adminHeaders = HttpHeaders().apply { setBearerAuth(adminToken) }
        val approveResp = restTemplate.exchange(
            "/v1/admin/finance/withdrawals/$withdrawalId/approve",
            HttpMethod.POST,
            HttpEntity(mapOf("notes" to "Approved E2E"), adminHeaders),
            Map::class.java,
        )
        if (approveResp.statusCode != HttpStatus.OK) {
            throw AssertionError("Withdrawal approval failed: ${approveResp.statusCode} - ${approveResp.body}")
        }

        val processResp = restTemplate.exchange(
            "/v1/admin/finance/withdrawals/$withdrawalId/process",
            HttpMethod.POST,
            HttpEntity<Any>(adminHeaders),
            Map::class.java,
        )
        if (processResp.statusCode != HttpStatus.OK) {
            throw AssertionError("Withdrawal processing failed: ${processResp.statusCode} - ${processResp.body}")
        }
    }

    private fun sha512(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-512")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
