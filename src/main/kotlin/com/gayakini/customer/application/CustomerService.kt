package com.gayakini.customer.application

import com.gayakini.customer.api.AddressUpsertRequest
import com.gayakini.customer.domain.Customer
import com.gayakini.customer.domain.CustomerAddress
import com.gayakini.customer.domain.CustomerAddressRepository
import com.gayakini.customer.domain.CustomerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.NoSuchElementException
import java.util.UUID

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val addressRepository: CustomerAddressRepository,
) {
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
