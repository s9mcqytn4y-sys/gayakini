package com.gayakini.finance.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.StandardResponse
import com.gayakini.finance.application.FinanceService
import com.gayakini.finance.domain.WithdrawalRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/v1/admin/finance")
class AdminFinanceController(
    private val financeService: FinanceService,
) {
    @GetMapping("/balance")
    @PreAuthorize("hasRole('ADMIN')")
    fun getBalance(): StandardResponse<BalanceResponse> {
        return StandardResponse(
            message = "Saldo berhasil diambil.",
            data = BalanceResponse(availableBalance = financeService.getAvailableBalance()),
            meta = ApiMeta(),
        )
    }

    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    fun listWithdrawals(): StandardResponse<List<WithdrawalResponse>> {
        return StandardResponse(
            message = "Daftar penarikan berhasil diambil.",
            data = financeService.listWithdrawals().map { mapToResponse(it) },
            meta = ApiMeta(),
        )
    }

    @PostMapping("/withdrawals/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    fun approveWithdrawal(
        @PathVariable id: UUID,
        @RequestBody request: ApproveWithdrawalRequest,
    ): StandardResponse<WithdrawalResponse> {
        val approved = financeService.approveWithdrawal(id, request.notes)
        return StandardResponse(
            message = "Penarikan disetujui.",
            data = mapToResponse(approved),
            meta = ApiMeta(),
        )
    }

    @PostMapping("/withdrawals/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    fun processWithdrawal(
        @PathVariable id: UUID,
    ): StandardResponse<WithdrawalResponse> {
        val processed = financeService.processWithdrawal(id)
        return StandardResponse(
            message = "Penarikan berhasil diproses (Sandbox Mock).",
            data = mapToResponse(processed),
            meta = ApiMeta(),
        )
    }

    @GetMapping("/payout-destinations")
    @PreAuthorize("hasRole('ADMIN')")
    fun getPayoutDestinations(): StandardResponse<List<PayoutDestinationDto>> {
        return StandardResponse(
            message = "Daftar tujuan pencairan berhasil diambil.",
            data =
                financeService.getPayoutDestinations().map {
                    PayoutDestinationDto(
                        id = it.id,
                        bankName = it.bankName,
                        accountName = it.accountName,
                        accountNumber = it.accountNumber,
                        branch = it.branch,
                    )
                },
            meta = ApiMeta(),
        )
    }

    @PostMapping("/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    fun createWithdrawal(
        @RequestBody request: WithdrawalRequestInput,
    ): StandardResponse<WithdrawalResponse> {
        val created = financeService.requestWithdrawal(request.amount, request.payoutDestinationId)
        return StandardResponse(
            message = "Permintaan penarikan berhasil dibuat.",
            data = mapToResponse(created),
            meta = ApiMeta(),
        )
    }

    @GetMapping("/ledger-entries")
    @PreAuthorize("hasRole('ADMIN')")
    fun listLedgerEntries(): StandardResponse<List<LedgerEntryDto>> {
        return StandardResponse(
            message = "Daftar entri buku besar berhasil diambil.",
            data =
                financeService.listLedgerEntries().map {
                    LedgerEntryDto(
                        id = it.id,
                        accountCode = it.account.code,
                        transactionType = it.transactionType,
                        referenceId = it.referenceId,
                        debitAmount = it.debitAmount,
                        creditAmount = it.creditAmount,
                        description = it.description,
                        postedAt = it.postedAt,
                    )
                },
            meta = ApiMeta(),
        )
    }

    private fun mapToResponse(req: WithdrawalRequest) =
        WithdrawalResponse(
            id = req.id,
            amount = req.amount,
            status = req.status,
            requestedAt = req.requestedAt,
            adminNotes = req.adminNotes,
            payoutDestination =
                PayoutDestinationDto(
                    id = req.payoutDestination.id,
                    bankName = req.payoutDestination.bankName,
                    accountName = req.payoutDestination.accountName,
                    accountNumber = req.payoutDestination.accountNumber,
                    branch = req.payoutDestination.branch,
                ),
        )
}
