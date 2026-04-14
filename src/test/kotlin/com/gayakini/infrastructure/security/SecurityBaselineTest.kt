package com.gayakini.infrastructure.security

import com.gayakini.BaseWebMvcTest
import com.gayakini.api.HelloController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(HelloController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class)
class SecurityBaselineTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `public hello endpoint is accessible`() {
        mockMvc.get("/v1/hello")
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `protected me endpoint returns 401 when no token provided`() {
        mockMvc.get("/v1/me")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.message") { value("Sesi tidak valid atau telah berakhir. Silakan login kembali.") }
            }
    }

    @Test
    fun `admin endpoint returns 401 when no token provided`() {
        mockMvc.get("/v1/admin/products")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `protected endpoint returns 401 with invalid token`() {
        mockMvc.get("/v1/me") {
            header("Authorization", "Bearer invalid-token")
        }
            .andExpect {
                status { isUnauthorized() }
            }
    }
}
