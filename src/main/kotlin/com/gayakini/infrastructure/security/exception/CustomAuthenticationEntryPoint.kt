package com.gayakini.infrastructure.security.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.common.api.StandardResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(private val objectMapper: ObjectMapper) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val body =
            StandardResponse<Unit>(
                success = false,
                message = "Sesi tidak valid atau telah berakhir. Silakan login kembali.",
            )

        objectMapper.writeValue(response.outputStream, body)
    }
}
