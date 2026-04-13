package com.gayakini.payment.api

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/payment")
@Hidden
class PaymentRedirectController {
    @GetMapping("/finish")
    fun finish(): String {
        val successUrl = "https://gayakini.com/payment/success"
        return "redirect:$successUrl"
    }

    @GetMapping("/unfinish")
    fun unfinish(): String {
        val pendingUrl = "https://gayakini.com/payment/pending"
        return "redirect:$pendingUrl"
    }

    @GetMapping("/error")
    fun error(): String {
        val failedUrl = "https://gayakini.com/payment/failed"
        return "redirect:$failedUrl"
    }
}
