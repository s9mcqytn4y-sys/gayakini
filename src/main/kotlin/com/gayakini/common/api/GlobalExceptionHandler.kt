package com.gayakini.common.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val fieldErrors =
            ex.bindingResult.allErrors.map { error ->
                ProblemFieldError(
                    field = (error as? FieldError)?.field ?: "unknown",
                    message = error.defaultMessage ?: "Kesalahan validasi",
                    userMessage = error.defaultMessage ?: "Data tidak valid.",
                )
            }

        val problem =
            ProblemDetails(
                type = URI.create("kb://probs/validation-error"),
                title = "Bad Request",
                status = HttpStatus.BAD_REQUEST.value(),
                detail = "Data yang Anda kirimkan tidak valid atau tidak lengkap.",
                userMessage = "Maaf, data yang Anda kirim belum lengkap. Silakan cek lagi.",
                fieldErrors = fieldErrors,
                instance = URI.create((request as ServletWebRequest).request.requestURI),
                requestId = UUID.randomUUID().toString(),
            )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        ex: IllegalStateException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetails> {
        val status =
            if (ex.message?.contains("stok", ignoreCase = true) == true) {
                HttpStatus.CONFLICT
            } else {
                HttpStatus.BAD_REQUEST
            }

        val problem =
            ProblemDetails(
                type = URI.create("kb://probs/business-rule-violation"),
                title = if (status == HttpStatus.CONFLICT) "Conflict" else "Bad Request",
                status = status.value(),
                detail = ex.message ?: "Permintaan tidak dapat diproses karena aturan bisnis.",
                userMessage = ex.message ?: "Maaf, permintaan Anda tidak dapat diproses saat ini.",
                instance = URI.create((request as ServletWebRequest).request.requestURI),
                requestId = UUID.randomUUID().toString(),
            )
        return ResponseEntity.status(status).body(problem)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(
        ex: UnauthorizedException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetails> {
        val problem =
            ProblemDetails(
                type = URI.create("kb://probs/unauthorized"),
                title = "Unauthorized",
                status = HttpStatus.UNAUTHORIZED.value(),
                detail = ex.message ?: "Autentikasi diperlukan.",
                userMessage = ex.message ?: "Silakan login atau gunakan token akses yang benar.",
                instance = URI.create((request as ServletWebRequest).request.requestURI),
                requestId = UUID.randomUUID().toString(),
            )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem)
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(
        ex: ForbiddenException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetails> {
        val problem =
            ProblemDetails(
                type = URI.create("kb://probs/forbidden"),
                title = "Forbidden",
                status = HttpStatus.FORBIDDEN.value(),
                detail = ex.message ?: "Akses ditolak.",
                userMessage = ex.message ?: "Anda tidak memiliki akses untuk operasi ini.",
                instance = URI.create((request as ServletWebRequest).request.requestURI),
                requestId = UUID.randomUUID().toString(),
            )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        ex: NoSuchElementException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetails> {
        val problem =
            ProblemDetails(
                type = URI.create("kb://probs/not-found"),
                title = "Not Found",
                status = HttpStatus.NOT_FOUND.value(),
                detail = ex.message ?: "Resource yang dicari tidak ditemukan.",
                userMessage = "Data yang Anda cari tidak dapat kami temukan.",
                instance = URI.create((request as ServletWebRequest).request.requestURI),
                requestId = UUID.randomUUID().toString(),
            )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetails> {
        val problem =
            ProblemDetails(
                type = URI.create("kb://probs/bad-request"),
                title = "Bad Request",
                status = HttpStatus.BAD_REQUEST.value(),
                detail = ex.message ?: "Permintaan tidak valid.",
                userMessage = "Permintaan tidak dapat diproses. Pastikan data benar.",
                instance = URI.create((request as ServletWebRequest).request.requestURI),
                requestId = UUID.randomUUID().toString(),
            )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }

    @ExceptionHandler(UnsupportedOperationException::class)
    fun handleNotImplemented(
        ex: UnsupportedOperationException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetails> {
        val problem =
            ProblemDetails(
                type = URI.create("kb://probs/not-implemented"),
                title = "Not Implemented",
                status = HttpStatus.NOT_IMPLEMENTED.value(),
                detail = ex.message ?: "Fitur belum tersedia.",
                userMessage = ex.message ?: "Fitur ini belum tersedia.",
                instance = URI.create((request as ServletWebRequest).request.requestURI),
                requestId = UUID.randomUUID().toString(),
            )
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(problem)
    }

    @ExceptionHandler(Exception::class)
    fun handleAllOtherExceptions(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<ProblemDetails> {
        log.error("Unhandled exception: ${ex.message}", ex)
        val problem =
            ProblemDetails(
                type = URI.create("kb://probs/internal-server-error"),
                title = "Internal Server Error",
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                detail = "Terjadi kesalahan sistem yang tidak terduga.",
                userMessage = "Terjadi gangguan teknis. Tim kami sedang menanganinya.",
                instance = URI.create((request as ServletWebRequest).request.requestURI),
                requestId = UUID.randomUUID().toString(),
            )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem)
    }
}
