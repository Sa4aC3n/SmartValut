package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SmartVaultRepository
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.DebtEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import com.example.ui.SmartVaultViewModel
import com.example.ui.utils.AppText
import com.example.worker.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProductionStartupSafetyTest {

    private lateinit var context: Context
    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var repository: SmartVaultRepository

    private val userA = "user_alpha_1"
    private val userB = "user_beta_2"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        app = ApplicationProvider.getApplicationContext()
        AppText.initialize(context)
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)

        // Use standard singleton database instance to mirror production startup
        db = AppDatabase.getInstance(context)
        repository = SmartVaultRepository(db)
    }

    @After
    fun tearDown() {
        // Leave database intact for clean test-run hygiene without dropping schema
    }

    private suspend fun populateSampleDataForUserAAndUserB() {
        // User A: 3 transactions, 2 vaults, savings, gold, debt
        db.vaultDao().insertVault(VaultEntity(name = "خزنة المصروف A", balance = 5000.0, userId = userA))
        db.vaultDao().insertVault(VaultEntity(name = "خزنة التوفير A", balance = 20000.0, userId = userA))

        db.transactionDao().insertTransaction(TransactionEntity(amount = 12000.0, type = "INCOME", category = "راتب", description = "راتب أساسي", vaultName = "خزنة المصروف A", userId = userA))
        db.transactionDao().insertTransaction(TransactionEntity(amount = 2500.0, type = "EXPENSE", category = "فواتير", description = "كهرباء وغاز", vaultName = "خزنة المصروف A", userId = userA))
        db.transactionDao().insertTransaction(TransactionEntity(amount = 1500.0, type = "EXPENSE", category = "طعام", description = "سوبر ماركت", vaultName = "خزنة المصروف A", userId = userA))

        db.cashSavingDao().insertCashSaving(CashSavingEntity(amount = 10000.0, currency = "EGP", notes = "طوارئ A", userId = userA))
        db.goldAssetDao().insertGoldAsset(GoldAssetEntity(name = "سبيكة 10 جرام", goldType = "سبيكة", karat = 24, weight = 10.0, purchasePrice = 52000.0, userId = userA))
        db.debtDao().insertDebt(DebtEntity(personName = "صديق A", originalAmount = 3000.0, remainingAmount = 3000.0, type = "OWED_TO_ME", userId = userA))

        // User B: transactions, vault
        db.vaultDao().insertVault(VaultEntity(name = "خزنة رئيسية B", balance = 8000.0, userId = userB))
        db.transactionDao().insertTransaction(TransactionEntity(amount = 4000.0, type = "INCOME", category = "مكافأة", description = "مكافأة B", vaultName = "خزنة رئيسية B", userId = userB))
    }

    @Test
    fun startup_doesNotDeleteOrModifyExistingFinancialData() = runBlocking {
        populateSampleDataForUserAAndUserB()

        val initialTxsA = db.transactionDao().getTransactionsForUser(userA).first()
        val initialVaultsA = db.vaultDao().getVaultsForUser(userA).first()
        val initialSavingsA = db.cashSavingDao().getCashSavingsForUser(userA).first()
        val initialGoldA = db.goldAssetDao().getGoldAssetsForUser(userA).first()
        val initialDebtsA = db.debtDao().getDebtsForUser(userA).first()

        val initialTxsB = db.transactionDao().getTransactionsForUser(userB).first()
        val initialVaultsB = db.vaultDao().getVaultsForUser(userB).first()

        // 1. Simulate First Application / ViewModel Startup
        val vm1 = SmartVaultViewModel(app)
        assertNotNull(vm1)

        // Assert all User A records are completely preserved
        val txsAfter1A = db.transactionDao().getTransactionsForUser(userA).first()
        val vaultsAfter1A = db.vaultDao().getVaultsForUser(userA).first()
        assertEquals("User A transactions must not be deleted on startup", initialTxsA.size, txsAfter1A.size)
        assertEquals("User A vaults must not be deleted on startup", initialVaultsA.size, vaultsAfter1A.size)
        assertEquals(initialSavingsA.size, db.cashSavingDao().getCashSavingsForUser(userA).first().size)
        assertEquals(initialGoldA.size, db.goldAssetDao().getGoldAssetsForUser(userA).first().size)
        assertEquals(initialDebtsA.size, db.debtDao().getDebtsForUser(userA).first().size)

        // Assert all User B records are preserved
        assertEquals("User B transactions must not be deleted on startup", initialTxsB.size, db.transactionDao().getTransactionsForUser(userB).first().size)
        assertEquals("User B vaults must not be deleted on startup", initialVaultsB.size, db.vaultDao().getVaultsForUser(userB).first().size)

        // 2. Simulate Cold App Restart / ViewModel Recreation 10 times consecutively
        for (i in 1..10) {
            val vmi = SmartVaultViewModel(app)
            assertNotNull(vmi)
        }

        // Final verification
        val finalTxsA = db.transactionDao().getTransactionsForUser(userA).first()
        val finalVaultsA = db.vaultDao().getVaultsForUser(userA).first()
        assertEquals(initialTxsA.size, finalTxsA.size)
        assertEquals(initialVaultsA.size, finalVaultsA.size)
    }

    @Test
    fun testStartupPreservesTransactions() = runBlocking {
        db.transactionDao().insertTransaction(TransactionEntity(amount = 777.0, type = "EXPENSE", category = "وقود", description = "بنزين", vaultName = "نقدية", userId = userA))
        val initialCount = db.transactionDao().getTransactionsForUser(userA).first().size

        val vm = SmartVaultViewModel(app)
        assertNotNull(vm)

        val afterCount = db.transactionDao().getTransactionsForUser(userA).first().size
        assertEquals(initialCount, afterCount)
    }

    @Test
    fun testStartupPreservesVaults() = runBlocking {
        db.vaultDao().insertVault(VaultEntity(name = "خزنة بنك مصر", balance = 45000.0, userId = userA))
        val initialCount = db.vaultDao().getVaultsForUser(userA).first().size

        val vm = SmartVaultViewModel(app)
        assertNotNull(vm)

        val afterCount = db.vaultDao().getVaultsForUser(userA).first().size
        assertEquals(initialCount, afterCount)
    }

    @Test
    fun testStartupPreservesSavings() = runBlocking {
        db.cashSavingDao().insertCashSaving(CashSavingEntity(amount = 25000.0, currency = "SAR", notes = "مدخرات عمرة", userId = userA))
        val initialCount = db.cashSavingDao().getCashSavingsForUser(userA).first().size

        val vm = SmartVaultViewModel(app)
        assertNotNull(vm)

        val afterCount = db.cashSavingDao().getCashSavingsForUser(userA).first().size
        assertEquals(initialCount, afterCount)
    }

    @Test
    fun testStartupPreservesDebts() = runBlocking {
        db.debtDao().insertDebt(DebtEntity(personName = "أحمد سداد", originalAmount = 5000.0, remainingAmount = 2500.0, type = "I_OWE", userId = userA))
        val initialCount = db.debtDao().getDebtsForUser(userA).first().size

        val vm = SmartVaultViewModel(app)
        assertNotNull(vm)

        val afterCount = db.debtDao().getDebtsForUser(userA).first().size
        assertEquals(initialCount, afterCount)
    }

    @Test
    fun testStartupPreservesGold() = runBlocking {
        db.goldAssetDao().insertGoldAsset(GoldAssetEntity(name = "سبيكة BTC", goldType = "سبيكة", karat = 24, weight = 5.0, purchasePrice = 26500.0, userId = userA))
        val initialCount = db.goldAssetDao().getGoldAssetsForUser(userA).first().size

        val vm = SmartVaultViewModel(app)
        assertNotNull(vm)

        val afterCount = db.goldAssetDao().getGoldAssetsForUser(userA).first().size
        assertEquals(initialCount, afterCount)
    }

    @Test
    fun testMultiUserIsolationPreserved() = runBlocking {
        db.vaultDao().insertVault(VaultEntity(name = "خزنة A", balance = 1000.0, userId = userA))
        db.vaultDao().insertVault(VaultEntity(name = "خزنة B", balance = 2000.0, userId = userB))

        val vm = SmartVaultViewModel(app)
        assertNotNull(vm)

        val vaultsA = db.vaultDao().getVaultsForUser(userA).first()
        val vaultsB = db.vaultDao().getVaultsForUser(userB).first()
        assertTrue(vaultsA.any { it.name == "خزنة A" })
        assertTrue(vaultsB.any { it.name == "خزنة B" })
        assertTrue(vaultsA.none { it.userId == userB })
        assertTrue(vaultsB.none { it.userId == userA })
    }

    @Test
    fun testActivityRecreationPreservesAllData() = runBlocking {
        populateSampleDataForUserAAndUserB()
        val countBefore = db.transactionDao().getTransactionsForUser(userA).first().size

        // Build and recreate MainActivity with Robolectric
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        controller.recreate()

        val countAfter = db.transactionDao().getTransactionsForUser(userA).first().size
        assertEquals("Transactions must remain intact across Activity lifecycle recreation", countBefore, countAfter)
    }

    @Test
    fun testSchedulingRemindersPreservesAllData() = runBlocking {
        populateSampleDataForUserAAndUserB()
        val countBefore = db.vaultDao().getVaultsForUser(userA).first().size

        // Call reminders multiple times
        ReminderScheduler.scheduleDailyReminder(context)
        ReminderScheduler.scheduleDailyReminder(context)
        ReminderScheduler.scheduleDailyGoldPriceUpdate(context)
        ReminderScheduler.scheduleDailyGoldPriceUpdate(context)

        val countAfter = db.vaultDao().getVaultsForUser(userA).first().size
        assertEquals("Vault count must remain identical after scheduling reminders", countBefore, countAfter)
    }

    @Test
    fun testSeedingIsIdempotentAndOnlyForGoldPrices() = runBlocking {
        repository.seedDefaultGoldPricesIfMissing()
        val firstPrices = db.goldPriceDao().getAllGoldPrices().first()
        assertTrue(firstPrices.isNotEmpty())

        // Calling a second time must not duplicate or clear
        repository.seedDefaultGoldPricesIfMissing()
        val secondPrices = db.goldPriceDao().getAllGoldPrices().first()
        assertEquals(firstPrices.size, secondPrices.size)
    }
}
