package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SmartVaultRepository
import com.example.data.backup.LocalBackupManager
import com.example.data.calculator.FinancialSummaryCalculator
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.DebtEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.NetWorthSnapshotEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransferEntity
import com.example.data.entity.VaultEntity
import com.example.data.insights.FinancialInsightEngine
import com.example.data.preferences.ModuleConfiguration
import com.example.data.preferences.ModulePreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhaseEVerificationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: SmartVaultRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SmartVaultRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testNetWorthCalculation_formulaAndComponents() {
        // Vaults: 20,000
        val vaults = listOf(
            VaultEntity(id = 1, name = "الرئيسية", balance = 15000.0),
            VaultEntity(id = 2, name = "خزنة إضافية", balance = 5000.0)
        )
        // Cash savings: 10,000
        val cashSavings = listOf(
            CashSavingEntity(id = 1, amount = 10000.0, currency = "ج.م")
        )
        // Gold: 10g 21k @ 4,000 = 40,000
        val goldAssets = listOf(
            GoldAssetEntity(id = 1, name = "سبيكة", karat = 21, weight = 10.0, purchasePrice = 38000.0, status = "ACTIVE")
        )
        val goldPriceMap = mapOf(21 to 4000.0)

        // Debts:
        // Owed to me (Asset): 5,000
        // I owe (Liability): 7,000
        val debts = listOf(
            DebtEntity(id = 1, personName = "أحمد", type = "OWED_TO_ME", originalAmount = 5000.0, remainingAmount = 5000.0, status = "ACTIVE"),
            DebtEntity(id = 2, personName = "محمود", type = "I_OWE", originalAmount = 7000.0, remainingAmount = 7000.0, status = "ACTIVE")
        )

        val breakdown = FinancialSummaryCalculator.getFullNetWorthBreakdown(
            vaults = vaults,
            cashSavings = cashSavings,
            goldAssets = goldAssets,
            goldPriceMap = goldPriceMap,
            debts = debts
        )

        // Assets = 20,000 (vaults) + 10,000 (cash) + 40,000 (gold) + 5,000 (owed to me) = 75,000
        assertEquals(75000.0, breakdown.totalAssets, 0.001)

        // Liabilities = 7,000 (I owe)
        assertEquals(7000.0, breakdown.totalLiabilities, 0.001)

        // Net Worth = Assets - Liabilities = 68,000
        assertEquals(68000.0, breakdown.netWorth, 0.001)

        // Percentages
        assertTrue(breakdown.assetsPercentage > 90.0)
        assertTrue(breakdown.liabilitiesPercentage < 10.0)
    }

    @Test
    fun testTransferBetweenVaults_atomicUpdateAndNonFinancialImpact() = runBlocking {
        val user = "user_transfer_test"

        // Insert initial vaults
        db.vaultDao().insertVault(VaultEntity(name = "الخزنة الرئيسية", balance = 10000.0, userId = user))
        db.vaultDao().insertVault(VaultEntity(name = "خزنة المصروف", balance = 2000.0, userId = user))

        // Execute transfer 3000 EGP
        val transferResult = repository.executeTransfer(
            fromVaultName = "الخزنة الرئيسية",
            toVaultName = "خزنة المصروف",
            amount = 3000.0,
            notes = "تمويل مصاريف الأسبوع",
            userId = user
        )

        assertTrue(transferResult.isSuccess)

        // Verify vault balances updated
        val v1 = db.vaultDao().getVaultByName("الخزنة الرئيسية", user)
        val v2 = db.vaultDao().getVaultByName("خزنة المصروف", user)
        assertNotNull(v1)
        assertNotNull(v2)
        assertEquals(7000.0, v1!!.balance, 0.001)
        assertEquals(5000.0, v2!!.balance, 0.001)

        // Verify transfer recorded
        val transfers = db.transferDao().getTransfersForUser(user).first()
        assertEquals(1, transfers.size)
        assertEquals(3000.0, transfers[0].amount, 0.001)
        assertEquals("الخزنة الرئيسية", transfers[0].fromVaultName)
        assertEquals("خزنة المصروف", transfers[0].toVaultName)

        // Crucial Check: Transfers must NOT appear as Income or Expense
        val txs = db.transactionDao().getTransactionsForUser(user).first()
        assertEquals(0, txs.size) // No transactions added
        val income = FinancialSummaryCalculator.calculateIncome(txs)
        val expense = FinancialSummaryCalculator.calculateExpenses(txs)
        assertEquals(0.0, income, 0.001)
        assertEquals(0.0, expense, 0.001)
    }

    @Test
    fun testTransferValidation_preventsInvalidOperations() = runBlocking {
        val user = "user_invalid_transfer"
        db.vaultDao().insertVault(VaultEntity(name = "خزنة A", balance = 500.0, userId = user))
        db.vaultDao().insertVault(VaultEntity(name = "خزنة B", balance = 1000.0, userId = user))

        // 1. Same vault transfer
        val selfTransfer = repository.executeTransfer(
            fromVaultName = "خزنة A",
            toVaultName = "خزنة A",
            amount = 100.0,
            userId = user
        )
        assertTrue(selfTransfer.isFailure)

        // 2. Zero or negative amount
        val zeroTransfer = repository.executeTransfer(
            fromVaultName = "خزنة A",
            toVaultName = "خزنة B",
            amount = 0.0,
            userId = user
        )
        assertTrue(zeroTransfer.isFailure)

        // 3. Insufficient balance (500 available, trying to transfer 800)
        val overTransfer = repository.executeTransfer(
            fromVaultName = "خزنة A",
            toVaultName = "خزنة B",
            amount = 800.0,
            userId = user
        )
        assertTrue(overTransfer.isFailure)

        // Verify balances untouched after rollback
        val vA = db.vaultDao().getVaultByName("خزنة A", user)
        assertEquals(500.0, vA!!.balance, 0.001)
    }

    @Test
    fun testDebtsManagement_partialAndFullPayment() = runBlocking {
        val user = "user_debts_test"

        // 1. Add debt: I owe 10,000 EGP
        val debtId = repository.addDebt(
            personName = "البنك - قرض شخصي",
            type = "I_OWE",
            amount = 10000.0,
            notes = "قرض سيارة",
            userId = user
        ).toInt()

        var debt = db.debtDao().getDebtById(debtId)
        assertNotNull(debt)
        assertEquals(10000.0, debt!!.originalAmount, 0.001)
        assertEquals(10000.0, debt.remainingAmount, 0.001)
        assertEquals(0.0, debt.paidAmount, 0.001)
        assertEquals("ACTIVE", debt.status)

        // 2. Partial Payment: 4,000 EGP
        val p1Result = repository.recordDebtPayment(
            debtId = debtId,
            paymentAmount = 4000.0,
            notes = "القسط الأول",
            userId = user
        )
        assertTrue(p1Result.isSuccess)

        debt = db.debtDao().getDebtById(debtId)
        assertNotNull(debt)
        assertEquals(4000.0, debt!!.paidAmount, 0.001)
        assertEquals(6000.0, debt.remainingAmount, 0.001)
        assertEquals("PARTIALLY_PAID", debt.status)

        // 3. Full Payment of remaining 6,000 EGP
        val p2Result = repository.recordDebtPayment(
            debtId = debtId,
            paymentAmount = 6000.0,
            notes = "سداد باقي القرض بالكامل",
            userId = user
        )
        assertTrue(p2Result.isSuccess)

        debt = db.debtDao().getDebtById(debtId)
        assertNotNull(debt)
        assertEquals(10000.0, debt!!.paidAmount, 0.001)
        assertEquals(0.0, debt.remainingAmount, 0.001)
        assertEquals("PAID", debt.status)

        // 4. Verify payment history
        val payments = db.debtPaymentDao().getPaymentsForDebt(debtId, user).first()
        assertEquals(2, payments.size)
        assertEquals(6000.0, payments[0].amount, 0.001) // newest first
        assertEquals(4000.0, payments[1].amount, 0.001)
    }

    @Test
    fun testFinancialCalculator_analyticsAndEdgeCases() {
        val txs = listOf(
            TransactionEntity(amount = 20000.0, type = "INCOME", category = "راتب", description = "راتب شهري"),
            TransactionEntity(amount = 5000.0, type = "EXPENSE", category = "إيجار", description = "إيجار الشقة"),
            TransactionEntity(amount = 3000.0, type = "EXPENSE", category = "طعام", description = "مطعم"),
            TransactionEntity(amount = 2000.0, type = "EXPENSE", category = "طعام", description = "سوبرماركت")
        )

        // Income & Expenses
        val income = FinancialSummaryCalculator.calculateIncome(txs)
        val expenses = FinancialSummaryCalculator.calculateExpenses(txs)
        val netCashFlow = FinancialSummaryCalculator.calculateNetCashFlow(income, expenses)
        assertEquals(20000.0, income, 0.001)
        assertEquals(10000.0, expenses, 0.001)
        assertEquals(10000.0, netCashFlow, 0.001)

        // Savings Rate: (20,000 - 10,000) / 20,000 * 100 = 50%
        val rate = FinancialSummaryCalculator.calculateSavingsRate(income, expenses)
        assertNotNull(rate)
        assertEquals(50.0, rate!!, 0.001)

        // Zero Income edge case (safe divide by zero)
        val zeroIncomeRate = FinancialSummaryCalculator.calculateSavingsRate(0.0, 500.0)
        assertNull(zeroIncomeRate)

        // Category breakdown
        val breakdown = FinancialSummaryCalculator.calculateCategoryBreakdown(txs)
        assertEquals(2, breakdown.size)
        // الطعام = 5000 (50%), الإيجار = 5000 (50%)
        assertEquals(5000.0, breakdown[0].amount, 0.001)
        assertEquals(50.0, breakdown[0].percentage, 0.001)

        // Period Comparison
        val comp = FinancialSummaryCalculator.calculatePeriodComparison(12000.0, 10000.0)
        assertTrue(comp.hasIncreased)
        assertEquals(20.0, comp.percentageChange!!, 0.001)

        // Previous zero comparison
        val compZero = FinancialSummaryCalculator.calculatePeriodComparison(5000.0, 0.0)
        assertNull(compZero.percentageChange)
    }

    @Test
    fun testModulePreferences_toggleAndDataPreservation() = runBlocking {
        val manager = ModulePreferencesManager(context)
        val userId = "user_modules_test"

        // Default config: all modules enabled
        val config = manager.getConfiguration(userId)
        assertTrue(config.netWorth)
        assertTrue(config.debts)
        assertTrue(config.calendar)

        // Disable Gold and Debts
        manager.saveFullConfiguration(
            userId,
            config.copy(gold = false, debts = false)
        )

        val updatedConfig = manager.getConfiguration(userId)
        assertFalse(updatedConfig.gold)
        assertFalse(updatedConfig.debts)
        assertTrue(updatedConfig.netWorth) // unchanged

        // Data preservation test: Add debt while module is hidden in UI
        repository.addDebt(
            personName = "أخي",
            type = "OWED_TO_ME",
            amount = 1500.0,
            userId = userId
        )
        // Data must exist in DB despite module being toggled off in UI
        val debts = repository.getDebts(userId).first()
        assertEquals(1, debts.size)
        assertEquals(1500.0, debts[0].originalAmount, 0.001)
    }

    @Test
    fun testMultiUserIsolation_transfersAndDebts() = runBlocking {
        val userA = "user_alpha"
        val userB = "user_beta"

        // User A operations
        db.vaultDao().insertVault(VaultEntity(name = "خزنة A", balance = 5000.0, userId = userA))
        repository.addDebt(personName = "مدين A", type = "OWED_TO_ME", amount = 3000.0, userId = userA)

        // User B operations
        db.vaultDao().insertVault(VaultEntity(name = "خزنة B", balance = 8000.0, userId = userB))
        repository.addDebt(personName = "مدين B", type = "I_OWE", amount = 4000.0, userId = userB)

        // Verify User A only sees User A's data
        val debtsA = repository.getDebts(userA).first()
        assertEquals(1, debtsA.size)
        assertEquals("مدين A", debtsA[0].personName)

        val vaultsA = repository.getVaults(userA).first()
        assertEquals(1, vaultsA.size)
        assertEquals("خزنة A", vaultsA[0].name)

        // Verify User B only sees User B's data
        val debtsB = repository.getDebts(userB).first()
        assertEquals(1, debtsB.size)
        assertEquals("مدين B", debtsB[0].personName)

        val vaultsB = repository.getVaults(userB).first()
        assertEquals(1, vaultsB.size)
        assertEquals("خزنة B", vaultsB[0].name)
    }

    @Test
    fun testBackupAndRestore_includesPhaseEEntities() = runBlocking {
        val user = "user_backup_phase_e"

        // Insert Phase E entities
        db.vaultDao().insertVault(VaultEntity(name = "الخزنة", balance = 5000.0, userId = user))
        db.transferDao().insertTransfer(
            TransferEntity(userId = user, fromVaultName = "خزنة 1", toVaultName = "خزنة 2", amount = 1200.0)
        )
        db.debtDao().insertDebt(
            DebtEntity(id = 1, userId = user, personName = "صديقي", type = "OWED_TO_ME", originalAmount = 2500.0, remainingAmount = 2500.0)
        )
        db.debtPaymentDao().insertPayment(
            DebtPaymentEntity(debtId = 1, userId = user, amount = 500.0)
        )
        db.netWorthSnapshotDao().insertSnapshot(
            NetWorthSnapshotEntity(userId = user, totalAssets = 10000.0, totalLiabilities = 2000.0, netWorth = 8000.0)
        )

        // Create Encrypted Backup
        val backupString = LocalBackupManager.createEncryptedBackup(context, db, user, "TestSecret123")
        assertNotNull(backupString)
        assertTrue(backupString.startsWith("SMARTVAULT_ENC_V1:"))

        // Clear user data in database
        repository.clearUserData(user)
        assertEquals(0, repository.getTransfers(user).first().size)
        assertEquals(0, repository.getDebts(user).first().size)
        assertEquals(0, repository.getNetWorthSnapshots(user).first().size)

        // Restore Backup
        val validation = LocalBackupManager.validateBackup(backupString, "TestSecret123")
        assertTrue(validation.isValid)
        assertNotNull(validation.decryptedJson)
        val restoreSuccess = LocalBackupManager.applyRestore(db, user, validation.decryptedJson!!)
        assertTrue(restoreSuccess)

        // Verify all Phase E entities are restored
        val restoredTransfers = repository.getTransfers(user).first()
        assertEquals(1, restoredTransfers.size)
        assertEquals(1200.0, restoredTransfers[0].amount, 0.001)

        val restoredDebts = repository.getDebts(user).first()
        assertEquals(1, restoredDebts.size)
        assertEquals("صديقي", restoredDebts[0].personName)

        val restoredSnapshots = repository.getNetWorthSnapshots(user).first()
        assertEquals(1, restoredSnapshots.size)
        assertEquals(8000.0, restoredSnapshots[0].netWorth, 0.001)
    }

    @Test
    fun testLocalSmartFinancialInsights() {
        val summary = com.example.data.calculator.FinancialSummaryResult(
            totalIncome = 30000.0,
            totalExpenses = 15000.0,
            netCashFlow = 15000.0,
            savingsRate = 50.0,
            categoryShares = listOf(
                com.example.data.calculator.CategoryExpenseShare("سوبر ماركت", 8000.0, 53.3, 12),
                com.example.data.calculator.CategoryExpenseShare("مواصلات", 3000.0, 20.0, 5)
            )
        )

        val budgets = listOf(
            com.example.data.entity.BudgetLimitEntity(category = "سوبر ماركت", monthlyLimit = 7000.0) // exceeded!
        )

        val insights = FinancialInsightEngine.generateInsights(
            summary = summary,
            budgets = budgets,
            commitments = emptyList(),
            lessons = emptyList()
        )

        assertTrue(insights.isNotEmpty())
        // Should detect budget exceeded
        val budgetWarning = insights.find { it.id.startsWith("budget_over_") }
        assertNotNull(budgetWarning)
        assertEquals(com.example.data.insights.InsightPriority.CRITICAL, budgetWarning!!.priority)

        // Should detect positive savings rate
        val savingsInsight = insights.find { it.id == "savings_rate_positive" }
        assertNotNull(savingsInsight)
    }
}
