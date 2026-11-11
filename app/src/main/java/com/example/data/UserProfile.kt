package com.example.data

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarId: Int = 1, // Avatar index (1..6)
    val photoUrl: String? = null,
    val isTwoFactorEnabled: Boolean = false,
    val isLoggedIn: Boolean = false,
    val loginMethod: String = "EMAIL" // "EMAIL"
)
