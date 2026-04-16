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
        var secret: String = ""
        var accessTokenExpirationMinutes: Long = 60
        var refreshTokenExpirationDays: Long = 30
    }

    class CorsProperties {
        @field:NotEmpty
        var allowedOrigins: List<String> = emptyList()
    }

    class MidtransProperties {
        var isProduction: Boolean = false

        @field:NotBlank
        var serverKey: String = ""

        @field:NotBlank
        var clientKey: String = ""

        @field:NotBlank
        var snapUrl: String = ""

        @field:NotBlank
        var apiUrl: String = ""
    }

    class BiteshipProperties {
        @field:NotBlank
        var apiKey: String = ""

        @field:NotBlank
        var apiUrl: String = ""

        @field:NotBlank
        var webhookSecret: String = ""
    }
}
