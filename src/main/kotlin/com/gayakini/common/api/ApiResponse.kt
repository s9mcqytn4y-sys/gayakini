package com.gayakini.common.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.MDC
import java.net.URI

/**
 * Standardized API Response Baseline for the Gayakini application.
 *
 * All successful and error responses from the application must conform to this contract.
 * Correlates with the trace context (RequestId) for observability.
 */

private const val MDC_REQUEST_ID = "requestId"

private fun currentRequestId(): String? = MDC.get(MDC_REQUEST_ID)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Metadata untuk respons API standar.")
data class ApiMeta(
    @Schema(
        description = "ID unik permintaan untuk tracing/observability",
        example = "550e8400-e29b-41d4-a716-446655440000",
    )
    val requestId: String? = currentRequestId(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Metadata untuk respons API yang memiliki pagination.")
data class PageMeta(
    @Schema(description = "Nomor halaman saat ini", example = "1")
    val page: Int,
    @Schema(description = "Jumlah item per halaman", example = "20")
    val size: Int,
    @Schema(description = "Total seluruh item", example = "150")
    val totalElements: Long,
    @Schema(description = "Total seluruh halaman", example = "8")
    val totalPages: Int,
    @Schema(
        description = "ID unik permintaan",
        example = "550e8400-e29b-41d4-a716-446655440000",
    )
    val requestId: String? = currentRequestId(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Representasi nilai mata uang.")
data class MoneyDto(
    @Schema(description = "Kode mata uang ISO 4217", example = "IDR")
    val currency: String = "IDR",
    @Schema(description = "Nilai dalam satuan terkecil (sen/rupiah)", example = "150000")
    val amount: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detail kesalahan standar berdasarkan RFC 7807 (Problem Details).")
data class ProblemDetails(
    @Schema(
        description = "URI yang mengidentifikasi tipe masalah",
        example = "https://gayakini.com/probs/auth/invalid-token",
    )
    val type: URI,
    @Schema(description = "Judul singkat masalah", example = "Token Tidak Valid")
    val title: String,
    @Schema(description = "Kode status HTTP", example = "401")
    val status: Int,
    @Schema(description = "Detail teknis kesalahan", example = "JWT expired at 2026-04-12T10:00:00Z")
    val detail: String? = null,
    @Schema(description = "URI instance yang menyebabkan masalah")
    val instance: URI? = null,
    @Schema(description = "Kode internal kesalahan", example = "AUTH_001")
    val code: String? = null,
    @Schema(
        description = "ID unik permintaan",
        example = "550e8400-e29b-41d4-a716-446655440000",
    )
    val requestId: String? = currentRequestId(),
    @Schema(
        description = "Pesan kesalahan ramah pengguna",
        example = "Sesi Anda telah berakhir, silakan login kembali.",
    )
    val userMessage: String,
    @Schema(description = "Daftar kesalahan per field (untuk validasi form)")
    val fieldErrors: List<ProblemFieldError>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detail kesalahan pada field tertentu.")
data class ProblemFieldError(
    @Schema(description = "Nama field yang bermasalah", example = "email")
    val field: String,
    @Schema(description = "Pesan kesalahan teknis", example = "must be a well-formed email address")
    val message: String,
    @Schema(description = "Pesan kesalahan ramah pengguna", example = "Format email tidak valid.")
    val userMessage: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Struktur respons error standar.")
data class ApiErrorResponse(
    @Schema(description = "Indikator kegagalan", example = "false")
    val success: Boolean = false,
    @Schema(
        description = "Pesan error yang dapat ditampilkan ke operator/frontend",
        example = "Permintaan tidak valid.",
    )
    val message: String,
    val meta: ApiMeta? = ApiMeta(),
)

/**
 * Standard Success Response as per OpenAPI Contract
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Struktur respons sukses standar.")
data class StandardResponse<T>(
    @Schema(description = "Indikator keberhasilan", example = "true")
    val success: Boolean = true,
    @Schema(description = "Pesan informasi", example = "Data berhasil diambil.")
    val message: String,
    @Schema(description = "Konten data respons")
    val data: T? = null,
    val meta: ApiMeta? = ApiMeta(),
) {
    companion object {
        fun <T> success(
            message: String,
            data: T? = null,
        ) = StandardResponse(true, message, data)

        fun <T> error(
            message: String,
            code: String? = null,
        ) = StandardResponse<T>(
            success = false,
            message = message,
            data = null,
            meta = ApiMeta(requestId = code ?: currentRequestId()),
        )
    }
}

typealias ApiResponse<T> = StandardResponse<T>

/**
 * Standard Paginated Response as per OpenAPI Contract
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Struktur respons paginasi standar.")
data class PaginatedResponse<T>(
    @Schema(description = "Indikator keberhasilan", example = "true")
    val success: Boolean = true,
    @Schema(description = "Pesan informasi", example = "Daftar produk berhasil diambil.")
    val message: String,
    @Schema(description = "Daftar konten data")
    val data: List<T>,
    val meta: PageMeta,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Struktur respons acknowledgement webhook.")
data class WebhookAckResponse(
    @Schema(description = "Indikator keberhasilan", example = "true")
    val success: Boolean = true,
    @Schema(description = "Pesan informasi", example = "Webhook berhasil diterima.")
    val message: String = "Webhook berhasil diterima.",
    val data: WebhookAckData = WebhookAckData(),
    val meta: ApiMeta? = ApiMeta(),
)

@Schema(description = "Data konfirmasi penerimaan webhook.")
data class WebhookAckData(
    @Schema(description = "Apakah webhook diterima dan akan diproses", example = "true")
    val accepted: Boolean = true,
)
