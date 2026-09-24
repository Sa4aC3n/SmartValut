package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "child_lessons")
data class ChildLessonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val childName: String,
    val subject: String,
    val teacherName: String,
    val amount: Double,
    val dueDateMillis: Long,
    val isPaid: Boolean = false,
    val receiptImagePath: String? = null
)
