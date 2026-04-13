package com.gayakini.customer.api

import com.gayakini.customer.domain.CustomerRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "Permintaan untuk mendaftarkan akun pelanggan baru.")
data class RegisterRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 254)
    @Schema(description = "Alamat email pelanggan", example = "pelanggan@example.com")
    val email: String,
    @field:NotBlank
    @field:Pattern(regexp = "^\\+?[0-9]{10,14}$")
    @Schema(description = "Nomor telepon pelanggan", example = "+628123456789")
    val phone: String,
    @field:NotBlank
    @field:Size(max = 120)
    @Schema(description = "Nama lengkap pelanggan", example = "Budi Santoso")
    val fullName: String,
    @field:NotBlank
    @field:Size(min = 8, max = 72)
    @Schema(description = "Password akun (min 8 karakter)", example = "P@ssw0rd123")
    val password: String,
)

@Schema(description = "Permintaan untuk memperbarui profil pelanggan.")
data class UpdateProfileRequest(
    @field:Email
    @field:Size(max = 254)
    @Schema(description = "Alamat email baru", example = "pelanggan.baru@example.com")
    val email: String? = null,
    @field:Pattern(regexp = "^\\+?[0-9]{10,14}$")
    @Schema(description = "Nomor telepon baru", example = "+6281299998888")
    val phone: String? = null,
    @field:Size(max = 120)
    @Schema(description = "Nama lengkap baru", example = "Budi S. Santoso")
    val fullName: String? = null,
)

@Schema(description = "Permintaan login untuk mendapatkan token.")
data class LoginRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 254)
    @Schema(description = "Alamat email terdaftar", example = "pelanggan@example.com")
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, max = 72)
    @Schema(description = "Password akun", example = "P@ssw0rd123")
    val password: String,
)

@Schema(description = "Permintaan untuk memperbarui access token menggunakan refresh token.")
data class RefreshTokenRequest(
    @field:NotBlank
    @field:Size(min = 20, max = 4096)
    @Schema(description = "Refresh token yang masih valid")
    val refreshToken: String,
)

@Schema(description = "Respons berisi token autentikasi.")
data class AuthTokensResponse(
    @Schema(description = "Pesan status", example = "Login berhasil.")
    val message: String,
    val data: AuthTokensData,
)

@Schema(description = "Data hasil autentikasi.")
data class AuthTokensData(
    val tokens: JwtTokenPair,
    val customer: CustomerProfileResponse,
)

@Schema(description = "Pasangan token JWT.")
data class JwtTokenPair(
    @Schema(description = "Access token untuk otorisasi API")
    val accessToken: String,
    @Schema(description = "Refresh token untuk mendapatkan access token baru")
    val refreshToken: String,
    @Schema(description = "Tipe token", example = "Bearer")
    val tokenType: String = "Bearer",
    @Schema(description = "Durasi berlaku access token (detik)", example = "3600")
    val expiresIn: Int,
)

@Schema(description = "Profil data pelanggan.")
data class CustomerProfileResponse(
    @Schema(description = "ID unik pelanggan", example = "550e8400-e29b-41d4-a716-446655440006")
    val id: UUID,
    @Schema(description = "Alamat email", example = "pelanggan@example.com")
    val email: String,
    @Schema(description = "Nomor telepon", example = "+628123456789")
    val phone: String?,
    @Schema(description = "Nama lengkap", example = "Budi Santoso")
    val fullName: String,
    @Schema(description = "Peran pelanggan dalam sistem", example = "CUSTOMER")
    val role: CustomerRole,
    @Schema(description = "Waktu akun dibuat")
    val createdAt: Instant,
)

@Schema(description = "Respons data alamat.")
data class AddressResponse(
    @Schema(description = "ID unik alamat", example = "550e8400-e29b-41d4-a716-446655440011")
    val id: UUID,
    @Schema(description = "Nama penerima", example = "Budi Santoso")
    val recipientName: String,
    @Schema(description = "Nomor telepon penerima", example = "+628123456789")
    val phone: String,
    @Schema(description = "Alamat baris 1", example = "Jl. Merdeka No. 123")
    val line1: String,
    @Schema(description = "Alamat baris 2 (opsional)")
    val line2: String?,
    @Schema(description = "Catatan pengiriman (opsional)")
    val notes: String?,
    @Schema(description = "ID area Biteship", example = "6.31.2.1")
    val areaId: String,
    @Schema(description = "Kecamatan/Distrik", example = "Gambir")
    val district: String,
    @Schema(description = "Kota/Kabupaten", example = "Jakarta Pusat")
    val city: String,
    @Schema(description = "Provinsi", example = "DKI Jakarta")
    val province: String,
    @Schema(description = "Kode Pos", example = "10110")
    val postalCode: String,
    @Schema(description = "Kode Negara ISO 3166-1 alpha-2", example = "ID")
    val countryCode: String,
    @Schema(description = "Apakah ini alamat utama", example = "true")
    val isDefault: Boolean,
)

@Schema(description = "Permintaan untuk menambah atau mengubah alamat.")
data class AddressUpsertRequest(
    @field:NotBlank
    @field:Size(max = 120)
    @Schema(description = "Nama penerima", example = "Budi Santoso")
    val recipientName: String,
    @field:NotBlank
    @field:Pattern(regexp = "^\\+?[0-9]{10,14}$")
    @Schema(description = "Nomor telepon penerima", example = "+628123456789")
    val phone: String,
    @field:NotBlank
    @field:Size(max = 200)
    @Schema(description = "Alamat baris 1", example = "Jl. Merdeka No. 123")
    val line1: String,
    @field:Size(max = 200)
    @Schema(description = "Alamat baris 2 (opsional)")
    val line2: String?,
    @field:Size(max = 200)
    @Schema(description = "Catatan pengiriman (opsional)")
    val notes: String?,
    @field:NotBlank
    @field:Size(max = 100)
    @Schema(description = "ID area Biteship", example = "6.31.2.1")
    val areaId: String,
    @field:NotBlank
    @field:Size(max = 120)
    @Schema(description = "Kecamatan/Distrik", example = "Gambir")
    val district: String,
    @field:NotBlank
    @field:Size(max = 120)
    @Schema(description = "Kota/Kabupaten", example = "Jakarta Pusat")
    val city: String,
    @field:NotBlank
    @field:Size(max = 120)
    @Schema(description = "Provinsi", example = "DKI Jakarta")
    val province: String,
    @field:NotBlank
    @field:Size(max = 20)
    @Schema(description = "Kode Pos", example = "10110")
    val postalCode: String,
    @field:Pattern(regexp = "^[A-Z]{2}$")
    @Schema(description = "Kode Negara ISO 3166-1 alpha-2", example = "ID")
    val countryCode: String = "ID",
    @Schema(description = "Apakah ini alamat utama", example = "false")
    val isDefault: Boolean = false,
)
