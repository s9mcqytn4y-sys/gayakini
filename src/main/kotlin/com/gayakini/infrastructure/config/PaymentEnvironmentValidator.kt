package com.gayakini.infrastructure.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class PaymentEnvironmentValidator(
    private val properties: GayakiniProperties,
    private val environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(PaymentEnvironmentValidator::class.java)

    @PostConstruct
    fun validate() {
        val activeProfiles = environment.activeProfiles.toList()
        val isSandbox = activeProfiles.contains("sandbox")
        val isLocal = activeProfiles.contains("local")

        logger.info("Validating payment environment. Active profiles: {}", activeProfiles)

        if (isSandbox || isLocal) {
            validateSandboxEnvironment()
        }

        // Fail-fast if production keys are found in sandbox/local
        if ((isSandbox || isLocal) && properties.midtrans.isProduction) {
            val errorMessage =
                "SECURITY CRITICAL: Midtrans production mode is ENABLED " +
                    "while running in sandbox/local profile. Shutdown immediately."
            logger.error(errorMessage)
            error(errorMessage)
        }

        // Fail-fast if production URLs are found in sandbox/local
        if (isSandbox || isLocal) {
            val apiUrl = properties.midtrans.apiUrl.lowercase()
            val snapUrl = properties.midtrans.snapUrl.lowercase()

            if (!apiUrl.contains("sandbox") || !snapUrl.contains("sandbox")) {
                val errorMessage =
                    "SECURITY CRITICAL: Midtrans Production URLs detected in sandbox/local mode. " +
                        "API URL: $apiUrl, SNAP URL: $snapUrl. Shutdown immediately."
                logger.error(errorMessage)
                error(errorMessage)
            }
        }
    }

    private fun validateSandboxEnvironment() {
        val midtrans = properties.midtrans
        val missing = mutableListOf<String>()

        if (midtrans.serverKey.isBlank() || midtrans.serverKey == "dummy-server-key") {
            missing.add("MIDTRANS_SERVER_KEY")
        }
        if (midtrans.clientKey.isBlank() || midtrans.clientKey == "dummy-client-key") {
            missing.add("MIDTRANS_CLIENT_KEY")
        }

        if (missing.isNotEmpty()) {
            val missingKeys = missing.joinToString(", ")
            val errorMessage =
                "MISSING SANDBOX CONFIGURATION: The following environment variables " +
                    "are missing or use dummy values: $missingKeys. " +
                    "Please check your .env file and ensure the 'sandbox' profile is correctly configured."
            logger.error(errorMessage)
        }
    }
}
