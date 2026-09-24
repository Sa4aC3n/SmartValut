package com.example.data.model

data class CloudTransaction(
    val id: String = "",
    val type: String = "INCOME", // "INCOME" or "EXPENSE"
    val amount: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val vaultId: String = "",
    val date: Long = System.currentTimeMillis(),
    val receiptImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
