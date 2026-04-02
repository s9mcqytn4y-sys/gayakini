package com.gayakini.common.api

import com.fasterxml.jackson.annotation.JsonInclude
import java.net.URI
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiMeta(
    val requestId: String? = UUID.randomUUID().toString(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PageMeta(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val requestId: String? = UUID.randomUUID().toString(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MoneyDto(
    val currency: String = "IDR",
    val amount: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProblemDetails(
    val type: URI,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: URI? = null,
    val code: String? = null,
    val requestId: String? = UUID.randomUUID().toString(),
    val userMessage: String,
    val fieldErrors: List<ProblemFieldError>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProblemFieldError(
    val field: String,
    val message: String,
    val userMessage: String? = null,
)

/**
 * Standard Success Response as per OpenAPI Contract
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class StandardResponse<T>(
    val success: Boolean = true,
    val message: String,
    val data: T? = null,
    val meta: ApiMeta? = ApiMeta()
)

/**
 * Standard Paginated Response as per OpenAPI Contract
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaginatedResponse<T>(
    val success: Boolean = true,
    val message: String,
    val data: List<T>,
    val meta: PageMeta
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WebhookAckResponse(
    val success: Boolean = true,
    val message: String = "Webhook berhasil diterima.",
    val data: WebhookAckData = WebhookAckData(),
    val meta: ApiMeta? = ApiMeta()
)

data class WebhookAckData(
    val accepted: Boolean = true
)
