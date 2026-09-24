package com.example.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryUtils {
    val expenseCategories = listOf(
        "خضروات", "فاكهة", "طلبات منزل", "سوبر ماركت", "مطاعم",
        "مواصلات", "بنزين", "ملابس", "صيدلية", "أدوية", "تعليم",
        "دروس أطفال", "إيجار المنزل", "فاتورة الكهرباء", "فاتورة المياه",
        "فاتورة الغاز", "فاتورة الإنترنت", "فاتورة الهاتف", "ترفيه", "هدايا",
        "صيانة", "إصلاحات", "قسط سيارة", "أخرى"
    )

    val incomeCategories = listOf(
        "الراتب", "مكافأة", "أرباح", "تحويل مالي", "أخرى"
    )

    fun getCategoryIcon(category: String): ImageVector {
        return when {
            category.contains("الراتب") || category.contains("أرباح") -> Icons.Default.AttachMoney
            category.contains("سوبر ماركت") || category.contains("طلبات") -> Icons.Default.ShoppingCart
            category.contains("خضروات") || category.contains("فاكهة") -> Icons.Default.ShoppingBag
            category.contains("مطاعم") || category.contains("أكل") -> Icons.Default.Fastfood
            category.contains("بنزين") -> Icons.Default.LocalGasStation
            category.contains("مواصلات") || category.contains("سيارة") -> Icons.Default.DirectionsCar
            category.contains("تعليم") || category.contains("دروس") -> Icons.Default.School
            category.contains("إيجار") || category.contains("منزل") -> Icons.Default.Home
            category.contains("كهرباء") -> Icons.Default.ElectricBolt
            category.contains("مياه") -> Icons.Default.WaterDrop
            category.contains("إنترنت") -> Icons.Default.Wifi
            category.contains("هاتف") || category.contains("تليفون") || category.contains("باقة") -> Icons.Default.PhoneAndroid
            category.contains("صيدلية") || category.contains("أدوية") -> Icons.Default.LocalPharmacy
            category.contains("بنك") -> Icons.Default.AccountBalance
            else -> Icons.Default.Receipt
        }
    }

    fun getCategoryColor(category: String): Color {
        return when {
            category.contains("الراتب") -> Color(0xFF2E7D32)
            category.contains("سوبر ماركت") -> Color(0xFF1976D2)
            category.contains("مطاعم") -> Color(0xFFE65100)
            category.contains("تعليم") || category.contains("دروس") -> Color(0xFF6A1B9A)
            category.contains("إيجار") || category.contains("منزل") -> Color(0xFFD84315)
            category.contains("كهرباء") || category.contains("غاز") -> Color(0xFFF57F17)
            category.contains("إنترنت") || category.contains("هاتف") -> Color(0xFF0277BD)
            category.contains("صيدلية") -> Color(0xFFC2185B)
            else -> Color(0xFF455A64)
        }
    }
}
