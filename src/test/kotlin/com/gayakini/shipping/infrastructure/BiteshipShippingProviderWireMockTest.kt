package com.gayakini.shipping.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.infrastructure.config.GayakiniProperties
import com.gayakini.shipping.domain.ShippingItem
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@SpringBootTest
@ActiveProfiles("test")
@EnableConfigurationProperties(GayakiniProperties::class)
@WireMockTest(httpPort = 8089)
@Import(BiteshipShippingProviderWireMockTest.TestConfig::class)
class BiteshipShippingProviderWireMockTest {
    @Autowired
    private lateinit var shippingProvider: BiteshipShippingProvider

    @Configuration
    class TestConfig {
        @Bean
        fun restTemplate(): RestTemplate = RestTemplate()

        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()

        @Bean
        fun biteshipShippingProvider(
            properties: GayakiniProperties,
            restTemplate: RestTemplate,
        ): BiteshipShippingProvider {
            return BiteshipShippingProvider(properties, restTemplate)
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("gayakini.biteship.api-url") { "http://localhost:8089" }
        }
    }

    @Test
    fun `should get rates successfully from biteship via wiremock`() {
        // Given
        stubFor(
            post(urlEqualTo("/rates/couriers"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "success": true,
                              "pricing": [
                                {
                                  "company": "jne",
                                  "type": "reg",
                                  "courier_name": "JNE",
                                  "courier_service_name": "Regular",
                                  "description": "Regular Service",
                                  "duration": "2-3",
                                  "price": 10000
                                }
                              ]
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val items =
            listOf(
                ShippingItem(name = "T-Shirt", weightGrams = 200, quantity = 1, valueIdr = 100000),
            )

        // When
        val rates = shippingProvider.getRates("origin", "dest", items)

        // Then
        assertEquals(1, rates.size)
        assertEquals("jne", rates[0].courierCode)
        assertEquals(10000L, rates[0].price)
    }
}
