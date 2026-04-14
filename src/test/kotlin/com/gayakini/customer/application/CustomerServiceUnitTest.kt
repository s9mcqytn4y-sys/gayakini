package com.gayakini.customer.application

import com.gayakini.customer.api.*
import com.gayakini.customer.domain.*
import com.gayakini.customer.infrastructure.persistence.repository.RefreshTokenRepository
import com.gayakini.infrastructure.security.JwtService
import com.gayakini.infrastructure.storage.StorageService
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

class CustomerServiceUnitTest {

    private val customerRepository = mockk<CustomerRepository>()
    private val addressRepository = mockk<CustomerAddressRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtService = mockk<JwtService>()
    private val storageService = mockk<StorageService>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()

    private val customerService = CustomerService(
        customerRepository,
        addressRepository,
        passwordEncoder,
        jwtService,
        storageService,
        refreshTokenRepository
    )

    @Test
    fun `register should create customer and return tokens`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "password123",
            fullName = "Test User",
            phone = "+628123456789"
        )

        every { customerRepository.findByEmail("test@example.com") } returns Optional.empty()
        every { passwordEncoder.encode(any()) } returns "hashed_password"
        every { customerRepository.save(any()) } answers { it.invocation.args[0] as Customer }
        every { jwtService.generateAccessToken(any(), any(), any()) } returns "access_token"
        every { jwtService.generateRefreshToken(any()) } returns "refresh_token"
        every { refreshTokenRepository.save(any()) } returns mockk()

        val result = customerService.register(request)

        assertNotNull(result)
        assertEquals("test@example.com", result.customer.email)
        assertEquals("access_token", result.tokens.accessToken)
        verify { customerRepository.save(any()) }
    }

    @Test
    fun `register should fail if email already exists`() {
        val request = RegisterRequest("test@example.com", "pass", "Name", "081")
        every { customerRepository.findByEmail("test@example.com") } returns Optional.of(mockk())

        assertThrows<IllegalStateException> {
            customerService.register(request)
        }
    }

    @Test
    fun `login should return tokens for valid credentials`() {
        val request = LoginRequest("test@example.com", "password123")
        val customer = Customer(
            email = "test@example.com",
            passwordHash = "hashed_password",
            fullName = "Test User",
            phone = "081"
        )

        every { customerRepository.findByEmail("test@example.com") } returns Optional.of(customer)
        every { passwordEncoder.matches("password123", "hashed_password") } returns true
        every { customerRepository.save(any()) } answers { it.invocation.args[0] as Customer }
        every { jwtService.generateAccessToken(any(), any(), any()) } returns "access_token"
        every { jwtService.generateRefreshToken(any()) } returns "refresh_token"
        every { refreshTokenRepository.save(any()) } returns mockk()

        val result = customerService.login(request)

        assertEquals("access_token", result.tokens.accessToken)
        verify { customerRepository.save(any()) }
    }

    @Test
    fun `upsertAddress should handle default address logic`() {
        val customerId = UUID.randomUUID()
        val customer = Customer(
            email = "test@example.com",
            passwordHash = "hash",
            fullName = "Test",
            phone = "081"
        )
        val request = AddressUpsertRequest(
            recipientName = "Recipient",
            phone = "+628123456789",
            line1 = "Jl. Test",
            line2 = null,
            notes = null,
            areaId = "area_1",
            district = "Dist",
            city = "City",
            province = "Prov",
            postalCode = "123",
            countryCode = "ID",
            isDefault = true
        )

        val existingDefault = CustomerAddress(
            customer = customer,
            recipientName = "Old",
            phone = "081",
            line1 = "Old",
            line2 = null,
            notes = null,
            areaId = "area_0",
            district = "D",
            city = "C",
            province = "P",
            postalCode = "0",
            countryCode = "ID",
            isDefault = true
        )

        every { customerRepository.findById(customerId) } returns Optional.of(customer)
        every { addressRepository.findByCustomerIdAndIsDefaultTrue(customerId) } returns Optional.of(existingDefault)
        every { addressRepository.save(any()) } answers { it.invocation.args[0] as CustomerAddress }

        customerService.upsertAddress(customerId, request)

        assert(!existingDefault.isDefault)
        verify { addressRepository.save(existingDefault) }
        verify { addressRepository.save(match { it.isDefault && it.recipientName == "Recipient" }) }
    }

    @Test
    fun `upsertAddress should create new address without affecting others if not default`() {
        val customerId = UUID.randomUUID()
        val customer = Customer(
            email = "test@example.com",
            passwordHash = "hash",
            fullName = "Test",
            phone = "081"
        )
        val request = AddressUpsertRequest(
            recipientName = "New Address",
            phone = "+628123456789",
            line1 = "Jl. New",
            line2 = null,
            notes = null,
            areaId = "area_2",
            district = "Dist",
            city = "City",
            province = "Prov",
            postalCode = "123",
            countryCode = "ID",
            isDefault = false
        )

        every { customerRepository.findById(customerId) } returns Optional.of(customer)
        every { addressRepository.save(any()) } answers { it.invocation.args[0] as CustomerAddress }

        customerService.upsertAddress(customerId, request)

        verify(exactly = 0) { addressRepository.findByCustomerIdAndIsDefaultTrue(any()) }
        verify { addressRepository.save(match { !it.isDefault && it.recipientName == "New Address" }) }
    }

}
