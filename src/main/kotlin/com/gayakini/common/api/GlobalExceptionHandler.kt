package com.gayakini.common.api

import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors =
            ex.bindingResult.allErrors.map { error ->
                ApiError(
                    field = (error as? FieldError)?.field,
                    message = error.defaultMessage ?: "Kesalahan validasi",
                )
            }

        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Data yang Anda kirimkan tidak valid.")
        problemDetail.title = "Kesalahan Validasi"
        problemDetail.type = URI.create("https://gayakini.com/probs/validation-error")
        problemDetail.setProperty("errors", errors)
        problemDetail.setProperty("timestamp", Instant.now())

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        ex: IllegalStateException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetail> {
        val status =
            if (ex.message?.contains("stok", ignoreCase = true) == true) {
                HttpStatus.CONFLICT
            } else {
                HttpStatus.BAD_REQUEST
            }

        val problemDetail = ProblemDetail.forStatusAndDetail(status, ex.message ?: "Permintaan tidak dapat diproses.")
        problemDetail.title = "Konflik Bisnis"
        problemDetail.setProperty("timestamp", Instant.now())

        return ResponseEntity.status(status).body(problemDetail)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        ex: NoSuchElementException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Data tidak ditemukan.")
        problemDetail.title = "Data Tidak Ditemukan"
        problemDetail.setProperty("timestamp", Instant.now())

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail)
    }

    @ExceptionHandler(Exception::class)
    fun handleAllOtherExceptions(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<ProblemDetail> {
        log.error("Unhandled exception occurred: ${ex.message}", ex)

        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Terjadi kesalahan pada sistem kami. Silakan coba beberapa saat lagi.",
            )
        problemDetail.title = "Kesalahan Server Internal"
        problemDetail.setProperty("timestamp", Instant.now())

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail)
    }
}
