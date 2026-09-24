package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outings")
data class OutingEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "",
    val name: String,
    val participantNamesJson: String = "[]",
    val dateMillis: Long = System.currentTimeMillis()
)
