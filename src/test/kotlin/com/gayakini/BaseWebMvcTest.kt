package com.gayakini

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.infrastructure.security.JwtAuthenticationFilter
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
import org.springframework.test.web.servlet.ResultActionsDsl
import java.util.UUID

@ActiveProfiles("test")
@EnableConfigurationProperties(GayakiniProperties::class)
abstract class BaseWebMvcTest {
    fun ResultActionsDsl.andExpectApiResponse(
        expectedStatus: Int = 200,
        success: Boolean = true,
        message: String? = null,
    ): ResultActionsDsl {
        return andExpect {
            status { isEqualTo(expectedStatus) }
            jsonPath("$.success") { value(success) }
            if (message != null) {
                jsonPath("$.message") { value(message) }
            }
        }
    }

    @Deprecated("Use andExpectApiResponse instead", ReplaceWith("andExpectApiResponse(expectedStatus, success, message)"))
    fun ResultActionsDsl.andExpectStandardResponse(
        expectedStatus: Int = 200,
        success: Boolean = true,
        message: String? = null,
    ): ResultActionsDsl = andExpectApiResponse(expectedStatus, success, message)

    @TestConfiguration
    class SecurityTestConfig {
        @Bean
        fun jwtService(): JwtService =
            mockk(relaxed = true) {
                val adminId = UUID.fromString("00000000-0000-0000-0000-000000000001")
                val customerId = UUID.fromString("00000000-0000-0000-0000-000000000002")

                every { parseToken(any()) } returns null
                every { parseToken("valid-admin-token") } returns
                    UserPrincipal(
                        id = adminId,
                        email = "admin@gayakini.com",
                        role = "ADMIN",
                    )
                every { parseToken("valid-customer-token") } returns
                    UserPrincipal(
                        id = customerId,
                        email = "customer@gayakini.com",
                        role = "CUSTOMER",
                    )
                every { parseToken("valid-operator-token") } returns
                    UserPrincipal(
                        id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        email = "operator@gayakini.com",
                        role = "OPERATOR",
                    )
            }

        @Bean
        fun jwtAuthenticationFilter(jwtService: JwtService): JwtAuthenticationFilter =
            JwtAuthenticationFilter(jwtService)

        @Bean
        fun requestIdFilter(): RequestIdFilter = RequestIdFilter()

        @Bean
        fun customAuthenticationEntryPoint(objectMapper: ObjectMapper): CustomAuthenticationEntryPoint =
            CustomAuthenticationEntryPoint(objectMapper)

        @Bean
        fun customAccessDeniedHandler(objectMapper: ObjectMapper): CustomAccessDeniedHandler =
            CustomAccessDeniedHandler(objectMapper)

        @Bean
        fun securityFilterChain(
            http: org.springframework.security.config.annotation.web.builders.HttpSecurity,
            jwtAuthenticationFilter: JwtAuthenticationFilter,
            requestIdFilter: RequestIdFilter,
            customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
            customAccessDeniedHandler: CustomAccessDeniedHandler,
        ): org.springframework.security.web.SecurityFilterChain {
            http
                .csrf { it.disable() }
                .formLogin { it.disable() }
                .httpBasic { it.disable() }
                .sessionManagement {
                    it.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
                }
                .authorizeHttpRequests { auth ->
                    auth
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/v1/me/**").hasRole("CUSTOMER")
                        .requestMatchers("/v1/operations/**").hasAnyRole("ADMIN", "OPERATOR")
                        .anyRequest().permitAll()
                }
                .exceptionHandling {
                    it.authenticationEntryPoint(customAuthenticationEntryPoint)
                    it.accessDeniedHandler(customAccessDeniedHandler)
                }
                .csrf { it.disable() }
                .addFilterBefore(
                    requestIdFilter,
                    org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter::class.java,
                )
                .addFilterBefore(
                    jwtAuthenticationFilter,
                    org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter::class.java,
                )

            return http.build()
        }
    }
}
