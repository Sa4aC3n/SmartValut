package com.example.data

data class UserProfile(
    val name: String = "M. Keshka",
    val email: String = "m.k3shka@gmail.com",
    val phone: String = "+20 100 123 4567",
    val avatarId: Int = 1, // Avatar index (1..6)
    val photoUrl: String? = null,
    val isTwoFactorEnabled: Boolean = false,
    val isLoggedIn: Boolean = true,
    val loginMethod: String = "GOOGLE" // "EMAIL", "PHONE", "GOOGLE"
)
