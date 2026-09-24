package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val balance: Double,
    val isDefault: Boolean = false,
    val userId: String = ""
)

@Entity(tableName = "budget_limits", primaryKeys = ["userId", "category"])
data class BudgetLimitEntity(
    val category: String,
    val monthlyLimit: Double,
    val userId: String = ""
)
