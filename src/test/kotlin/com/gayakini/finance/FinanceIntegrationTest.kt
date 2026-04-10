package com.gayakini.finance

import com.gayakini.finance.application.FinanceService
import com.gayakini.finance.domain.*
import com.gayakini.infrastructure.security.SecurityUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinanceIntegrationTest {
    @Autowired
    private lateinit var financeService: FinanceService

    @Autowired
    private lateinit var accountRepository: LedgerAccountRepository

    @Autowired
    private lateinit var entryRepository: LedgerEntryRepository

    @Autowired
    private lateinit var destinationRepository: PayoutDestinationRepository

    @Autowired
    private lateinit var withdrawalRepository: WithdrawalRequestRepository

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        mockkObject(SecurityUtils)
        every { SecurityUtils.getCurrentUserId() } returns userId

        // Seed accounts if they don't exist (H2 mem db is empty)
        if (accountRepository.findByCode(FinanceService.ACC_GATEWAY).isEmpty) {
            accountRepository.saveAll(
                listOf(
                    LedgerAccount(UUID.randomUUID(), FinanceService.ACC_GATEWAY, "Gateway", AccountType.ASSET, null),
                    LedgerAccount(UUID.randomUUID(), FinanceService.ACC_REVENUE, "Revenue", AccountType.REVENUE, null),
                    LedgerAccount(
                        UUID.randomUUID(),
                        FinanceService.ACC_LIABILITY,
                        "Liability",
                        AccountType.LIABILITY,
                        null,
                    ),
                ),
            )
        }

        // Seed payout destination
        if (destinationRepository.findAll().isEmpty()) {
            destinationRepository.save(
                PayoutDestination(
                    bankName = "BCA",
                    accountName = "Test User",
                    accountNumber = "1234567890",
                    branch = "Jakarta",
                ),
            )
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SecurityUtils)
    }

    @Test
    fun `recordPaymentSettlement - should create balanced double entries`() {
        val transactionId = UUID.randomUUID()
        val orderNumber = "ORD-TEST-001"
        val amount = 150000L

        financeService.recordPaymentSettlement(transactionId, orderNumber, amount)

        val entries = entryRepository.findAllByTransactionId(transactionId)
        assertEquals(2, entries.size, "Should have exactly 2 entries for a double-entry transaction")

        val debitTotal = entries.sumOf { it.debitAmount }
        val creditTotal = entries.sumOf { it.creditAmount }
        assertEquals(amount, debitTotal)
        assertEquals(amount, creditTotal)

        val gatewayAcc = accountRepository.findByCode(FinanceService.ACC_GATEWAY).get()
        val revenueAcc = accountRepository.findByCode(FinanceService.ACC_REVENUE).get()

        assertTrue(
            entries.any {
                it.account.id == gatewayAcc.id && it.debitAmount == amount
            },
            "Gateway account should be debited",
        )
        assertTrue(
            entries.any {
                it.account.id == revenueAcc.id && it.creditAmount == amount
            },
            "Revenue account should be credited",
        )
    }

    @Test
    fun `recordPaymentSettlement - should be idempotent`() {
        val transactionId = UUID.randomUUID()
        val orderNumber = "ORD-TEST-002"
        val amount = 100000L

        financeService.recordPaymentSettlement(transactionId, orderNumber, amount)
        val countAfterFirst = entryRepository.findAllByTransactionId(transactionId).size

        // Second call with same transaction ID
        financeService.recordPaymentSettlement(transactionId, orderNumber, amount)
        val countAfterSecond = entryRepository.findAllByTransactionId(transactionId).size

        assertEquals(countAfterFirst, countAfterSecond, "Second call should not create new entries")
    }

    @Test
    fun `getAvailableBalance - should derive from Asset account`() {
        val initialBalance = financeService.getAvailableBalance()

        val amount = 50000L
        financeService.recordPaymentSettlement(UUID.randomUUID(), "ORD-BAL-1", amount)

        val newBalance = financeService.getAvailableBalance()
        assertEquals(initialBalance + amount, newBalance)
    }

    @Test
    fun `withdrawal workflow - should track funds correctly`() {
        // 1. Seed balance
        financeService.recordPaymentSettlement(UUID.randomUUID(), "ORD-WITHDRAW-1", 200000L)
        val initialBalance = financeService.getAvailableBalance()

        // 2. Request withdrawal
        val destination = destinationRepository.findAll().first()
        val request = financeService.requestWithdrawal(150000L, destination.id)

        // Check balance reduced
        val balanceAfterCommit = financeService.getAvailableBalance()
        assertEquals(initialBalance - 150000L, balanceAfterCommit)

        // Check entries
        val commitEntries =
            entryRepository.findAllByTransactionId(request.id)
                .filter { it.transactionType == "WITHDRAWAL_COMMIT" }
        assertEquals(2, commitEntries.size)
        assertEquals(150000L, commitEntries.sumOf { it.debitAmount })
        assertEquals(150000L, commitEntries.sumOf { it.creditAmount })

        // 3. Approve and Process
        financeService.approveWithdrawal(request.id, "Approved for testing")
        financeService.processWithdrawal(request.id)

        val finalizedRequest = withdrawalRepository.findById(request.id).get()
        assertEquals(WithdrawalStatus.PROCESSED, finalizedRequest.status)

        // Check balance still same (already reduced on commit)
        assertEquals(balanceAfterCommit, financeService.getAvailableBalance())
    }
}
