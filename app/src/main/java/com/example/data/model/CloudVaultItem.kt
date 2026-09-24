package com.example.data.model

import androidx.annotation.Keep

@Keep
data class CloudVaultItem(
    val id: String = "",
    val title: String = "",
    val type: String = "password", // "password" | "note" | "card" | "document"
    val encryptedData: String = "", // Client-side AES-256 encrypted string
    val category: String = "عام",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // No-arg constructor required by Firebase Firestore deserializer
    constructor() : this("", "", "password", "", "عام", 0L, 0L)
}

enum class VaultItemType(val rawType: String, val titleAr: String, val iconEmoji: String) {
    PASSWORD("password", "كلمة مرور", "🔒"),
    NOTE("note", "ملاحظة سرية", "📝"),
    CARD("card", "بطاقة بنكية", "💳"),
    DOCUMENT("document", "مستند سري", "📄");

    companion object {
        fun fromRaw(raw: String): VaultItemType =
            entries.find { it.rawType.equals(raw, ignoreCase = true) } ?: PASSWORD
    }
}
