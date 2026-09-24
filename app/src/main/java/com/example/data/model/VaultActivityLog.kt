package com.example.data.model

import androidx.annotation.Keep

@Keep
data class VaultActivityLog(
    val id: String = "",
    val action: String = "created", // "created" | "updated" | "deleted"
    val itemId: String = "",
    val itemTitle: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    // No-arg constructor required by Firebase Firestore deserializer
    constructor() : this("", "created", "", "", 0L)
}
