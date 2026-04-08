package com.gayakini.infrastructure.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class AppConfig {
    companion object {
        private const val DEFAULT_TIMEOUT_SECONDS = 10L
    }

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .readTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .build()
    }
}
