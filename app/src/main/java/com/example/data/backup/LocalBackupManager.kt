package com.example.data.backup

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.crypto.CryptoManager
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.DebtEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.NetWorthSnapshotEntity
import com.example.data.entity.OutingEntity
import com.example.data.entity.OutingExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransferEntity
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

enum class RestorePolicy {
    REPLACE,
    SAFE_MERGE
}

object LocalBackupManager {
    const val MAGIC_HEADER_V1 = "SMARTVAULT_ENC_V1"
    const val MAGIC_HEADER_V2 = "SMARTVAULT_ENC_V2"
    const val MAGIC_HEADER_V3 = "SMARTVAULT_ENC_V3"
    private const val CURRENT_BACKUP_VERSION = 3
    private const val CURRENT_SCHEMA_VERSION = 7
    private const val DEFAULT_BACKUP_SECRET = "SmartVault_Secure_Local_Key_2026"

    /**
     * Generates a fully encrypted, tamper-evident backup string of the user's local financial data.
     * Uses a portable V3 envelope and requires an explicit user-provided password.
     */
    suspend fun createEncryptedBackup(
        context: Context,
        db: AppDatabase,
        userId: String,
        userPassword: String
    ): String = withContext(Dispatchers.IO) {
        if (userPassword.isBlank() || userPassword == DEFAULT_BACKUP_SECRET) {
            throw IllegalArgumentException("يجب تحديد كلمة مرور لحماية وتشفير النسخة الاحتياطية")
        }
        db.withTransaction {
        val root = JSONObject()
        root.put("app", "SmartVault")
        root.put("backupVersion", CURRENT_BACKUP_VERSION)
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("mediaIncluded", false)
        root.put("mediaNote", "مسارات إيصالات الصور هي مراجع محلية على الجهاز ولا يتم تضمينها كملفات ثنائية محمولة")

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
            // This plaintext is only serialized inside the password-encrypted envelope.
            o.put("portableData", CryptoManager.decrypt(vi.encryptedData))
            o.put("category", vi.category)
            o.put("createdAt", vi.createdAt)
            o.put("updatedAt", vi.updatedAt)
            vItemArray.put(o)
        }
        dataObj.put("vault_items", vItemArray)

        // 10. Transfers
        val transfers = db.transferDao().getTransfersForUser(userId).first()
        val transferArray = JSONArray()
        for (t in transfers) {
            val o = JSONObject()
            o.put("fromVaultName", t.fromVaultName)
            o.put("toVaultName", t.toVaultName)
            o.put("amount", t.amount)
            o.put("dateMillis", t.dateMillis)
            o.put("notes", t.notes)
            transferArray.put(o)
        }
        dataObj.put("transfers", transferArray)

        // 11. Debts
        val debts = db.debtDao().getDebtsForUser(userId).first()
        val debtArray = JSONArray()
        for (d in debts) {
            val o = JSONObject()
            o.put("id", d.id)
            o.put("personName", d.personName)
            o.put("type", d.type)
            o.put("originalAmount", d.originalAmount)
            o.put("paidAmount", d.paidAmount)
            o.put("remainingAmount", d.remainingAmount)
            o.put("startDateMillis", d.startDateMillis)
            if (d.dueDateMillis != null) o.put("dueDateMillis", d.dueDateMillis)
            o.put("notes", d.notes)
            o.put("status", d.status)
            o.put("createdAt", d.createdAt)
            o.put("updatedAt", d.updatedAt)
            debtArray.put(o)
        }
        dataObj.put("debts", debtArray)

        // 12. Debt Payments
        val payments = db.debtPaymentDao().getAllPaymentsForUser(userId).first()
        val payArray = JSONArray()
        for (p in payments) {
            val o = JSONObject()
            o.put("debtId", p.debtId)
            o.put("amount", p.amount)
            o.put("dateMillis", p.dateMillis)
            o.put("notes", p.notes)
            payArray.put(o)
        }
        dataObj.put("debt_payments", payArray)

        // 13. Net Worth Snapshots
        val snapshots = db.netWorthSnapshotDao().getSnapshotsForUser(userId).first()
        val snapArray = JSONArray()
        for (s in snapshots) {
            val o = JSONObject()
            o.put("dateMillis", s.dateMillis)
            o.put("dateKey", s.dateKey)
            o.put("totalAssets", s.totalAssets)
            o.put("totalLiabilities", s.totalLiabilities)
            o.put("netWorth", s.netWorth)
            o.put("vaultsTotal", s.vaultsTotal)
            o.put("cashSavingsTotal", s.cashSavingsTotal)
            o.put("goldValueTotal", s.goldValueTotal)
            o.put("debtsOwedToMeTotal", s.debtsOwedToMeTotal)
            o.put("debtsIOweTotal", s.debtsIOweTotal)
            snapArray.put(o)
        }
        dataObj.put("net_worth_snapshots", snapArray)

        // Calculate SHA-256 Checksum for Data Integrity Verification
        val dataString = dataObj.toString()
        val checksum = sha256(dataString)
        root.put("checksum", checksum)
        root.put("data", dataObj)

        val plaintextJson = root.toString()
        encryptData(plaintextJson, userPassword)
        }
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
            val trimmed = encryptedPayload.trim()
            val isV2 = trimmed.startsWith(MAGIC_HEADER_V2) || trimmed.startsWith(MAGIC_HEADER_V3)

            val decryptedJson = if (isV2) {
                // V2 requires explicit user password and strictly rejects legacy default password fallback
                if (userPassword.isBlank() || userPassword == DEFAULT_BACKUP_SECRET) {
                    throw IllegalArgumentException("تتطلب هذه النسخة إدخال كلمة المرور المخصصة التي تم إنشاؤها بها")
                }
                decryptData(trimmed, userPassword)
            } else {
                // V1 legacy support: try user password first, then fallback to legacy secret
                try {
                    decryptData(trimmed, userPassword)
                } catch (e: Exception) {
                    if (userPassword != DEFAULT_BACKUP_SECRET) {
                        try {
                            decryptData(trimmed, DEFAULT_BACKUP_SECRET)
                        } catch (_: Exception) {
                            throw e
                        }
                    } else {
                        throw e
                    }
                }
            }
            val root = JSONObject(decryptedJson)

            val app = root.optString("app")
            if (app != "SmartVault") {
                return BackupValidationResult(false, errorMessage = "الملف ليس نسخة احتياطية صالحة لتطبيق الخزنة الذكية")
            }

            val bVersion = root.optInt("backupVersion", 1)
            require(bVersion in 1..CURRENT_BACKUP_VERSION) { "إصدار النسخة الاحتياطية غير مدعوم" }
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

            // --- STRICT RELATIONAL INTEGRITY VALIDATION ---
            // 1. Debt -> Payments validation: reject backups with orphaned payments
            val debtArray = dataObj.optJSONArray("debts")
            val debtIds = mutableSetOf<Int>()
            if (debtArray != null) {
                for (i in 0 until debtArray.length()) {
                    val d = debtArray.getJSONObject(i)
                    val id = d.optInt("id", 0)
                    if (id > 0) debtIds.add(id)
                    val orig = d.optDouble("originalAmount", 0.0)
                    val rem = d.optDouble("remainingAmount", 0.0)
                    val paid = d.optDouble("paidAmount", 0.0)
                    if (orig.isNaN() || orig.isInfinite() || rem.isNaN() || rem.isInfinite() || paid.isNaN() || paid.isInfinite()) {
                        return BackupValidationResult(false, errorMessage = "النسخة تحتوي على مبالغ ديون غير صالحة (NaN أو Infinity)")
                    }
                }
            }

            val payArray = dataObj.optJSONArray("debt_payments")
            if (payArray != null) {
                for (i in 0 until payArray.length()) {
                    val p = payArray.getJSONObject(i)
                    val debtId = p.optInt("debtId", 0)
                    if (!debtIds.contains(debtId)) {
                        return BackupValidationResult(
                            false,
                            errorMessage = "النسخة تحتوي على دفعات دين يتيمة لا ترتبط بأي دين مسجل (سجلات يتيمة: debtId = $debtId)"
                        )
                    }
                    val amt = p.optDouble("amount", 0.0)
                    if (amt.isNaN() || amt.isInfinite() || amt <= 0.0) {
                        return BackupValidationResult(false, errorMessage = "النسخة تحتوي على مبالغ دفعات دين غير صالحة")
                    }
                }
            }

            // 2. Outing -> Outing Expenses validation: reject backups with orphaned outing expenses
            val outingArray = dataObj.optJSONArray("outings")
            val outingIds = mutableSetOf<String>()
            if (outingArray != null) {
                for (i in 0 until outingArray.length()) {
                    val out = outingArray.getJSONObject(i)
                    val id = out.optString("id", "")
                    if (id.isNotBlank()) outingIds.add(id)
                }
            }

            val outingExpArray = dataObj.optJSONArray("outing_expenses")
            if (outingExpArray != null) {
                for (i in 0 until outingExpArray.length()) {
                    val oe = outingExpArray.getJSONObject(i)
                    val oId = oe.optString("outingId", "")
                    if (oId.isNotBlank() && !outingIds.contains(oId)) {
                        return BackupValidationResult(
                            false,
                            errorMessage = "النسخة تحتوي على مصاريف خرجة يتيمة لا ترتبط بأي خرجة مسجلة (سجلات يتيمة: outingId = $oId)"
                        )
                    }
                    val amt = oe.optDouble("amount", 0.0)
                    if (amt.isNaN() || amt.isInfinite() || amt < 0.0) {
                        return BackupValidationResult(false, errorMessage = "النسخة تحتوي على مبالغ مصاريف خرجة غير صالحة")
                    }
                }
            }

            // 3. Transactions validation
            val txArray = dataObj.optJSONArray("transactions")
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val tx = txArray.getJSONObject(i)
                    val amt = tx.optDouble("amount", 0.0)
                    if (amt.isNaN() || amt.isInfinite() || amt <= 0.0) {
                        return BackupValidationResult(false, errorMessage = "النسخة تحتوي على معاملات مالية بمبالغ غير صالحة")
                    }
                }
            }

            // 4. Cash savings validation
            val cashArray = dataObj.optJSONArray("cash_savings")
            if (cashArray != null) {
                for (i in 0 until cashArray.length()) {
                    val cs = cashArray.getJSONObject(i)
                    val amt = cs.optDouble("amount", 0.0)
                    if (amt.isNaN() || amt.isInfinite() || amt < 0.0) {
                        return BackupValidationResult(false, errorMessage = "النسخة تحتوي على مدخرات نقدية بمبالغ غير صالحة")
                    }
                }
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
     * Restores data safely, atomically, and idempotently into Room for the specified userId.
     * Prevents duplication on repeated restores by clearing existing records in an atomic transaction.
     * Preserves strict relational integrity by mapping old debt IDs to new auto-generated IDs.
     */
    suspend fun applyRestore(
        db: AppDatabase,
        userId: String,
        decryptedJson: String,
        policy: RestorePolicy = RestorePolicy.REPLACE
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Legacy backups have no stable identities for every record. Merging them
            // can duplicate assets and corrupt balances; fail before any database write.
            require(policy == RestorePolicy.REPLACE) { "الدمج غير مدعوم؛ استخدم الاستبدال بعد حفظ نسخة من بياناتك" }
            val root = JSONObject(decryptedJson)
            val dataObj = root.getJSONObject("data")

            db.withTransaction {
                if (policy == RestorePolicy.REPLACE) {
                    // Clear existing financial data for target user inside atomic transaction
                    db.transactionDao().clearUserTransactions(userId)
                    db.vaultDao().clearUserVaults(userId)
                    db.budgetLimitDao().clearUserLimits(userId)
                    db.goldAssetDao().clearUserGoldAssets(userId)
                    db.cashSavingDao().clearUserCashSavings(userId)
                    db.commitmentDao().clearUserCommitments(userId)
                    db.childLessonDao().clearUserChildLessons(userId)
                    db.vaultItemDao().clearUserVaultItems(userId)
                    db.activityLogDao().clearUserLogs(userId)
                    db.outingDao().clearUserOutings(userId)
                    db.outingExpenseDao().clearUserOutingExpenses(userId)
                    db.transferDao().clearUserTransfers(userId)
                    db.debtDao().clearUserDebts(userId)
                    db.debtPaymentDao().clearUserDebtPayments(userId)
                    db.netWorthSnapshotDao().clearUserSnapshots(userId)
                }

                // 1. Transactions
                val txArray = dataObj.optJSONArray("transactions")
                if (txArray != null) {
                    val existingTxs = if (policy == RestorePolicy.SAFE_MERGE) {
                        db.transactionDao().getTransactionsForUser(userId).first()
                    } else emptyList()

                    val list = mutableListOf<TransactionEntity>()
                    for (i in 0 until txArray.length()) {
                        val o = txArray.getJSONObject(i)
                        val type = o.optString("type", "EXPENSE")
                        val amount = o.optDouble("amount", 0.0)
                        val category = o.optString("category", "عام")
                        val description = o.optString("description", "")
                        val dateMillis = o.optLong("dateMillis", System.currentTimeMillis())
                        val vaultName = o.optString("vaultName", "الخزنة الرئيسية")
                        val receiptPath = o.optString("receiptImagePath", null)

                        if (policy == RestorePolicy.SAFE_MERGE) {
                            val isDuplicate = existingTxs.any {
                                it.amount == amount && it.dateMillis == dateMillis && it.type == type && it.description == description
                            }
                            if (isDuplicate) continue
                        }

                        list.add(
                            TransactionEntity(
                                userId = userId,
                                type = type,
                                amount = amount,
                                category = category,
                                description = description,
                                dateMillis = dateMillis,
                                vaultName = vaultName,
                                receiptImagePath = receiptPath
                            )
                        )
                    }
                    if (list.isNotEmpty()) db.transactionDao().insertAllTransactions(list)
                }

                // 2. Vaults
                val vaultArray = dataObj.optJSONArray("vaults")
                if (vaultArray != null) {
                    val existingVaults = if (policy == RestorePolicy.SAFE_MERGE) {
                        db.vaultDao().getVaultsForUser(userId).first()
                    } else emptyList()

                    val list = mutableListOf<VaultEntity>()
                    for (i in 0 until vaultArray.length()) {
                        val o = vaultArray.getJSONObject(i)
                        val name = o.optString("name", "الخزنة الرئيسية")
                        val balance = o.optDouble("balance", 0.0)
                        val isDefault = o.optBoolean("isDefault", false)

                        if (policy == RestorePolicy.SAFE_MERGE) {
                            val exists = existingVaults.any { it.name == name }
                            if (exists) continue
                        }

                        list.add(
                            VaultEntity(
                                userId = userId,
                                name = name,
                                balance = balance,
                                isDefault = isDefault
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
                val restoredOutingIds = mutableMapOf<String, String>()
                val outingArray = dataObj.optJSONArray("outings")
                if (outingArray != null) {
                    val list = mutableListOf<OutingEntity>()
                    for (i in 0 until outingArray.length()) {
                        val o = outingArray.getJSONObject(i)
                        val restoredId = java.util.UUID.randomUUID().toString()
                        restoredOutingIds[o.getString("id")] = restoredId
                        list.add(
                            OutingEntity(
                                id = restoredId,
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
                                outingId = o.optString("outingId", "").let { oldId ->
                                    if (oldId.isBlank()) "" else restoredOutingIds[oldId]
                                        ?: error("تعذر ربط مصروف بالخروجة الأصلية")
                                }
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
                                id = java.util.UUID.randomUUID().toString(),
                                userId = userId,
                                title = o.optString("title", ""),
                                type = o.optString("type", "password"),
                                encryptedData = if (o.has("portableData")) {
                                    CryptoManager.encrypt(o.getString("portableData"))
                                } else {
                                    // Old device-bound backups must still be decryptable here.
                                    // Throwing rolls back the entire restore if the key is gone.
                                    CryptoManager.encrypt(CryptoManager.decrypt(o.getString("encryptedData")))
                                },
                                category = o.optString("category", "عام"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                    if (list.isNotEmpty()) db.vaultItemDao().insertAllVaultItems(list)
                }

                // 10. Transfers
                val transferArray = dataObj.optJSONArray("transfers")
                if (transferArray != null) {
                    val list = mutableListOf<TransferEntity>()
                    for (i in 0 until transferArray.length()) {
                        val o = transferArray.getJSONObject(i)
                        list.add(
                            TransferEntity(
                                userId = userId,
                                fromVaultName = o.optString("fromVaultName", ""),
                                toVaultName = o.optString("toVaultName", ""),
                                amount = o.optDouble("amount", 0.0),
                                dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                                notes = o.optString("notes", "")
                            )
                        )
                    }
                    if (list.isNotEmpty()) db.transferDao().insertAllTransfers(list)
                }

                // 11. Debts with oldID -> newID relationship mapping
                val oldToNewDebtId = mutableMapOf<Int, Int>()
                val debtArray = dataObj.optJSONArray("debts")
                if (debtArray != null) {
                    for (i in 0 until debtArray.length()) {
                        val o = debtArray.getJSONObject(i)
                        val oldId = o.optInt("id", 0)
                        val debt = DebtEntity(
                            userId = userId,
                            personName = o.optString("personName", ""),
                            type = o.optString("type", "OWED_TO_ME"),
                            originalAmount = o.optDouble("originalAmount", 0.0),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            remainingAmount = o.optDouble("remainingAmount", 0.0),
                            startDateMillis = o.optLong("startDateMillis", System.currentTimeMillis()),
                            dueDateMillis = if (o.has("dueDateMillis")) o.getLong("dueDateMillis") else null,
                            notes = o.optString("notes", ""),
                            status = o.optString("status", "ACTIVE"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                        val newId = db.debtDao().insertDebt(debt).toInt()
                        if (oldId > 0) {
                            oldToNewDebtId[oldId] = newId
                        }
                    }
                }

                // 12. Debt Payments with mapped foreign key reference
                val payArray = dataObj.optJSONArray("debt_payments")
                if (payArray != null) {
                    val list = mutableListOf<DebtPaymentEntity>()
                    for (i in 0 until payArray.length()) {
                        val o = payArray.getJSONObject(i)
                        val oldDebtId = o.optInt("debtId", 0)
                        val targetDebtId = oldToNewDebtId[oldDebtId]
                            ?: throw IllegalStateException("فشل ربط دفعة الدين: الدين الأصلي (id=$oldDebtId) غير موجود في النسخة")
                        list.add(
                            DebtPaymentEntity(
                                debtId = targetDebtId,
                                userId = userId,
                                amount = o.optDouble("amount", 0.0),
                                dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                                notes = o.optString("notes", "")
                            )
                        )
                    }
                    if (list.isNotEmpty()) db.debtPaymentDao().insertAllPayments(list)
                }

                // 13. Net Worth Snapshots
                val snapArray = dataObj.optJSONArray("net_worth_snapshots")
                if (snapArray != null) {
                    val list = mutableListOf<NetWorthSnapshotEntity>()
                    for (i in 0 until snapArray.length()) {
                        val o = snapArray.getJSONObject(i)
                        list.add(
                            NetWorthSnapshotEntity(
                                userId = userId,
                                dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                                dateKey = o.optString("dateKey", ""),
                                totalAssets = o.optDouble("totalAssets", 0.0),
                                totalLiabilities = o.optDouble("totalLiabilities", 0.0),
                                netWorth = o.optDouble("netWorth", 0.0),
                                vaultsTotal = o.optDouble("vaultsTotal", 0.0),
                                cashSavingsTotal = o.optDouble("cashSavingsTotal", 0.0),
                                goldValueTotal = o.optDouble("goldValueTotal", 0.0),
                                debtsOwedToMeTotal = o.optDouble("debtsOwedToMeTotal", 0.0),
                                debtsIOweTotal = o.optDouble("debtsIOweTotal", 0.0)
                            )
                        )
                    }
                    if (list.isNotEmpty()) db.netWorthSnapshotDao().insertAllSnapshots(list)
                }
            } // end db.withTransaction

            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
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

        // Format: MAGIC_HEADER_V3:Base64(salt):Base64(iv):Base64(ciphertext)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        return "$MAGIC_HEADER_V3:$saltB64:$ivB64:$cipherB64"
    }

    private fun decryptData(payload: String, secret: String): String {
        val parts = payload.trim().split(":")
        if (parts.size != 4 || parts[0] !in setOf(MAGIC_HEADER_V1, MAGIC_HEADER_V2, MAGIC_HEADER_V3)) {
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
