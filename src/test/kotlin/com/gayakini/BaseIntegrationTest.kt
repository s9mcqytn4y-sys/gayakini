package com.gayakini

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {

    @BeforeEach
    fun resetWireMock() {
        WireMock.reset()
    }

    companion object {
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }

        @JvmStatic
        @DynamicPropertySource
        fun registerWireMockProperties(registry: DynamicPropertyRegistry) {
            // We use a fixed port for WireMock in tests for simplicity,
            // or we could start a WireMockContainer.
            // For now, let's assume standard WireMock usage on port 8089 as seen in existing tests.
            registry.add("gayakini.biteship.api-url") { "http://localhost:8089" }
            registry.add("gayakini.midtrans.api-url") { "http://localhost:8089/v2" }
            registry.add("gayakini.midtrans.snap-url") { "http://localhost:8089/snap/v1/transactions" }
        }
    }
}
