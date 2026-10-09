package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val category: String,
    val description: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val vaultName: String = "الخزنة الرئيسية",
    val receiptImagePath: String? = null,
    // Optional stable linkage to the domain operation that created this ledger entry.
    // Used for auditable reversals (for example COMMITMENT_PAYMENT) without relying
    // on user-visible descriptions or database auto-increment IDs.
    val referenceType: String? = null,
    val referenceId: String? = null
)
