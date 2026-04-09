package com.gayakini.common.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
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
}
