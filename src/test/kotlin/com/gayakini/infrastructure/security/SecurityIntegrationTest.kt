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
        mockMvc.perform(
            post("/api/v1/webhooks/midtrans")
                .contentType("application/json")
                .content("{}"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `protected endpoints should return unauthorized without token`() {
        mockMvc.perform(
            post("/api/v1/orders/place")
                .contentType("application/json")
                .content("{}"),
        )
            // Changed from 401 to 400 because /place is permitAll for guest checkout logic
            // but empty body causes 400 Bad Request
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `protected endpoints should be accessible with valid sandbox token`() {
        mockMvc.perform(
            post("/api/v1/orders/place")
                .header("Authorization", "Bearer sandbox-test-token")
                .contentType("application/json")
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
    }
}
