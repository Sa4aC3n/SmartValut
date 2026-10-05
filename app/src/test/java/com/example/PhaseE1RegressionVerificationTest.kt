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
import com.example.data.entity.VaultEntity
import com.example.data.preferences.ModulePreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhaseE1RegressionVerificationTest {

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

    // 1. BACKUP & RESTORE — PROBLEM A: Debt Payment FK Disconnect
    @Test
    fun testRestore_preservesDebtPaymentRelationships_withRemappedIds() = runBlocking {
        val user = "user_debt_restore"
        val originalDebtId = 999

        db.debtDao().insertDebt(
            DebtEntity(
                id = originalDebtId,
                userId = user,
                personName = "أحمد قرض",
                type = "I_OWE",
                originalAmount = 10000.0,
                paidAmount = 2000.0,
                remainingAmount = 8000.0,
                status = "PARTIALLY_PAID"
            )
        )
        db.debtPaymentDao().insertPayment(
            DebtPaymentEntity(
                debtId = originalDebtId,
                userId = user,
                amount = 2000.0,
                notes = "الدفعة الأولى"
            )
        )

        // Create backup
        val backupPayload = LocalBackupManager.createEncryptedBackup(context, db, user, "SafePassword123")
        val validation = LocalBackupManager.validateBackup(backupPayload, "SafePassword123")
        assertTrue(validation.isValid)

        // Clear local data and insert some dummy debt to shift auto-increment IDs
        repository.clearUserData(user)
        db.debtDao().insertDebt(
            DebtEntity(userId = "other_user", personName = "Dummy", type = "OWED_TO_ME", originalAmount = 50.0, remainingAmount = 50.0)
        )

        // Restore backup
        val restoreSuccess = LocalBackupManager.applyRestore(db, user, validation.decryptedJson!!)
        assertTrue(restoreSuccess)

        // Verify restored debt and its re-linked payment
        val restoredDebts = repository.getDebts(user).first()
        assertEquals(1, restoredDebts.size)
        val restoredDebt = restoredDebts[0]
        assertEquals("أحمد قرض", restoredDebt.personName)

        val restoredPayments = repository.getDebtPayments(restoredDebt.id, user).first()
        assertEquals(1, restoredPayments.size)
        assertEquals(2000.0, restoredPayments[0].amount, 0.001)
        assertEquals(restoredDebt.id, restoredPayments[0].debtId) // Must point to newly assigned ID, not 999!
    }

    // 2. BACKUP & RESTORE — PROBLEM B: Duplication Prevention on Repeated Restore
    @Test
    fun testRepeatedRestore_doesNotDuplicateRecords() = runBlocking {
        val user = "user_repeated_restore"

        db.vaultDao().insertVault(VaultEntity(name = "الخزنة", balance = 5000.0, userId = user))
        db.transactionDao().insertTransaction(
            TransactionEntity(userId = user, type = "EXPENSE", amount = 150.0, category = "طعام", description = "غداء")
        )

        val backupPayload = LocalBackupManager.createEncryptedBackup(context, db, user, "MyPass789")
        val validation = LocalBackupManager.validateBackup(backupPayload, "MyPass789")
        assertTrue(validation.isValid)

        // Apply restore 3 times consecutively
        LocalBackupManager.applyRestore(db, user, validation.decryptedJson!!)
        LocalBackupManager.applyRestore(db, user, validation.decryptedJson!!)
        LocalBackupManager.applyRestore(db, user, validation.decryptedJson!!)

        // Must still have exactly 1 vault and 1 transaction, NOT 3 or 4!
        val vaults = repository.getVaults(user).first()
        val txs = repository.getTransactions(user).first()
        assertEquals(1, vaults.size)
        assertEquals(1, txs.size)
        assertEquals(5000.0, vaults[0].balance, 0.001)
    }

    // 3. BACKUP & RESTORE — PROBLEM D: Enforce Non-Blank User Password
    @Test
    fun testBackupPassword_requiresExplicitUserPassword_andValidatesCorrectly() = runBlocking {
        val user = "user_pass_test"
        db.vaultDao().insertVault(VaultEntity(name = "خزنة", balance = 100.0, userId = user))

        // Blank password must be rejected
        try {
            LocalBackupManager.createEncryptedBackup(context, db, user, "   ")
            fail("Expected IllegalArgumentException for blank password")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("كلمة مرور"))
        }

        // Valid password succeeds
        val validBackup = LocalBackupManager.createEncryptedBackup(context, db, user, "Complex#Pass!2026")
        val result = LocalBackupManager.validateBackup(validBackup, "Complex#Pass!2026")
        assertTrue(result.isValid)

        // Wrong password fails
        val wrongPassResult = LocalBackupManager.validateBackup(validBackup, "WrongPassword")
        assertFalse(wrongPassResult.isValid)
    }

    // 4. STRICT MULTI-USER DATA ISOLATION
    @Test
    fun testCrossUserAccess_cannotDeleteOrModifyOtherUserData() = runBlocking {
        val userA = "user_owner"
        val userB = "user_attacker"

        val debtAId = repository.addDebt("دين أ", "OWED_TO_ME", 5000.0, userId = userA).toInt()
        val vaultA = VaultEntity(name = "خزنة أ", balance = 10000.0, userId = userA)
        val vaultAId = db.vaultDao().insertVault(vaultA).toInt()

        // User B attempts to delete User A's debt
        repository.deleteDebt(debtAId, userId = userB)
        // Debt A must still exist!
        val debtAAfter = db.debtDao().getDebtById(debtAId, userA)
        assertNotNull(debtAAfter)

        // User B attempts to record payment on User A's debt
        val paymentResult = repository.recordDebtPayment(debtAId, 1000.0, userId = userB)
        assertTrue(paymentResult.isFailure)

        // User B attempts to delete User A's vault
        repository.deleteVault(vaultAId, userId = userB)
        val vaultAAfter = db.vaultDao().getVaultById(vaultAId, userA)
        assertNotNull(vaultAAfter)
    }

    // 5. ATOMIC FINANCIAL OPERATIONS — Transaction and Balance
    @Test
    fun testAddTransaction_atomicVaultBalanceUpdate() = runBlocking {
        val user = "user_tx_atomic"
        db.vaultDao().insertVault(VaultEntity(name = "الخزنة الرئيسية", balance = 2000.0, userId = user))

        // Valid Income
        repository.addIncome(1500.0, "راتب", "مكافأة", vaultName = "الخزنة الرئيسية", userId = user)
        var vault = db.vaultDao().getVaultByName("الخزنة الرئيسية", user)
        assertNotNull(vault)
        assertEquals(3500.0, vault!!.balance, 0.001)

        // Valid Expense
        repository.addExpense(500.0, "طعام", "غداء", vaultName = "الخزنة الرئيسية", userId = user)
        vault = db.vaultDao().getVaultByName("الخزنة الرئيسية", user)
        assertEquals(3000.0, vault!!.balance, 0.001)

        // Invalid Amounts Rejected
        try {
            repository.addIncome(Double.NaN, "راتب", "invalid", vaultName = "الخزنة الرئيسية", userId = user)
            fail("Expected exception for NaN amount")
        } catch (_: IllegalArgumentException) {}

        try {
            repository.addExpense(-100.0, "طعام", "invalid", vaultName = "الخزنة الرئيسية", userId = user)
            fail("Expected exception for negative amount")
        } catch (_: IllegalArgumentException) {}
    }

    // 6. EXPENSE ACCOUNTING — Preserves True Mathematical Balance Without Arbitrary 0.0 Coercion
    @Test
    fun testExpense_preservesTrueMathematicalBalance() = runBlocking {
        val user = "user_overdraft"
        db.vaultDao().insertVault(VaultEntity(name = "الخزنة", balance = 200.0, userId = user))

        // Expense exceeds vault balance (200 - 350 = -150)
        repository.addExpense(350.0, "طارئ", "إصلاح سيارة", vaultName = "الخزنة", userId = user)

        val vault = db.vaultDao().getVaultByName("الخزنة", user)
        assertNotNull(vault)
        assertEquals(-150.0, vault!!.balance, 0.001)
    }

    // 7. TRANSFER ACCOUNTING — Auditable Transfer Reversal
    @Test
    fun testTransferReversal_restoresBothVaultBalancesAtomically() = runBlocking {
        val user = "user_transfer_rev"
        db.vaultDao().insertVault(VaultEntity(name = "خزنة 1", balance = 5000.0, userId = user))
        db.vaultDao().insertVault(VaultEntity(name = "خزنة 2", balance = 1000.0, userId = user))

        val result = repository.executeTransfer("خزنة 1", "خزنة 2", 2000.0, userId = user)
        assertTrue(result.isSuccess)

        val transfers = repository.getTransfers(user).first()
        assertEquals(1, transfers.size)
        val transferId = transfers[0].id

        // Balances after transfer: 3000 and 3000
        assertEquals(3000.0, db.vaultDao().getVaultByName("خزنة 1", user)!!.balance, 0.001)
        assertEquals(3000.0, db.vaultDao().getVaultByName("خزنة 2", user)!!.balance, 0.001)

        // Perform auditable reversal
        val revResult = repository.reverseTransfer(transferId, user)
        assertTrue(revResult.isSuccess)

        // Balances restored to initial 5000 and 1000
        assertEquals(5000.0, db.vaultDao().getVaultByName("خزنة 1", user)!!.balance, 0.001)
        assertEquals(1000.0, db.vaultDao().getVaultByName("خزنة 2", user)!!.balance, 0.001)

        // Transfer record deleted upon reversal
        assertEquals(0, repository.getTransfers(user).first().size)
    }

    // 8. DEBT ACCOUNTING — Strict Overpayment Rejection
    @Test
    fun testDebtPayment_rejectsExceedingAmount() = runBlocking {
        val user = "user_debt_overpay"
        val debtId = repository.addDebt("خالد", "I_OWE", 1000.0, userId = user).toInt()

        // Attempting to pay 1500 on a 1000 debt must fail with explicit error
        val result = repository.recordDebtPayment(debtId, 1500.0, userId = user)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("يتجاوز"))

        // Debt balance must remain untouched
        val debt = db.debtDao().getDebtById(debtId, user)
        assertNotNull(debt)
        assertEquals(1000.0, debt!!.remainingAmount, 0.001)
        assertEquals(0.0, debt.paidAmount, 0.001)
    }

    // 9. NET WORTH ACCOUNTING — Liabilities distinguish confirmed debt from future obligations
    @Test
    fun testNetWorth_doesNotTreatFutureBillsAsExistingDebt() {
        val debts = listOf(
            DebtEntity(id = 1, personName = "بنك", type = "I_OWE", originalAmount = 5000.0, remainingAmount = 5000.0, status = "ACTIVE")
        )
        val commitments = listOf(
            CommitmentEntity(title = "إنترنت الشهر القادم", amount = 600.0, dueDateMillis = System.currentTimeMillis(), isPaid = false)
        )
        val lessons = listOf(
            ChildLessonEntity(childName = "علي", subject = "علوم", teacherName = "سالم", amount = 800.0, dueDateMillis = System.currentTimeMillis(), isPaid = false)
        )
        val vaults = listOf(
            VaultEntity(name = "الرئيسية", balance = 20000.0)
        )

        val summary = FinancialSummaryCalculator.calculate(
            vaults = vaults,
            transactions = emptyList(),
            cashSavings = emptyList(),
            goldAssets = emptyList(),
            debts = debts,
            goldPriceMap = emptyMap(),
            commitments = commitments,
            lessons = lessons
        )

        // Total liabilities must only be 5000 (the confirmed bank debt), NOT 5000 + 600 + 800 = 6400!
        assertEquals(5000.0, summary.netWorthBreakdown.totalLiabilities, 0.001)
        assertEquals(15000.0, summary.netWorthBreakdown.netWorth, 0.001)

        // Unpaid commitments and lessons are preserved informatively
        assertEquals(600.0, summary.netWorthBreakdown.unpaidCommitments, 0.001)
        assertEquals(800.0, summary.netWorthBreakdown.unpaidLessons, 0.001)
    }

    // 10. FINANCIAL CALCULATION — Savings Rate without Artificial Clamping
    @Test
    fun testSavingsRate_correctlyReflectsDeepDeficitWithoutClamping() {
        // Income = 10,000, Expenses = 30,000 -> Deficit = -20,000 -> Rate = -200%
        val rate = FinancialSummaryCalculator.calculateSavingsRate(10000.0, 30000.0)
        assertNotNull(rate)
        assertEquals(-200.0, rate!!, 0.001) // Must NOT be clamped to -100.0!

        // Income = 0 -> Safe null
        val zeroRate = FinancialSummaryCalculator.calculateSavingsRate(0.0, 500.0)
        assertNull(zeroRate)
    }

    // 11. NET WORTH SNAPSHOTS — Daily DateKey Preserves History Across Days
    @Test
    fun testNetWorthSnapshots_dailyDateKeyPreservesHistoryAcrossDays() = runBlocking {
        val user = "user_snapshots_history"

        // Day 1 snapshot
        val snap1 = NetWorthSnapshotEntity(
            userId = user,
            dateMillis = 1000000000L,
            dateKey = "2026-09-01",
            totalAssets = 50000.0,
            totalLiabilities = 10000.0,
            netWorth = 40000.0
        )
        repository.saveNetWorthSnapshot(snap1)

        // Day 2 snapshot
        val snap2 = NetWorthSnapshotEntity(
            userId = user,
            dateMillis = 1000086400L,
            dateKey = "2026-09-02",
            totalAssets = 55000.0,
            totalLiabilities = 10000.0,
            netWorth = 45000.0
        )
        repository.saveNetWorthSnapshot(snap2)

        // Both daily snapshots must be preserved!
        val snapshots = repository.getNetWorthSnapshots(user).first()
        assertEquals(2, snapshots.size)
        assertEquals(40000.0, snapshots[0].netWorth, 0.001)
        assertEquals(45000.0, snapshots[1].netWorth, 0.001)
    }

    // 12. MODULE PREFERENCES — User Switching Data Isolation
    @Test
    fun testModulePreferences_switchesCleanlyWithActiveUser() = runBlocking {
        val manager = ModulePreferencesManager(context)
        val userA = "user_modules_a"
        val userB = "user_modules_b"

        val flowA = manager.getConfigurationFlowForUser(userA)
        val flowB = manager.getConfigurationFlowForUser(userB)

        // Disable Gold for User A only
        manager.saveFullConfiguration(userA, flowA.value.copy(gold = false))

        assertEquals(false, manager.getConfiguration(userA).gold)
        assertEquals(true, manager.getConfiguration(userB).gold) // User B unaffected
    }

    // 13. DATABASE MIGRATION — Authentic Version 6 to 7 Migration Fixture
    @Test
    fun testDatabaseMigration_6_to_7_preservesExistingDataAndCreatesPhaseETables() {
        val dbFile = context.getDatabasePath("test_migration_v6_to_v7.db")
        if (dbFile.exists()) dbFile.delete()

        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_migration_v6_to_v7.db")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(sDb: androidx.sqlite.db.SupportSQLiteDatabase) {
                        sDb.execSQL("""
                            CREATE TABLE IF NOT EXISTS transactions (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                userId TEXT NOT NULL,
                                type TEXT NOT NULL,
                                amount REAL NOT NULL,
                                category TEXT NOT NULL,
                                description TEXT NOT NULL,
                                dateMillis INTEGER NOT NULL,
                                vaultName TEXT NOT NULL,
                                receiptImagePath TEXT
                            )
                        """.trimIndent())
                        sDb.execSQL("""
                            CREATE TABLE IF NOT EXISTS vaults (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                balance REAL NOT NULL,
                                isDefault INTEGER NOT NULL,
                                userId TEXT NOT NULL
                            )
                        """.trimIndent())
                        sDb.execSQL("INSERT INTO vaults (id, name, balance, isDefault, userId) VALUES (10, 'خزنة قديمة', 4500.0, 1, 'user_v6')")
                        sDb.execSQL("INSERT INTO transactions (id, userId, type, amount, category, description, dateMillis, vaultName) VALUES (20, 'user_v6', 'EXPENSE', 250.0, 'عام', 'مصروف قديم', 1000, 'خزنة قديمة')")
                    }

                    override fun onUpgrade(sDb: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val v6Db = helper.writableDatabase
        try {
            // Execute real migration from 6 to 7
            AppDatabase.MIGRATION_6_7.migrate(v6Db)

            // 1. Verify existing V6 data survived intact
            val vaultCursor = v6Db.query("SELECT balance, name FROM vaults WHERE id = 10")
            assertTrue(vaultCursor.moveToFirst())
            assertEquals(4500.0, vaultCursor.getDouble(0), 0.001)
            assertEquals("خزنة قديمة", vaultCursor.getString(1))
            vaultCursor.close()

            val txCursor = v6Db.query("SELECT amount, description FROM transactions WHERE id = 20")
            assertTrue(txCursor.moveToFirst())
            assertEquals(250.0, txCursor.getDouble(0), 0.001)
            assertEquals("مصروف قديم", txCursor.getString(1))
            txCursor.close()

            // 2. Verify all 4 Phase E tables now exist
            val tableCursor = v6Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('transfers', 'debts', 'debt_payments', 'net_worth_snapshots')")
            val createdTables = mutableListOf<String>()
            while (tableCursor.moveToNext()) {
                createdTables.add(tableCursor.getString(0))
            }
            tableCursor.close()

            assertTrue(createdTables.contains("transfers"))
            assertTrue(createdTables.contains("debts"))
            assertTrue(createdTables.contains("debt_payments"))
            assertTrue(createdTables.contains("net_worth_snapshots"))

            // 3. Verify insertion works on newly migrated tables
            v6Db.execSQL("INSERT INTO debts (id, userId, personName, type, originalAmount, paidAmount, remainingAmount, startDateMillis, status, createdAt, updatedAt) VALUES (1, 'user_v6', 'سعيد', 'OWED_TO_ME', 500.0, 0.0, 500.0, 1000, 'ACTIVE', 1000, 1000)")
            val debtCursor = v6Db.query("SELECT personName, remainingAmount FROM debts WHERE id = 1")
            assertTrue(debtCursor.moveToFirst())
            assertEquals("سعيد", debtCursor.getString(0))
            assertEquals(500.0, debtCursor.getDouble(1), 0.001)
            debtCursor.close()
        } finally {
            v6Db.close()
            helper.close()
            dbFile.delete()
        }
    }

    // 14. MULTIPLE CURRENCIES — Preserves distinct currencies without inventing conversions
    @Test
    fun testCashSavings_distinctCurrencies() = runBlocking {
        val user = "user_currencies"
        db.cashSavingDao().insertCashSaving(
            CashSavingEntity(userId = user, amount = 1000.0, currency = "USD", notes = "Dollar savings", dateMillis = 1L)
        )
        db.cashSavingDao().insertCashSaving(
            CashSavingEntity(userId = user, amount = 50000.0, currency = "EGP", notes = "Egyptian pounds", dateMillis = 2L)
        )

        val savings = repository.getCashSavings(user).first()
        assertEquals(2, savings.size)
        val usdSaving = savings.find { it.currency == "USD" }
        val egpSaving = savings.find { it.currency == "EGP" }
        assertNotNull(usdSaving)
        assertNotNull(egpSaving)
        assertEquals(1000.0, usdSaving!!.amount, 0.001)
        assertEquals(50000.0, egpSaving!!.amount, 0.001)
    }

    // 15. BACKUP & RESTORE — Explicit Safe Merge Policy
    @Test
    fun testRestore_unsafeMergeIsRejectedWithoutChangingExistingData() = runBlocking {
        val user = "user_safe_merge"

        // Existing local data
        db.vaultDao().insertVault(VaultEntity(name = "الخزنة المحلية", balance = 1000.0, userId = user))
        db.transactionDao().insertTransaction(
            TransactionEntity(userId = user, type = "EXPENSE", amount = 100.0, category = "عام", description = "معاملة سابقة", dateMillis = 5000L)
        )

        // Incoming backup has "الخزنة المحلية" plus a new "خزنة إضافية" and a new transaction
        val sourceUser = "source_user"
        db.vaultDao().insertVault(VaultEntity(name = "الخزنة المحلية", balance = 2000.0, userId = sourceUser))
        db.vaultDao().insertVault(VaultEntity(name = "خزنة إضافية", balance = 3000.0, userId = sourceUser))
        db.transactionDao().insertTransaction(
            TransactionEntity(userId = sourceUser, type = "EXPENSE", amount = 100.0, category = "عام", description = "معاملة سابقة", dateMillis = 5000L)
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(userId = sourceUser, type = "INCOME", amount = 500.0, category = "راتب", description = "دخل جديد", dateMillis = 6000L)
        )

        val payload = LocalBackupManager.createEncryptedBackup(context, db, sourceUser, "MergePass123")
        val validation = LocalBackupManager.validateBackup(payload, "MergePass123")
        assertTrue(validation.isValid)

        // Clean source user data
        repository.clearUserData(sourceUser)

        // Apply SAFE_MERGE restore
        val mergeSuccess = LocalBackupManager.applyRestore(db, user, validation.decryptedJson!!, policy = com.example.data.backup.RestorePolicy.SAFE_MERGE)
        assertFalse(mergeSuccess)

        // Local vault balance preserved without duplicate vault creation
        val vaults = repository.getVaults(user).first()
        assertEquals(1, vaults.size)
        val localVault = vaults.find { it.name == "الخزنة المحلية" }
        assertNotNull(localVault)
        assertEquals(1000.0, localVault!!.balance, 0.001) // Preserved original

        // Duplicate transaction filtered out, only new transaction added
        val txs = repository.getTransactions(user).first()
        assertEquals(1, txs.size)
    }

    // 16. ATOMIC OPERATIONS — Outing Expense Deducts from Vault Atomically
    @Test
    fun testAtomicOutingExpense_deductsFromVaultAtomically() = runBlocking {
        val user = "user_outing_atomic"
        db.vaultDao().insertVault(VaultEntity(name = "خزنة الخروجات", balance = 2000.0, userId = user))
        repository.addOuting("out_1", "رحلة الصيف", listOf("أحمد", "محمد"), userId = user)

        repository.addOutingExpense(
            title = "غداء",
            amount = 450.0,
            payerName = "أحمد",
            outingId = "out_1",
            userId = user,
            vaultName = "خزنة الخروجات"
        )

        val vault = db.vaultDao().getVaultByName("خزنة الخروجات", user)
        assertNotNull(vault)
        assertEquals(1550.0, vault!!.balance, 0.001)

        val expenses = repository.getExpensesForOuting("out_1", user).first()
        assertEquals(1, expenses.size)
        assertEquals(450.0, expenses[0].amount, 0.001)
    }

    // 17. ATOMIC OPERATIONS — Gold Liquidation Credits Vault Atomically
    @Test
    fun testAtomicGoldLiquidation_creditsVaultAtomically() = runBlocking {
        val user = "user_gold_atomic"
        db.vaultDao().insertVault(VaultEntity(name = "الخزنة الرئيسية", balance = 10000.0, userId = user))
        val goldId = repository.addGoldAsset(
            GoldAssetEntity(
                userId = user,
                name = "سبيكة 10 جرام",
                goldType = "سبيكة",
                karat = 24,
                weight = 10.0,
                purchasePrice = 50000.0
            )
        ).toInt()

        repository.sellGoldAsset(
            id = goldId,
            salePrice = 55000.0,
            userId = user,
            vaultName = "الخزنة الرئيسية"
        )

        val vault = db.vaultDao().getVaultByName("الخزنة الرئيسية", user)
        assertNotNull(vault)
        assertEquals(65000.0, vault!!.balance, 0.001)

        val assets = repository.getGoldAssets(user).first()
        val soldAsset = assets.find { it.id == goldId }
        assertNotNull(soldAsset)
        assertEquals("SOLD", soldAsset!!.status)
        assertEquals(55000.0, soldAsset.salePrice ?: 0.0, 0.001)
    }

    // 18. MULTI-USER DATA ISOLATION — Direct Repository Calls Negative Security Tests
    @Test
    fun testNegativeSecurity_userBCannotAccessGuestOrUserARecords() = runBlocking {
        val userA = "auth_user_a"
        val userB = "auth_user_b"
        val guest = "local_guest"

        // Create records for User A
        val vaultAId = db.vaultDao().insertVault(VaultEntity(name = "خزنة A", balance = 5000.0, userId = userA)).toInt()
        val debtAId = repository.addDebt("دين A", "OWED_TO_ME", 3000.0, userId = userA).toInt()
        val txAId = db.transactionDao().insertTransaction(
            TransactionEntity(userId = userA, type = "EXPENSE", amount = 100.0, category = "عام", description = "A tx")
        ).toInt()

        // Create records for Guest
        val vaultGId = db.vaultDao().insertVault(VaultEntity(name = "خزنة Guest", balance = 1000.0, userId = guest)).toInt()

        // 1. User B cannot see User A or Guest records in Flow queries
        val vaultsB = repository.getVaults(userB).first()
        assertTrue(vaultsB.isEmpty())

        val debtsB = repository.getDebts(userB).first()
        assertTrue(debtsB.isEmpty())

        // 2. User B cannot delete User A's transaction
        repository.deleteTransaction(txAId, userId = userB)
        val txAAfter = db.transactionDao().getTransactionById(txAId, userA)
        assertNotNull(txAAfter)

        // 3. User B cannot delete User A's vault
        repository.deleteVault(vaultAId, userId = userB)
        val vaultAAfter = db.vaultDao().getVaultById(vaultAId, userA)
        assertNotNull(vaultAAfter)

        // 4. User B cannot delete Guest vault
        repository.deleteVault(vaultGId, userId = userB)
        val vaultGAfter = db.vaultDao().getVaultById(vaultGId, guest)
        assertNotNull(vaultGAfter)

        // 5. Guest cannot delete User A's vault
        repository.deleteVault(vaultAId, userId = guest)
        val vaultAAfterGuest = db.vaultDao().getVaultById(vaultAId, userA)
        assertNotNull(vaultAAfterGuest)

        // 6. User B cannot record payment on User A's debt
        val payResult = repository.recordDebtPayment(debtAId, 500.0, userId = userB)
        assertTrue(payResult.isFailure)
    }

    // 19. MULTI-CURRENCY CALCULATIONS — Net Worth Breakdown separates foreign assets without fake conversion
    @Test
    fun testMultiCurrency_getFullNetWorthBreakdown_separatesUnconvertedForeignCurrency() {
        val cashSavings = listOf(
            CashSavingEntity(amount = 20000.0, currency = "EGP"),
            CashSavingEntity(amount = 1000.0, currency = "USD")
        )
        val vaults = listOf(
            VaultEntity(name = "الرئيسية", balance = 10000.0)
        )

        // Without exchange rate for USD
        val breakdown = FinancialSummaryCalculator.getFullNetWorthBreakdown(
            vaults = vaults,
            cashSavings = cashSavings,
            goldAssets = emptyList(),
            goldPriceMap = emptyMap(),
            debts = emptyList(),
            reportingCurrency = "EGP",
            exchangeRates = emptyMap() // No USD rate provided
        )

        // vaultsTotal = 10000, cashSavingsTotal = 20000 (only EGP), netWorth = 30000
        assertEquals(10000.0, breakdown.vaultsTotal, 0.001)
        assertEquals(20000.0, breakdown.cashSavingsTotal, 0.001)
        assertEquals(30000.0, breakdown.netWorth, 0.001)

        // Separate totals recorded accurately
        assertEquals(20000.0, breakdown.separateCurrencyTotals["EGP"] ?: 0.0, 0.001)
        assertEquals(1000.0, breakdown.separateCurrencyTotals["USD"] ?: 0.0, 0.001)

        // USD is tracked in unconvertedForeignAssets
        assertEquals(1000.0, breakdown.unconvertedForeignAssets["USD"] ?: 0.0, 0.001)
    }
}
