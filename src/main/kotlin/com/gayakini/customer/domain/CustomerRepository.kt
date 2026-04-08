package com.gayakini.customer.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CustomerRepository : JpaRepository<Customer, UUID> {
    fun findByEmail(email: String): Optional<Customer>

    fun existsByEmail(email: String): Boolean
}

interface CustomerAddressRepository : JpaRepository<CustomerAddress, UUID> {
    fun findByCustomerId(customerId: UUID): List<CustomerAddress>

    fun findByCustomerIdAndIsDefaultTrue(customerId: UUID): Optional<CustomerAddress>
}
