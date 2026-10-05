package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SmartVaultRepository
import com.example.data.backup.LocalBackupManager
import com.example.data.backup.RestorePolicy
import com.example.data.crypto.CryptoManager
import com.example.data.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupAndLedgerRegressionTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: SmartVaultRepository

    private fun newDeviceKey() {
        CryptoManager.testSecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = SmartVaultRepository(db)
        newDeviceKey()
    }

    @After fun tearDown() {
        CryptoManager.testSecretKey = null
        db.close()
    }

    @Test fun deletionReversesExpenseAndIncomeExactlyOnce() = runBlocking {
        repository.addVault("cash", 100.0, "A")
        repository.addExpense(20.0, "food", "lunch", vaultName = "cash", userId = "A")
        val expenseId = repository.getTransactions("A").first().single().id
        repository.deleteTransaction(expenseId, "B")
        assertEquals(80.0, db.vaultDao().getVaultByName("cash", "A")!!.balance, 0.001)
        repository.deleteTransaction(expenseId, "A")
        repository.deleteTransaction(expenseId, "A")
        assertEquals(100.0, db.vaultDao().getVaultByName("cash", "A")!!.balance, 0.001)
        repository.addIncome(50.0, "salary", "income", vaultName = "cash", userId = "A")
        repository.deleteTransaction(repository.getTransactions("A").first().single().id, "A")
        assertEquals(100.0, db.vaultDao().getVaultByName("cash", "A")!!.balance, 0.001)
        assertTrue(repository.getTransactions("A").first().isEmpty())
    }

    @Test fun missingVaultDoesNotPartiallyDeleteTransaction() = runBlocking {
        val id = db.transactionDao().insertTransaction(TransactionEntity(userId = "A", amount = 20.0, type = "EXPENSE", category = "food", description = "orphan", vaultName = "missing")).toInt()
        try {
            repository.deleteTransaction(id, "A")
            fail("Missing vault must be reported")
        } catch (_: IllegalStateException) { }
        assertNotNull(db.transactionDao().getTransactionById(id, "A"))
    }

    @Test fun portableBackupRestoresWithADifferentDeviceKey() = runBlocking {
        val encrypted = CryptoManager.encrypt("my-test-secret")
        db.vaultItemDao().insertAllVaultItems(listOf(VaultItemEntity(id = "secret-A", userId = "A", title = "login", encryptedData = encrypted)))
        val payload = LocalBackupManager.createEncryptedBackup(context, db, "A", "BackupPassword123")
        assertTrue(payload.startsWith(LocalBackupManager.MAGIC_HEADER_V3))
        assertFalse(payload.contains("my-test-secret"))
        assertFalse(LocalBackupManager.validateBackup(payload, "wrong").isValid)
        val validation = LocalBackupManager.validateBackup(payload, "BackupPassword123")
        assertTrue(validation.isValid)
        newDeviceKey()
        assertTrue(LocalBackupManager.applyRestore(db, "B", validation.decryptedJson!!))
        val restored = repository.getVaultItems("B").first().single()
        assertEquals("my-test-secret", CryptoManager.decrypt(restored.encryptedData))
        assertNotEquals("secret-A", restored.id)
        assertEquals(encrypted, repository.getVaultItems("A").first().single().encryptedData)
    }

    @Test fun crossAccountRestoreRemapsOutingsWithoutOverwritingSource() = runBlocking {
        repository.addOuting("outing-A", "trip", listOf("person"), "A")
        repository.addOutingExpense("food", 10.0, outingId = "outing-A", userId = "A")
        val payload = LocalBackupManager.createEncryptedBackup(context, db, "A", "Password123")
        val json = LocalBackupManager.validateBackup(payload, "Password123").decryptedJson!!
        repeat(2) { assertTrue(LocalBackupManager.applyRestore(db, "B", json)) }
        val source = repository.getOutings("A").first().single()
        val restored = repository.getOutings("B").first().single()
        assertEquals("outing-A", source.id)
        assertNotEquals(source.id, restored.id)
        assertEquals(restored.id, repository.getOutingExpenses("B").first().single().outingId)
        assertEquals(source.id, repository.getOutingExpenses("A").first().single().outingId)
    }

    @Test fun guestReplacementDoesNotLeaveLegacyBlankUserRows() = runBlocking {
        db.vaultDao().insertVault(VaultEntity(name = "old guest", balance = 10.0, userId = ""))
        val payload = LocalBackupManager.createEncryptedBackup(context, db, "local_guest", "Password123")
        val json = LocalBackupManager.validateBackup(payload, "Password123").decryptedJson!!
        repeat(2) { assertTrue(LocalBackupManager.applyRestore(db, "local_guest", json)) }
        assertEquals(1, repository.getVaults("local_guest").first().size)
    }

    @Test fun unsupportedMergeLeavesAssetsUnchanged() = runBlocking {
        db.cashSavingDao().insertCashSaving(CashSavingEntity(userId = "A", amount = 50.0, currency = "EGP"))
        val payload = LocalBackupManager.createEncryptedBackup(context, db, "A", "Password123")
        val json = LocalBackupManager.validateBackup(payload, "Password123").decryptedJson!!
        assertFalse(LocalBackupManager.applyRestore(db, "A", json, RestorePolicy.SAFE_MERGE))
        assertEquals(1, repository.getCashSavings("A").first().size)
    }

    @Test fun unreadableLegacySecretRollsBackReplacement() = runBlocking {
        repository.addVault("existing", 100.0, "A")
        val secret = CryptoManager.encrypt("old-device-secret")
        newDeviceKey()
        val item = JSONObject().put("id", "old").put("title", "secret").put("encryptedData", secret)
        val root = JSONObject().put("app", "SmartVault").put("backupVersion", 2)
            .put("data", JSONObject().put("vault_items", JSONArray().put(item)))
        assertFalse(LocalBackupManager.applyRestore(db, "A", root.toString()))
        assertEquals(100.0, repository.getVaults("A").first().single().balance, 0.001)
    }
}
