package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.GoldAccent

@Composable
fun UserAvatarView(
    avatarId: Int,
    userName: String,
    photoUrl: String? = null,
    size: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickableModifier = if (onClick != null) modifier.clickable { onClick() } else modifier

    val bgBrush = when (avatarId) {
        1 -> Brush.linearGradient(listOf(EmeraldGreenPrimary, EmeraldGreenDark))
        2 -> Brush.linearGradient(listOf(GoldAccent, Color(0xFFB8860B)))
        3 -> Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)))
        4 -> Brush.linearGradient(listOf(Color(0xFF8E24AA), Color(0xFF4A148C)))
        5 -> Brush.linearGradient(listOf(Color(0xFFD81B60), Color(0xFF880E4F)))
        else -> Brush.linearGradient(listOf(Color(0xFF00897B), Color(0xFF004D40)))
    }

    val iconVector = when (avatarId) {
        1 -> Icons.Default.Shield
        2 -> Icons.Default.Star
        3 -> Icons.Default.VerifiedUser
        4 -> Icons.Default.BusinessCenter
        5 -> Icons.Default.Wallet
        else -> Icons.Default.Person
    }

    Box(
        modifier = clickableModifier
            .size(size)
            .clip(CircleShape)
            .background(bgBrush)
            .border(2.dp, GoldAccent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = "صورة المستخدم",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                error = {
                    FallbackAvatarContent(avatarId, userName, size, iconVector)
                }
            )
        } else {
            FallbackAvatarContent(avatarId, userName, size, iconVector)
        }
    }
}

@Composable
private fun FallbackAvatarContent(
    avatarId: Int,
    userName: String,
    size: Dp,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector
) {
    if (userName.isNotBlank() && avatarId == 1) {
        val initial = userName.trim().take(1).uppercase()
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    } else {
        Icon(
            imageVector = iconVector,
            contentDescription = "صورة المستخدم",
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
