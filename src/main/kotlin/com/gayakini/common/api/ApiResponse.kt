package com.gayakini.common.api

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Instant = Instant.now(),
    val errors: List<ApiError>? = null,
) {
    companion object {
        fun <T> success(
            data: T,
            message: String = "Operasi berhasil",
        ): ApiResponse<T> = ApiResponse(success = true, message = message, data = data)

        fun success(message: String = "Operasi berhasil"): ApiResponse<Unit> = ApiResponse(success = true, message = message)

        fun <T> error(
            message: String,
            errors: List<ApiError>? = null,
        ): ApiResponse<T> = ApiResponse(success = false, message = message, errors = errors)
    }
}

data class ApiError(
    val field: String?,
    val message: String,
)
