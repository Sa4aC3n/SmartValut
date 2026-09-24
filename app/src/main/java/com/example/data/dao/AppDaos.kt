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
import com.example.data.entity.OutingEntity
import com.example.data.entity.OutingExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import com.example.data.entity.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY dateMillis DESC")
    fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearUserTransactions(userId: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY id ASC")
    fun getVaultsForUser(userId: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults ORDER BY id ASC")
    fun getAllVaults(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE name = :name AND (userId = :userId OR userId = '') LIMIT 1")
    suspend fun getVaultByName(name: String, userId: String = ""): VaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVault(vault: VaultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVaults(vaults: List<VaultEntity>)

    @Update
    suspend fun updateVault(vault: VaultEntity)

    @Query("UPDATE vaults SET balance = :newBalance WHERE id = :id")
    suspend fun updateVaultBalance(id: Int, newBalance: Double)

    @Query("DELETE FROM vaults WHERE id = :id")
    suspend fun deleteVaultById(id: Int)

    @Query("DELETE FROM vaults WHERE userId = :userId")
    suspend fun clearUserVaults(userId: String)

    @Query("DELETE FROM vaults")
    suspend fun clearAll()
}

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM budget_limits WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest'))")
    fun getLimitsForUser(userId: String): Flow<List<BudgetLimitEntity>>

    @Query("SELECT * FROM budget_limits")
    fun getAllBudgetLimits(): Flow<List<BudgetLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLimit(limit: BudgetLimitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLimits(limits: List<BudgetLimitEntity>)

    @Query("DELETE FROM budget_limits WHERE category = :category AND (userId = :userId OR userId = '')")
    suspend fun deleteLimit(category: String, userId: String = "")

    @Query("DELETE FROM budget_limits WHERE userId = :userId")
    suspend fun clearUserLimits(userId: String)
}

@Dao
interface OutingExpenseDao {
    @Query("SELECT * FROM outing_expenses WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY dateMillis DESC")
    fun getExpensesForUser(userId: String): Flow<List<OutingExpenseEntity>>

    @Query("SELECT * FROM outing_expenses WHERE (userId = :userId OR :userId = '') AND (outingId = :outingId OR :outingId = '') ORDER BY dateMillis DESC")
    fun getExpensesForOuting(outingId: String, userId: String): Flow<List<OutingExpenseEntity>>

    @Query("SELECT * FROM outing_expenses ORDER BY dateMillis DESC")
    fun getAllOutingExpenses(): Flow<List<OutingExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutingExpense(expense: OutingExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOutingExpenses(expenses: List<OutingExpenseEntity>)

    @Query("DELETE FROM outing_expenses WHERE id = :id")
    suspend fun deleteOutingExpenseById(id: Int)

    @Query("DELETE FROM outing_expenses WHERE userId = :userId")
    suspend fun clearUserOutingExpenses(userId: String)

    @Query("DELETE FROM outing_expenses")
    suspend fun clearAllOutingExpenses()
}

@Dao
interface OutingDao {
    @Query("SELECT * FROM outings WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY dateMillis DESC")
    fun getOutingsForUser(userId: String): Flow<List<OutingEntity>>

    @Query("SELECT * FROM outings WHERE id = :id LIMIT 1")
    suspend fun getOutingById(id: String): OutingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOuting(outing: OutingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOutings(outings: List<OutingEntity>)

    @Query("DELETE FROM outings WHERE id = :id")
    suspend fun deleteOuting(id: String)

    @Query("DELETE FROM outings WHERE userId = :userId")
    suspend fun clearUserOutings(userId: String)
}

@Dao
interface GoldAssetDao {
    @Query("SELECT * FROM gold_assets WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY purchaseDateMillis DESC")
    fun getGoldAssetsForUser(userId: String): Flow<List<GoldAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoldAsset(asset: GoldAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGoldAssets(assets: List<GoldAssetEntity>)

    @Update
    suspend fun updateGoldAsset(asset: GoldAssetEntity)

    @Query("DELETE FROM gold_assets WHERE id = :id")
    suspend fun deleteGoldAssetById(id: Int)

    @Query("SELECT * FROM gold_assets WHERE id = :id LIMIT 1")
    suspend fun getGoldAssetById(id: Int): GoldAssetEntity?

    @Query("DELETE FROM gold_assets WHERE userId = :userId")
    suspend fun clearUserGoldAssets(userId: String)
}

@Dao
interface CashSavingDao {
    @Query("SELECT * FROM cash_savings WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY dateMillis DESC")
    fun getCashSavingsForUser(userId: String): Flow<List<CashSavingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashSaving(saving: CashSavingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCashSavings(savings: List<CashSavingEntity>)

    @Update
    suspend fun updateCashSaving(saving: CashSavingEntity)

    @Query("DELETE FROM cash_savings WHERE id = :id")
    suspend fun deleteCashSavingById(id: Int)

    @Query("DELETE FROM cash_savings WHERE userId = :userId")
    suspend fun clearUserCashSavings(userId: String)
}

@Dao
interface CommitmentDao {
    @Query("SELECT * FROM commitments WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY dueDateMillis ASC")
    fun getCommitmentsForUser(userId: String): Flow<List<CommitmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(commitment: CommitmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCommitments(commitments: List<CommitmentEntity>)

    @Update
    suspend fun updateCommitment(commitment: CommitmentEntity)

    @Query("DELETE FROM commitments WHERE id = :id")
    suspend fun deleteCommitmentById(id: Int)

    @Query("DELETE FROM commitments WHERE userId = :userId")
    suspend fun clearUserCommitments(userId: String)
}

@Dao
interface ChildLessonDao {
    @Query("SELECT * FROM child_lessons WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY dueDateMillis ASC")
    fun getChildLessonsForUser(userId: String): Flow<List<ChildLessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildLesson(lesson: ChildLessonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChildLessons(lessons: List<ChildLessonEntity>)

    @Update
    suspend fun updateChildLesson(lesson: ChildLessonEntity)

    @Query("DELETE FROM child_lessons WHERE id = :id")
    suspend fun deleteChildLessonById(id: Int)

    @Query("DELETE FROM child_lessons WHERE userId = :userId")
    suspend fun clearUserChildLessons(userId: String)
}

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY updatedAt DESC")
    fun getVaultItemsForUser(userId: String): Flow<List<VaultItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVaultItems(items: List<VaultItemEntity>)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItemById(id: String)

    @Query("DELETE FROM vault_items WHERE userId = :userId")
    suspend fun clearUserVaultItems(userId: String)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE (userId = :userId) OR ((:userId = 'local_guest' OR :userId = '') AND (userId = '' OR userId = 'local_guest')) ORDER BY timestamp DESC")
    fun getLogsForUser(userId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<ActivityLogEntity>)

    @Query("DELETE FROM activity_logs WHERE userId = :userId")
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
