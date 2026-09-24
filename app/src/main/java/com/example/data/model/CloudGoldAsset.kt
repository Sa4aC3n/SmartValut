package com.example.data.model

data class CloudGoldAsset(
    val id: String = "",
    val name: String = "",
    val goldType: String = "سبيكة",
    val karat: Int = 24,
    val weight: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val purpose: String = "SAVING",
    val imagePath: String? = null,
    val status: String = "ACTIVE",
    val salePrice: Double? = null,
    val saleDateMillis: Long? = null,
    val saleNotes: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
