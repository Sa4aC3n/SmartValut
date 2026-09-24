package com.example.data.model

data class CloudBudgetLimit(
    val category: String = "",
    val monthlyLimit: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
