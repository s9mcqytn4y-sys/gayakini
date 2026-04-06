package com.gayakini.customer.domain

enum class CustomerRole(val permissions: Set<Permission>) {
    CUSTOMER(
        setOf(
            Permission.CUSTOMER_READ,
            Permission.CUSTOMER_WRITE,
        ),
    ),
    ADMIN(
        setOf(
            Permission.CUSTOMER_READ,
            Permission.CATALOG_READ,
            Permission.CATALOG_WRITE,
            Permission.ORDER_READ,
            Permission.ORDER_WRITE,
        ),
    ),
    SUPER_ADMIN(Permission.entries.toSet()),
}

enum class Permission {
    CUSTOMER_READ,
    CUSTOMER_WRITE,
    CATALOG_READ,
    CATALOG_WRITE,
    ORDER_READ,
    ORDER_WRITE,
    ADMIN_MANAGE,
}
