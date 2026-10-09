package com.example.ui.screens

import com.example.ui.utils.AppStrings

import com.example.ui.utils.AppText

import android.widget.Toast
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import java.util.Locale

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import com.example.data.UserProfile
import com.example.ui.components.UserAvatarView

import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import androidx.compose.material.icons.filled.Language
import com.example.ui.utils.LocalStrings
import com.example.ui.utils.GoogleDriveBackupHelper
import com.example.ui.utils.PdfExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndVaultsScreen(
    userProfile: UserProfile,
    vaults: List<VaultEntity>,
    budgetLimits: List<BudgetLimitEntity>,
    transactions: List<TransactionEntity>,
    commitments: List<CommitmentEntity> = emptyList(),
    lessons: List<ChildLessonEntity> = emptyList(),
    currency: String,
    selectedLanguage: String = "ar",
    isDarkMode: Boolean,
    isPinEnabled: Boolean,
    isDailyReminderEnabled: Boolean,
    onSelectCurrency: (String) -> Unit,
    onSelectLanguage: (String) -> Unit = {},
    onToggleDarkMode: (Boolean) -> Unit,
    onTogglePin: (Boolean) -> Unit,
    onToggleDailyReminder: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenAddVault: () -> Unit,
    onEditVault: (VaultEntity) -> Unit = {},
    onDeleteVault: (Int) -> Unit = {},
    onOpenSetBudget: (String, Double) -> Unit,
    onDeleteBudget: (String) -> Unit = {},
    onOpenEditProfile: () -> Unit,
    onToggleTwoFactor: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onOpenCloudVault: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currencyExpanded by remember { mutableStateOf(false) }
    var lastDriveSyncTime by remember { mutableStateOf(AppText.text(com.example.R.string.text_d1768f19993f)) }
    var vaultToDelete by remember { mutableStateOf<VaultEntity?>(null) }
    var isAboutExpanded by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var legalDialog by remember { mutableStateOf<String?>(null) }
    val currencies = listOf("ج.م", "ريال سعودي", "درهم إماراتي", "دولار أمريكي")

    if (vaultToDelete != null) {
        val target = vaultToDelete!!
        AlertDialog(
            onDismissRequest = { vaultToDelete = null },
            title = {
                Text(
                    text = AppText.text(com.example.R.string.text_daa818587279),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = AppText.text(com.example.R.string.text_9e5ba8f6616f, target.name, String.format(Locale.US, "%,.0f", target.balance), AppText.currency(currency))
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVault(target.id)
                        vaultToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppText.text(com.example.R.string.text_2f1f7ddd4129), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vaultToDelete = null }) {
                    Text(AppText.text(com.example.R.string.text_98df46fbd83b))
                }
            }
        )
    }

    if (showUpdateDialog) {
        UpdateStatusDialog(
            versionName = com.example.BuildConfig.VERSION_NAME,
            versionCode = com.example.BuildConfig.VERSION_CODE,
            onDismiss = { showUpdateDialog = false }
        )
    }

    legalDialog?.let { type ->
        LegalInfoDialog(
            title = if (type == "privacy") {
                AppText.text(com.example.R.string.about_privacy_policy)
            } else {
                AppText.text(com.example.R.string.about_terms_of_use)
            },
            body = if (type == "privacy") {
                AppText.text(com.example.R.string.about_privacy_body)
            } else {
                AppText.text(com.example.R.string.about_terms_body)
            },
            icon = if (type == "privacy") Icons.Default.Security else Icons.Default.Gavel,
            onDismiss = { legalDialog = null }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // USER PROFILE & ACCOUNT CARD
        item {
            OutlinedButton(onClick = onOpenBackup, modifier = Modifier.fillMaxWidth()) {
                Text(AppText.text(com.example.R.string.text_5662250c3ec4))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatarView(
                                avatarId = userProfile.avatarId,
                                userName = userProfile.name,
                                size = 56.dp,
                                onClick = onOpenEditProfile
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = userProfile.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = userProfile.email,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = userProfile.phone,
                                    fontSize = 11.sp,
                                    color = EmeraldGreenPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(onClick = onOpenEditProfile) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = AppText.text(com.example.R.string.text_f55b07f1ae61),
                                tint = GoldAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenEditProfile,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppText.text(com.example.R.string.text_41510ce0badf), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppText.text(com.example.R.string.text_8710d64ff1ad), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        // MULTIPLE VAULTS / ACCOUNTS SECTION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = AppText.text(com.example.R.string.text_e0844ef2c543), tint = EmeraldGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = AppText.text(com.example.R.string.text_b7ad4afa609d), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(onClick = onOpenAddVault) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = AppText.text(com.example.R.string.text_e55a7fa423a7), tint = EmeraldGreenPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    vaults.forEach { vault ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(GoldAccent.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = vault.name,
                                        tint = EmeraldGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = vault.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", vault.balance)} ${AppText.currency(currency)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGreenPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Edit Vault Button
                                IconButton(
                                    onClick = { onEditVault(vault) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = AppText.text(com.example.R.string.text_47e4c5109ec1),
                                        tint = EmeraldGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete/Cancel Vault Button
                                IconButton(
                                    onClick = { vaultToDelete = vault },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = AppText.text(com.example.R.string.text_278eeef2a57d),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // CATEGORY BUDGET LIMITS SECTION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PieChart, contentDescription = AppText.text(com.example.R.string.text_185744848ec0), tint = EmeraldGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = AppText.text(com.example.R.string.text_3a2c390f55f5), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(onClick = { onOpenSetBudget("", 0.0) }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = AppText.text(com.example.R.string.text_c32f3d7416d3), tint = EmeraldGreenPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (budgetLimits.isEmpty()) {
                        Text(text = AppText.text(com.example.R.string.text_3372c9d2ab39), color = Color.Gray, fontSize = 12.sp)
                    } else {
                        budgetLimits.forEach { limit ->
                            val spent = transactions.filter { it.type == "EXPENSE" && it.category == limit.category }.sumOf { it.amount }
                            val pct = if (limit.monthlyLimit > 0) (spent / limit.monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onOpenSetBudget(limit.category, limit.monthlyLimit) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = AppStrings(AppText.language).translateCategory(limit.category), fontWeight = FontWeight.Medium, fontSize = 13.sp)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.0f", spent)} / ${String.format(Locale.US, "%.0f", limit.monthlyLimit)} ${AppText.currency(currency)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (spent > limit.monthlyLimit) ExpenseRed else EmeraldGreenPrimary
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = { onOpenSetBudget(limit.category, limit.monthlyLimit) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = AppText.text(com.example.R.string.text_b4f76c3aa21e),
                                                tint = EmeraldGreenPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteBudget(limit.category) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = AppText.text(com.example.R.string.text_2d2bbdc2d694),
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (pct >= 0.9f) ExpenseRed else EmeraldGreenPrimary,
                                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // APP PREFERENCES SECTION
        item {
            val appStrings = LocalStrings.current
            var isPreferencesExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { isPreferencesExpanded = !isPreferencesExpanded },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBF9)),
                border = BorderStroke(1.dp, Color(0xFFE2EBE4))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title, Subtitle, and Settings Badge on the Right (RTL)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD6ECE0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = appStrings.appPreferences,
                                    tint = Color(0xFF133621),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = appStrings.appPreferences,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B241E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = AppText.text(com.example.R.string.text_615f47f69884),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF6B7B70)
                                )
                            }
                        }

                        // Arrow Left Icon on the Left (RTL)
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = Color(0xFF5A665E),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Expanded Content
                    AnimatedVisibility(visible = isPreferencesExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFE2EBE4))
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Currency Selector
                            ExposedDropdownMenuBox(
                                expanded = currencyExpanded,
                                onExpandedChange = { currencyExpanded = !currencyExpanded }
                            ) {
                                OutlinedTextField(
                                    value = AppText.currency(currency),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(appStrings.appCurrency) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = currencyExpanded,
                                    onDismissRequest = { currencyExpanded = false }
                                ) {
                                    currencies.forEach { cur ->
                                        DropdownMenuItem(
                                            text = { Text(AppText.currency(cur)) },
                                            onClick = {
                                                onSelectCurrency(cur)
                                                currencyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Dark Mode Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(checked = isDarkMode, onCheckedChange = onToggleDarkMode)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = AppText.text(com.example.R.string.text_59d33e6d1d08), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1B241E))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(imageVector = Icons.Default.DarkMode, contentDescription = AppText.text(com.example.R.string.text_3318a3c350a9), tint = EmeraldGreenPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Fingerprint/PIN Protection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(checked = isPinEnabled, onCheckedChange = onTogglePin)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = AppText.text(com.example.R.string.text_b2aa3b181ef7), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1B241E))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = AppText.text(com.example.R.string.text_2cd8f97fe205), tint = EmeraldGreenPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Two-Factor Authentication (2FA) - Unavailable until real Firebase MFA
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = false,
                                    onCheckedChange = null,
                                    enabled = false
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = AppText.text(com.example.R.string.text_e408bd278ef3), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                        Text(
                                            text = "قيد التطوير عبر Firebase MFA (غير مفعل)",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = AppText.text(com.example.R.string.text_ca1e582768c3),
                                        tint = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Daily Reminder WorkManager Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(checked = isDailyReminderEnabled, onCheckedChange = onToggleDailyReminder)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = AppText.text(com.example.R.string.text_518c30ca8f7f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B241E))
                                        Text(text = AppText.text(com.example.R.string.text_3ce2fa4d9cb9), fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = if (isDailyReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                        contentDescription = AppText.text(com.example.R.string.text_7274e30c25f2),
                                        tint = if (isDailyReminderEnabled) EmeraldGreenPrimary else Color.Gray
                                    )
                                }
                            }

                            if (isDailyReminderEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        onSendTestNotification()
                                        Toast.makeText(context, AppText.text(com.example.R.string.text_d32fd57d806f), Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = AppText.text(com.example.R.string.text_21011df2bcce), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = AppText.text(com.example.R.string.text_eba265c3744d), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }


        // PDF & EXCEL REPORT EXPORTS SECTION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBF9)),
                border = BorderStroke(1.dp, Color(0xFFE2EBE4))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD6ECE0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = AppText.text(com.example.R.string.text_92fec7ad5c88),
                                    tint = Color(0xFF133621),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_7d11e29cfdd6),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B241E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = AppText.text(com.example.R.string.text_d2a75b7bc22e),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF6B7B70)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PDF & Excel Report Exports Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // PDF Export Button
                        Button(
                            onClick = {
                                val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                                val unpaidCommitmentsSum = commitments.filter { !it.isPaid }.sumOf { it.amount }
                                val unpaidLessonsSum = lessons.filter { !it.isPaid }.sumOf { it.amount }
                                val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } + unpaidCommitmentsSum + unpaidLessonsSum
                                PdfExporter.generateAndOpenFinancialPdf(
                                    context = context,
                                    userProfile = userProfile,
                                    vaults = vaults,
                                    transactions = transactions,
                                    currency = currency,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)), // PDF Red
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = AppText.text(com.example.R.string.text_770c11e048d1),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = AppText.text(com.example.R.string.text_0f7b9bca13a1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Excel Export Button
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, AppText.text(com.example.R.string.text_99f67b516741), Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = AppText.text(com.example.R.string.text_692e087830e9),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = AppText.text(com.example.R.string.text_692e087830e9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ABOUT APP SECTION
        item {
            Text(
                text = AppText.text(com.example.R.string.about_section_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutActionCard(
                    title = AppText.text(com.example.R.string.about_app_title),
                    subtitle = AppText.text(
                        com.example.R.string.about_app_version_subtitle,
                        com.example.BuildConfig.VERSION_NAME
                    ),
                    icon = Icons.Default.Info,
                    onClick = { isAboutExpanded = !isAboutExpanded },
                    modifier = Modifier.weight(1f)
                )

                AboutActionCard(
                    title = AppText.text(com.example.R.string.about_update_title),
                    subtitle = AppText.text(com.example.R.string.about_update_subtitle),
                    icon = Icons.Default.SystemUpdateAlt,
                    onClick = { showUpdateDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            AnimatedVisibility(visible = isAboutExpanded) {
                AboutDetailsContent(
                    versionName = com.example.BuildConfig.VERSION_NAME,
                    versionCode = com.example.BuildConfig.VERSION_CODE,
                    onShare = {
                        val playUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                AppText.text(com.example.R.string.about_share_text, playUrl)
                            )
                        }
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                AppText.text(com.example.R.string.about_share_app)
                            )
                        )
                    },
                    onRate = {
                        val marketUri = Uri.parse("market://details?id=${context.packageName}")
                        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, marketUri))
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    onPrivacy = { legalDialog = "privacy" },
                    onTerms = { legalDialog = "terms" }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(178.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun AboutDetailsContent(
    versionName: String,
    versionCode: Int,
    onShare: () -> Unit,
    onRate: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = AppText.text(com.example.R.string.about_message_section),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = AppText.text(com.example.R.string.about_message_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = AppText.text(com.example.R.string.about_message_body_1),
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = AppText.text(com.example.R.string.about_message_body_2),
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = AppText.text(com.example.R.string.about_share_heading),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = AppText.text(com.example.R.string.about_share_supporting),
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppText.text(com.example.R.string.about_share_app), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoldAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppText.text(com.example.R.string.about_rate_app), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            text = AppText.text(com.example.R.string.about_legal_section),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                LegalInfoRow(
                    title = AppText.text(com.example.R.string.about_privacy_policy),
                    subtitle = AppText.text(com.example.R.string.about_privacy_subtitle),
                    icon = Icons.Default.Security,
                    onClick = onPrivacy
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )
                LegalInfoRow(
                    title = AppText.text(com.example.R.string.about_terms_of_use),
                    subtitle = AppText.text(com.example.R.string.about_terms_subtitle),
                    icon = Icons.Default.Gavel,
                    onClick = onTerms
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = AppText.text(com.example.R.string.about_app_name),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppText.text(
                        com.example.R.string.about_version_full,
                        versionName,
                        versionCode
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppText.text(com.example.R.string.about_footer),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LegalInfoRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun UpdateStatusDialog(
    versionName: String,
    versionCode: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = AppText.text(com.example.R.string.about_latest_version_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = AppText.text(com.example.R.string.about_latest_version_body),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = AppText.text(
                            com.example.R.string.about_current_version,
                            versionName,
                            versionCode
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = AppText.text(com.example.R.string.about_ok),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalInfoDialog(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(title, fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Text(
                text = body,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(AppText.text(com.example.R.string.about_ok))
            }
        }
    )
}

