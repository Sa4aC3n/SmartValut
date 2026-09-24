package com.example.data.model

data class CloudCashSaving(
    val id: String = "",
    val amount: Double = 0.0,
    val currency: String = "EGP",
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
