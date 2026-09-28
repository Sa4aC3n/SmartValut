package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val personName: String,
    val type: String, // "OWED_TO_ME" (أموال لي عند الغير) or "I_OWE" (أموال عليّ للغير)
    val originalAmount: Double,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double,
    val startDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long? = null,
    val notes: String = "",
    val status: String = "ACTIVE", // "ACTIVE", "PARTIALLY_PAID", "PAID", "OVERDUE"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val debtId: Int,
    val userId: String = "",
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
