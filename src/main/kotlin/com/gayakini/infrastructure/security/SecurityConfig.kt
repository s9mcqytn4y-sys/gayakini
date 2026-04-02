package com.gayakini.infrastructure.security

import com.gayakini.infrastructure.config.GayakiniProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val properties: GayakiniProperties,
    private val jwtService: JwtService,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public Endpoints
                    .requestMatchers("/api/v1/hello", "/v1/hello").permitAll()
                    .requestMatchers("/api/v1/products/**", "/v1/products/**").permitAll()
                    .requestMatchers("/api/v1/locations/**", "/v1/locations/**").permitAll()
                    .requestMatchers("/api/v1/auth/**", "/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/webhooks/**", "/v1/webhooks/**").permitAll()
                    // Cart & Checkout
                    .requestMatchers("/api/v1/carts/**", "/v1/carts/**").permitAll()
                    .requestMatchers("/api/v1/checkouts/**", "/v1/checkouts/**").permitAll()
                    .requestMatchers("/api/v1/orders/{orderId}", "/v1/orders/{orderId}").permitAll()
                    // Customer Profile & Personal Orders
                    .requestMatchers("/api/v1/me/**", "/v1/me/**").authenticated()
                    // Admin
                    .requestMatchers("/api/v1/admin/**", "/v1/admin/**").hasRole("ADMIN")
                    // Documentation & Actuator
                    .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
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
