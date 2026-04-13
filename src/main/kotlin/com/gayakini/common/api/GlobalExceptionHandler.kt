package com.gayakini.common.api

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.util.NoSuchElementException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: NoSuchElementException): StandardResponse<Unit> {
        return StandardResponse.error(e.message ?: "Data tidak ditemukan.", "ERR_NOT_FOUND")
    }

    @ExceptionHandler(ForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbiddenException(e: ForbiddenException): StandardResponse<Unit> {
        logger.warn("Forbidden access: {}", e.message)
        return StandardResponse.error(e.message ?: "Akses ditolak", "ERR_FORBIDDEN")
    }

    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorizedException(e: UnauthorizedException): StandardResponse<Unit> {
        logger.warn("Unauthorized access: {}", e.message)
        return StandardResponse.error(e.message ?: "Silakan login terlebih dahulu", "ERR_UNAUTHORIZED")
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxSizeException(e: MaxUploadSizeExceededException): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Upload size exceeded: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(StandardResponse.error("Ukuran file melebihi batas 5MB", "ERR_FILE_TOO_LARGE"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Invalid argument: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(StandardResponse.error(e.message ?: "Permintaan tidak valid", "ERR_INVALID_REQUEST"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Validation failed: {}", e.message)
        return badRequestResponse(buildValidationMessage(e.bindingResult.allErrors.mapNotNull { it.defaultMessage }))
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(e: BindException): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Binding failed: {}", e.message)
        return badRequestResponse(buildValidationMessage(e.bindingResult.allErrors.mapNotNull { it.defaultMessage }))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(e: ConstraintViolationException): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Constraint violation: {}", e.message)
        val messages = e.constraintViolations.map { violation -> violation.message }
        return badRequestResponse(buildValidationMessage(messages))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Unreadable request body: {}", e.message)
        return badRequestResponse("Body permintaan tidak valid atau tidak dapat diproses.")
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(e: MissingRequestHeaderException): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Missing request header: {}", e.message)
        return badRequestResponse("Header ${e.headerName} wajib dikirim.")
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingRequestParameter(
        e: MissingServletRequestParameterException,
    ): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Missing request parameter: {}", e.message)
        return badRequestResponse("Parameter ${e.parameterName} wajib dikirim.")
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(
        e: MethodArgumentTypeMismatchException,
    ): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Argument type mismatch: {}", e.message)
        return badRequestResponse("Parameter ${e.name} memiliki format yang tidak valid.")
    }

    @ExceptionHandler(SecurityException::class)
    fun handleSecurityException(e: SecurityException): ResponseEntity<StandardResponse<Unit>> {
        logger.error("Security violation: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(StandardResponse.error("Akses ditolak", "ERR_ACCESS_DENIED"))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<StandardResponse<Unit>> {
        logger.warn("Illegal state: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(StandardResponse.error(e.message ?: "Terjadi kesalahan konflik data", "ERR_CONFLICT"))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleAllExceptions(e: Exception): StandardResponse<Unit> {
        logger.error("Unhandled exception: ", e)
        return StandardResponse.error(e.message ?: "Terjadi kesalahan pada server", "ERR_INTERNAL")
    }

    private fun badRequestResponse(message: String): ResponseEntity<StandardResponse<Unit>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(StandardResponse.error(message, "ERR_INVALID_REQUEST"))

    private fun buildValidationMessage(messages: List<String>): String {
        val normalized = messages.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        return when {
            normalized.isEmpty() -> "Permintaan tidak valid."
            normalized.size == 1 -> normalized.first()
            else ->
                normalized.joinToString(
                    prefix = "Permintaan tidak valid: ",
                    separator = "; ",
                )
        }
    }
}
