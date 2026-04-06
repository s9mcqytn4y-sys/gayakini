package com.gayakini.customer.application

import com.gayakini.customer.api.*
import com.gayakini.customer.domain.*
import com.gayakini.infrastructure.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.NoSuchElementException
import java.util.UUID

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val addressRepository: CustomerAddressRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    @Transactional
    fun register(request: RegisterRequest): AuthTokensData {
        if (customerRepository.findByEmail(request.email).isPresent) {
            throw IllegalStateException("Email sudah terdaftar.")
        }

        val customer =
            Customer(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
                fullName = request.fullName,
                phone = request.phone,
            )
        val savedCustomer = customerRepository.save(customer)

        val tokens = generateTokenPair(savedCustomer)
        return AuthTokensData(
            tokens = tokens,
            customer = mapToProfileResponse(savedCustomer),
        )
    }

    @Transactional
    fun login(request: LoginRequest): AuthTokensData {
        val customer =
            customerRepository.findByEmail(request.email)
                .filter { passwordEncoder.matches(request.password, it.passwordHash) }
                .orElseThrow { IllegalArgumentException("Email atau password salah.") }

        customer.lastLoginAt = Instant.now()
        customer.updatedAt = Instant.now()
        val savedCustomer = customerRepository.save(customer)

        val tokens = generateTokenPair(savedCustomer)
        return AuthTokensData(
            tokens = tokens,
            customer = mapToProfileResponse(savedCustomer),
        )
    }

    private fun generateTokenPair(customer: Customer): JwtTokenPair {
        val accessToken =
            jwtService.generateAccessToken(
                userId = customer.id,
                email = customer.email,
                role = customer.role.name,
                permissions = customer.role.permissions.map { it.name }.toSet(),
            )
        val refreshToken = jwtService.generateRefreshToken(customer.id)
        return JwtTokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 3600, // This should ideally come from config or JwtService
        )
    }

    private fun mapToProfileResponse(customer: Customer) =
        CustomerProfileResponse(
            id = customer.id,
            email = customer.email,
            phone = customer.phone,
            fullName = customer.fullName,
            role = customer.role,
            createdAt = customer.createdAt,
        )

    fun getProfile(customerId: UUID): CustomerProfileResponse = mapToProfileResponse(getCustomer(customerId))

    fun getCustomer(id: UUID): Customer {
        return customerRepository.findById(id)
            .orElseThrow { NoSuchElementException("Customer tidak ditemukan.") }
    }

    fun getAddresses(customerId: UUID): List<CustomerAddress> {
        return addressRepository.findByCustomerId(customerId)
    }

    @Transactional
    fun upsertAddress(
        customerId: UUID,
        request: AddressUpsertRequest,
    ): CustomerAddress {
        val customer = getCustomer(customerId)

        if (request.isDefault) {
            addressRepository.findByCustomerIdAndIsDefaultTrue(customerId).ifPresent {
                it.isDefault = false
                it.updatedAt = Instant.now()
                addressRepository.save(it)
            }
        }

        val address =
            CustomerAddress(
                customer = customer,
                recipientName = request.recipientName,
                phone = request.phone,
                line1 = request.line1,
                line2 = request.line2,
                notes = request.notes,
                areaId = request.areaId,
                district = request.district,
                city = request.city,
                province = request.province,
                postalCode = request.postalCode,
                countryCode = request.countryCode,
                isDefault = request.isDefault,
            )

        return addressRepository.save(address)
    }
}
