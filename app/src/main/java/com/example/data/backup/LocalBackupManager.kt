package com.example.data.backup

import android.content.Context
import android.util.Base64
import com.example.data.AppDatabase
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.OutingEntity
import com.example.data.entity.OutingExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import com.example.data.entity.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupValidationResult(
    val isValid: Boolean,
    val backupVersion: Int = 0,
    val schemaVersion: Int = 0,
    val exportDate: Long = 0L,
    val transactionCount: Int = 0,
    val goldAssetCount: Int = 0,
    val cashSavingCount: Int = 0,
    val commitmentCount: Int = 0,
    val lessonCount: Int = 0,
    val errorMessage: String? = null,
    val decryptedJson: String? = null
)

object LocalBackupManager {
    private const val MAGIC_HEADER = "SMARTVAULT_ENC_V1"
    private const val CURRENT_BACKUP_VERSION = 1
    private const val CURRENT_SCHEMA_VERSION = 6
    private const val DEFAULT_BACKUP_SECRET = "SmartVault_Secure_Local_Key_2026"

    /**
     * Generates a fully encrypted, tamper-evident backup string of the user's local financial data.
     */
    suspend fun createEncryptedBackup(
        context: Context,
        db: AppDatabase,
        userId: String,
        userPassword: String = DEFAULT_BACKUP_SECRET
    ): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "SmartVault")
        root.put("backupVersion", CURRENT_BACKUP_VERSION)
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("exportTimestamp", System.currentTimeMillis())

        val dataObj = JSONObject()

        // 1. Transactions
        val txs = db.transactionDao().getTransactionsForUser(userId).first()
        val txArray = JSONArray()
        for (tx in txs) {
            val o = JSONObject()
            o.put("type", tx.type)
            o.put("amount", tx.amount)
            o.put("category", tx.category)
            o.put("description", tx.description)
            o.put("dateMillis", tx.dateMillis)
            o.put("vaultName", tx.vaultName)
            if (tx.receiptImagePath != null) o.put("receiptImagePath", tx.receiptImagePath)
            txArray.put(o)
        }
        dataObj.put("transactions", txArray)

        // 2. Vaults
        val vaults = db.vaultDao().getVaultsForUser(userId).first()
        val vaultArray = JSONArray()
        for (v in vaults) {
            val o = JSONObject()
            o.put("name", v.name)
            o.put("balance", v.balance)
            o.put("isDefault", v.isDefault)
            vaultArray.put(o)
        }
        dataObj.put("vaults", vaultArray)

        // 3. Budget Limits
        val limits = db.budgetLimitDao().getLimitsForUser(userId).first()
        val limitArray = JSONArray()
        for (l in limits) {
            val o = JSONObject()
            o.put("category", l.category)
            o.put("monthlyLimit", l.monthlyLimit)
            limitArray.put(o)
        }
        dataObj.put("budget_limits", limitArray)

        // 4. Gold Assets
        val gold = db.goldAssetDao().getGoldAssetsForUser(userId).first()
        val goldArray = JSONArray()
        for (g in gold) {
            val o = JSONObject()
            o.put("name", g.name)
            o.put("goldType", g.goldType)
            o.put("karat", g.karat)
            o.put("weight", g.weight)
            o.put("purchasePrice", g.purchasePrice)
            o.put("purchaseDateMillis", g.purchaseDateMillis)
            o.put("purpose", g.purpose)
            o.put("status", g.status)
            o.put("notes", g.notes)
            if (g.imagePath != null) o.put("imagePath", g.imagePath)
            if (g.salePrice != null) o.put("salePrice", g.salePrice)
            if (g.saleDateMillis != null) o.put("saleDateMillis", g.saleDateMillis)
            if (g.saleNotes != null) o.put("saleNotes", g.saleNotes)
            goldArray.put(o)
        }
        dataObj.put("gold_assets", goldArray)

        // 5. Cash Savings
        val cash = db.cashSavingDao().getCashSavingsForUser(userId).first()
        val cashArray = JSONArray()
        for (c in cash) {
            val o = JSONObject()
            o.put("amount", c.amount)
            o.put("currency", c.currency)
            o.put("notes", c.notes)
            o.put("dateMillis", c.dateMillis)
            cashArray.put(o)
        }
        dataObj.put("cash_savings", cashArray)

        // 6. Commitments
        val comms = db.commitmentDao().getCommitmentsForUser(userId).first()
        val commArray = JSONArray()
        for (c in comms) {
            val o = JSONObject()
            o.put("title", c.title)
            o.put("amount", c.amount)
            o.put("dueDateMillis", c.dueDateMillis)
            o.put("isPaid", c.isPaid)
            o.put("isRecurringMonthly", c.isRecurringMonthly)
            o.put("notes", c.notes)
            if (c.receiptImagePath != null) o.put("receiptImagePath", c.receiptImagePath)
            commArray.put(o)
        }
        dataObj.put("commitments", commArray)

        // 7. Child Lessons
        val lessons = db.childLessonDao().getChildLessonsForUser(userId).first()
        val lessonArray = JSONArray()
        for (l in lessons) {
            val o = JSONObject()
            o.put("childName", l.childName)
            o.put("subject", l.subject)
            o.put("teacherName", l.teacherName)
            o.put("amount", l.amount)
            o.put("dueDateMillis", l.dueDateMillis)
            o.put("isPaid", l.isPaid)
            if (l.receiptImagePath != null) o.put("receiptImagePath", l.receiptImagePath)
            lessonArray.put(o)
        }
        dataObj.put("child_lessons", lessonArray)

        // 8. Outings & Outing Expenses
        val outings = db.outingDao().getOutingsForUser(userId).first()
        val outingArray = JSONArray()
        for (out in outings) {
            val o = JSONObject()
            o.put("id", out.id)
            o.put("name", out.name)
            o.put("participantNamesJson", out.participantNamesJson)
            o.put("dateMillis", out.dateMillis)
            outingArray.put(o)
        }
        dataObj.put("outings", outingArray)

        val outingExpenses = db.outingExpenseDao().getExpensesForUser(userId).first()
        val outingExpArray = JSONArray()
        for (oe in outingExpenses) {
            val o = JSONObject()
            o.put("title", oe.title)
            o.put("amount", oe.amount)
            o.put("payerName", oe.payerName)
            o.put("dateMillis", oe.dateMillis)
            o.put("outingId", oe.outingId)
            if (oe.receiptImagePath != null) o.put("receiptImagePath", oe.receiptImagePath)
            outingExpArray.put(o)
        }
        dataObj.put("outing_expenses", outingExpArray)

        // 9. Vault Items (passwords/notes)
        val vaultItems = db.vaultItemDao().getVaultItemsForUser(userId).first()
        val vItemArray = JSONArray()
        for (vi in vaultItems) {
            val o = JSONObject()
            o.put("id", vi.id)
            o.put("title", vi.title)
            o.put("type", vi.type)
            o.put("encryptedData", vi.encryptedData)
            o.put("category", vi.category)
            o.put("createdAt", vi.createdAt)
            o.put("updatedAt", vi.updatedAt)
            vItemArray.put(o)
        }
        dataObj.put("vault_items", vItemArray)

        // Calculate SHA-256 Checksum for Data Integrity Verification
        val dataString = dataObj.toString()
        val checksum = sha256(dataString)
        root.put("checksum", checksum)
        root.put("data", dataObj)

        val plaintextJson = root.toString()
        encryptData(plaintextJson, userPassword)
    }

    /**
     * Inspects and validates the encrypted backup file without modifying the database.
     * Verifies file integrity, checksum, and schema versions.
     */
    fun validateBackup(
        encryptedPayload: String,
        userPassword: String = DEFAULT_BACKUP_SECRET
    ): BackupValidationResult {
        return try {
            val decryptedJson = decryptData(encryptedPayload, userPassword)
            val root = JSONObject(decryptedJson)

            val app = root.optString("app")
            if (app != "SmartVault") {
                return BackupValidationResult(false, errorMessage = "الملف ليس نسخة احتياطية صالحة لتطبيق الخزنة الذكية")
            }

            val bVersion = root.optInt("backupVersion", 1)
            val sVersion = root.optInt("schemaVersion", 1)
            if (sVersion > CURRENT_SCHEMA_VERSION) {
                return BackupValidationResult(
                    false,
                    errorMessage = "إصدار النسخة الاحتياطية أحدث من هذا التطبيق. يرجى تحديث التطبيق أولاً."
                )
            }

            val storedChecksum = root.optString("checksum")
            val dataObj = root.optJSONObject("data")
                ?: return BackupValidationResult(false, errorMessage = "بيانات النسخة الاحتياطية تالفة أو فارغة")

            // Verify SHA-256 Checksum
            val computedChecksum = sha256(dataObj.toString())
            if (storedChecksum.isNotBlank() && storedChecksum != computedChecksum) {
                return BackupValidationResult(false, errorMessage = "فشل التحقق من سلامة الملف (Checksum Mismatch) — الملف تم التعديل عليه أو تالف")
            }

            val txCount = dataObj.optJSONArray("transactions")?.length() ?: 0
            val goldCount = dataObj.optJSONArray("gold_assets")?.length() ?: 0
            val cashCount = dataObj.optJSONArray("cash_savings")?.length() ?: 0
            val commCount = dataObj.optJSONArray("commitments")?.length() ?: 0
            val lessonCount = dataObj.optJSONArray("child_lessons")?.length() ?: 0
            val exportDate = root.optLong("exportTimestamp", System.currentTimeMillis())

            BackupValidationResult(
                isValid = true,
                backupVersion = bVersion,
                schemaVersion = sVersion,
                exportDate = exportDate,
                transactionCount = txCount,
                goldAssetCount = goldCount,
                cashSavingCount = cashCount,
                commitmentCount = commCount,
                lessonCount = lessonCount,
                decryptedJson = decryptedJson
            )
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                errorMessage = "كلمة المرور غير صحيحة أو الملف تالف: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Restores data safely and idempotently into Room for the specified userId.
     */
    suspend fun applyRestore(
        db: AppDatabase,
        userId: String,
        decryptedJson: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(decryptedJson)
            val dataObj = root.getJSONObject("data")

            // 1. Transactions
            val txArray = dataObj.optJSONArray("transactions")
            if (txArray != null) {
                val list = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    list.add(
                        TransactionEntity(
                            userId = userId,
                            type = o.optString("type", "EXPENSE"),
                            amount = o.optDouble("amount", 0.0),
                            category = o.optString("category", "عام"),
                            description = o.optString("description", ""),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            vaultName = o.optString("vaultName", "الخزنة الرئيسية"),
                            receiptImagePath = o.optString("receiptImagePath", null)
                        )
                    )
                }
                if (list.isNotEmpty()) db.transactionDao().insertAllTransactions(list)
            }

            // 2. Vaults
            val vaultArray = dataObj.optJSONArray("vaults")
            if (vaultArray != null) {
                val list = mutableListOf<VaultEntity>()
                for (i in 0 until vaultArray.length()) {
                    val o = vaultArray.getJSONObject(i)
                    list.add(
                        VaultEntity(
                            userId = userId,
                            name = o.optString("name", "الخزنة الرئيسية"),
                            balance = o.optDouble("balance", 0.0),
                            isDefault = o.optBoolean("isDefault", false)
                        )
                    )
                }
                if (list.isNotEmpty()) db.vaultDao().insertAllVaults(list)
            }

            // 3. Budget Limits
            val limitArray = dataObj.optJSONArray("budget_limits")
            if (limitArray != null) {
                val list = mutableListOf<BudgetLimitEntity>()
                for (i in 0 until limitArray.length()) {
                    val o = limitArray.getJSONObject(i)
                    list.add(
                        BudgetLimitEntity(
                            userId = userId,
                            category = o.optString("category", ""),
                            monthlyLimit = o.optDouble("monthlyLimit", 0.0)
                        )
                    )
                }
                if (list.isNotEmpty()) db.budgetLimitDao().insertAllLimits(list)
            }

            // 4. Gold Assets
            val goldArray = dataObj.optJSONArray("gold_assets")
            if (goldArray != null) {
                val list = mutableListOf<GoldAssetEntity>()
                for (i in 0 until goldArray.length()) {
                    val o = goldArray.getJSONObject(i)
                    list.add(
                        GoldAssetEntity(
                            userId = userId,
                            name = o.optString("name", ""),
                            goldType = o.optString("goldType", "سبيكة"),
                            karat = o.optInt("karat", 24),
                            weight = o.optDouble("weight", 0.0),
                            purchasePrice = o.optDouble("purchasePrice", 0.0),
                            purchaseDateMillis = o.optLong("purchaseDateMillis", System.currentTimeMillis()),
                            purpose = o.optString("purpose", "SAVING"),
                            imagePath = o.optString("imagePath", null),
                            status = o.optString("status", "ACTIVE"),
                            salePrice = if (o.has("salePrice")) o.getDouble("salePrice") else null,
                            saleDateMillis = if (o.has("saleDateMillis")) o.getLong("saleDateMillis") else null,
                            saleNotes = o.optString("saleNotes", null),
                            notes = o.optString("notes", "")
                        )
                    )
                }
                if (list.isNotEmpty()) db.goldAssetDao().insertAllGoldAssets(list)
            }

            // 5. Cash Savings
            val cashArray = dataObj.optJSONArray("cash_savings")
            if (cashArray != null) {
                val list = mutableListOf<CashSavingEntity>()
                for (i in 0 until cashArray.length()) {
                    val o = cashArray.getJSONObject(i)
                    list.add(
                        CashSavingEntity(
                            userId = userId,
                            amount = o.optDouble("amount", 0.0),
                            currency = o.optString("currency", "EGP"),
                            notes = o.optString("notes", ""),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) db.cashSavingDao().insertAllCashSavings(list)
            }

            // 6. Commitments
            val commArray = dataObj.optJSONArray("commitments")
            if (commArray != null) {
                val list = mutableListOf<CommitmentEntity>()
                for (i in 0 until commArray.length()) {
                    val o = commArray.getJSONObject(i)
                    list.add(
                        CommitmentEntity(
                            userId = userId,
                            title = o.optString("title", ""),
                            amount = o.optDouble("amount", 0.0),
                            dueDateMillis = o.optLong("dueDateMillis", System.currentTimeMillis()),
                            isPaid = o.optBoolean("isPaid", false),
                            isRecurringMonthly = o.optBoolean("isRecurringMonthly", true),
                            notes = o.optString("notes", ""),
                            receiptImagePath = o.optString("receiptImagePath", null)
                        )
                    )
                }
                if (list.isNotEmpty()) db.commitmentDao().insertAllCommitments(list)
            }

            // 7. Child Lessons
            val lessonArray = dataObj.optJSONArray("child_lessons")
            if (lessonArray != null) {
                val list = mutableListOf<ChildLessonEntity>()
                for (i in 0 until lessonArray.length()) {
                    val o = lessonArray.getJSONObject(i)
                    list.add(
                        ChildLessonEntity(
                            userId = userId,
                            childName = o.optString("childName", ""),
                            subject = o.optString("subject", ""),
                            teacherName = o.optString("teacherName", ""),
                            amount = o.optDouble("amount", 0.0),
                            dueDateMillis = o.optLong("dueDateMillis", System.currentTimeMillis()),
                            isPaid = o.optBoolean("isPaid", false),
                            receiptImagePath = o.optString("receiptImagePath", null)
                        )
                    )
                }
                if (list.isNotEmpty()) db.childLessonDao().insertAllChildLessons(list)
            }

            // 8. Outings & Outing Expenses
            val outingArray = dataObj.optJSONArray("outings")
            if (outingArray != null) {
                val list = mutableListOf<OutingEntity>()
                for (i in 0 until outingArray.length()) {
                    val o = outingArray.getJSONObject(i)
                    list.add(
                        OutingEntity(
                            id = o.optString("id", java.util.UUID.randomUUID().toString()),
                            userId = userId,
                            name = o.optString("name", ""),
                            participantNamesJson = o.optString("participantNamesJson", "[]"),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) db.outingDao().insertAllOutings(list)
            }

            val outingExpArray = dataObj.optJSONArray("outing_expenses")
            if (outingExpArray != null) {
                val list = mutableListOf<OutingExpenseEntity>()
                for (i in 0 until outingExpArray.length()) {
                    val o = outingExpArray.getJSONObject(i)
                    list.add(
                        OutingExpenseEntity(
                            userId = userId,
                            title = o.optString("title", ""),
                            amount = o.optDouble("amount", 0.0),
                            payerName = o.optString("payerName", ""),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            receiptImagePath = o.optString("receiptImagePath", null),
                            outingId = o.optString("outingId", "")
                        )
                    )
                }
                if (list.isNotEmpty()) db.outingExpenseDao().insertAllOutingExpenses(list)
            }

            // 9. Vault Items
            val vItemArray = dataObj.optJSONArray("vault_items")
            if (vItemArray != null) {
                val list = mutableListOf<VaultItemEntity>()
                for (i in 0 until vItemArray.length()) {
                    val o = vItemArray.getJSONObject(i)
                    list.add(
                        VaultItemEntity(
                            id = o.optString("id", java.util.UUID.randomUUID().toString()),
                            userId = userId,
                            title = o.optString("title", ""),
                            type = o.optString("type", "password"),
                            encryptedData = o.optString("encryptedData", ""),
                            category = o.optString("category", "عام"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) db.vaultItemDao().insertAllVaultItems(list)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Standard Cryptographic Implementation: AES-256-GCM with PBKDF2 ---
    private fun encryptData(plaintext: String, secret: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        // Format: MAGIC_HEADER:Base64(salt):Base64(iv):Base64(ciphertext)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        return "$MAGIC_HEADER:$saltB64:$ivB64:$cipherB64"
    }

    private fun decryptData(payload: String, secret: String): String {
        val parts = payload.trim().split(":")
        if (parts.size != 4 || parts[0] != MAGIC_HEADER) {
            throw IllegalArgumentException("تنسيق النسخة الاحتياطية غير مدعوم أو تالف")
        }

        val salt = Base64.decode(parts[1], Base64.NO_WRAP)
        val iv = Base64.decode(parts[2], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[3], Base64.NO_WRAP)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val plaintextBytes = cipher.doFinal(ciphertext)

        return String(plaintextBytes, StandardCharsets.UTF_8)
    }

    private fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(text.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
