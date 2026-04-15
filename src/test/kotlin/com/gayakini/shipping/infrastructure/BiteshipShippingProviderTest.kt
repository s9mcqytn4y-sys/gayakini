package com.gayakini.shipping.infrastructure

import com.gayakini.BaseIntegrationTest
import com.gayakini.BaseWireMockTest
import com.gayakini.shipping.domain.ShippingItem
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Tag("integration")
class BiteshipShippingProviderTest : BaseWireMockTest() {
    @Autowired
    private lateinit var shippingProvider: BiteshipShippingProvider

    @Test
    @Disabled("Only run manually to verify real Biteship connectivity")
    fun `test get rates with real biteship sandbox`() {
        val rates =
            shippingProvider.getRates(
                origin = "IDNP001",
                destination = "IDNP002",
                items =
                    listOf(
                        ShippingItem(name = "T-Shirt", weightGrams = 200, quantity = 1, valueIdr = 100000),
                    ),
            )

        println("Rates found: ${rates.size}")
        rates.forEach { println("${it.courierName} - ${it.serviceName}: ${it.price}") }
        assert(rates.isNotEmpty())
    }
}
