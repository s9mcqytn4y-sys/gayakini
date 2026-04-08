package com.gayakini.audit.domain

import com.gayakini.order.domain.Order

object OrderAuditMapper {
    fun toMap(order: Order): Map<String, Any?> {
        return mapOf(
            "id" to order.id,
            "orderNumber" to order.orderNumber,
            "status" to order.status.name,
            "paymentStatus" to order.paymentStatus.name,
            "fulfillmentStatus" to order.fulfillmentStatus.name,
            "totalAmount" to order.totalAmount,
            "promoCode" to order.promoCode,
            "customerId" to order.customerId,
        )
    }
}
