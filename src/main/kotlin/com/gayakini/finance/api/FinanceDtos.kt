package com.gayakini.finance.api

import com.gayakini.finance.domain.WithdrawalStatus
import java.time.Instant
import java.util.UUID

data class BalanceResponse(
    val availableBalance: Long,
    val currency: String = "IDR",
)

data class WithdrawalRequestInput(
    val amount: Long,
    val payoutDestinationId: UUID,
)

data class WithdrawalResponse(
    val id: UUID,
    val amount: Long,
    val status: WithdrawalStatus,
    val requestedAt: Instant,
    val payoutDestination: PayoutDestinationDto,
    val adminNotes: String? = null,
)

data class PayoutDestinationDto(
    val id: UUID,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val branch: String?,
)

data class ApproveWithdrawalRequest(
    val notes: String?,
)

data class LedgerEntryDto(
    val id: java.util.UUID,
    val accountCode: String,
    val transactionType: String,
    val referenceId: String?,
    val debitAmount: Long,
    val creditAmount: Long,
    val description: String?,
    val postedAt: java.time.OffsetDateTime,
)
