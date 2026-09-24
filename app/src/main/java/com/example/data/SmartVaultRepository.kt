package com.example.data

import com.example.data.entity.ActivityLogEntity
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.GoldPriceEntity
import com.example.data.entity.OutingEntity
import com.example.data.entity.OutingExpenseEntity
import com.example.data.entity.TransactionEntity
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
        val tx = TransactionEntity(
            userId = userId,
            type = "INCOME",
            amount = amount,
            category = category,
            description = description,
            dateMillis = dateMillis,
            vaultName = vaultName
        )
        db.transactionDao().insertTransaction(tx)

        // Add to vault balance
        val vault = db.vaultDao().getVaultByName(vaultName, userId)
        if (vault != null) {
            db.vaultDao().updateVaultBalance(vault.id, vault.balance + amount)
        } else {
            db.vaultDao().insertVault(VaultEntity(name = vaultName, balance = amount, isDefault = true, userId = userId))
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
        val tx = TransactionEntity(
            userId = userId,
            type = "EXPENSE",
            amount = amount,
            category = category,
            description = description,
            dateMillis = dateMillis,
            vaultName = vaultName,
            receiptImagePath = receiptPath
        )
        db.transactionDao().insertTransaction(tx)

        // Deduct from vault balance
        val vault = db.vaultDao().getVaultByName(vaultName, userId)
        if (vault != null) {
            db.vaultDao().updateVaultBalance(vault.id, (vault.balance - amount).coerceAtLeast(0.0))
        }
    }

    suspend fun deleteTransaction(id: Int) {
        db.transactionDao().deleteTransactionById(id)
    }

    // 2. Vault Actions
    suspend fun addVault(name: String, initialBalance: Double, userId: String = "") {
        db.vaultDao().insertVault(VaultEntity(name = name, balance = initialBalance, isDefault = false, userId = userId))
    }

    suspend fun updateVault(id: Int, name: String, balance: Double, userId: String = "") {
        val existing = db.vaultDao().getVaultByName(name, userId)
        if (existing != null && existing.id != id) {
            db.vaultDao().updateVaultBalance(id, balance)
        } else {
            db.vaultDao().updateVault(VaultEntity(id = id, name = name, balance = balance, userId = userId))
        }
    }

    suspend fun deleteVault(id: Int) {
        db.vaultDao().deleteVaultById(id)
    }

    // 3. Budget Limits
    suspend fun setBudgetLimit(category: String, limit: Double, userId: String = "") {
        db.budgetLimitDao().insertOrUpdateLimit(BudgetLimitEntity(category = category, monthlyLimit = limit, userId = userId))
    }

    suspend fun deleteBudgetLimit(category: String, userId: String = "") {
        db.budgetLimitDao().deleteLimit(category, userId)
    }

    // 4. Outings & Expenses
    suspend fun addOuting(id: String, name: String, participantNames: List<String>, userId: String = "") {
        val jsonArray = JSONArray()
        participantNames.forEach { jsonArray.put(it) }
        db.outingDao().insertOuting(
            OutingEntity(
                id = id,
                userId = userId,
                name = name,
                participantNamesJson = jsonArray.toString(),
                dateMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteOuting(id: String) {
        db.outingDao().deleteOuting(id)
    }

    suspend fun addOutingExpense(
        title: String,
        amount: Double,
        payerName: String = "",
        receiptPath: String? = null,
        outingId: String = "",
        userId: String = ""
    ) {
        db.outingExpenseDao().insertOutingExpense(
            OutingExpenseEntity(
                title = title,
                amount = amount,
                payerName = payerName,
                receiptImagePath = receiptPath,
                outingId = outingId,
                userId = userId
            )
        )
    }

    suspend fun deleteOutingExpense(id: Int) {
        db.outingExpenseDao().deleteOutingExpenseById(id)
    }

    suspend fun clearAllOutingExpenses(userId: String = "") {
        if (userId.isNotBlank()) {
            db.outingExpenseDao().clearUserOutingExpenses(userId)
        } else {
            db.outingExpenseDao().clearAllOutingExpenses()
        }
    }

    // 5. Gold Assets
    suspend fun addGoldAsset(asset: GoldAssetEntity): Long {
        return db.goldAssetDao().insertGoldAsset(asset)
    }

    suspend fun updateGoldAsset(asset: GoldAssetEntity) {
        db.goldAssetDao().updateGoldAsset(asset)
    }

    suspend fun deleteGoldAsset(id: Int) {
        db.goldAssetDao().deleteGoldAssetById(id)
    }

    suspend fun sellGoldAsset(id: Int, salePrice: Double, saleDateMillis: Long = System.currentTimeMillis(), saleNotes: String? = null) {
        val asset = db.goldAssetDao().getGoldAssetById(id)
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
        return db.cashSavingDao().insertCashSaving(
            CashSavingEntity(
                userId = userId,
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

    suspend fun deleteCashSaving(id: Int) {
        db.cashSavingDao().deleteCashSavingById(id)
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
        return db.commitmentDao().insertCommitment(
            CommitmentEntity(
                userId = userId,
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

    suspend fun deleteCommitment(id: Int) {
        db.commitmentDao().deleteCommitmentById(id)
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
        return db.childLessonDao().insertChildLesson(
            ChildLessonEntity(
                userId = userId,
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

    suspend fun deleteChildLesson(id: Int) {
        db.childLessonDao().deleteChildLessonById(id)
    }

    // 9. Local Vault Items (Encrypted client-side)
    suspend fun saveVaultItem(item: VaultItemEntity) {
        db.vaultItemDao().insertVaultItem(item)
    }

    suspend fun deleteVaultItem(id: String) {
        db.vaultItemDao().deleteVaultItemById(id)
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
        } catch (_: Exception) {}
    }

    suspend fun clearAllLocalUserData() {
        try {
            db.transactionDao().clearAll()
            db.vaultDao().clearAll()
        } catch (_: Exception) {}
    }
}
