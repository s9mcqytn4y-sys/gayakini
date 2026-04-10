package com.gayakini.infrastructure.security.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.gayakini.common.api.StandardResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class CustomAccessDeniedHandler(private val objectMapper: ObjectMapper) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val body =
            StandardResponse<Unit>(
                success = false,
                message = "Anda tidak memiliki izin untuk mengakses sumber daya ini.",
            )

        objectMapper.writeValue(response.outputStream, body)
    }
}
