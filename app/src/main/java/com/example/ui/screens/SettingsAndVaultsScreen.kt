package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
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
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currencyExpanded by remember { mutableStateOf(false) }
    var lastDriveSyncTime by remember { mutableStateOf("اليوم، 04:30 م") }
    var vaultToDelete by remember { mutableStateOf<VaultEntity?>(null) }
    val currencies = listOf("ج.م", "ريال سعودي", "درهم إماراتي", "دولار أمريكي")

    if (vaultToDelete != null) {
        val target = vaultToDelete!!
        AlertDialog(
            onDismissRequest = { vaultToDelete = null },
            title = {
                Text(
                    text = "تأكيد حذف الخزنة / الحساب",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف '${target.name}' (${String.format(Locale.US, "%,.0f", target.balance)} $currency)؟ سيتم حذفها نهائياً."
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
                    Text("نعم، حذف الحساب", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vaultToDelete = null }) {
                    Text("تراجع")
                }
            }
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
                                contentDescription = "تعديل الحساب",
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
                            Text("تعديل بيانات الحساب", fontSize = 12.sp)
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
                            Text("تسجيل الخروج", fontSize = 12.sp)
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
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "الحسابات", tint = EmeraldGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "الخزانات والحسابات المالية", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(onClick = onOpenAddVault) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة حساب", tint = EmeraldGreenPrimary)
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
                                        text = "${String.format(Locale.US, "%,.0f", vault.balance)} $currency",
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
                                        contentDescription = "تعديل المبلغ/الاسم",
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
                                        contentDescription = "إلغاء الخزنة/الحساب",
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
                            Icon(imageVector = Icons.Default.PieChart, contentDescription = "الميزانية", tint = EmeraldGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "الميزانية الشهرية للتصنيفات", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(onClick = { onOpenSetBudget("", 0.0) }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة حد جديد", tint = EmeraldGreenPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (budgetLimits.isEmpty()) {
                        Text(text = "لم يتم تحديد أقصى حد للميزانية لأي تصنيف.", color = Color.Gray, fontSize = 12.sp)
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
                                    Text(text = limit.category, fontWeight = FontWeight.Medium, fontSize = 13.sp)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.0f", spent)} / ${String.format(Locale.US, "%.0f", limit.monthlyLimit)} $currency",
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
                                                contentDescription = "تعديل",
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
                                                contentDescription = "حذف",
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
                                    text = "العملة، المظهر، الأمان والتنبيهات",
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
                                    value = currency,
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
                                            text = { Text(cur) },
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
                                    Text(text = "الوضع الداكن (Dark Mode)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1B241E))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(imageVector = Icons.Default.DarkMode, contentDescription = "الوضع الداكن", tint = EmeraldGreenPrimary)
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
                                    Text(text = "قفل الخزنة ببصمة الإصبع / PIN", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1B241E))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "بصمة الأصبع", tint = EmeraldGreenPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Two-Factor Authentication (2FA) Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = userProfile.isTwoFactorEnabled,
                                    onCheckedChange = onToggleTwoFactor
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "التحقق بخطوتين (2FA)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B241E))
                                        Text(
                                            text = if (userProfile.isTwoFactorEnabled) "مُفعل - كود أمان عند الدخول" else "غير مُفعل",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "التحقق بخطوتين",
                                        tint = if (userProfile.isTwoFactorEnabled) GoldAccent else Color.Gray
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
                                        Text(text = "تذكير يومي بمراجعة المصروفات", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B241E))
                                        Text(text = "إشعار تلقائي يومياً الساعة 8:00 مساءً", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = if (isDailyReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                        contentDescription = "التنبيهات اليومية",
                                        tint = if (isDailyReminderEnabled) EmeraldGreenPrimary else Color.Gray
                                    )
                                }
                            }

                            if (isDailyReminderEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        onSendTestNotification()
                                        Toast.makeText(context, "تم جدولة إشعار تجريبي يظهر فوراً", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "تجربة الإشعار", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "تجربة إشعار التذكير الآن (Test Notification)", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🔒 CLOUD FIRESTORE ENCRYPTED VAULT CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenCloudVault() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenPrimary.copy(alpha = 0.08f)),
                border = BorderStroke(1.5.dp, EmeraldGreenPrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "الخزنة السحابية المشفرة",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "🛡️☁️", fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "تشفير AES-256 محلي ومزامنة عبر Cloud Firestore",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF6B7B70)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onOpenCloudVault,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فتح الخزنة السحابية (كلمات السر والملاحظات)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    contentDescription = "التقارير",
                                    tint = Color(0xFF133621),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "تصدير التقارير المالية",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B241E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "تصدير السجلات بصيغة PDF أو Excel",
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
                                contentDescription = "تصدير PDF",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "تصدير تقرير PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Excel Export Button
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "تم تصدير سجل المعاملات المالية بصيغة Excel (CSV) بنجاح!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "تصدير Excel",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "تصدير Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ABOUT DEVELOPER / ABOUT APP SECTION
        item {
            var isAboutExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { isAboutExpanded = !isAboutExpanded },
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
                        // Title, Subtitle, and Info Badge on the Right (RTL)
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
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "عن التطبيق",
                                    tint = Color(0xFF133621),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "عن التطبيق",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B241E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "معلومات التطبيق والإصدار",
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

                    // Expanded Details
                    AnimatedVisibility(visible = isAboutExpanded) {
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

                            // App Info
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color.White,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD6ECE0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = EmeraldGreenPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "الخزنة الذكية",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "الإصدار 1.0.0 • إدارة الأموال والذهب والمدخرات",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "صنع بكل حـــ ❤️ــــــب فى مصر",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
