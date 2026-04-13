package com.gayakini.checkout.api

import com.gayakini.cart.api.CartItemDto
import com.gayakini.checkout.domain.CheckoutStatus
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "Permintaan untuk memulai proses checkout.")
data class CreateCheckoutRequest(
    @Schema(description = "ID unik keranjang yang akan dicheckout", example = "550e8400-e29b-41d4-a716-446655440005")
    val cartId: UUID,
)

@Schema(description = "Representasi proses checkout.")
data class CheckoutDto(
    @Schema(description = "ID unik checkout", example = "550e8400-e29b-41d4-a716-446655440010")
    val id: UUID,
    @Schema(description = "ID keranjang asal", example = "550e8400-e29b-41d4-a716-446655440005")
    val cartId: UUID,
    @Schema(description = "ID pelanggan (null untuk guest)", example = "550e8400-e29b-41d4-a716-446655440006")
    val customerId: UUID?,
    @Schema(description = "Token akses untuk checkout guest", example = "checkout_access_token_abc")
    val accessToken: String? = null,
    @Schema(description = "Status checkout", example = "PENDING")
    val status: CheckoutStatus,
    @Schema(description = "Kode mata uang", example = "IDR")
    val currency: String,
    @Schema(description = "Alamat pengiriman yang dipilih")
    val shippingAddress: CheckoutAddressDto? = null,
    @Schema(description = "Opsi pengiriman yang dipilih")
    val selectedShippingQuote: ShippingQuoteDto? = null,
    @Schema(description = "Daftar opsi pengiriman yang tersedia")
    val availableShippingQuotes: List<ShippingQuoteDto> = listOf(),
    @Schema(description = "Daftar item yang dicheckout")
    val items: List<CartItemDto>,
    @Schema(description = "Subtotal harga item")
    val subtotal: MoneyDto,
    @Schema(description = "Biaya pengiriman")
    val shippingCost: MoneyDto,
    @Schema(description = "Total diskon")
    val discount: MoneyDto,
    @Schema(description = "Total yang harus dibayar")
    val total: MoneyDto,
    @Schema(description = "Kode promo yang digunakan", example = "GAYAKINI2026")
    val promoCode: String? = null,
    @Schema(description = "Waktu kedaluwarsa sesi checkout")
    val expiresAt: Instant?,
)

@Schema(description = "Permintaan untuk menggunakan kode promo.")
data class ApplyPromoRequest(
    @field:NotBlank
    @Schema(description = "Kode promo", example = "GAYAKINI2026")
    val promoCode: String,
)

@Schema(description = "Detail alamat pengiriman pada checkout.")
data class CheckoutAddressDto(
    @Schema(description = "ID alamat (null jika alamat kustom/guest)", example = "550e8400-e29b-41d4-a716-446655440011")
    val id: UUID?,
    @Schema(description = "Nama penerima", example = "Budi Santoso")
    val recipientName: String,
    @Schema(description = "Nomor telepon penerima", example = "+628123456789")
    val phone: String,
    @Schema(description = "Email penerima (opsional)", example = "budi@example.com")
    val email: String? = null,
    @Schema(description = "Alamat baris 1", example = "Jl. Merdeka No. 123")
    val line1: String,
    @Schema(description = "Alamat baris 2 (opsional)", example = "Blok A, No. 4")
    val line2: String? = null,
    @Schema(description = "Catatan pengiriman", example = "Pagar warna hitam")
    val notes: String? = null,
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
)

@Schema(description = "Opsi kutipan pengiriman (shipping quote).")
data class ShippingQuoteDto(
    @Schema(description = "ID unik kutipan", example = "550e8400-e29b-41d4-a716-446655440012")
    val quoteId: UUID,
    @Schema(description = "Penyedia layanan (misal: Biteship)", example = "BITESHIP")
    val provider: String,
    @Schema(description = "Referensi eksternal dari penyedia")
    val providerReference: String? = null,
    @Schema(description = "Kode kurir", example = "jne")
    val courierCode: String,
    @Schema(description = "Nama kurir", example = "JNE")
    val courierName: String,
    @Schema(description = "Kode layanan", example = "reg")
    val serviceCode: String,
    @Schema(description = "Nama layanan", example = "Reguler")
    val serviceName: String,
    @Schema(description = "Deskripsi layanan", example = "Layanan reguler 2-3 hari")
    val description: String? = null,
    @Schema(description = "Biaya pengiriman")
    val cost: MoneyDto,
    @Schema(description = "Estimasi hari minimal", example = "2")
    val estimatedDaysMin: Int? = null,
    @Schema(description = "Estimasi hari maksimal", example = "3")
    val estimatedDaysMax: Int? = null,
    @Schema(description = "Apakah layanan ini direkomendasikan", example = "true")
    val isRecommended: Boolean = false,
)

@Schema(description = "Respons detail checkout.")
data class CheckoutResponse(
    @Schema(description = "Pesan status", example = "Checkout berhasil diperbarui.")
    val message: String,
    val data: CheckoutDto,
    val meta: ApiMeta? = null,
)

@Schema(description = "Permintaan untuk menetapkan alamat pengiriman checkout.")
data class CheckoutShippingAddressRequest(
    @Schema(description = "ID alamat yang tersimpan di buku alamat", example = "550e8400-e29b-41d4-a716-446655440011")
    val addressId: UUID? = null,
    @field:Valid
    @Schema(description = "Detail alamat baru (untuk guest atau alamat kustom)")
    val guestAddress: GuestAddressRequest? = null,
)

@Schema(description = "Permintaan detail alamat untuk guest.")
data class GuestAddressRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    @Schema(description = "Email penerima", example = "guest@example.com")
    val email: String,
    @field:NotBlank
    @field:Size(max = 120)
    @Schema(description = "Nama penerima", example = "Guest User")
    val recipientName: String,
    @field:NotBlank
    @field:Pattern(regexp = "^\\+?[0-9]{10,14}$")
    @Schema(description = "Nomor telepon penerima", example = "+628123456789")
    val phone: String,
    @field:NotBlank
    @field:Size(max = 200)
    @Schema(description = "Alamat baris 1", example = "Jl. Sudirman No. 1")
    val line1: String,
    @field:Size(max = 200)
    @Schema(description = "Alamat baris 2 (opsional)")
    val line2: String? = null,
    @field:Size(max = 200)
    @Schema(description = "Catatan pengiriman (opsional)")
    val notes: String? = null,
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
)

@Schema(description = "Permintaan untuk memilih opsi pengiriman.")
data class SelectShippingQuoteRequest(
    @Schema(description = "ID unik kutipan pengiriman yang dipilih", example = "550e8400-e29b-41d4-a716-446655440012")
    val quoteId: UUID,
)
