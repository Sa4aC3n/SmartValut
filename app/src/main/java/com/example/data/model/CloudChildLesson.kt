package com.example.data.model

data class CloudChildLesson(
    val id: String = "",
    val childName: String = "",
    val subject: String = "",
    val teacherName: String = "",
    val amount: Double = 0.0,
    val dueDateMillis: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val receiptImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
