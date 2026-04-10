package com.gayakini.customer.application

import com.gayakini.common.api.ForbiddenException
import com.gayakini.common.api.UnauthorizedException
import com.gayakini.customer.api.*
import com.gayakini.customer.domain.*
import com.gayakini.customer.infrastructure.persistence.entity.RefreshToken
import com.gayakini.customer.infrastructure.persistence.repository.RefreshTokenRepository
import com.gayakini.infrastructure.security.JwtService
import com.gayakini.infrastructure.storage.StorageCategory
import com.gayakini.infrastructure.storage.StorageService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.Instant
import java.util.NoSuchElementException
import java.util.UUID

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val addressRepository: CustomerAddressRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val storageService: StorageService,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    companion object {
        private const val ACCESS_TOKEN_EXPIRY_SECONDS = 3600L
    }

    @Transactional
    fun register(request: RegisterRequest): AuthTokensData {
        val normalizedEmail = request.email.trim().lowercase()
        check(customerRepository.findByEmail(normalizedEmail).isEmpty) { "Email sudah terdaftar." }

        val customer =
            Customer(
                email = normalizedEmail,
                passwordHash = passwordEncoder.encode(request.password),
                fullName = request.fullName.trim(),
                phone = request.phone.trim(),
            )
        val savedCustomer = customerRepository.save(customer)

        val tokens = generateAndSaveTokenPair(savedCustomer)
        return AuthTokensData(
            tokens = tokens,
            customer = mapToProfileResponse(savedCustomer),
        )
    }

    @Transactional
    fun login(request: LoginRequest): AuthTokensData {
        val customer =
            customerRepository.findByEmail(request.email.trim().lowercase())
                .filter { passwordEncoder.matches(request.password, it.passwordHash) }
                .orElseThrow { IllegalArgumentException("Email atau password salah.") }

        customer.lastLoginAt = Instant.now()
        customer.updatedAt = Instant.now()
        val savedCustomer = customerRepository.save(customer)

        val tokens = generateAndSaveTokenPair(savedCustomer)
        return AuthTokensData(
            tokens = tokens,
            customer = mapToProfileResponse(savedCustomer),
        )
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): AuthTokensData {
        val oldTokenEntity =
            refreshTokenRepository.findByToken(request.refreshToken)
                .orElseThrow { UnauthorizedException("Refresh token tidak valid.") }

        if (oldTokenEntity.revoked) {
            // Token reuse detected! Revoke all tokens in family for safety (Zero Trust)
            val family = refreshTokenRepository.findAllByFamilyId(oldTokenEntity.familyId)
            family.forEach { it.revoked = true }
            refreshTokenRepository.saveAll(family)
            throw ForbiddenException("Sesi telah berakhir karena deteksi penggunaan ganda. Silakan login kembali.")
        }

        if (oldTokenEntity.isExpired) {
            throw UnauthorizedException("Sesi telah berakhir. Silakan login kembali.")
        }

        val customer = oldTokenEntity.customer
        if (!customer.isActive) {
            throw ForbiddenException("Akun tidak aktif.")
        }

        // Rotate token
        val newTokens = generateAndSaveTokenPair(customer, familyId = oldTokenEntity.familyId)

        oldTokenEntity.revoked = true
        oldTokenEntity.replacedByToken = newTokens.refreshToken
        refreshTokenRepository.save(oldTokenEntity)

        return AuthTokensData(
            tokens = newTokens,
            customer = mapToProfileResponse(customer),
        )
    }

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent {
            it.revoked = true
            refreshTokenRepository.save(it)
        }
    }

    private fun generateAndSaveTokenPair(
        customer: Customer,
        familyId: UUID? = null,
    ): JwtTokenPair {
        val accessToken =
            jwtService.generateAccessToken(
                userId = customer.id,
                email = customer.email,
                role = customer.role.name,
            )
        val refreshTokenString = jwtService.generateRefreshToken(customer.id)

        val expiry = Instant.now().plusSeconds(30L * 24 * 60 * 60) // 30 days
        val refreshTokenEntity =
            RefreshToken(
                customer = customer,
                token = refreshTokenString,
                expiryDate = expiry,
                familyId = familyId ?: UUID.randomUUID(),
            )
        refreshTokenRepository.save(refreshTokenEntity)

        return JwtTokenPair(
            accessToken = accessToken,
            refreshToken = refreshTokenString,
            expiresIn = ACCESS_TOKEN_EXPIRY_SECONDS.toInt(),
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
    fun updateAddress(
        customerId: UUID,
        addressId: UUID,
        request: AddressUpsertRequest,
    ): CustomerAddress {
        val address =
            addressRepository.findById(addressId)
                .filter { it.customer.id == customerId }
                .orElseThrow { NoSuchElementException("Alamat tidak ditemukan.") }

        if (request.isDefault) {
            addressRepository.findByCustomerIdAndIsDefaultTrue(customerId).ifPresent {
                if (it.id != address.id) {
                    it.isDefault = false
                    it.updatedAt = Instant.now()
                    addressRepository.save(it)
                }
            }
        }

        address.recipientName = request.recipientName.trim()
        address.phone = request.phone.trim()
        address.line1 = request.line1.trim()
        address.line2 = request.line2?.trim()?.takeIf { it.isNotBlank() }
        address.notes = request.notes?.trim()?.takeIf { it.isNotBlank() }
        address.areaId = request.areaId.trim()
        address.district = request.district.trim()
        address.city = request.city.trim()
        address.province = request.province.trim()
        address.postalCode = request.postalCode.trim()
        address.countryCode = request.countryCode.trim().uppercase()
        address.isDefault = request.isDefault
        address.updatedAt = Instant.now()
        return addressRepository.save(address)
    }

    @Transactional
    fun deleteAddress(
        customerId: UUID,
        addressId: UUID,
    ) {
        val address =
            addressRepository.findById(addressId)
                .filter { it.customer.id == customerId }
                .orElseThrow { NoSuchElementException("Alamat tidak ditemukan.") }
        addressRepository.delete(address)
    }

    @Transactional
    fun updateProfile(
        customerId: UUID,
        request: UpdateProfileRequest,
    ): CustomerProfileResponse {
        val customer = getCustomer(customerId)

        request.email?.trim()?.lowercase()?.let { newEmail ->
            if (newEmail != customer.email) {
                check(customerRepository.findByEmail(newEmail).isEmpty) { "Email sudah terdaftar." }
                customer.email = newEmail
            }
        }

        request.phone?.trim()?.let { customer.phone = it }
        request.fullName?.trim()?.let { customer.fullName = it }

        customer.updatedAt = Instant.now()
        val savedCustomer = customerRepository.save(customer)
        return mapToProfileResponse(savedCustomer)
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
                recipientName = request.recipientName.trim(),
                phone = request.phone.trim(),
                line1 = request.line1.trim(),
                line2 = request.line2?.trim()?.takeIf { it.isNotBlank() },
                notes = request.notes?.trim()?.takeIf { it.isNotBlank() },
                areaId = request.areaId.trim(),
                district = request.district.trim(),
                city = request.city.trim(),
                province = request.province.trim(),
                postalCode = request.postalCode.trim(),
                countryCode = request.countryCode.trim().uppercase(),
                isDefault = request.isDefault,
            )

        return addressRepository.save(address)
    }

    @Transactional
    fun updateProfilePicture(
        customerId: UUID,
        inputStream: InputStream,
        originalFilename: String,
    ): String {
        val customer = getCustomer(customerId)

        // Delete old profile picture if exists
        customer.profileUrl?.let { oldUrl ->
            storageService.delete(oldUrl, StorageCategory.PROFILES)
        }

        val relativePath =
            storageService.store(
                inputStream = inputStream,
                filename = originalFilename,
                category = StorageCategory.PROFILES,
            )

        customer.profileUrl = relativePath
        customer.updatedAt = Instant.now()
        customerRepository.save(customer)

        return relativePath
    }
}
