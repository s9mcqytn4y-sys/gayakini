package com.gayakini.order.api

import com.gayakini.cart.api.ProductVariantAttributeDto
import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.MoneyDto
import com.gayakini.common.api.PageMeta
import com.gayakini.order.domain.FulfillmentStatus
import com.gayakini.order.domain.OrderStatus
import com.gayakini.order.domain.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "Permintaan untuk membuat pesanan.")
data class PlaceOrderRequest(
    @field:Schema(description = "Catatan dari pelanggan", example = "Tolong dipacking aman ya")
    @field:Size(max = 500)
    val customerNotes: String? = null,
)

@Schema(description = "Informasi lengkap pesanan.")
data class OrderDto(
    @Schema(description = "ID unik pesanan", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: UUID,
    @Schema(description = "Nomor pesanan yang mudah dibaca", example = "ORD-20240412-ABCDE")
    val orderNumber: String,
    @Schema(description = "Token akses untuk guest (jika ada)", example = "gk_ot_12345")
    val accessToken: String? = null,
    @Schema(description = "ID customer (jika bukan guest)", example = "550e8400-e29b-41d4-a716-446655440001")
    val customerId: UUID?,
    @Schema(description = "Status utama pesanan", example = "OPEN")
    val status: OrderStatus,
    @Schema(description = "Status pemenuhan/pengiriman", example = "UNFULFILLED")
    val fulfillmentStatus: FulfillmentStatus,
    val paymentSummary: OrderPaymentSummaryDto,
    val shippingAddress: OrderAddressDto,
    val selectedShippingQuote: OrderShippingQuoteDto? = null,
    val shipment: OrderShipmentDto? = null,
    val items: List<OrderItemDto>,
    val subtotal: MoneyDto,
    val shippingCost: MoneyDto,
    val total: MoneyDto,
    @Schema(description = "Mata uang pesanan", example = "IDR")
    val currency: String,
    @Schema(description = "Catatan dari pelanggan", example = "Tolong dipacking aman ya")
    val customerNotes: String? = null,
    @Schema(description = "Waktu pesanan dibuat", example = "2024-04-12T10:00:00Z")
    val createdAt: Instant,
    @Schema(description = "Waktu pesanan dibayar", example = "2024-04-12T10:05:00Z")
    val paidAt: Instant? = null,
    @Schema(description = "Waktu pesanan dibatalkan", example = "2024-04-12T11:00:00Z")
    val cancelledAt: Instant? = null,
)

@Schema(description = "Ringkasan status pembayaran pesanan.")
data class OrderPaymentSummaryDto(
    @Schema(description = "Penyedia pembayaran", example = "midtrans")
    val provider: String,
    @Schema(description = "Status pembayaran", example = "PENDING")
    val status: PaymentStatus,
    @Schema(description = "Alur pembayaran yang digunakan", example = "gopay")
    val flow: String? = null,
    @Schema(description = "ID pesanan di sisi penyedia", example = "mid-12345")
    val providerOrderId: String? = null,
    @Schema(description = "ID transaksi di sisi penyedia", example = "tx-67890")
    val providerTransactionId: String? = null,
    @Schema(description = "Waktu pembayaran berhasil", example = "2024-04-12T10:05:00Z")
    val paidAt: Instant? = null,
    @Schema(description = "Jumlah kotor yang dibayarkan")
    val grossAmount: MoneyDto? = null,
    @Schema(description = "Status mentah dari penyedia", example = "settlement")
    val providerStatus: String? = null,
)

@Schema(description = "Informasi alamat pengiriman pesanan.")
data class OrderAddressDto(
    @Schema(description = "ID alamat (jika tersimpan)", example = "550e8400-e29b-41d4-a716-446655440003")
    val id: UUID? = null,
    @Schema(description = "Nama penerima", example = "Budi Santoso")
    val recipientName: String,
    @Schema(description = "Nomor telepon penerima", example = "081234567890")
    val phone: String,
    @Schema(description = "Email penerima", example = "budi@example.com")
    val email: String? = null,
    @Schema(description = "Alamat baris 1", example = "Jl. Merdeka No. 123")
    val line1: String,
    @Schema(description = "Alamat baris 2", example = "Blok C, No. 45")
    val line2: String? = null,
    @Schema(description = "Catatan alamat", example = "Pagar warna hitam")
    val notes: String? = null,
    @Schema(description = "ID area Biteship", example = "ID_12345")
    val areaId: String,
    @Schema(description = "Kecamatan", example = "Kebayoran Baru")
    val district: String,
    @Schema(description = "Kota/Kabupaten", example = "Jakarta Selatan")
    val city: String,
    @Schema(description = "Provinsi", example = "DKI Jakarta")
    val province: String,
    @Schema(description = "Kode Pos", example = "12110")
    val postalCode: String,
    @Schema(description = "Kode Negara (ISO 3166-1 alpha-2)", example = "ID")
    val countryCode: String,
)

@Schema(description = "Informasi kutipan pengiriman yang dipilih.")
data class OrderShippingQuoteDto(
    @Schema(description = "ID kutipan pengiriman", example = "550e8400-e29b-41d4-a716-446655440004")
    val quoteId: UUID,
    @Schema(description = "Penyedia pengiriman", example = "biteship")
    val provider: String,
    @Schema(description = "Kode kurir", example = "jne")
    val courierCode: String,
    @Schema(description = "Nama kurir", example = "JNE")
    val courierName: String,
    @Schema(description = "Kode layanan kurir", example = "reg")
    val serviceCode: String,
    @Schema(description = "Nama layanan kurir", example = "REG")
    val serviceName: String,
    val cost: MoneyDto,
)

@Schema(description = "Informasi pengiriman pesanan.")
data class OrderShipmentDto(
    @Schema(description = "ID pengiriman", example = "550e8400-e29b-41d4-a716-446655440005")
    val shipmentId: UUID,
    @Schema(description = "Penyedia pengiriman", example = "biteship")
    val provider: String,
    @Schema(description = "ID pengiriman di sisi penyedia", example = "ship-12345")
    val providerShipmentId: String? = null,
    @Schema(description = "Kode kurir", example = "jne")
    val courierCode: String? = null,
    @Schema(description = "Nama kurir", example = "JNE")
    val courierName: String? = null,
    @Schema(description = "Kode layanan kurir", example = "reg")
    val serviceCode: String? = null,
    @Schema(description = "Nama layanan kurir", example = "REG")
    val serviceName: String? = null,
    @Schema(description = "Nomor resi", example = "1234567890123")
    val trackingNumber: String? = null,
    @Schema(description = "URL pelacakan resi", example = "https://jne.co.id/track/123")
    val trackingUrl: String? = null,
    @Schema(description = "Status pengiriman", example = "PICKUP")
    val status: FulfillmentStatus,
    @Schema(description = "Waktu pemesanan pengiriman", example = "2024-04-12T12:00:00Z")
    val bookedAt: Instant? = null,
    @Schema(description = "Waktu barang dikirim (oleh kurir)", example = "2024-04-12T15:00:00Z")
    val shippedAt: Instant? = null,
    @Schema(description = "Waktu barang sampai tujuan", example = "2024-04-14T10:00:00Z")
    val deliveredAt: Instant? = null,
)

@Schema(description = "Informasi item dalam pesanan.")
data class OrderItemDto(
    @Schema(description = "ID item pesanan", example = "550e8400-e29b-41d4-a716-446655440006")
    val id: UUID,
    @Schema(description = "ID produk", example = "550e8400-e29b-41d4-a716-446655440007")
    val productId: UUID,
    @Schema(description = "ID varian", example = "550e8400-e29b-41d4-a716-446655440008")
    val variantId: UUID,
    @Schema(description = "Snapshot SKU saat pesanan dibuat", example = "GK-KPS-BLK-M")
    val skuSnapshot: String,
    @Schema(description = "Snapshot judul produk saat pesanan dibuat", example = "Kaos Polos Hitam")
    val titleSnapshot: String,
    @Schema(description = "Snapshot atribut varian")
    val attributesSnapshot: List<ProductVariantAttributeDto> = listOf(),
    @Schema(description = "Jumlah item", example = "2")
    val quantity: Int,
    val unitPrice: MoneyDto,
    val lineTotal: MoneyDto,
)

@Schema(description = "Respons detail pesanan.")
data class OrderResponse(
    @Schema(description = "Indikator keberhasilan", example = "true")
    val success: Boolean = true,
    @Schema(description = "Pesan status", example = "Pesanan berhasil dibuat.")
    val message: String,
    val data: OrderDto,
    val meta: ApiMeta? = null,
)

@Schema(description = "Respons daftar pesanan (paginasi).")
data class OrderPageResponse(
    @Schema(description = "Indikator keberhasilan", example = "true")
    val success: Boolean = true,
    @Schema(description = "Pesan status", example = "Daftar pesanan berhasil diambil.")
    val message: String,
    val data: List<OrderDto>,
    val meta: PageMeta,
)

@Schema(description = "Permintaan pembatalan pesanan oleh customer.")
data class CancelOrderRequest(
    @field:Schema(description = "Alasan pembatalan", example = "Ingin mengubah pesanan")
    @field:Size(max = 300)
    val reason: String? = null,
)

@Schema(description = "Permintaan pembatalan pesanan oleh admin.")
data class AdminCancelOrderRequest(
    @field:Schema(description = "Alasan pembatalan oleh admin", example = "Stok habis")
    @field:Size(max = 300)
    val reason: String? = null,
)

@Schema(description = "Permintaan pembuatan pengiriman oleh admin.")
data class AdminCreateShipmentRequest(
    @field:Schema(description = "Catatan pengiriman", example = "Siap kirim sore ini")
    @field:Size(max = 300)
    val note: String? = null,
)
