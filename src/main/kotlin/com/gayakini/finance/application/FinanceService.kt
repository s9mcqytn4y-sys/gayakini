package com.gayakini.finance.application

import com.gayakini.finance.domain.LedgerAccountRepository
import com.gayakini.finance.domain.LedgerEntry
import com.gayakini.finance.domain.LedgerEntryRepository
import com.gayakini.finance.domain.PayoutDestination
import com.gayakini.finance.domain.PayoutDestinationRepository
import com.gayakini.finance.domain.WithdrawalRequest
import com.gayakini.finance.domain.WithdrawalRequestRepository
import com.gayakini.finance.domain.WithdrawalStatus
import com.gayakini.infrastructure.security.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class FinanceService(
    private val accountRepository: LedgerAccountRepository,
    private val entryRepository: LedgerEntryRepository,
    private val payoutDestinationRepository: PayoutDestinationRepository,
    private val withdrawalRepository: WithdrawalRequestRepository,
) {
    private val logger = LoggerFactory.getLogger(FinanceService::class.java)

    companion object {
        const val ACC_GATEWAY = "ASSET_GATEWAY"
        const val ACC_REVENUE = "REVENUE_SALES"
        const val ACC_LIABILITY = "LIABILITY_PAYOUT"
    }

    @Transactional
    fun recordPaymentSettlement(
        transactionId: UUID,
        orderNumber: String,
        amount: Long,
        metadata: Map<String, Any?>? = null,
    ) {
        if (entryRepository.existsByTransactionIdAndTransactionType(transactionId, "PAYMENT_SETTLEMENT")) {
            logger.info("Payment settlement already posted for transaction: {}", transactionId)
            return
        }

        val gatewayAcc = accountRepository.findByCode(ACC_GATEWAY).orElseThrow()
        val revenueAcc = accountRepository.findByCode(ACC_REVENUE).orElseThrow()

        // Double-entry: Debit Gateway (Asset up), Credit Revenue (Revenue up)
        entryRepository.save(
            LedgerEntry(
                account = gatewayAcc,
                transactionId = transactionId,
                transactionType = "PAYMENT_SETTLEMENT",
                referenceId = orderNumber,
                debitAmount = amount,
                description = "Settlement for order $orderNumber",
                metadata = metadata,
            ),
        )

        entryRepository.save(
            LedgerEntry(
                account = revenueAcc,
                transactionId = transactionId,
                transactionType = "PAYMENT_SETTLEMENT",
                referenceId = orderNumber,
                creditAmount = amount,
                description = "Revenue from order $orderNumber",
                metadata = metadata,
            ),
        )

        logger.info("Posted finance ledger for order settlement: {} (Amount: {})", orderNumber, amount)
    }

    @Transactional(readOnly = true)
    fun getAvailableBalance(): Long {
        val gatewayAcc =
            accountRepository.findByCode(ACC_GATEWAY)
                .orElseThrow { IllegalStateException("Account $ACC_GATEWAY not found") }
        val liabilityAcc =
            accountRepository.findByCode(ACC_LIABILITY)
                .orElseThrow { IllegalStateException("Account $ACC_LIABILITY not found") }

        // Available Balance = Gateway Assets (Debit Balance) - Payout Liabilities (Credit Balance)
        // This ensures we don't over-withdraw when funds are committed to pending payouts.
        val assets = entryRepository.getDebitBalance(gatewayAcc.id)
        val liabilities = entryRepository.getCreditBalance(liabilityAcc.id)

        val balance = assets - liabilities
        logger.debug("Available balance calculation: Assets({}) - Liabilities({}) = {}", assets, liabilities, balance)
        return balance
    }

    @Transactional
    fun requestWithdrawal(
        amount: Long,
        destinationId: UUID,
    ): WithdrawalRequest {
        val balance = getAvailableBalance()
        require(amount > 0) { "Jumlah penarikan harus lebih besar dari nol." }
        require(balance >= amount) { "Saldo tidak mencukupi. Tersedia: $balance" }

        val destination =
            payoutDestinationRepository.findById(destinationId)
                .orElseThrow { NoSuchElementException("Tujuan pencairan tidak ditemukan.") }

        val currentUserId = SecurityUtils.getCurrentUserId() ?: throw AccessDeniedException("Sesi tidak valid.")

        val request =
            WithdrawalRequest(
                payoutDestination = destination,
                amount = amount,
                requestedBy = currentUserId,
            )
        val savedRequest = withdrawalRepository.save(request)

        // Commit balance to liability
        postWithdrawalCommit(savedRequest)

        return savedRequest
    }

    private fun postWithdrawalCommit(request: WithdrawalRequest) {
        if (entryRepository.existsByTransactionIdAndTransactionType(request.id, "WITHDRAWAL_COMMIT")) {
            logger.info("Withdrawal commit already posted for request: {}", request.id)
            return
        }

        val revenueAcc =
            accountRepository.findByCode(ACC_REVENUE)
                .orElseThrow { IllegalStateException("Account $ACC_REVENUE not found") }
        val liabilityAcc =
            accountRepository.findByCode(ACC_LIABILITY)
                .orElseThrow { IllegalStateException("Account $ACC_LIABILITY not found") }

        // Debit Revenue (Revenue down), Credit Liability (Liability up)
        entryRepository.save(
            LedgerEntry(
                account = revenueAcc,
                transactionId = request.id,
                transactionType = "WITHDRAWAL_COMMIT",
                referenceId = request.id.toString(),
                debitAmount = request.amount,
                description = "Withdrawal request committed: ${request.id}",
            ),
        )

        entryRepository.save(
            LedgerEntry(
                account = liabilityAcc,
                transactionId = request.id,
                transactionType = "WITHDRAWAL_COMMIT",
                referenceId = request.id.toString(),
                creditAmount = request.amount,
                description = "Pending payout liability: ${request.id}",
            ),
        )
        logger.info("Posted withdrawal commit ledger for request: {}", request.id)
    }

    @Transactional
    fun approveWithdrawal(
        id: UUID,
        notes: String?,
    ): WithdrawalRequest {
        val request = withdrawalRepository.findById(id).orElseThrow()
        require(request.status == WithdrawalStatus.PENDING) { "Status request tidak valid untuk persetujuan." }

        val adminId = SecurityUtils.getCurrentUserId() ?: throw AccessDeniedException("Sesi tidak valid.")

        request.status = WithdrawalStatus.APPROVED
        request.approvedBy = adminId
        request.approvedAt = Instant.now()
        request.adminNotes = notes
        request.updatedAt = Instant.now()

        return withdrawalRepository.save(request)
    }

    @Transactional
    fun processWithdrawal(id: UUID): WithdrawalRequest {
        val request = withdrawalRepository.findById(id).orElseThrow()
        require(
            request.status == WithdrawalStatus.APPROVED,
        ) { "Hanya request yang sudah disetujui yang dapat diproses." }

        val adminId = SecurityUtils.getCurrentUserId() ?: throw AccessDeniedException("Sesi tidak valid.")

        request.status = WithdrawalStatus.PROCESSED
        request.processedBy = adminId
        request.processedAt = Instant.now()
        request.updatedAt = Instant.now()

        // Finalize ledger: Clear liability
        postWithdrawalFinalize(request)

        return withdrawalRepository.save(request)
    }

    private fun postWithdrawalFinalize(request: WithdrawalRequest) {
        if (entryRepository.existsByTransactionIdAndTransactionType(request.id, "WITHDRAWAL_FINALIZE")) {
            logger.info("Withdrawal finalize already posted for request: {}", request.id)
            return
        }

        val liabilityAcc =
            accountRepository.findByCode(ACC_LIABILITY)
                .orElseThrow { IllegalStateException("Account $ACC_LIABILITY not found") }
        val gatewayAcc =
            accountRepository.findByCode(ACC_GATEWAY)
                .orElseThrow { IllegalStateException("Account $ACC_GATEWAY not found") }

        // Debit Liability (Liability down), Credit Gateway (Asset down)
        entryRepository.save(
            LedgerEntry(
                account = liabilityAcc,
                transactionId = request.id,
                transactionType = "WITHDRAWAL_FINALIZE",
                referenceId = request.id.toString(),
                debitAmount = request.amount,
                description = "Withdrawal liability settled: ${request.id}",
            ),
        )

        entryRepository.save(
            LedgerEntry(
                account = gatewayAcc,
                transactionId = request.id,
                transactionType = "WITHDRAWAL_FINALIZE",
                referenceId = request.id.toString(),
                creditAmount = request.amount,
                description = "Funds transferred from gateway: ${request.id}",
            ),
        )
        logger.info("Posted withdrawal finalize ledger for request: {}", request.id)
    }

    fun listWithdrawals(): List<WithdrawalRequest> = withdrawalRepository.findAll()

    fun listLedgerEntries(): List<LedgerEntry> = entryRepository.findAll()

    fun getPayoutDestinations(): List<PayoutDestination> = payoutDestinationRepository.findAllByIsActiveTrue()
}
