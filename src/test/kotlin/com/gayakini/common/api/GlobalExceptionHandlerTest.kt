package com.gayakini.common.api

import com.gayakini.BaseWebMvcTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(TestController::class)
@Import(BaseWebMvcTest.SecurityTestConfig::class, GlobalExceptionHandler::class)
class GlobalExceptionHandlerTest : BaseWebMvcTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @com.ninjasquad.springmockk.MockkBean
    private lateinit var properties: com.gayakini.infrastructure.config.GayakiniProperties

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        io.mockk.every { properties.isProduction } returns false
    }

    @Test
    @WithMockUser
    fun `should handle NoSuchElementException with 404`() {
        mockMvc.get("/test-error/not-found")
            .andDo { print() }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.success") { value(false) }
            }
    }

    @Test
    @WithMockUser
    fun `should handle ForbiddenException with 403`() {
        mockMvc.get("/test-error/forbidden")
            .andDo { print() }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.success") { value(false) }
            }
    }

    @Test
    @WithMockUser
    fun `should handle UnauthorizedException with 401`() {
        mockMvc.get("/test-error/unauthorized")
            .andDo { print() }
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.success") { value(false) }
            }
    }

    @Test
    @WithMockUser
    fun `should handle IllegalArgumentException with 400`() {
        mockMvc.get("/test-error/illegal-argument")
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.success") { value(false) }
            }
    }

    @Test
    @WithMockUser
    fun `should handle IllegalStateException with 409`() {
        mockMvc.get("/test-error/illegal-state")
            .andDo { print() }
            .andExpect {
                status { isConflict() }
                jsonPath("$.success") { value(false) }
            }
    }

    @Test
    @WithMockUser
    fun `should handle generic Exception with 500`() {
        mockMvc.get("/test-error/internal-error")
            .andDo { print() }
            .andExpect {
                status { isInternalServerError() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.message") { value("Boom") }
            }
    }

    @Test
    @WithMockUser
    fun `should mask generic Exception message in production`() {
        io.mockk.every { properties.isProduction } returns true

        mockMvc.get("/test-error/internal-error")
            .andDo { print() }
            .andExpect {
                status { isInternalServerError() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.message") { value("Terjadi kesalahan pada server") }
            }
    }
}

@RestController
@RequestMapping("/test-error")
@Suppress("TooGenericExceptionThrown", "UseCheckOrError")
class TestController {
    @GetMapping("/not-found")
    fun notFound(): Nothing = throw NoSuchElementException("Item not found")

    @GetMapping("/forbidden")
    fun forbidden(): Nothing = throw ForbiddenException("No access")

    @GetMapping("/unauthorized")
    fun unauthorized(): Nothing = throw UnauthorizedException("Please login")

    @GetMapping("/illegal-argument")
    fun illegalArgument(): Nothing = throw IllegalArgumentException("Bad input")

    @GetMapping("/illegal-state")
    fun illegalState(): Nothing = throw IllegalStateException("Conflict state")

    @GetMapping("/internal-error")
    fun internalError(): Nothing = throw RuntimeException("Boom")
}
