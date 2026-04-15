package com.gayakini.shipping.infrastructure

import com.gayakini.BaseWireMockTest
import com.gayakini.shipping.domain.ShippingItem
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@WireMockTest(httpPort = 8089)
class BiteshipShippingProviderWireMockTest : BaseWireMockTest() {
    @Autowired
    private lateinit var shippingProvider: BiteshipShippingProvider

    @Test
    fun `should get rates successfully from biteship via wiremock`() {
        // Given
        stubFor(
            post(urlEqualTo("/v1/rates/couriers"))
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
