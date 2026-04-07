package com.gayakini.e2e

import com.gayakini.customer.api.RegisterRequest
import com.gayakini.customer.api.LoginRequest

object E2EDataFactory {
    fun createRegisterRequest(
        email: String = "e2e.budi.santoso@gayakini.local",
        fullName: String = "Budi Santoso",
        phone: String = "081234567890",
    ) = RegisterRequest(
        email = email,
        password = "Password123!",
        fullName = fullName,
        phone = phone,
    )

    fun createLoginRequest(email: String = "e2e.budi.santoso@gayakini.local") =
        LoginRequest(
            email = email,
            password = "Password123!",
        )
}
