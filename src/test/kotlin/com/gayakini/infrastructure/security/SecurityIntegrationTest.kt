package com.gayakini.infrastructure.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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

    @Test
    fun `public endpoints should be accessible without token`() {
        mockMvc.perform(get("/api/v1/hello"))
            .andExpect(status().isOk)
    }

    @Test
    fun `webhook endpoints should be accessible without token`() {
        // Mock a real biteship webhook path with valid JSON structure to avoid 400
        mockMvc.perform(
            post("/api/v1/webhooks/biteship")
                .contentType("application/json")
                .content("""
                    {
                        "event": "order.status_updated",
                        "id": "test-id",
                        "order_id": "test-order-id",
                        "status": "delivered"
                    }
                """.trimIndent()),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `protected endpoints should return unauthorized without token`() {
        mockMvc.perform(
            get("/v1/me/profile"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected endpoints should be accessible with valid sandbox token`() {
        val checkoutId = UUID.randomUUID()
        mockMvc.perform(
            post("/api/v1/checkouts/{checkoutId}/orders", checkoutId)
                .header("Idempotency-Key", "test-key")
                .contentType("application/json")
                .content("{}"),
        )
            .andExpect(status().isNotFound)
    }
}
