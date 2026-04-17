package com.gayakini.finance.api

import com.gayakini.common.api.ApiMeta
import com.gayakini.common.api.StandardResponse
import com.gayakini.finance.application.FinanceService
import com.gayakini.finance.domain.WithdrawalRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
@Tag(
    name = "Admin Finance",
    description = "Financial management, balances, and withdrawals for administrators (Internal/English).",
)
class AdminFinanceController(
    private val financeService: FinanceService,
) {
    @GetMapping("/balance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get balance", description = "View total balance available for withdrawal.")
    fun getBalance(): StandardResponse<BalanceResponse> {
        return StandardResponse(
            message = "Balance retrieved successfully.",
            data = BalanceResponse(availableBalance = financeService.getAvailableBalance()),
            meta = ApiMeta(),
        )
    }

    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List withdrawals", description = "View history of all withdrawal requests.")
    fun listWithdrawals(): StandardResponse<List<WithdrawalResponse>> {
        return StandardResponse(
            message = "Withdrawal list retrieved successfully.",
            data = financeService.listWithdrawals().map { mapToResponse(it) },
            meta = ApiMeta(),
        )
    }

    @PostMapping("/withdrawals/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve withdrawal", description = "Approve a withdrawal request.")
    fun approveWithdrawal(
        @Parameter(description = "Withdrawal UUID") @PathVariable id: UUID,
        @Valid @RequestBody request: ApproveWithdrawalRequest,
    ): StandardResponse<WithdrawalResponse> {
        val approved = financeService.approveWithdrawal(id, request.notes)
        return StandardResponse(
            message = "Withdrawal approved.",
            data = mapToResponse(approved),
            meta = ApiMeta(),
        )
    }

    @PostMapping("/withdrawals/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process withdrawal", description = "Mark withdrawal as processed/disbursed.")
    fun processWithdrawal(
        @Parameter(description = "Withdrawal UUID") @PathVariable id: UUID,
    ): StandardResponse<WithdrawalResponse> {
        val processed = financeService.processWithdrawal(id)
        return StandardResponse(
            message = "Withdrawal processed successfully (Sandbox Mock).",
            data = mapToResponse(processed),
            meta = ApiMeta(),
        )
    }

    @GetMapping("/payout-destinations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "List payout destinations",
        description = "View list of registered bank accounts for withdrawals.",
    )
    fun getPayoutDestinations(): StandardResponse<List<PayoutDestinationDto>> {
        return StandardResponse(
            message = "Payout destinations retrieved successfully.",
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
    @Operation(
        summary = "Create withdrawal request",
        description = "Submit a request to withdraw funds to a specific account.",
    )
    fun createWithdrawal(
        @Valid @RequestBody request: WithdrawalRequestInput,
    ): StandardResponse<WithdrawalResponse> {
        val created = financeService.requestWithdrawal(request.amount, request.payoutDestinationId)
        return StandardResponse(
            message = "Withdrawal request created successfully.",
            data = mapToResponse(created),
            meta = ApiMeta(),
        )
    }

    @GetMapping("/ledger-entries")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List ledger entries", description = "View all financial transactions in the ledger.")
    fun listLedgerEntries(): StandardResponse<List<LedgerEntryDto>> {
        return StandardResponse(
            message = "Ledger entries retrieved successfully.",
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
