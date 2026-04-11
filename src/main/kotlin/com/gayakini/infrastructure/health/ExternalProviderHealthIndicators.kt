package com.gayakini.infrastructure.health

import com.gayakini.infrastructure.config.GayakiniProperties
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class MidtransHealthIndicator(
    private val properties: GayakiniProperties,
    private val restTemplate: RestTemplate,
) : HealthIndicator {
    override fun health(): Health {
        return try {
            // Midtrans status URL is publicly accessible for basic ping,
            // but we'll use a simple HEAD or GET to the base API
            val start = System.currentTimeMillis()
            restTemplate.getForEntity(properties.midtrans.apiUrl + "/ping", String::class.java)
            val duration = System.currentTimeMillis() - start

            Health.up()
                .withDetail("provider", "Midtrans")
                .withDetail("responseTimeMs", duration)
                .build()
        } catch (e: org.springframework.web.client.RestClientException) {
            Health.down()
                .withDetail("provider", "Midtrans")
                .withDetail("error", e.message ?: "Network error")
                .build()
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: RuntimeException,
        ) {
            Health.down()
                .withDetail("provider", "Midtrans")
                .withDetail("error", "Unexpected runtime error: ${e.message}")
                .build()
        }
    }
}

@Component
class BiteshipHealthIndicator(
    private val properties: GayakiniProperties,
    private val restTemplate: RestTemplate,
) : HealthIndicator {
    override fun health(): Health {
        return try {
            val start = System.currentTimeMillis()
            // Biteship has a maps/areas endpoint that is relatively lightweight
            restTemplate.getForEntity(properties.biteship.apiUrl + "/maps/areas?countries=id", String::class.java)
            val duration = System.currentTimeMillis() - start

            Health.up()
                .withDetail("provider", "Biteship")
                .withDetail("responseTimeMs", duration)
                .build()
        } catch (e: org.springframework.web.client.RestClientException) {
            Health.down()
                .withDetail("provider", "Biteship")
                .withDetail("error", e.message ?: "Network error")
                .build()
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: RuntimeException,
        ) {
            Health.down()
                .withDetail("provider", "Biteship")
                .withDetail("error", "Unexpected runtime error: ${e.message}")
                .build()
        }
    }
}
