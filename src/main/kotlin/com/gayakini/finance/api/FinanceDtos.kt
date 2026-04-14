package com.gayakini.finance.api

import com.gayakini.finance.domain.WithdrawalStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Respons saldo akun keuangan.")
data class BalanceResponse(
    val success: Boolean = true,
    @Schema(description = "Jumlah saldo tersedia", example = "5000000")
    val availableBalance: Long,
    @Schema(description = "Kode mata uang", example = "IDR")
    val currency: String = "IDR",
)

@Schema(description = "Permintaan penarikan dana.")
data class WithdrawalRequestInput(
    @Schema(description = "Jumlah dana yang ditarik", example = "1000000")
    val amount: Long,
    @Schema(description = "ID tujuan pencairan dana")
    val payoutDestinationId: UUID,
)

@Schema(description = "Informasi detail permintaan penarikan.")
data class WithdrawalResponse(
    val success: Boolean = true,
    @Schema(description = "ID unik penarikan")
    val id: UUID,
    @Schema(description = "Jumlah dana", example = "1000000")
    val amount: Long,
    @Schema(description = "Status penarikan", example = "REQUESTED")
    val status: WithdrawalStatus,
    @Schema(description = "Waktu permintaan diajukan")
    val requestedAt: Instant,
    val payoutDestination: PayoutDestinationDto,
    @Schema(description = "Catatan dari admin", example = "Disetujui untuk diproses")
    val adminNotes: String? = null,
)

@Schema(description = "Data rekening tujuan pencairan.")
data class PayoutDestinationDto(
    @Schema(description = "ID unik tujuan pencairan")
    val id: UUID,
    @Schema(description = "Nama Bank", example = "BCA")
    val bankName: String,
    @Schema(description = "Nama Pemilik Rekening", example = "Gayakini Store")
    val accountName: String,
    @Schema(description = "Nomor Rekening", example = "1234567890")
    val accountNumber: String,
    @Schema(description = "Cabang Bank", example = "KCP Sudirman")
    val branch: String?,
)

@Schema(description = "Permintaan persetujuan penarikan.")
data class ApproveWithdrawalRequest(
    @Schema(description = "Catatan persetujuan", example = "Dokumen lengkap")
    val notes: String?,
)

@Schema(description = "Entri riwayat buku besar keuangan.")
data class LedgerEntryDto(
    @Schema(description = "ID unik entri")
    val id: java.util.UUID,
    @Schema(description = "Kode akun buku besar", example = "CASH_IN_HAND")
    val accountCode: String,
    @Schema(description = "Tipe transaksi", example = "SALES")
    val transactionType: String,
    @Schema(description = "Referensi ID terkait (misal ID Pesanan)")
    val referenceId: String?,
    @Schema(description = "Nilai debit", example = "0")
    val debitAmount: Long,
    @Schema(description = "Nilai kredit", example = "150000")
    val creditAmount: Long,
    @Schema(description = "Deskripsi transaksi", example = "Penjualan produk ORD-123")
    val description: String?,
    @Schema(description = "Waktu pencatatan")
    val postedAt: java.time.OffsetDateTime,
)
