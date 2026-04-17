package com.gayakini.infrastructure.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter : OncePerRequestFilter() {
    private val authBuckets = ConcurrentHashMap<String, Bucket>()
    private val checkoutBuckets = ConcurrentHashMap<String, Bucket>()
    private val globalBuckets = ConcurrentHashMap<String, Bucket>()

    private fun createAuthBucket(): Bucket {
        val limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)))
        return Bucket.builder().addLimit(limit).build()
    }

    private fun createCheckoutBucket(): Bucket {
        val limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)))
        return Bucket.builder().addLimit(limit).build()
    }

    private fun createGlobalBucket(): Bucket {
        val limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)))
        return Bucket.builder().addLimit(limit).build()
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val uri = request.requestURI

        // Whitelist internal/health-check endpoints
        if (uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim() ?: request.remoteAddr

        val bucket =
            when {
                uri.startsWith("/v1/auth") -> authBuckets.computeIfAbsent(clientIp) { createAuthBucket() }
                uri.startsWith("/v1/checkouts") -> checkoutBuckets.computeIfAbsent(clientIp) { createCheckoutBucket() }
                uri.startsWith("/v1/webhooks") ->
                    globalBuckets.computeIfAbsent(clientIp) {
                        createGlobalBucket()
                    } // Defaulting webhook to global map, though signature validation is more important
                else -> globalBuckets.computeIfAbsent(clientIp) { createGlobalBucket() }
            }

        val probe = bucket.tryConsumeAndReturnRemaining(1)
        if (probe.isConsumed) {
            response.addHeader("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            filterChain.doFilter(request, response)
        } else {
            val waitForRefill = probe.nanosToWaitForRefill / 1_000_000_000
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", waitForRefill.toString())
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests. Please try again later.")
        }
    }
}
