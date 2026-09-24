package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.MonetizationOn
import com.example.data.UserProfile
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.ChildLessonEntity
import com.example.ui.DashboardUiState
import com.example.ui.dialogs.UnpaidBillsDialog
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LightBackground
import com.example.ui.theme.MintBackground
import com.example.ui.utils.CategoryUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.utils.LocalStrings

data class AchievementBadge(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isUnlocked: Boolean = false
)

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    vaults: List<VaultEntity>,
    recentTransactions: List<TransactionEntity>,
    userProfile: UserProfile,
    commitments: List<CommitmentEntity> = emptyList(),
    lessons: List<ChildLessonEntity> = emptyList(),
    onPayCommitment: (CommitmentEntity) -> Unit = {},
    onPayLesson: (ChildLessonEntity) -> Unit = {},
    onSelectVault: (String) -> Unit,
    onOpenAddIncome: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenAddVault: () -> Unit,
    onOpenSetBudget: (String, Double) -> Unit,
    onOpenSettings: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onNavigateTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appStrings = LocalStrings.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("smart_vault_prefs", android.content.Context.MODE_PRIVATE) }
    var isSavingsCardVisible by rememberSaveable {
        mutableStateOf(prefs.getBoolean("dashboard_savings_card_visible", false))
    }
    var isBalanceVisible by remember { mutableStateOf(true) }
    var showUnpaidBillsDialog by remember { mutableStateOf(false) }

    if (showUnpaidBillsDialog) {
        UnpaidBillsDialog(
            commitments = commitments,
            lessons = lessons,
            currency = state.currency,
            onPayCommitment = onPayCommitment,
            onPayLesson = onPayLesson,
            onDismiss = { showUnpaidBillsDialog = false }
        )
    }

    val formattedCurrentMonth = remember {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("ar"))
        sdf.format(Date())
    }

    val badgesList = remember {
        listOf(
            AchievementBadge("أفضل من الشهر الماضي", "مصروفاتك أقل من الشهر السابق", Icons.Default.TrendingDown),
            AchievementBadge("اقتصادي", "وفّر 20% أو أكثر من دخل الشهر", Icons.Default.AccountBalanceWallet),
            AchievementBadge("مُتابع يومي", "سجّل عمليات في 15 يوم أو أكثر هذا الشهر", Icons.Default.EventNote),
            AchievementBadge("منظم الالتزامات", "سدّد كل التزامات ودروس الشهر", Icons.Default.CheckCircle),
            AchievementBadge("تحت السيطرة", "توقع نهاية الشهر في المنطقة الآمنة", Icons.Default.Verified),
            AchievementBadge("أرشيف الفواتير", "أرفق صور لنصف مصروفات الشهر على الأقل", Icons.Default.InsertDriveFile)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER (الخزنة الذكية + أغسطس 2026 + زر الإعدادات)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title & Subtitle (Right in RTL)
                Column {
                    Text(
                        text = appStrings.titleHome,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedCurrentMonth,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Top Action Buttons: Language, Savings Toggle & Settings Gear (Left in RTL)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, CardBorderColor, CircleShape)
                            .clickable { onToggleLanguage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Toggle Savings Card (Right beside settings gear)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSavingsCardVisible) GoldAccent.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (isSavingsCardVisible) Color(0xFFB8860B) else CardBorderColor,
                                CircleShape
                            )
                            .clickable {
                                isSavingsCardVisible = !isSavingsCardVisible
                                prefs.edit().putBoolean("dashboard_savings_card_visible", isSavingsCardVisible).apply()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = if (isSavingsCardVisible) "إخفاء بطاقة المدخرات" else "إظهار بطاقة المدخرات",
                            tint = if (isSavingsCardVisible) Color(0xFFB8860B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, CardBorderColor, CircleShape)
                            .clickable { onOpenSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = appStrings.titleSettings,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. MAIN HERO VAULT BALANCE CARD (الرصيد الحالي في الخزنة)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00875A),
                                    Color(0xFF006B47)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // Current Vault Balance Label & Action Icons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = appStrings.currentBalance,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Alarm / Due Badge
                                Row(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(17.dp))
                                        .background(Color(0xFFDC2626))
                                        .clickable { showUnpaidBillsDialog = true }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = "المستحقات",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${state.dueBillsCount} مستحق",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // Eye Toggle Box
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f))
                                        .clickable { isBalanceVisible = !isBalanceVisible },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "إظهار/إخفاء الرصيد",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isBalanceVisible)
                                "${String.format(Locale.US, "%,.0f", state.currentVaultBalance)} ${state.currency}"
                            else
                                "•••••• ${state.currency}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sub-Pills Row: دخل الشهر & مصروفات الشهر
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Monthly Income Pill (Right)
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E).copy(alpha = 0.28f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "سهم للأعلى - دخل",
                                                tint = Color(0xFF4ADE80),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = appStrings.monthlyIncome,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBalanceVisible)
                                            "${String.format(Locale.US, "%,.0f", state.totalIncomeThisMonth)} ${state.currency}"
                                        else
                                            "•••• ${state.currency}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Monthly Expense Pill (Left)
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444).copy(alpha = 0.28f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "سهم للأسفل - مصروفات",
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = appStrings.monthlyExpenses,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBalanceVisible)
                                            "${String.format(Locale.US, "%,.0f", state.totalExpenseThisMonth)} ${state.currency}"
                                        else
                                            "•••• ${state.currency}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2.5. SAVINGS & GOLD ASSETS CARD (المدخرات والذهب)
        if (isSavingsCardVisible) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateTab(4) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row with Gold Icon + Title + Arrow to open tab
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "المدخرات",
                                    tint = Color(0xFFB8860B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "المدخرات والأصول المالية 💰",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ذهب وسبائك + نقدية (أصل مالي)",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = "عرض الكل ←",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Total Savings Highlight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MintBackground)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("إجمالي المدخرات (كاش + ذهب)", fontSize = 11.5.sp, color = EmeraldGreenDark, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBalanceVisible)
                                        "${String.format(Locale.US, "%,.0f", state.totalSavings)} ${state.currency}"
                                    else
                                        "•••• ${state.currency}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldGreenDark
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldGreenPrimary)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "أصل مالي",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 Mini stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gold Value
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("💛 قيمة الذهب", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBalanceVisible) "${String.format(Locale.US, "%,.0f", state.totalGoldValue)}" else "••••",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Cash Savings
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("💵 نقدية", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBalanceVisible) "${String.format(Locale.US, "%,.0f", state.totalCashSavings)}" else "••••",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Gold Weight
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("⚖️ وزن الذهب", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", state.totalGoldWeightGrams)}g",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Pieces Count
                        Card(
                            modifier = Modifier.weight(0.85f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("📦 القطع", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${state.totalGoldPiecesCount} قطع",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // 3. SMART ANALYTICS CARD (التحليل الذكي)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row with Brain Icon next to text on the right
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MintBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "التحليل الذكي",
                                tint = EmeraldGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = appStrings.smartAnalytics,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Light Mint Notification Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MintBackground)
                    ) {
                        Text(
                            text = "متوقع يتبقى لك ${String.format(Locale.US, "%,.0f", state.remainingSalary)} ${state.currency} بنهاية الشهر بنفس معدل الصرف الحالي.",
                            fontSize = 12.5.sp,
                            color = EmeraldGreenDark,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2x2 Grid of Metrics Cards
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Row 1: مصروفات الشهر الماضي / دخل الشهر الماضي
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Right Pill: مصروفات الشهر الماضي
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = LightBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("مصروفات الشهر الماضي", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("0 ${state.currency}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("لا مقارنة متاحة", fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            // Left Pill: دخل الشهر الماضي
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = LightBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("دخل الشهر الماضي", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("0 ${state.currency}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("لا مقارنة متاحة", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Row 2: متوسط الصرف اليومي / حد الصرف الآمن يومياً
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Right Pill: متوسط الصرف اليومي
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = LightBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("متوسط الصرف اليومي", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val dailyAvg = state.totalExpenseThisMonth / 30.0
                                    Text("${String.format(Locale.US, "%,.0f", dailyAvg)} ${state.currency}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Left Pill: حد الصرف الآمن يومياً
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = LightBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("حد الصرف الآمن يومياً", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val safeDaily = (state.remainingSalary / 23.0).coerceAtLeast(0.0)
                                    Text("${String.format(Locale.US, "%,.0f", safeDaily)} ${state.currency}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ACTION BUTTONS ROW ("+ إضافة دخل" & "+ إضافة مصروف")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Add Income Button (Solid Green Container - Right RTL)
                        Button(
                            onClick = onOpenAddIncome,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "إضافة دخل",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(appStrings.addIncome, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }

                        // Add Expense Button (Soft Mint Tint Container - Left RTL)
                        Button(
                            onClick = onOpenAddExpense,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MintBackground,
                                contentColor = EmeraldGreenPrimary
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = appStrings.addExpense,
                                tint = EmeraldGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(appStrings.addExpense, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldGreenPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Card 1: نسبة المستهلك من الراتب
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.salarySpentPercentage.toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldGreenDark
                                )
                                Text(
                                    text = "نسبة المستهلك من الراتب",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { (state.salarySpentPercentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = EmeraldGreenDark,
                                trackColor = Color(0xFFEFEFEF)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Bottom Metrics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المتبقي: ${String.format(Locale.US, "%,.0f", state.remainingSalary)} ${state.currency}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreenDark
                                )
                                Text(
                                    text = "المنفق: ${String.format(Locale.US, "%,.0f", state.totalExpenseThisMonth)} ${state.currency}",
                                    fontSize = 12.5.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Card 2: أين ذهب راتبي هذا الشهر؟
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF6E9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3E8C8))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Gold Circle with Pie Chart Icon (First element in RTL Row = Right side)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD9A726)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = "تحليل الراتب",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "أين ذهب راتبي هذا الشهر؟",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreenDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "لقد قمت بإدارة ${state.salarySpentPercentage.toInt()}% من راتبك بحكمة. تفقد صفحة التقارير لرؤية التحليل التفصيلي.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4B5563),
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. MONTHLY REWARDS CARD (مكافآتك هذا الشهر)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row with Trophy Icon & Badge Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MintBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "مكافآتك",
                                    tint = EmeraldGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "مكافآتك هذا الشهر",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${badgesList.count { it.isUnlocked }} / 6",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2 Column Grid of Badges
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in badgesList.indices step 2) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BadgeCardItem(
                                    badge = badgesList[i],
                                    modifier = Modifier.weight(1f)
                                )
                                if (i + 1 < badgesList.size) {
                                    BadgeCardItem(
                                        badge = badgesList[i + 1],
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "كمّل تسجيل عملياتك وسدّد التزاماتك في وقتها لتحصل على باقي الشارات.",
                        fontSize = 11.5.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 5. RECENT TRANSACTIONS ("آخر العمليات")
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخر العمليات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "عرض الكل",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenPrimary,
                        modifier = Modifier.clickable { onNavigateTab(1) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (recentTransactions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد عمليات بعد",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentTransactions.take(4).forEach { tx ->
                            TransactionRowItem(tx = tx, currency = state.currency)
                        }
                    }
                }
            }
        }

        // 9. BOTTOM SHORTCUT CARDS ROW ("الالتزامات الشهرية 💵" & "أين ذهب راتبي؟ 👛")
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Right Shortcut Card: الالتزامات الشهرية
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable { onNavigateTab(2) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الالتزامات الشهرية",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Left Shortcut Card: أين ذهب راتبي؟
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable { onNavigateTab(3) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "أين ذهب راتبي؟",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun BadgeCardItem(
    badge: AchievementBadge,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LightBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = badge.icon,
                    contentDescription = badge.title,
                    tint = Color(0xFF55606E),
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = badge.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = badge.subtitle,
                fontSize = 10.sp,
                color = Color.Gray,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun TransactionRowItem(tx: TransactionEntity, currency: String) {
    val isIncome = tx.type == "INCOME"
    val icon = CategoryUtils.getCategoryIcon(tx.category)
    val iconColor = if (isIncome) IncomeGreen else CategoryUtils.getCategoryColor(tx.category)
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale("ar")) }
    val formattedDate = remember(tx.dateMillis) { sdf.format(Date(tx.dateMillis)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tx.category,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.category,
                        fontSize = 11.sp,
                        color = iconColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = " • ", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = tx.vaultName,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(text = " • ", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Text(
                text = "${if (isIncome) "+" else "-"}${String.format(Locale.US, "%,.0f", tx.amount)} $currency",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = if (isIncome) IncomeGreen else ExpenseRed
            )
        }
    }
}

