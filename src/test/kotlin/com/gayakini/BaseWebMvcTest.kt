package com.gayakini

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.infrastructure.security.JwtService
import com.gayakini.infrastructure.security.RequestIdFilter
import com.gayakini.infrastructure.security.UserPrincipal
import com.gayakini.infrastructure.security.exception.CustomAccessDeniedHandler
import com.gayakini.infrastructure.security.exception.CustomAuthenticationEntryPoint
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@EnableConfigurationProperties(GayakiniProperties::class)
abstract class BaseWebMvcTest {

    @TestConfiguration
    class SecurityTestConfig {
        @Bean
        fun jwtService(): JwtService = mockk(relaxed = true) {
            every { parseToken(any()) } returns null
            every { parseToken("invalid-token") } returns null
        }

        @Bean
        fun requestIdFilter(): RequestIdFilter = RequestIdFilter()

        @Bean
        fun customAuthenticationEntryPoint(objectMapper: ObjectMapper): CustomAuthenticationEntryPoint =
            CustomAuthenticationEntryPoint(objectMapper)

        @Bean
        fun customAccessDeniedHandler(objectMapper: ObjectMapper): CustomAccessDeniedHandler =
            CustomAccessDeniedHandler(objectMapper)
    }
}
