package com.gayakini.common.api

class UnauthorizedException(
    message: String = "Autentikasi diperlukan.",
) : RuntimeException(message)

class ForbiddenException(
    message: String = "Akses ditolak.",
) : RuntimeException(message)
