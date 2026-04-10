package com.gayakini.finance.application

import com.gayakini.payment.domain.PaymentSettledEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FinanceEventListener(
    private val financeService: FinanceService,
) {
    private val logger = LoggerFactory.getLogger(FinanceEventListener::class.java)

    @EventListener
    @Transactional
    fun handlePaymentSettled(event: PaymentSettledEvent) {
        logger.info("Handling payment settlement event for order: {}", event.transactionNumber)

        financeService.recordPaymentSettlement(
            transactionId = event.paymentId,
            orderNumber = event.transactionNumber,
            amount = event.amount,
            metadata =
                mapOf(
                    "orderId" to event.orderId,
                    "provider" to event.provider,
                ),
        )
    }
}
