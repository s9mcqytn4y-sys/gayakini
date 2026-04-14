package com.gayakini.api

import com.gayakini.BaseWebMvcTest
import com.gayakini.infrastructure.security.SecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(HelloController::class)
@Import(SecurityConfig::class, BaseWebMvcTest.SecurityTestConfig::class)
class HelloControllerTest : BaseWebMvcTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @WithMockUser
    fun `should return hello message`() {
        mockMvc.get("/v1/hello")
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { value("Halo, layanan gayakini API sudah aktif.") }
            }
    }
}
