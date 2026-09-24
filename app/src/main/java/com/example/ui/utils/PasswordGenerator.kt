package com.example.ui.utils

import java.security.SecureRandom

object PasswordGenerator {
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    fun generate(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val safeLength = length.coerceIn(8, 64)
        val charPool = StringBuilder(LOWERCASE)
        if (includeUppercase) charPool.append(UPPERCASE)
        if (includeNumbers) charPool.append(NUMBERS)
        if (includeSymbols) charPool.append(SYMBOLS)

        val random = SecureRandom()
        val result = StringBuilder()

        // Ensure at least one of each selected character type
        result.append(LOWERCASE[random.nextInt(LOWERCASE.length)])
        if (includeUppercase) result.append(UPPERCASE[random.nextInt(UPPERCASE.length)])
        if (includeNumbers) result.append(NUMBERS[random.nextInt(NUMBERS.length)])
        if (includeSymbols) result.append(SYMBOLS[random.nextInt(SYMBOLS.length)])

        while (result.length < safeLength) {
            val char = charPool[random.nextInt(charPool.length)]
            result.append(char)
        }

        // Shuffle characters
        val charArray = result.toString().toCharArray()
        for (i in charArray.indices) {
            val j = random.nextInt(charArray.size)
            val temp = charArray[i]
            charArray[i] = charArray[j]
            charArray[j] = temp
        }

        return String(charArray)
    }

    enum class PasswordStrength(val labelAr: String, val score: Int) {
        VERY_WEAK("ضعيفة جداً", 1),
        WEAK("ضعيفة", 2),
        MEDIUM("متوسطة", 3),
        STRONG("قوية", 4),
        VERY_STRONG("فائقة القوة", 5)
    }

    fun evaluateStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.VERY_WEAK
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 14) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when (score) {
            0, 1 -> PasswordStrength.VERY_WEAK
            2 -> PasswordStrength.WEAK
            3 -> PasswordStrength.MEDIUM
            4 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
}
