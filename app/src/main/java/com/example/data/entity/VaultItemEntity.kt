package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "",
    val title: String,
    val type: String = "password", // password | note | card | document
    val encryptedData: String,
    val category: String = "عام",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
