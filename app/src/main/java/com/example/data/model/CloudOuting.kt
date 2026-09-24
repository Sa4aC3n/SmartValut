package com.example.data.model

data class CloudOuting(
    val id: String = "",
    val name: String = "",
    val participantNames: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
