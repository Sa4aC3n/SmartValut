package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.backup.LocalBackupManager
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.OutingEntity
import com.example.data.entity.OutingExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import com.example.data.firestore.FirestoreVaultRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhaseDVerificationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testRoomFinancialStorage_allEntitiesPass() = runBlocking {
        val user1 = "test_user_alpha"

        // 1. Vault
        db.vaultDao().insertVault(
            VaultEntity(name = "خزنة المصروف", balance = 5000.0, userId = user1)
        )
        val vaults = db.vaultDao().getVaultsForUser(user1).first()
        assertEquals(1, vaults.size)
        assertEquals(5000.0, vaults[0].balance, 0.001)

        // 2. Transactions (Income & Expense)
        db.transactionDao().insertTransaction(
            TransactionEntity(amount = 10000.0, type = "INCOME", category = "راتب", description = "راتب شهر", vaultName = "خزنة المصروف", userId = user1)
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(amount = 1500.0, type = "EXPENSE", category = "طعام", description = "مشتريات", vaultName = "خزنة المصروف", userId = user1)
        )
        val txs = db.transactionDao().getTransactionsForUser(user1).first()
        assertEquals(2, txs.size)

        // 3. Cash Savings
        db.cashSavingDao().insertCashSaving(
            CashSavingEntity(amount = 15000.0, currency = "EGP", notes = "صندوق الطوارئ", userId = user1)
        )
        val savings = db.cashSavingDao().getCashSavingsForUser(user1).first()
        assertEquals(1, savings.size)
        assertEquals(15000.0, savings[0].amount, 0.001)

        // 4. Gold Assets
        db.goldAssetDao().insertGoldAsset(
            GoldAssetEntity(name = "جنيه ذهب جورج", goldType = "جنيه ذهب", karat = 21, weight = 8.0, purchasePrice = 28000.0, userId = user1)
        )
        val gold = db.goldAssetDao().getGoldAssetsForUser(user1).first()
        assertEquals(1, gold.size)
        assertEquals(8.0, gold[0].weight, 0.001)

        // 5. Commitments
        db.commitmentDao().insertCommitment(
            CommitmentEntity(title = "إيجار الشقة", amount = 4500.0, dueDateMillis = System.currentTimeMillis() + 86400000L, userId = user1)
        )
        val commitments = db.commitmentDao().getCommitmentsForUser(user1).first()
        assertEquals(1, commitments.size)

        // 6. Child Lessons
        db.childLessonDao().insertChildLesson(
            ChildLessonEntity(childName = "أحمد", subject = "رياضيات", teacherName = "أ. محمود", amount = 600.0, dueDateMillis = System.currentTimeMillis() + 86400000L, userId = user1)
        )
        val lessons = db.childLessonDao().getChildLessonsForUser(user1).first()
        assertEquals(1, lessons.size)

        // 7. Budget Limits
        db.budgetLimitDao().insertOrUpdateLimit(
            BudgetLimitEntity(category = "طعام", monthlyLimit = 4000.0, userId = user1)
        )
        val limits = db.budgetLimitDao().getLimitsForUser(user1).first()
        assertEquals(1, limits.size)
        assertEquals(4000.0, limits[0].monthlyLimit, 0.001)

        // 8. Outings & Expenses
        db.outingDao().insertOuting(
            OutingEntity(id = "out1", name = "عشاء الأصدقاء", participantNamesJson = "[\"أحمد\",\"محمد\"]", userId = user1)
        )
        db.outingExpenseDao().insertOutingExpense(
            OutingExpenseEntity(outingId = "out1", title = "الحساب", amount = 1200.0, payerName = "أحمد", userId = user1)
        )
        val outings = db.outingDao().getOutingsForUser(user1).first()
        val outingExpenses = db.outingExpenseDao().getExpensesForOuting("out1", user1).first()
        assertEquals(1, outings.size)
        assertEquals(1, outingExpenses.size)
    }

    @Test
    fun testMultiUserIsolation_dataIsStrictlySeparated() = runBlocking {
        val userAlpha = "user_alpha_uid"
        val userBeta = "user_beta_uid"

        db.vaultDao().insertVault(VaultEntity(name = "خزنة ألفا", balance = 9999.0, userId = userAlpha))
        db.vaultDao().insertVault(VaultEntity(name = "خزنة بيتا", balance = 1111.0, userId = userBeta))

        db.transactionDao().insertTransaction(TransactionEntity(amount = 500.0, type = "EXPENSE", category = "عام", description = "ألفا", vaultName = "خزنة ألفا", userId = userAlpha))
        db.transactionDao().insertTransaction(TransactionEntity(amount = 300.0, type = "EXPENSE", category = "عام", description = "بيتا", vaultName = "خزنة بيتا", userId = userBeta))

        val alphaVaults = db.vaultDao().getVaultsForUser(userAlpha).first()
        val betaVaults = db.vaultDao().getVaultsForUser(userBeta).first()

        assertEquals(1, alphaVaults.size)
        assertEquals("خزنة ألفا", alphaVaults[0].name)

        assertEquals(1, betaVaults.size)
        assertEquals("خزنة بيتا", betaVaults[0].name)

        val alphaTxs = db.transactionDao().getTransactionsForUser(userAlpha).first()
        val betaTxs = db.transactionDao().getTransactionsForUser(userBeta).first()

        assertEquals(1, alphaTxs.size)
        assertEquals("ألفا", alphaTxs[0].description)

        assertEquals(1, betaTxs.size)
        assertEquals("بيتا", betaTxs[0].description)
    }

    @Test
    fun testLocalEncryptedBackupAndRestore() = runBlocking {
        val user = "test_user_backup"
        db.vaultDao().insertVault(VaultEntity(name = "خزنة احتياطية", balance = 7777.0, userId = user))
        db.transactionDao().insertTransaction(TransactionEntity(amount = 250.0, type = "EXPENSE", category = "طوارئ", description = "سداد", vaultName = "خزنة احتياطية", userId = user))

        val password = "SuperSecretPassword123!"
        val backupCiphertext = LocalBackupManager.createEncryptedBackup(context, db, user, password)
        assertNotNull(backupCiphertext)
        assertTrue(backupCiphertext.startsWith("SMARTVAULT_ENC_V1:"))
        assertFalse(backupCiphertext.contains("خزنة احتياطية")) // Plaintext is encrypted

        // Clear local database
        db.vaultDao().clearUserVaults(user)
        db.transactionDao().clearUserTransactions(user)
        assertEquals(0, db.vaultDao().getVaultsForUser(user).first().size)

        // Validate and apply restore from encrypted backup
        val validation = LocalBackupManager.validateBackup(backupCiphertext, password)
        assertTrue(validation.isValid)
        assertNotNull(validation.decryptedJson)

        val restored = LocalBackupManager.applyRestore(db, user, validation.decryptedJson!!)
        assertTrue(restored)
        assertEquals(1, db.vaultDao().getVaultsForUser(user).first().size)
        assertEquals(1, db.transactionDao().getTransactionsForUser(user).first().size)
        assertEquals("خزنة احتياطية", db.vaultDao().getVaultsForUser(user).first()[0].name)
    }

    @Test
    fun testCloudCodeCleanup_noFinancialMethodsInFirestoreRepo() {
        val repoClass = FirestoreVaultRepository::class.java
        val methods = repoClass.declaredMethods.map { it.name }

        // Confirm zero financial methods exist in FirestoreVaultRepository
        val forbiddenFinancialMethods = listOf(
            "createVault",
            "getVaultsFlow",
            "addTransaction",
            "getTransactionsFlow",
            "deleteTransaction",
            "deleteVault",
            "updateVault",
            "getGoldAssetsFlow",
            "addGoldAsset",
            "updateGoldAsset",
            "deleteGoldAsset",
            "sellGoldAsset",
            "getCashSavingsFlow",
            "addCashSaving",
            "updateCashSaving",
            "deleteCashSaving",
            "addCommitment",
            "updateCommitment",
            "deleteCommitment",
            "addChildLesson",
            "updateChildLesson",
            "deleteChildLesson",
            "setBudgetLimit",
            "deleteBudgetLimit",
            "getGoldPricesFlow",
            "saveGoldPrice",
            "saveAllGoldPrices",
            "createOuting",
            "updateOuting",
            "deleteOuting",
            "addOutingExpense",
            "deleteOutingExpense",
            "getOutingsFlow",
            "getOutingExpensesFlow"
        )

        for (methodName in forbiddenFinancialMethods) {
            assertFalse(
                "Method '$methodName' must NOT exist in FirestoreVaultRepository!",
                methods.contains(methodName)
            )
        }
    }

    @Test
    fun testFirestoreRules_leastPrivilegeEnforced() {
        val rulesFile = File("/app/applet/firestore.rules").takeIf { it.exists() } ?: File("../firestore.rules")
        assertTrue("firestore.rules file must exist", rulesFile.exists())
        val content = rulesFile.readText()

        // Verify that all subcollections are blocked
        assertTrue("Subcollections must be explicitly denied", content.contains("allow read, write: if false;"))
        // Verify only account/profile fields are allowed
        assertTrue(content.contains("hasOnly"))
        assertTrue(content.contains("displayName"))
        assertTrue(content.contains("email"))
        // Verify financial fields are NOT allowed in hasOnly list
        assertFalse(content.contains("'balance'"))
        assertFalse(content.contains("'income'"))
        assertFalse(content.contains("'expense'"))
        assertFalse(content.contains("'gold'"))
        assertFalse(content.contains("'savings'"))
    }
}
