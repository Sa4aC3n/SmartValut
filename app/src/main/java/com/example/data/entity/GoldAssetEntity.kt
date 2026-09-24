package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gold_assets")
data class GoldAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val name: String,
    val goldType: String = "سبيكة", // سبيكة, مشغولات, عملة ذهبية, جنيه ذهب, أخرى
    val karat: Int = 24, // 24, 22, 21, 18, 14, 12
    val weight: Double, // in grams
    val purchasePrice: Double, // in currency (e.g. EGP)
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val purpose: String = "SAVING", // SAVING (للادخار), ADORNMENT (للزينة)
    val imagePath: String? = null,
    val status: String = "ACTIVE", // ACTIVE (غير مباعة / بالخزنة), SOLD (تم البيع)
    val salePrice: Double? = null,
    val saleDateMillis: Long? = null,
    val saleNotes: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "gold_prices")
data class GoldPriceEntity(
    @PrimaryKey
    val karat: Int,
    val pricePerGram: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
