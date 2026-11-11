package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SmartVaultRepository
import com.example.data.backup.LocalBackupManager
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
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
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhaseE2AuditRegressionVerificationTest {

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
    fun testTransferReversalOnDeleteRestoresVaultBalances() = runBlocking {
        val userId = "test_user"
        repository.addVault("خزنة أ", 1000.0, userId)
        repository.addVault("خزنة ب", 500.0, userId)

        // Execute transfer 200 from A to B
        val transferResult = repository.executeTransfer(
            fromVaultName = "خزنة أ",
            toVaultName = "خزنة ب",
            amount = 200.0,
            userId = userId
        )
        assertTrue("Transfer execution should succeed", transferResult.isSuccess)

        val vaultsAfterTransfer = repository.getVaults(userId).first()
        val vaultAAfter = vaultsAfterTransfer.first { it.name == "خزنة أ" }
        val vaultBAfter = vaultsAfterTransfer.first { it.name == "خزنة ب" }
        assertEquals(800.0, vaultAAfter.balance, 0.001)
        assertEquals(700.0, vaultBAfter.balance, 0.001)

        val transfers = db.transferDao().getTransfersForUser(userId).first()
        assertEquals(1, transfers.size)
        val transfer = transfers.first()

        // Delete transfer -> Must reverse and restore balances
        val deleteResult = repository.deleteTransfer(transfer.id, userId)
        assertTrue("Transfer deletion/reversal should succeed", deleteResult.isSuccess)

        val vaultsAfterDelete = repository.getVaults(userId).first()
        val vaultAFinal = vaultsAfterDelete.first { it.name == "خزنة أ" }
        val vaultBFinal = vaultsAfterDelete.first { it.name == "خزنة ب" }
        assertEquals(1000.0, vaultAFinal.balance, 0.001)
        assertEquals(500.0, vaultBFinal.balance, 0.001)

        val transfersFinal = db.transferDao().getTransfersForUser(userId).first()
        assertEquals(0, transfersFinal.size)
    }

    @Test
    fun testPayChildLessonAndReversalOnDelete() = runBlocking {
        val userId = "test_user"
        repository.addVault("الخزنة الرئيسية", 1500.0, userId)

        val lessonId = repository.addChildLesson(
            childName = "عمر",
            subject = "رياضيات",
            teacherName = "أستاذ أحمد",
            amount = 350.0,
            userId = userId
        ).toInt()

        // 1. Pay Child Lesson
        repository.payChildLesson(
            lessonId = lessonId,
            vaultName = "الخزنة الرئيسية",
            userId = userId
        )

        val vaultsAfterPay = repository.getVaults(userId).first()
        assertEquals(1150.0, vaultsAfterPay.first().balance, 0.001)

        val lessonsAfterPay = repository.getChildLessons(userId).first()
        val paidLesson = lessonsAfterPay.first { it.id == lessonId }
        assertTrue("Lesson must be marked as paid", paidLesson.isPaid)

        val txsAfterPay = repository.getTransactions(userId).first()
        assertEquals(1, txsAfterPay.size)
        val tx = txsAfterPay.first()
        assertEquals("EXPENSE", tx.type)
        assertEquals(350.0, tx.amount, 0.001)
        assertEquals("CHILD_LESSON_PAYMENT", tx.referenceType)
        assertEquals("LESSON_$lessonId", tx.referenceId)

        // 2. Delete Paid Child Lesson -> Must reverse payment and restore vault balance
        repository.deleteChildLesson(lessonId, userId)

        val vaultsAfterDelete = repository.getVaults(userId).first()
        assertEquals(1500.0, vaultsAfterDelete.first().balance, 0.001)

        val txsAfterDelete = repository.getTransactions(userId).first()
        assertEquals(0, txsAfterDelete.size)

        val lessonsAfterDelete = repository.getChildLessons(userId).first()
        assertEquals(0, lessonsAfterDelete.size)
    }

    @Test
    fun testCurrentMonthBudgetExclusionOfHistoricalExpenses() {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val pastCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear - 1)
            set(Calendar.MONTH, currentMonth)
        }

        val allTxs = listOf(
            TransactionEntity(id = 1, type = "EXPENSE", category = "طعام", description = "وجبة سابقة", amount = 1000.0, dateMillis = pastCal.timeInMillis),
            TransactionEntity(id = 2, type = "EXPENSE", category = "طعام", description = "غداء اليوم", amount = 250.0, dateMillis = System.currentTimeMillis()),
            TransactionEntity(id = 3, type = "INCOME", category = "راتب", description = "مرتب", amount = 5000.0, dateMillis = System.currentTimeMillis())
        )

        val currentMonthSpent = allTxs.filter { tx ->
            if (tx.type != "EXPENSE" || tx.category != "طعام") return@filter false
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }

        assertEquals(250.0, currentMonthSpent, 0.001)
    }

    @Test
    fun testLocalGuestSessionPersistenceAndVaultOperations() = runBlocking {
        val guestUserId = "local_guest"

        // Add a vault and income under guest session
        repository.addVault("الخزينة النقدية الرئيسية", 2000.0, guestUserId)
        val vaults = repository.getVaults(guestUserId).first()
        assertEquals(1, vaults.size)
        assertEquals(2000.0, vaults.first().balance, 0.001)

        // Add an expense under guest session
        repository.addExpense(
            amount = 450.0,
            category = "سوبر ماركت",
            description = "مشتريات بقالة",
            vaultName = "الخزينة النقدية الرئيسية",
            dateMillis = System.currentTimeMillis(),
            userId = guestUserId
        )

        val updatedVaults = repository.getVaults(guestUserId).first()
        assertEquals(1550.0, updatedVaults.first().balance, 0.001)

        val txs = repository.getTransactions(guestUserId).first()
        assertEquals(1, txs.size)
        assertEquals("مشتريات بقالة", txs.first().description)
        assertEquals(450.0, txs.first().amount, 0.001)
    }
}
