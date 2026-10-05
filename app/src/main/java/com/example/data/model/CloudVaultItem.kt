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

    val localizedTitle: String get() = com.example.ui.utils.AppText.text(when (this) {
        PASSWORD -> com.example.R.string.text_e6b96ce194c0
        NOTE -> com.example.R.string.text_e4627f47b69b
        CARD -> com.example.R.string.text_b4316420bcc3
        DOCUMENT -> com.example.R.string.text_7822deda09a6
    })

    companion object {
        fun fromRaw(raw: String): VaultItemType =
            entries.find { it.rawType.equals(raw, ignoreCase = true) } ?: PASSWORD
    }
}
