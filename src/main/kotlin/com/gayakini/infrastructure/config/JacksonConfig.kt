package com.gayakini.infrastructure.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Standardizes JSON serialization/deserialization for the Gayakini application.
 *
 * Configures the primary ObjectMapper with:
 * 1. Kotlin module support for null-safety and data class reflections.
 * 2. JavaTimeModule for ISO-8601 date handling.
 * 3. Consistent null-inclusion policy (NON_NULL).
 * 4. Reliable default behavior for API payloads and internal serialization.
 */
@Configuration
class JacksonConfig {
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            // Serialize dates as ISO-8601 strings (e.g., "2024-03-21T10:00:00Z")
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Prevent failure on unknown properties to allow for contract evolutions
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // Global policy for excluding null fields in API responses
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
    }
}
