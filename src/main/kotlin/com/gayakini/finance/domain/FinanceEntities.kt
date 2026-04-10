package com.gayakini.finance.domain

import jakarta.persistence.*
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "finance_ledger_accounts", schema = "commerce")
class LedgerAccount(
    @Id
    val id: UUID,
    @Column(unique = true, nullable = false)
    val code: String,
    @Column(nullable = false)
    val name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: AccountType,
    @Column(columnDefinition = "TEXT")
    val description: String?,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

enum class AccountType { ASSET, REVENUE, LIABILITY, EXPENSE }

@Entity
@Table(name = "finance_ledger_entries", schema = "commerce")
class LedgerEntry(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: LedgerAccount,
    @Column(name = "transaction_id", nullable = false)
    val transactionId: UUID,
    @Column(name = "transaction_type", nullable = false)
    val transactionType: String,
    @Column(name = "reference_id")
    val referenceId: String?,
    @Column(name = "debit_amount", nullable = false)
    val debitAmount: Long = 0,
    @Column(name = "credit_amount", nullable = false)
    val creditAmount: Long = 0,
    @Column(columnDefinition = "TEXT")
    val description: String?,
    @Column(name = "posted_at", nullable = false)
    val postedAt: OffsetDateTime = OffsetDateTime.now(),
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    val metadata: Map<String, Any?>? = null,
)

@Entity
@Table(name = "finance_payout_destinations", schema = "commerce")
class PayoutDestination(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "bank_name", nullable = false)
    val bankName: String,
    @Column(name = "account_name", nullable = false)
    val accountName: String,
    @Column(name = "account_number", nullable = false)
    val accountNumber: String,
    val branch: String?,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "is_sandbox_mock", nullable = false)
    val isSandboxMock: Boolean = false,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "finance_withdrawal_requests", schema = "commerce")
class WithdrawalRequest(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_destination_id", nullable = false)
    val payoutDestination: PayoutDestination,
    @Column(nullable = false)
    val amount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WithdrawalStatus = WithdrawalStatus.PENDING,
    @Column(name = "requested_by", nullable = false)
    val requestedBy: UUID,
    @Column(name = "approved_by")
    var approvedBy: UUID? = null,
    @Column(name = "processed_by")
    var processedBy: UUID? = null,
    @Column(name = "requested_at", nullable = false)
    val requestedAt: Instant = Instant.now(),
    @Column(name = "approved_at")
    var approvedAt: Instant? = null,
    @Column(name = "processed_at")
    var processedAt: Instant? = null,
    @Column(name = "rejection_reason")
    var rejectionReason: String? = null,
    @Column(name = "admin_notes")
    var adminNotes: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

enum class WithdrawalStatus { PENDING, APPROVED, PROCESSED, REJECTED }
