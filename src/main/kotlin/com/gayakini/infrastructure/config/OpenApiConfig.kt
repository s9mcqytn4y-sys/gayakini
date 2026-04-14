package com.gayakini.infrastructure.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val JSON_MEDIA_TYPE = "application/json"
private const val AUTH_SCHEME_NAME = "Bearer Authentication"

@Configuration
@OpenAPIDefinition(
    info =
        Info(
            title = "Gayakini Backend API",
            version = "v1.0.0",
            description =
                "Kontrak HTTP resmi Gayakini dihasilkan otomatis dari controller, DTO, " +
                    "dan global exception handler. Gunakan /api-docs dan /swagger-ui.html " +
                    "sebagai sumber kebenaran utama untuk integrasi frontend dan QA.",
            contact =
                Contact(
                    name = "Gayakini Dev Team",
                    url = "https://gayakini.com",
                ),
            license =
                License(
                    name = "Apache 2.0",
                    url = "https://www.apache.org/licenses/LICENSE-2.0",
                ),
        ),
)
@SecurityScheme(
    name = AUTH_SCHEME_NAME,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Masukkan access token JWT untuk mengakses endpoint yang memerlukan autentikasi.",
)
class OpenApiConfig {
    @Bean
    fun gayakiniOpenApi(): OpenAPI = OpenAPI()

    @Bean
    fun defaultOpenApiCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            val components = openApi.components ?: Components().also(openApi::components)
            registerStandardResponses(components)

            openApi.paths.orEmpty().forEach { (path, pathItem) ->
                pathItem.readOperations().forEach { operation ->
                    operation.security =
                        if (requiresJwtSecurity(path)) {
                            mutableListOf(SecurityRequirement().addList(AUTH_SCHEME_NAME))
                        } else {
                            mutableListOf()
                        }

                    attachStandardErrorResponses(operation.responses)
                }
            }
        }

    private fun registerStandardResponses(components: Components) {
        components.schemas =
            (components.schemas ?: linkedMapOf()).apply {
                putIfAbsent(
                    "ApiErrorResponse",
                    ObjectSchema()
                        .addProperty("success", BooleanSchema().example(false))
                        .addProperty("message", StringSchema().example("Permintaan tidak valid."))
                        .addProperty(
                            "meta",
                            ObjectSchema().addProperty("requestId", StringSchema().example("REQ-400-EXAMPLE")),
                        ),
                )
            }

        components.responses =
            (components.responses ?: linkedMapOf()).apply {
                putIfAbsent("BadRequest", errorResponse("Permintaan tidak valid atau gagal diproses.", "400"))
                putIfAbsent("Unauthorized", errorResponse("Autentikasi diperlukan atau token tidak valid.", "401"))
                putIfAbsent("Forbidden", errorResponse("Akses ke resource ini ditolak.", "403"))
                putIfAbsent("NotFound", errorResponse("Resource yang diminta tidak ditemukan.", "404"))
                putIfAbsent("Conflict", errorResponse("Permintaan tidak dapat diproses karena konflik data.", "409"))
                putIfAbsent("PayloadTooLarge", errorResponse("Ukuran payload melebihi batas yang diizinkan.", "413"))
                putIfAbsent("InternalServerError", errorResponse("Terjadi kesalahan internal pada server.", "500"))
            }
    }

    private fun attachStandardErrorResponses(responses: ApiResponses?) {
        val target = responses ?: return

        target.putIfAbsent("400", referencedResponse("BadRequest"))
        target.putIfAbsent("401", referencedResponse("Unauthorized"))
        target.putIfAbsent("403", referencedResponse("Forbidden"))
        target.putIfAbsent("404", referencedResponse("NotFound"))
        target.putIfAbsent("409", referencedResponse("Conflict"))
        target.putIfAbsent("413", referencedResponse("PayloadTooLarge"))
        target.putIfAbsent("500", referencedResponse("InternalServerError"))
    }

    private fun errorResponse(
        description: String,
        exampleStatus: String,
    ): ApiResponse =
        ApiResponse()
            .description(description)
            .content(
                Content().addMediaType(
                    JSON_MEDIA_TYPE,
                    MediaType().schema(
                        Schema<Any>()
                            .`$ref`("#/components/schemas/ApiErrorResponse")
                            .example(
                                mapOf(
                                    "success" to false,
                                    "message" to description,
                                    "meta" to
                                        mapOf(
                                            "requestId" to "REQ-$exampleStatus-EXAMPLE",
                                        ),
                                ),
                            ),
                    ),
                ),
            )

    private fun referencedResponse(name: String): ApiResponse = ApiResponse().`$ref`("#/components/responses/$name")

    private fun requiresJwtSecurity(path: String): Boolean =
        when {
            path.startsWith("/v1/me/") || path == "/v1/me" -> true
            path.startsWith("/v1/admin/") -> true
            path.startsWith("/v1/finance/") -> true
            path.startsWith("/v1/operations/") -> true
            path.startsWith("/v1/payments/") &&
                path != "/v1/payments/config" &&
                !path.startsWith("/v1/payments/orders/") -> true
            path.startsWith("/v1/media/secure/") -> true
            // Orders: Some are public (GET by ID, Cancel), some are private (list)
            path == "/v1/orders" || path.startsWith("/v1/orders/") && !isPublicOrderEndpoint(path) -> true
            else -> false
        }

    private fun isPublicOrderEndpoint(path: String): Boolean {
        // Matches GET /v1/orders/{orderId} and POST /v1/orders/{orderId}/cancellations
        return path.matches(Regex("/v1/orders/[^/]+")) || path.matches(Regex("/v1/orders/[^/]+/cancellations"))
    }
}
