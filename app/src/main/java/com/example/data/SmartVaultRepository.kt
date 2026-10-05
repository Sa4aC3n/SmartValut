package com.example.data

import com.example.ui.utils.AppText

import androidx.room.withTransaction
import com.example.data.entity.ActivityLogEntity
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.DebtEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.GoldPriceEntity
import com.example.data.entity.NetWorthSnapshotEntity
import com.example.data.entity.OutingEntity
import com.example.data.entity.OutingExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransferEntity
import com.example.data.entity.VaultEntity
import com.example.data.entity.VaultItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray

class SmartVaultRepository(private val db: AppDatabase) {
    // Flow getters scoped by userId for strict Multi-User Isolation
    fun getTransactions(userId: String): Flow<List<TransactionEntity>> =
        db.transactionDao().getTransactionsForUser(userId)

    fun getVaults(userId: String): Flow<List<VaultEntity>> =
        db.vaultDao().getVaultsForUser(userId)

    fun getBudgetLimits(userId: String): Flow<List<BudgetLimitEntity>> =
        db.budgetLimitDao().getLimitsForUser(userId)

    fun getOutings(userId: String): Flow<List<OutingEntity>> =
        db.outingDao().getOutingsForUser(userId)

    fun getOutingExpenses(userId: String): Flow<List<OutingExpenseEntity>> =
        db.outingExpenseDao().getExpensesForUser(userId)

    fun getExpensesForOuting(outingId: String, userId: String): Flow<List<OutingExpenseEntity>> =
        db.outingExpenseDao().getExpensesForOuting(outingId, userId)

    fun getGoldAssets(userId: String): Flow<List<GoldAssetEntity>> =
        db.goldAssetDao().getGoldAssetsForUser(userId)

    fun getCashSavings(userId: String): Flow<List<CashSavingEntity>> =
        db.cashSavingDao().getCashSavingsForUser(userId)

    fun getCommitments(userId: String): Flow<List<CommitmentEntity>> =
        db.commitmentDao().getCommitmentsForUser(userId)

    fun getChildLessons(userId: String): Flow<List<ChildLessonEntity>> =
        db.childLessonDao().getChildLessonsForUser(userId)

    fun getVaultItems(userId: String): Flow<List<VaultItemEntity>> =
        db.vaultItemDao().getVaultItemsForUser(userId)

    fun getActivityLogs(userId: String): Flow<List<ActivityLogEntity>> =
        db.activityLogDao().getLogsForUser(userId)

    fun getTransfers(userId: String): Flow<List<TransferEntity>> =
        db.transferDao().getTransfersForUser(userId)

    fun getDebts(userId: String): Flow<List<DebtEntity>> =
        db.debtDao().getDebtsForUser(userId)

    fun getDebtPayments(debtId: Int, userId: String): Flow<List<DebtPaymentEntity>> =
        db.debtPaymentDao().getPaymentsForDebt(debtId, userId)

    fun getAllDebtPayments(userId: String): Flow<List<DebtPaymentEntity>> =
        db.debtPaymentDao().getAllPaymentsForUser(userId)

    fun getNetWorthSnapshots(userId: String): Flow<List<NetWorthSnapshotEntity>> =
        db.netWorthSnapshotDao().getSnapshotsForUser(userId)

    // Backward compatibility flows
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val allVaults: Flow<List<VaultEntity>> = db.vaultDao().getAllVaults()
    val allBudgetLimits: Flow<List<BudgetLimitEntity>> = db.budgetLimitDao().getAllBudgetLimits()
    val allOutingExpenses: Flow<List<OutingExpenseEntity>> = db.outingExpenseDao().getAllOutingExpenses()
    val allGoldPrices: Flow<List<GoldPriceEntity>> = db.goldPriceDao().getAllGoldPrices()

    // 1. Transactions Actions
    suspend fun addIncome(
        amount: Double,
        category: String,
        description: String,
        dateMillis: Long = System.currentTimeMillis(),
        vaultName: String = "الخزنة الرئيسية",
        userId: String = ""
    ) {
        if (amount.isNaN() || amount.isInfinite() || amount <= 0.0) {
            throw IllegalArgumentException(AppText.text(com.example.R.string.text_cd638da461ee))
        }

        val effectiveUser = userId.ifBlank { "local_guest" }
        db.withTransaction {
            val tx = TransactionEntity(
                userId = effectiveUser,
                type = "INCOME",
                amount = amount,
                category = category,
                description = description,
                dateMillis = dateMillis,
                vaultName = vaultName
            )
            db.transactionDao().insertTransaction(tx)

            // Add to vault balance atomically
            val vault = db.vaultDao().getVaultByName(vaultName, effectiveUser)
            if (vault != null) {
                db.vaultDao().updateVaultBalance(vault.id, vault.balance + amount, effectiveUser)
            } else {
                db.vaultDao().insertVault(VaultEntity(name = vaultName, balance = amount, isDefault = true, userId = effectiveUser))
            }
        }
    }

    suspend fun addExpense(
        amount: Double,
        category: String,
        description: String,
        dateMillis: Long = System.currentTimeMillis(),
        vaultName: String = "الخزنة الرئيسية",
        receiptPath: String? = null,
        userId: String = ""
    ) {
        if (amount.isNaN() || amount.isInfinite() || amount <= 0.0) {
            throw IllegalArgumentException(AppText.text(com.example.R.string.text_cd638da461ee))
        }

        val effectiveUser = userId.ifBlank { "local_guest" }
        db.withTransaction {
            val tx = TransactionEntity(
                userId = effectiveUser,
                type = "EXPENSE",
                amount = amount,
                category = category,
                description = description,
                dateMillis = dateMillis,
                vaultName = vaultName,
                receiptImagePath = receiptPath
            )
            db.transactionDao().insertTransaction(tx)

            // Deduct from vault balance atomically preserving true mathematical accounting
            val vault = db.vaultDao().getVaultByName(vaultName, effectiveUser)
            if (vault != null) {
                db.vaultDao().updateVaultBalance(vault.id, vault.balance - amount, effectiveUser)
            }
        }
    }

    suspend fun deleteTransaction(id: Int, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.withTransaction {
            val transaction = db.transactionDao().getTransactionById(id, effectiveUser)
                ?: return@withTransaction
            val vault = db.vaultDao().getVaultByName(transaction.vaultName, effectiveUser)
                ?: error(AppText.text(com.example.R.string.text_324813c834f1))
            val adjustment = when (transaction.type) {
                "INCOME" -> -transaction.amount
                "EXPENSE" -> transaction.amount
                else -> error(AppText.text(com.example.R.string.text_d375d2c8d4ee))
            }
            db.vaultDao().updateVaultBalance(vault.id, vault.balance + adjustment, effectiveUser)
            db.transactionDao().deleteTransactionByIdAndUser(id, effectiveUser)
        }
    }

    // 2. Vault Actions
    suspend fun addVault(name: String, initialBalance: Double, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.vaultDao().insertVault(VaultEntity(name = name, balance = initialBalance, isDefault = false, userId = effectiveUser))
    }

    suspend fun updateVault(id: Int, name: String, balance: Double, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        val existing = db.vaultDao().getVaultById(id, effectiveUser)
        if (existing != null) {
            db.vaultDao().updateVault(existing.copy(name = name, balance = balance))
        }
    }

    suspend fun deleteVault(id: Int, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.vaultDao().deleteVaultByIdAndUser(id, effectiveUser)
    }

    // 3. Budget Limits
    suspend fun setBudgetLimit(category: String, limit: Double, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.budgetLimitDao().insertOrUpdateLimit(BudgetLimitEntity(category = category, monthlyLimit = limit, userId = effectiveUser))
    }

    suspend fun deleteBudgetLimit(category: String, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.budgetLimitDao().deleteLimit(category, effectiveUser)
    }

    // 4. Outings & Expenses
    suspend fun addOuting(id: String, name: String, participantNames: List<String>, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        val jsonArray = JSONArray()
        participantNames.forEach { jsonArray.put(it) }
        db.outingDao().insertOuting(
            OutingEntity(
                id = id,
                userId = effectiveUser,
                name = name,
                participantNamesJson = jsonArray.toString(),
                dateMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteOuting(id: String, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.outingDao().deleteOutingByIdAndUser(id, effectiveUser)
    }

    suspend fun addOutingExpense(
        title: String,
        amount: Double,
        payerName: String = "",
        receiptPath: String? = null,
        outingId: String = "",
        userId: String = "",
        vaultName: String? = null
    ) {
        val effectiveUser = userId.ifBlank { "local_guest" }
        if (amount.isNaN() || amount.isInfinite() || amount < 0.0) {
            throw IllegalArgumentException(AppText.text(com.example.R.string.text_a4de2402af53))
        }
        db.withTransaction {
            db.outingExpenseDao().insertOutingExpense(
                OutingExpenseEntity(
                    title = title,
                    amount = amount,
                    payerName = payerName,
                    receiptImagePath = receiptPath,
                    outingId = outingId,
                    userId = effectiveUser
                )
            )
            if (!vaultName.isNullOrBlank() && amount > 0.0) {
                val vault = db.vaultDao().getVaultByName(vaultName, effectiveUser)
                if (vault != null) {
                    db.vaultDao().updateVaultBalance(vault.id, vault.balance - amount, effectiveUser)
                }
            }
        }
    }

    suspend fun deleteOutingExpense(id: Int, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.outingExpenseDao().deleteOutingExpenseByIdAndUser(id, effectiveUser)
    }

    suspend fun clearAllOutingExpenses(userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.outingExpenseDao().clearUserOutingExpenses(effectiveUser)
    }

    // 5. Gold Assets
    suspend fun addGoldAsset(asset: GoldAssetEntity): Long {
        return db.goldAssetDao().insertGoldAsset(asset)
    }

    suspend fun updateGoldAsset(asset: GoldAssetEntity) {
        db.goldAssetDao().updateGoldAsset(asset)
    }

    suspend fun deleteGoldAsset(id: Int, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.goldAssetDao().deleteGoldAssetByIdAndUser(id, effectiveUser)
    }

    suspend fun sellGoldAsset(
        id: Int,
        salePrice: Double,
        saleDateMillis: Long = System.currentTimeMillis(),
        saleNotes: String? = null,
        userId: String = "",
        vaultName: String? = null
    ) {
        val effectiveUser = userId.ifBlank { "local_guest" }
        if (salePrice.isNaN() || salePrice.isInfinite() || salePrice < 0.0) {
            throw IllegalArgumentException(AppText.text(com.example.R.string.text_709b83a58038))
        }
        db.withTransaction {
            val asset = db.goldAssetDao().getGoldAssetByIdAndUser(id, effectiveUser)
            if (asset != null) {
                db.goldAssetDao().updateGoldAsset(
                    asset.copy(
                        status = "SOLD",
                        salePrice = salePrice,
                        saleDateMillis = saleDateMillis,
                        saleNotes = saleNotes,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                if (!vaultName.isNullOrBlank() && salePrice > 0.0) {
                    val vault = db.vaultDao().getVaultByName(vaultName, effectiveUser)
                    if (vault != null) {
                        db.vaultDao().updateVaultBalance(vault.id, vault.balance + salePrice, effectiveUser)
                    }
                }
            }
        }
    }

    // 6. Cash Savings
    suspend fun addCashSaving(
        amount: Double,
        currency: String = "EGP",
        notes: String = "",
        dateMillis: Long = System.currentTimeMillis(),
        userId: String = ""
    ): Long {
        val effectiveUser = userId.ifBlank { "local_guest" }
        return db.cashSavingDao().insertCashSaving(
            CashSavingEntity(
                userId = effectiveUser,
                amount = amount,
                currency = currency,
                notes = notes,
                dateMillis = dateMillis
            )
        )
    }

    suspend fun updateCashSaving(saving: CashSavingEntity) {
        db.cashSavingDao().updateCashSaving(saving)
    }

    suspend fun deleteCashSaving(id: Int, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.cashSavingDao().deleteCashSavingByIdAndUser(id, effectiveUser)
    }

    // 7. Commitments
    suspend fun addCommitment(
        title: String,
        amount: Double,
        dueDateMillis: Long = System.currentTimeMillis(),
        isPaid: Boolean = false,
        isRecurring: Boolean = true,
        notes: String = "",
        receiptPath: String? = null,
        userId: String = ""
    ): Long {
        val effectiveUser = userId.ifBlank { "local_guest" }
        return db.commitmentDao().insertCommitment(
            CommitmentEntity(
                userId = effectiveUser,
                title = title,
                amount = amount,
                dueDateMillis = dueDateMillis,
                isPaid = isPaid,
                isRecurringMonthly = isRecurring,
                notes = notes,
                receiptImagePath = receiptPath
            )
        )
    }

    suspend fun updateCommitment(commitment: CommitmentEntity) {
        db.commitmentDao().updateCommitment(commitment)
    }

    suspend fun deleteCommitment(id: Int, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.commitmentDao().deleteCommitmentByIdAndUser(id, effectiveUser)
    }

    // 8. Child Lessons
    suspend fun addChildLesson(
        childName: String,
        subject: String,
        teacherName: String,
        amount: Double,
        dueDateMillis: Long = System.currentTimeMillis(),
        isPaid: Boolean = false,
        receiptPath: String? = null,
        userId: String = ""
    ): Long {
        val effectiveUser = userId.ifBlank { "local_guest" }
        return db.childLessonDao().insertChildLesson(
            ChildLessonEntity(
                userId = effectiveUser,
                childName = childName,
                subject = subject,
                teacherName = teacherName,
                amount = amount,
                dueDateMillis = dueDateMillis,
                isPaid = isPaid,
                receiptImagePath = receiptPath
            )
        )
    }

    suspend fun updateChildLesson(lesson: ChildLessonEntity) {
        db.childLessonDao().updateChildLesson(lesson)
    }

    suspend fun deleteChildLesson(id: Int, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.childLessonDao().deleteChildLessonByIdAndUser(id, effectiveUser)
    }

    // 9. Local Vault Items (Encrypted client-side)
    suspend fun saveVaultItem(item: VaultItemEntity) {
        db.vaultItemDao().insertVaultItem(item)
    }

    suspend fun deleteVaultItem(id: String, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.vaultItemDao().deleteVaultItemByIdAndUser(id, effectiveUser)
    }

    // 10. Activity Logs
    suspend fun logActivity(userId: String, action: String, itemId: String, itemTitle: String) {
        db.activityLogDao().insertLog(
            ActivityLogEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                action = action,
                itemId = itemId,
                itemTitle = itemTitle,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // 11. Gold Prices Operations
    suspend fun updateGoldPrice(karat: Int, pricePerGram: Double) {
        db.goldPriceDao().insertOrUpdatePrice(
            GoldPriceEntity(
                karat = karat,
                pricePerGram = pricePerGram,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateAllGoldPrices(prices: Map<Int, Double>) {
        val entities = prices.map { (karat, price) ->
            GoldPriceEntity(karat = karat, pricePerGram = price, updatedAt = System.currentTimeMillis())
        }
        db.goldPriceDao().insertAllPrices(entities)
    }

    suspend fun seedSampleDataIfEmpty() {
        val currentPrices = db.goldPriceDao().getAllGoldPrices().first()
        if (currentPrices.isEmpty()) {
            val defaultPrices = listOf(
                GoldPriceEntity(karat = 24, pricePerGram = 5250.0),
                GoldPriceEntity(karat = 22, pricePerGram = 4810.0),
                GoldPriceEntity(karat = 21, pricePerGram = 4590.0),
                GoldPriceEntity(karat = 18, pricePerGram = 3935.0),
                GoldPriceEntity(karat = 14, pricePerGram = 3060.0),
                GoldPriceEntity(karat = 12, pricePerGram = 2625.0)
            )
            db.goldPriceDao().insertAllPrices(defaultPrices)
        }
    }

    // 12. Transfers (Atomic execution)
    suspend fun executeTransfer(
        fromVaultName: String,
        toVaultName: String,
        amount: Double,
        dateMillis: Long = System.currentTimeMillis(),
        notes: String = "",
        userId: String = ""
    ): Result<Unit> {
        if (amount.isNaN() || amount.isInfinite() || amount <= 0.0) {
            return Result.failure(IllegalArgumentException(AppText.text(com.example.R.string.text_321e7ab95b7e)))
        }
        if (fromVaultName == toVaultName) {
            return Result.failure(IllegalArgumentException(AppText.text(com.example.R.string.text_cb0aaf62ada7)))
        }

        val effectiveUser = userId.ifBlank { "local_guest" }
        return try {
            db.withTransaction {
                val fromVault = db.vaultDao().getVaultByName(fromVaultName, effectiveUser)
                    ?: throw IllegalStateException(AppText.text(com.example.R.string.text_89acb7abfb61))

                if (fromVault.balance < amount) {
                    throw IllegalStateException(AppText.text(com.example.R.string.text_f72448989b3c, fromVault.balance, amount))
                }

                var toVault = db.vaultDao().getVaultByName(toVaultName, effectiveUser)
                if (toVault == null) {
                    db.vaultDao().insertVault(VaultEntity(name = toVaultName, balance = amount, isDefault = false, userId = effectiveUser))
                } else {
                    db.vaultDao().updateVaultBalance(toVault.id, toVault.balance + amount, effectiveUser)
                }

                db.vaultDao().updateVaultBalance(fromVault.id, fromVault.balance - amount, effectiveUser)

                db.transferDao().insertTransfer(
                    TransferEntity(
                        userId = effectiveUser,
                        fromVaultName = fromVaultName,
                        toVaultName = toVaultName,
                        amount = amount,
                        dateMillis = dateMillis,
                        notes = notes
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseTransfer(transferId: Int, userId: String = ""): Result<Unit> {
        val effectiveUser = userId.ifBlank { "local_guest" }
        return try {
            db.withTransaction {
                val transfer = db.transferDao().getTransferById(transferId, effectiveUser)
                    ?: throw IllegalStateException(AppText.text(com.example.R.string.text_7a962d7fe6e8))

                val fromVault = db.vaultDao().getVaultByName(transfer.fromVaultName, effectiveUser)
                    ?: throw IllegalStateException(AppText.text(com.example.R.string.text_5f125b7987c4))
                val toVault = db.vaultDao().getVaultByName(transfer.toVaultName, effectiveUser)
                    ?: throw IllegalStateException(AppText.text(com.example.R.string.text_574658039b19))

                // Return funds to source vault and deduct from target vault
                db.vaultDao().updateVaultBalance(fromVault.id, fromVault.balance + transfer.amount, effectiveUser)
                db.vaultDao().updateVaultBalance(toVault.id, toVault.balance - transfer.amount, effectiveUser)

                db.transferDao().deleteTransferByIdAndUser(transferId, effectiveUser)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransfer(id: Int, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.transferDao().deleteTransferByIdAndUser(id, effectiveUser)
    }

    // 13. Debts (Owed to me / I owe)
    suspend fun addDebt(debt: DebtEntity): Long {
        val effectiveUser = debt.userId.ifBlank { "local_guest" }
        return db.debtDao().insertDebt(debt.copy(userId = effectiveUser))
    }

    suspend fun addDebt(
        personName: String,
        type: String, // "OWED_TO_ME" or "I_OWE"
        amount: Double,
        dueDateMillis: Long? = null,
        notes: String = "",
        userId: String = ""
    ): Long {
        if (amount.isNaN() || amount.isInfinite() || amount <= 0.0) {
            throw IllegalArgumentException(AppText.text(com.example.R.string.text_24e31759427c))
        }
        val effectiveUser = userId.ifBlank { "local_guest" }
        val debt = DebtEntity(
            userId = effectiveUser,
            personName = personName,
            type = type,
            originalAmount = amount,
            paidAmount = 0.0,
            remainingAmount = amount,
            startDateMillis = System.currentTimeMillis(),
            dueDateMillis = dueDateMillis,
            notes = notes,
            status = "ACTIVE"
        )
        return db.debtDao().insertDebt(debt)
    }

    suspend fun recordDebtPayment(
        debtId: Int,
        paymentAmount: Double,
        dateMillis: Long = System.currentTimeMillis(),
        notes: String = "",
        userId: String = "",
        paymentDateMillis: Long = dateMillis
    ): Result<Unit> {
        val effectiveUser = userId.ifBlank { "local_guest" }
        if (paymentAmount.isNaN() || paymentAmount.isInfinite() || paymentAmount <= 0.0) {
            return Result.failure(IllegalArgumentException(AppText.text(com.example.R.string.text_bb0319b35c1c)))
        }

        return try {
            db.withTransaction {
                val debt = db.debtDao().getDebtById(debtId, effectiveUser)
                    ?: throw IllegalStateException(AppText.text(com.example.R.string.text_bcdddf713495))

                if (paymentAmount > debt.remainingAmount + 0.0001) {
                    throw IllegalArgumentException(AppText.text(com.example.R.string.text_c3a83f2c4743, paymentAmount, debt.remainingAmount))
                }

                val rawNewPaid = debt.paidAmount + paymentAmount
                val rawNewRemaining = debt.originalAmount - rawNewPaid
                val roundedPaid = kotlin.math.round(rawNewPaid * 100.0) / 100.0
                val roundedRemaining = kotlin.math.max(0.0, kotlin.math.round(rawNewRemaining * 100.0) / 100.0)
                val newStatus = if (roundedRemaining <= 0.001) "PAID" else "PARTIALLY_PAID"

                db.debtDao().updateDebt(
                    debt.copy(
                        paidAmount = roundedPaid,
                        remainingAmount = roundedRemaining,
                        status = newStatus,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                db.debtPaymentDao().insertPayment(
                    DebtPaymentEntity(
                        debtId = debtId,
                        userId = debt.userId,
                        amount = paymentAmount,
                        dateMillis = paymentDateMillis,
                        notes = notes
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDebt(id: Int, userId: String = "") {
        val effectiveUser = userId.ifBlank { "local_guest" }
        db.withTransaction {
            db.debtPaymentDao().deletePaymentsForDebt(id, effectiveUser)
            db.debtDao().deleteDebtByIdAndUser(id, effectiveUser)
        }
    }

    // 14. Net Worth Snapshots
    suspend fun saveNetWorthSnapshot(snapshot: NetWorthSnapshotEntity): Long {
        val existing = db.netWorthSnapshotDao().getSnapshotByDateKey(snapshot.userId, snapshot.dateKey)
        return if (existing != null) {
            db.netWorthSnapshotDao().insertSnapshot(snapshot.copy(id = existing.id))
        } else {
            db.netWorthSnapshotDao().insertSnapshot(snapshot)
        }
    }

    suspend fun insertNetWorthSnapshot(snapshot: NetWorthSnapshotEntity): Long =
        saveNetWorthSnapshot(snapshot)

    // Direct access to Room Database for Backup & Restore and Migration
    fun getRawDatabase(): AppDatabase = db

    suspend fun clearUserData(userId: String) {
        try {
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
        } catch (_: Exception) {}
    }

    suspend fun clearAllLocalUserData() {
        try {
            db.transactionDao().clearAll()
            db.vaultDao().clearAll()
        } catch (_: Exception) {}
    }
}
