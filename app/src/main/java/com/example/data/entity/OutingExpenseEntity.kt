package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outing_expenses")
data class OutingExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val payerName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val receiptImagePath: String? = null,
    val userId: String = "",
    val outingId: String = ""
)
