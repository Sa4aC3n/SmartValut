package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "",
    val action: String = "created", // created | updated | deleted
    val itemId: String = "",
    val itemTitle: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
