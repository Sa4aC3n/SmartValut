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
    val receiptImagePath: String? = null
)
