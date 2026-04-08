package com.gayakini.payment.domain

class PaymentGatewayException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
