package com.gayakini.e2e

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AuthE2ETest : BaseE2ETest() {
    @BeforeEach
    fun setup() {
        cleanupDatabase()
    }

    @Test
    fun `should register a new customer successfully`() {
        val request = E2EDataFactory.createRegisterRequest()

        val response =
            restTemplate.postForEntity(
                "/v1/auth/register",
                request,
                Map::class.java,
            )

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body ?: throw AssertionError("Response body is null")
        assertEquals("Akun berhasil dibuat.", body["message"])
        assertEquals(true, body["success"])

        val data = body["data"] as Map<*, *>
        val tokens = data["tokens"] as Map<*, *>
        assertNotNull(tokens["accessToken"])
        assertNotNull(tokens["refreshToken"])
    }

    @Test
    fun `should login successfully after registration`() {
        // 1. Register
        val registerRequest =
            E2EDataFactory.createRegisterRequest(
                email = "e2e.siti.aisyah@gayakini.local",
                fullName = "Siti Aisyah",
            )
        restTemplate.postForEntity("/v1/auth/register", registerRequest, Map::class.java)

        // 2. Login
        val loginRequest = E2EDataFactory.createLoginRequest(email = "e2e.siti.aisyah@gayakini.local")
        val loginResponse =
            restTemplate.postForEntity(
                "/v1/auth/login",
                loginRequest,
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, loginResponse.statusCode)
        val body = loginResponse.body ?: throw AssertionError("Response body is null")
        assertEquals("Login berhasil.", body["message"])
        assertEquals(true, body["success"])

        val data = body["data"] as Map<*, *>
        val tokens = data["tokens"] as Map<*, *>
        assertNotNull(tokens["accessToken"])
    }
}
