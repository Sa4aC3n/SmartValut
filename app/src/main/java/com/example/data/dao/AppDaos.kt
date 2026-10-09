package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ActivityLogEntity
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.GoldPriceEntity
import com.example.data.entity.TransferEntity
import com.example.data.entity.DebtEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.NetWorthSnapshotEntity
import com.example.data.entity.OutingEntity
import com.example.data.entity.OutingExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import com.example.data.entity.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis DESC")
    fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM transactions WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getTransactionById(id: Int, userId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE referenceType = :referenceType AND referenceId = :referenceId AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getTransactionByReference(referenceType: String, referenceId: String, userId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' AND amount = :amount AND category = :category AND description = :description AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis DESC")
    suspend fun findLegacyExpenseCandidates(amount: Double, category: String, description: String, userId: String): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteTransactionByIdAndUser(id: Int, userId: String): Int

    @Query("DELETE FROM transactions WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserTransactions(userId: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY id ASC")
    fun getVaultsForUser(userId: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults ORDER BY id ASC")
    fun getAllVaults(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE name = :name AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getVaultByName(name: String, userId: String): VaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVault(vault: VaultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVaults(vaults: List<VaultEntity>)

    @Update
    suspend fun updateVault(vault: VaultEntity)

    @Query("SELECT * FROM vaults WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getVaultById(id: Int, userId: String): VaultEntity?

    @Query("UPDATE vaults SET balance = :newBalance WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun updateVaultBalance(id: Int, newBalance: Double, userId: String): Int

    @Query("DELETE FROM vaults WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteVaultByIdAndUser(id: Int, userId: String): Int

    @Query("DELETE FROM vaults WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserVaults(userId: String)

    @Query("DELETE FROM vaults")
    suspend fun clearAll()
}

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM budget_limits WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    fun getLimitsForUser(userId: String): Flow<List<BudgetLimitEntity>>

    @Query("SELECT * FROM budget_limits")
    fun getAllBudgetLimits(): Flow<List<BudgetLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLimit(limit: BudgetLimitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLimits(limits: List<BudgetLimitEntity>)

    @Query("DELETE FROM budget_limits WHERE category = :category AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteLimit(category: String, userId: String)

    @Query("DELETE FROM budget_limits WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserLimits(userId: String)
}

@Dao
interface OutingExpenseDao {
    @Query("SELECT * FROM outing_expenses WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis DESC")
    fun getExpensesForUser(userId: String): Flow<List<OutingExpenseEntity>>

    @Query("SELECT * FROM outing_expenses WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) AND outingId = :outingId ORDER BY dateMillis DESC")
    fun getExpensesForOuting(outingId: String, userId: String): Flow<List<OutingExpenseEntity>>

    @Query("SELECT * FROM outing_expenses ORDER BY dateMillis DESC")
    fun getAllOutingExpenses(): Flow<List<OutingExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutingExpense(expense: OutingExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOutingExpenses(expenses: List<OutingExpenseEntity>)

    @Query("DELETE FROM outing_expenses WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteOutingExpenseByIdAndUser(id: Int, userId: String): Int

    @Query("DELETE FROM outing_expenses WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserOutingExpenses(userId: String)

    @Query("DELETE FROM outing_expenses")
    suspend fun clearAllOutingExpenses()
}

@Dao
interface OutingDao {
    @Query("SELECT * FROM outings WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis DESC")
    fun getOutingsForUser(userId: String): Flow<List<OutingEntity>>

    @Query("SELECT * FROM outings WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getOutingByIdAndUser(id: String, userId: String): OutingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOuting(outing: OutingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOutings(outings: List<OutingEntity>)

    @Query("DELETE FROM outings WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteOutingByIdAndUser(id: String, userId: String): Int

    @Query("DELETE FROM outings WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserOutings(userId: String)
}

@Dao
interface GoldAssetDao {
    @Query("SELECT * FROM gold_assets WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY purchaseDateMillis DESC")
    fun getGoldAssetsForUser(userId: String): Flow<List<GoldAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoldAsset(asset: GoldAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGoldAssets(assets: List<GoldAssetEntity>)

    @Update
    suspend fun updateGoldAsset(asset: GoldAssetEntity)

    @Query("DELETE FROM gold_assets WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteGoldAssetByIdAndUser(id: Int, userId: String): Int

    @Query("SELECT * FROM gold_assets WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getGoldAssetByIdAndUser(id: Int, userId: String): GoldAssetEntity?

    @Query("DELETE FROM gold_assets WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserGoldAssets(userId: String)
}

@Dao
interface CashSavingDao {
    @Query("SELECT * FROM cash_savings WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis DESC")
    fun getCashSavingsForUser(userId: String): Flow<List<CashSavingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashSaving(saving: CashSavingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCashSavings(savings: List<CashSavingEntity>)

    @Update
    suspend fun updateCashSaving(saving: CashSavingEntity)

    @Query("DELETE FROM cash_savings WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteCashSavingByIdAndUser(id: Int, userId: String): Int

    @Query("SELECT * FROM cash_savings WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getCashSavingByIdAndUser(id: Int, userId: String): CashSavingEntity?

    @Query("DELETE FROM cash_savings WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserCashSavings(userId: String)
}

@Dao
interface CommitmentDao {
    @Query("SELECT * FROM commitments WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dueDateMillis ASC")
    fun getCommitmentsForUser(userId: String): Flow<List<CommitmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(commitment: CommitmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCommitments(commitments: List<CommitmentEntity>)

    @Update
    suspend fun updateCommitment(commitment: CommitmentEntity)

    @Query("DELETE FROM commitments WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteCommitmentByIdAndUser(id: Int, userId: String): Int

    @Query("SELECT * FROM commitments WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getCommitmentByIdAndUser(id: Int, userId: String): CommitmentEntity?

    @Query("DELETE FROM commitments WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserCommitments(userId: String)
}

@Dao
interface ChildLessonDao {
    @Query("SELECT * FROM child_lessons WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dueDateMillis ASC")
    fun getChildLessonsForUser(userId: String): Flow<List<ChildLessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildLesson(lesson: ChildLessonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChildLessons(lessons: List<ChildLessonEntity>)

    @Update
    suspend fun updateChildLesson(lesson: ChildLessonEntity)

    @Query("DELETE FROM child_lessons WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteChildLessonByIdAndUser(id: Int, userId: String): Int

    @Query("SELECT * FROM child_lessons WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getChildLessonByIdAndUser(id: Int, userId: String): ChildLessonEntity?

    @Query("DELETE FROM child_lessons WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserChildLessons(userId: String)
}

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY updatedAt DESC")
    fun getVaultItemsForUser(userId: String): Flow<List<VaultItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVaultItems(items: List<VaultItemEntity>)

    @Query("DELETE FROM vault_items WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteVaultItemByIdAndUser(id: String, userId: String): Int

    @Query("DELETE FROM vault_items WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserVaultItems(userId: String)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY timestamp DESC")
    fun getLogsForUser(userId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<ActivityLogEntity>)

    @Query("DELETE FROM activity_logs WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserLogs(userId: String)
}

@Dao
interface GoldPriceDao {
    @Query("SELECT * FROM gold_prices")
    fun getAllGoldPrices(): Flow<List<GoldPriceEntity>>

    @Query("SELECT * FROM gold_prices WHERE karat = :karat LIMIT 1")
    suspend fun getGoldPriceByKarat(karat: Int): GoldPriceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePrice(price: GoldPriceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPrices(prices: List<GoldPriceEntity>)
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis DESC")
    fun getTransfersForUser(userId: String): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getTransferById(id: Int, userId: String): TransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransfers(transfers: List<TransferEntity>)

    @Query("DELETE FROM transfers WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteTransferByIdAndUser(id: Int, userId: String): Int

    @Query("DELETE FROM transfers WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserTransfers(userId: String)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY createdAt DESC")
    fun getDebtsForUser(userId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) LIMIT 1")
    suspend fun getDebtById(id: Int, userId: String): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDebts(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deleteDebtByIdAndUser(id: Int, userId: String): Int

    @Query("DELETE FROM debts WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserDebts(userId: String)
}

@Dao
interface DebtPaymentDao {
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis DESC")
    fun getPaymentsForDebt(debtId: Int, userId: String): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis DESC")
    fun getAllPaymentsForUser(userId: String): Flow<List<DebtPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(payments: List<DebtPaymentEntity>)

    @Query("DELETE FROM debt_payments WHERE id = :id AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deletePaymentByIdAndUser(id: Int, userId: String): Int

    @Query("DELETE FROM debt_payments WHERE debtId = :debtId AND (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun deletePaymentsForDebt(debtId: Int, userId: String): Int

    @Query("DELETE FROM debt_payments WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserDebtPayments(userId: String)
}

@Dao
interface NetWorthSnapshotDao {
    @Query("SELECT * FROM net_worth_snapshots WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis ASC")
    fun getSnapshotsForUser(userId: String): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_snapshots WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) AND dateKey = :dateKey LIMIT 1")
    suspend fun getSnapshotByDateKey(userId: String, dateKey: String): NetWorthSnapshotEntity?

    @Query("SELECT * FROM net_worth_snapshots WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL))) ORDER BY dateMillis DESC LIMIT 1")
    suspend fun getLatestSnapshot(userId: String): NetWorthSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: NetWorthSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSnapshots(snapshots: List<NetWorthSnapshotEntity>)

    @Query("DELETE FROM net_worth_snapshots WHERE (userId = :userId OR (:userId = 'local_guest' AND (userId = '' OR userId IS NULL)))")
    suspend fun clearUserSnapshots(userId: String)
}
