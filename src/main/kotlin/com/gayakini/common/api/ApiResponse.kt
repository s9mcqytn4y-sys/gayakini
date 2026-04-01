package com.gayakini.common.api

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val message: String,
    val data: T? = null,
    val meta: ApiMeta? = null
) {
    companion object {
        fun <T> success(data: T, message: String = "Operasi berhasil", meta: ApiMeta? = null): ApiResponse<T> =
            ApiResponse(message = message, data = data, meta = meta)

        fun success(message: String = "Operasi berhasil", meta: ApiMeta? = null): ApiResponse<Unit> =
            ApiResponse(message = message, meta = meta)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiMeta(
    val requestId: String? = null,
    val page: Int? = null,
    val size: Int? = null,
    val totalElements: Long? = null,
    val totalPages: Int? = null
)
