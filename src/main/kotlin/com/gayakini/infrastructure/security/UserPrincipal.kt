package com.gayakini.infrastructure.security

import java.util.UUID

data class UserPrincipal(
    val id: UUID,
    val email: String,
    val role: String,
    val isGuest: Boolean = false
)
