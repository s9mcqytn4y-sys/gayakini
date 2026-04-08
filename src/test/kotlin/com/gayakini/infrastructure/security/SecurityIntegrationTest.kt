package com.gayakini.infrastructure.security

import com.gayakini.customer.domain.Customer
import com.gayakini.customer.domain.CustomerRepository
import com.gayakini.customer.domain.CustomerRole
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `public endpoints should be accessible without token`() {
        mockMvc.perform(get("/v1/hello"))
            .andExpect(status().isOk)
    }

    @Test
    fun `webhook endpoints should be accessible without token`() {
        // Mock a real biteship webhook path with valid JSON structure to avoid 400
        mockMvc.perform(
            post("/v1/webhooks/biteship")
                .contentType("application/json")
                .content(
                    """
                    {
                        "event": "order.status_updated",
                        "id": "test-id",
                        "order_id": "test-order-id",
                        "status": "delivered"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `protected endpoints should return unauthorized without token`() {
        mockMvc.perform(
            get("/v1/me"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `admin endpoints should return unauthorized without token`() {
        mockMvc.perform(
            get("/v1/admin/orders"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `non public actuator endpoints should return unauthorized without token`() {
        mockMvc.perform(
            get("/actuator/prometheus"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `guest checkout endpoint should not be blocked by authentication layer`() {
        val checkoutId = UUID.randomUUID()
        mockMvc.perform(
            post("/v1/checkouts/{checkoutId}/orders", checkoutId)
                .header("Idempotency-Key", "test-key")
                .contentType("application/json")
                .content("{}"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `guest payment endpoint should not be blocked by authentication layer`() {
        mockMvc.perform(
            post("/v1/orders/{orderId}/payments", UUID.randomUUID())
                .header("Idempotency-Key", "payment-key")
                .contentType("application/json")
                .content("{}"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `guest cancellation endpoint should not be blocked by authentication layer`() {
        mockMvc.perform(
            post("/v1/orders/{orderId}/cancellations", UUID.randomUUID())
                .header("Idempotency-Key", "cancel-key")
                .contentType("application/json")
                .content("{}"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `me endpoint should be accessible with authenticated principal`() {
        val customer =
            customerRepository.save(
                Customer(
                    email = "security-test@example.com",
                    passwordHash = passwordEncoder.encode("Password123!"),
                    fullName = "Security Test",
                    phone = "08123456789",
                ),
            )
        val principal =
            UserPrincipal(
                id = customer.id,
                email = customer.email,
                role = customer.role.name,
            )
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.toAuthorities())

        mockMvc.perform(
            get("/v1/me").with(authentication(auth)),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `admin shipment endpoint should be mapped for admin principal`() {
        val admin =
            customerRepository.save(
                Customer(
                    email = "admin-security-test@example.com",
                    passwordHash = passwordEncoder.encode("Password123!"),
                    fullName = "Admin Security Test",
                    phone = "08123456780",
                    role = CustomerRole.ADMIN,
                ),
            )
        val principal =
            UserPrincipal(
                id = admin.id,
                email = admin.email,
                role = admin.role.name,
            )
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.toAuthorities())

        mockMvc.perform(
            post("/v1/admin/orders/{orderId}/shipments", UUID.randomUUID())
                .with(authentication(auth))
                .header("Idempotency-Key", "shipment-route-check")
                .contentType("application/json")
                .content("{}"),
        )
            .andExpect(status().isNotFound)
    }
}
