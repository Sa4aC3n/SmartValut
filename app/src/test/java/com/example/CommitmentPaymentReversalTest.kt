package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SmartVaultRepository
import com.example.data.backup.LocalBackupManager
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.VaultEntity
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CommitmentPaymentReversalTest {
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
    fun paidCommitmentDeletion_restoresOriginalVaultAndRemovesExpenseExactlyOnce() = runBlocking {
        val user = "commitment_user"
        repository.addVault("الخزنة الرئيسية", 1_000.0, user)
        val commitmentId = repository.addCommitment(
            title = "فاتورة",
            amount = 250.0,
            isRecurring = false,
            userId = user
        ).toInt()

        repository.payCommitment(commitmentId, "الخزنة الرئيسية", user)

        val paid = repository.getCommitments(user).first().single()
        assertTrue(paid.isPaid)
        assertNotNull(paid.paymentReferenceId)
        assertEquals("الخزنة الرئيسية", paid.paidFromVaultName)
        assertEquals(750.0, db.vaultDao().getVaultByName("الخزنة الرئيسية", user)!!.balance, 0.001)

        val paymentTx = repository.getTransactions(user).first().single()
        assertEquals("EXPENSE", paymentTx.type)
        assertEquals("COMMITMENT_PAYMENT", paymentTx.referenceType)
        assertEquals(paid.paymentReferenceId, paymentTx.referenceId)

        repository.deleteCommitment(commitmentId, user)

        assertTrue(repository.getCommitments(user).first().isEmpty())
        assertTrue(repository.getTransactions(user).first().isEmpty())
        assertEquals(1_000.0, db.vaultDao().getVaultByName("الخزنة الرئيسية", user)!!.balance, 0.001)

        // Idempotency: deleting the already-deleted commitment must not credit again.
        repository.deleteCommitment(commitmentId, user)
        assertEquals(1_000.0, db.vaultDao().getVaultByName("الخزنة الرئيسية", user)!!.balance, 0.001)
    }

    @Test
    fun anotherUserCannotReverseOrDeletePaidCommitment() = runBlocking {
        val owner = "owner"
        val attacker = "attacker"
        repository.addVault("cash", 500.0, owner)
        val commitmentId = repository.addCommitment(
            title = "Internet",
            amount = 100.0,
            isRecurring = false,
            userId = owner
        ).toInt()
        repository.payCommitment(commitmentId, "cash", owner)

        repository.deleteCommitment(commitmentId, attacker)

        assertEquals(400.0, db.vaultDao().getVaultByName("cash", owner)!!.balance, 0.001)
        assertEquals(1, repository.getCommitments(owner).first().size)
        assertEquals(1, repository.getTransactions(owner).first().size)
    }

    @Test
    fun legacyPaidCommitmentWithOneExactExpense_isSafelyReversed() = runBlocking {
        val user = "legacy"
        repository.addVault("cash", 900.0, user)
        val commitmentId = repository.addCommitment(
            title = "إيجار",
            amount = 200.0,
            isPaid = true,
            isRecurring = false,
            userId = user
        ).toInt()

        // Reproduce the pre-schema-8 payment flow: paid flag + ordinary expense,
        // with no stable paymentReferenceId on the commitment.
        repository.addExpense(
            amount = 200.0,
            category = "إيجار",
            description = "دفع التزام: إيجار",
            vaultName = "cash",
            userId = user
        )
        assertEquals(700.0, db.vaultDao().getVaultByName("cash", user)!!.balance, 0.001)

        repository.deleteCommitment(commitmentId, user)

        assertEquals(900.0, db.vaultDao().getVaultByName("cash", user)!!.balance, 0.001)
        assertTrue(repository.getTransactions(user).first().isEmpty())
        assertTrue(repository.getCommitments(user).first().isEmpty())
    }

    @Test
    fun legacyPaidCommitmentWithAmbiguousExpenses_failsClosedWithoutChangingMoney() = runBlocking {
        val user = "ambiguous"
        repository.addVault("cash", 1_000.0, user)
        val commitmentId = repository.addCommitment(
            title = "قسط",
            amount = 100.0,
            isPaid = true,
            isRecurring = false,
            userId = user
        ).toInt()

        repeat(2) {
            repository.addExpense(
                amount = 100.0,
                category = "قسط",
                description = "دفع التزام: قسط",
                vaultName = "cash",
                userId = user
            )
        }
        assertEquals(800.0, db.vaultDao().getVaultByName("cash", user)!!.balance, 0.001)

        var failed = false
        try {
            repository.deleteCommitment(commitmentId, user)
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue(failed)
        assertEquals(800.0, db.vaultDao().getVaultByName("cash", user)!!.balance, 0.001)
        assertEquals(1, repository.getCommitments(user).first().size)
        assertEquals(2, repository.getTransactions(user).first().size)
    }

    @Test
    fun backupRestore_preservesPaymentReferenceAndReversalSemantics() = runBlocking {
        val source = "source"
        val restored = "restored"

        repository.addVault("wallet", 1_000.0, source)
        val commitmentId = repository.addCommitment(
            title = "اشتراك",
            amount = 150.0,
            isRecurring = false,
            userId = source
        ).toInt()
        repository.payCommitment(commitmentId, "wallet", source)

        val payload = LocalBackupManager.createEncryptedBackup(context, db, source, "Password123")
        val validation = LocalBackupManager.validateBackup(payload, "Password123")
        assertTrue(validation.isValid)
        assertTrue(LocalBackupManager.applyRestore(db, restored, validation.decryptedJson!!))

        val restoredCommitment = repository.getCommitments(restored).first().single()
        val restoredTx = repository.getTransactions(restored).first().single()
        assertNotNull(restoredCommitment.paymentReferenceId)
        assertEquals(restoredCommitment.paymentReferenceId, restoredTx.referenceId)
        assertEquals("COMMITMENT_PAYMENT", restoredTx.referenceType)
        assertEquals(850.0, db.vaultDao().getVaultByName("wallet", restored)!!.balance, 0.001)

        repository.deleteCommitment(restoredCommitment.id, restored)
        assertEquals(1_000.0, db.vaultDao().getVaultByName("wallet", restored)!!.balance, 0.001)
        assertTrue(repository.getTransactions(restored).first().isEmpty())
    }

    @Test
    fun migration7To8_addsPaymentLinkageColumnsWithoutChangingBalances() {
        val dbFile = context.getDatabasePath("commitment_migration_7_8.db")
        if (dbFile.exists()) dbFile.delete()

        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("commitment_migration_7_8.db")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(sDb: androidx.sqlite.db.SupportSQLiteDatabase) {
                        sDb.execSQL("""
                            CREATE TABLE transactions (
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
                            CREATE TABLE commitments (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                userId TEXT NOT NULL,
                                title TEXT NOT NULL,
                                amount REAL NOT NULL,
                                dueDateMillis INTEGER NOT NULL,
                                isPaid INTEGER NOT NULL,
                                isRecurringMonthly INTEGER NOT NULL,
                                notes TEXT NOT NULL,
                                receiptImagePath TEXT
                            )
                        """.trimIndent())
                        sDb.execSQL("INSERT INTO transactions (userId,type,amount,category,description,dateMillis,vaultName) VALUES ('u','EXPENSE',100.0,'x','old',1,'cash')")
                        sDb.execSQL("INSERT INTO commitments (userId,title,amount,dueDateMillis,isPaid,isRecurringMonthly,notes) VALUES ('u','old',100.0,1,1,0,'')")
                    }

                    override fun onUpgrade(
                        sDb: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )

        val raw = helper.writableDatabase
        try {
            AppDatabase.MIGRATION_7_8.migrate(raw)
            val tx = raw.query("SELECT amount, referenceType, referenceId FROM transactions LIMIT 1")
            assertTrue(tx.moveToFirst())
            assertEquals(100.0, tx.getDouble(0), 0.001)
            assertTrue(tx.isNull(1))
            assertTrue(tx.isNull(2))
            tx.close()

            val comm = raw.query("SELECT amount, paymentReferenceId, paidFromVaultName, paidAtMillis FROM commitments LIMIT 1")
            assertTrue(comm.moveToFirst())
            assertEquals(100.0, comm.getDouble(0), 0.001)
            assertTrue(comm.isNull(1))
            assertTrue(comm.isNull(2))
            assertTrue(comm.isNull(3))
            comm.close()
        } finally {
            helper.close()
            if (dbFile.exists()) dbFile.delete()
        }
    }
}
