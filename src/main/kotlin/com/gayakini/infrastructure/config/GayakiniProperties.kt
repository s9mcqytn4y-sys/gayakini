package com.gayakini.infrastructure.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "gayakini")
class GayakiniProperties {
    @field:Valid
    @field:NotNull
    var jwt: JwtProperties = JwtProperties()

    @field:Valid
    @field:NotNull
    var cors: CorsProperties = CorsProperties()

    @field:Valid
    @field:NotNull
    var midtrans: MidtransProperties = MidtransProperties()

    @field:Valid
    @field:NotNull
    var biteship: BiteshipProperties = BiteshipProperties()

    class JwtProperties {
        @field:NotBlank
        var secret: String = "default-secret-key-that-must-be-changed-in-production-at-least-thirty-two-chars"
        var accessTokenExpirationMinutes: Long = 60
        var refreshTokenExpirationDays: Long = 30
    }

    class CorsProperties {
        @field:NotEmpty
        var allowedOrigins: List<String> = listOf("http://localhost:3000")
    }

    class MidtransProperties {
        var isProduction: Boolean = false
        @field:NotBlank
        var serverKey: String = "dummy-server-key"
        @field:NotBlank
        var clientKey: String = "dummy-client-key"
        @field:NotBlank
        var snapUrl: String = "https://app.sandbox.midtrans.com/snap/v1/transactions"
        @field:NotBlank
        var apiUrl: String = "https://api.sandbox.midtrans.com/v2"
    }

    class BiteshipProperties {
        @field:NotBlank
        var apiKey: String = "dummy-api-key"
        @field:NotBlank
        var apiUrl: String = "https://api.biteship.com/v1"
        @field:NotBlank
        var webhookSecret: String = "dummy-webhook-secret"
    }
}
