package com.gayakini.infrastructure.security

import com.gayakini.common.util.UuidV7Generator
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Standardizes request correlation and observability.
 *
 * This filter:
 * 1. Assigns a unique UUIDv7 to every incoming HTTP request.
 * 2. Populates the Mapped Diagnostic Context (MDC) with "requestId".
 * 3. Exposes the "X-Request-Id" header in the HTTP response.
 *
 * All logs emitted during the request lifecycle will contain this ID,
 * and the client can use it for troubleshooting and error reporting.
 */
@Component
class RequestIdFilter : OncePerRequestFilter() {
    companion object {
        const val MDC_REQUEST_ID = "requestId"
        const val HEADER_REQUEST_ID = "X-Request-Id"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = request.getHeader(HEADER_REQUEST_ID) ?: UuidV7Generator.generateString()

        try {
            MDC.put(MDC_REQUEST_ID, requestId)
            response.setHeader(HEADER_REQUEST_ID, requestId)
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_REQUEST_ID)
        }
    }
}
