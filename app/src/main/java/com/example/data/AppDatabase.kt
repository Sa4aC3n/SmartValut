package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ActivityLogDao
import com.example.data.dao.BudgetLimitDao
import com.example.data.dao.CashSavingDao
import com.example.data.dao.ChildLessonDao
import com.example.data.dao.CommitmentDao
import com.example.data.dao.GoldAssetDao
import com.example.data.dao.GoldPriceDao
import com.example.data.dao.OutingDao
import com.example.data.dao.OutingExpenseDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.VaultDao
import com.example.data.dao.VaultItemDao
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

@Database(
    entities = [
        TransactionEntity::class,
        VaultEntity::class,
        BudgetLimitEntity::class,
        OutingExpenseEntity::class,
        OutingEntity::class,
        GoldPriceEntity::class,
        GoldAssetEntity::class,
        CashSavingEntity::class,
        CommitmentEntity::class,
        ChildLessonEntity::class,
        VaultItemEntity::class,
        ActivityLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun vaultDao(): VaultDao
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun outingExpenseDao(): OutingExpenseDao
    abstract fun outingDao(): OutingDao
    abstract fun goldAssetDao(): GoldAssetDao
    abstract fun cashSavingDao(): CashSavingDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun childLessonDao(): ChildLessonDao
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun goldPriceDao(): GoldPriceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add userId column to transactions if it doesn't exist
                try {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}

                // Add userId column to vaults if it doesn't exist
                try {
                    db.execSQL("ALTER TABLE vaults ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}

                // Add userId and outingId column to outing_expenses
                try {
                    db.execSQL("ALTER TABLE outing_expenses ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE outing_expenses ADD COLUMN outingId TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}

                // Recreate budget_limits with composite primary key (userId, category)
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS budget_limits_new (category TEXT NOT NULL, monthlyLimit REAL NOT NULL, userId TEXT NOT NULL DEFAULT '', PRIMARY KEY(userId, category))")
                    db.execSQL("INSERT OR IGNORE INTO budget_limits_new (category, monthlyLimit, userId) SELECT category, monthlyLimit, '' FROM budget_limits")
                    db.execSQL("DROP TABLE budget_limits")
                    db.execSQL("ALTER TABLE budget_limits_new RENAME TO budget_limits")
                } catch (_: Exception) {}

                // Create outings table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS outings (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL,
                        participantNamesJson TEXT NOT NULL DEFAULT '[]',
                        dateMillis INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create gold_assets table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS gold_assets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL,
                        goldType TEXT NOT NULL,
                        karat INTEGER NOT NULL,
                        weight REAL NOT NULL,
                        purchasePrice REAL NOT NULL,
                        purchaseDateMillis INTEGER NOT NULL,
                        purpose TEXT NOT NULL,
                        imagePath TEXT,
                        status TEXT NOT NULL,
                        salePrice REAL,
                        saleDateMillis INTEGER,
                        saleNotes TEXT,
                        notes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create cash_savings table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cash_savings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        amount REAL NOT NULL,
                        currency TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        dateMillis INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create commitments table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS commitments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        dueDateMillis INTEGER NOT NULL,
                        isPaid INTEGER NOT NULL,
                        isRecurringMonthly INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        receiptImagePath TEXT
                    )
                """.trimIndent())

                // Create child_lessons table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS child_lessons (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        childName TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        teacherName TEXT NOT NULL,
                        amount REAL NOT NULL,
                        dueDateMillis INTEGER NOT NULL,
                        isPaid INTEGER NOT NULL,
                        receiptImagePath TEXT
                    )
                """.trimIndent())

                // Create vault_items table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vault_items (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        encryptedData TEXT NOT NULL,
                        category TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create activity_logs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_logs (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        action TEXT NOT NULL,
                        itemId TEXT NOT NULL,
                        itemTitle TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_vault_db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
