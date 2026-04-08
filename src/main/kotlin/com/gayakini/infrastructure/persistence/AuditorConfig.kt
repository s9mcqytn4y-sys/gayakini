package com.gayakini.infrastructure.persistence

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional
import java.util.UUID

@Configuration
@EnableJpaAuditing
class AuditorConfig {
    @Bean
    fun auditorProvider(): AuditorAware<UUID> {
        return AuditorAware {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication == null || !authentication.isAuthenticated) {
                Optional.empty()
            } else {
                // Assuming the principal's name is the UUID of the user
                try {
                    Optional.of(UUID.fromString(authentication.name))
                } catch (e: IllegalArgumentException) {
                    Optional.empty()
                }
            }
        }
    }
}
