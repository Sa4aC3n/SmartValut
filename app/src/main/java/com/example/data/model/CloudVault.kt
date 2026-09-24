package com.example.data.model

data class CloudVault(
    val id: String = "",
    val name: String = "",
    val balance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
