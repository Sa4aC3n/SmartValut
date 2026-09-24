package com.example.data.model

data class CloudGoldPrice(
    val karat: Int = 24,
    val pricePerGram: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
