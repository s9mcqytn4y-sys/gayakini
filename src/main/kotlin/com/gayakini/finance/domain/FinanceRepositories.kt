package com.gayakini.finance.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface LedgerAccountRepository : JpaRepository<LedgerAccount, UUID> {
    fun findByCode(code: String): Optional<LedgerAccount>
}

interface LedgerEntryRepository : JpaRepository<LedgerEntry, UUID> {
    fun findAllByTransactionId(transactionId: UUID): List<LedgerEntry>

    @Query(
        "SELECT COALESCE(SUM(e.debitAmount) - SUM(e.creditAmount), 0) " +
            "FROM LedgerEntry e WHERE e.account.id = :accountId",
    )
    fun getDebitBalance(accountId: UUID): Long

    @Query(
        "SELECT COALESCE(SUM(e.creditAmount) - SUM(e.debitAmount), 0) " +
            "FROM LedgerEntry e WHERE e.account.id = :accountId",
    )
    fun getCreditBalance(accountId: UUID): Long

    fun existsByTransactionIdAndTransactionType(
        transactionId: UUID,
        transactionType: String,
    ): Boolean
}

interface PayoutDestinationRepository : JpaRepository<PayoutDestination, UUID> {
    fun findAllByIsActiveTrue(): List<PayoutDestination>
}

interface WithdrawalRequestRepository : JpaRepository<WithdrawalRequest, UUID> {
    fun findAllByRequestedByOrderByRequestedAtDesc(requestedBy: UUID): List<WithdrawalRequest>
}
