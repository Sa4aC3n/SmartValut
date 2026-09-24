package com.example.data.model

data class CloudCommitment(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val dueDateMillis: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val isRecurringMonthly: Boolean = true,
    val notes: String = "",
    val receiptImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
