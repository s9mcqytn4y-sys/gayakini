package com.gayakini.infrastructure.monitoring

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * Component for tracking business metrics related to the order lifecycle.
 */
@Component
class OrderMetrics(private val meterRegistry: MeterRegistry) {
    companion object {
        private const val PREFIX = "gayakini.orders"
    }

    fun recordOrderPaid(provider: String) {
        meterRegistry.counter("$PREFIX.paid", "provider", provider).increment()
    }

    fun recordPaymentFailure(
        provider: String,
        status: String,
    ) {
        meterRegistry.counter("gayakini.payments.failed", "provider", provider, "status", status).increment()
    }

    fun recordOrderCancelled(reason: String) {
        meterRegistry.counter("$PREFIX.cancelled", "reason", reason).increment()
    }

    fun recordOrderReadyToShip() {
        meterRegistry.counter("$PREFIX.ready_to_ship").increment()
    }
}
