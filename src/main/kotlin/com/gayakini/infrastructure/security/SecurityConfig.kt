package com.gayakini.infrastructure.security

import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.infrastructure.security.exception.CustomAccessDeniedHandler
import com.gayakini.infrastructure.security.exception.CustomAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val properties: GayakiniProperties,
    private val jwtService: JwtService,
    private val requestIdFilter: RequestIdFilter,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Whitelist Public Endpoints
                    .requestMatchers("/v1/hello").permitAll()
                    .requestMatchers("/v1/products/**").permitAll()
                    .requestMatchers("/v1/locations/**").permitAll()
                    .requestMatchers("/v1/auth/**").permitAll()
                    .requestMatchers("/v1/webhooks/**").permitAll()
                    // Cart & Checkout (Guest usage permitted, but ownership verified in service)
                    .requestMatchers("/v1/carts/**").permitAll()
                    .requestMatchers("/v1/checkouts/**").permitAll()
                    // Restricted - No broad permitAll()
                    .requestMatchers("/v1/orders/**").authenticated()
                    .requestMatchers("/v1/payments/**").authenticated()
                    // RBAC - Specific roles
                    .requestMatchers("/v1/me/**").hasRole("CUSTOMER")
                    .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers("/v1/finance/**").hasAnyRole("ADMIN", "FINANCE")
                    .requestMatchers("/v1/operations/**").hasAnyRole("ADMIN", "OPERATOR")
                    // Actuator & Docs
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(customAuthenticationEntryPoint)
                it.accessDeniedHandler(customAccessDeniedHandler)
            }
            .addFilterBefore(
                requestIdFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .addFilterBefore(
                JwtAuthenticationFilter(jwtService),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = properties.cors.allowedOrigins
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
