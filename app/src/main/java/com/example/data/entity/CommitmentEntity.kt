package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val title: String,
    val amount: Double,
    val dueDateMillis: Long,
    val isPaid: Boolean = false,
    val isRecurringMonthly: Boolean = true,
    val notes: String = "",
    val receiptImagePath: String? = null
)
