package com.gayakini.infrastructure.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.net.http.HttpClient
import java.time.Duration
import java.util.function.Supplier

@Configuration
class RestTemplateConfig {
    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 5L
        private const val READ_TIMEOUT_SECONDS = 10L
    }

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        val httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build()

        val factory = JdkClientHttpRequestFactory(httpClient)
        factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))

        return builder
            .requestFactory(Supplier { factory })
            .build()
    }
}
