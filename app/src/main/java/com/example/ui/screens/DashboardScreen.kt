package com.example.ui.screens

import com.example.ui.utils.AppStrings

import com.example.ui.utils.AppText

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyRow
import java.util.Calendar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.example.data.calculator.FinancialSummaryResult
import com.example.data.insights.FinancialInsight
import com.example.data.insights.InsightPriority
import com.example.data.preferences.ModuleConfiguration
import com.example.ui.dialogs.AllInsightsDialog
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
import androidx.compose.ui.text.style.TextOverflow
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
    financialSummary: FinancialSummaryResult = FinancialSummaryResult(),
    insights: List<FinancialInsight> = emptyList(),
    config: ModuleConfiguration = ModuleConfiguration(),
    onOpenNetWorth: () -> Unit = {},
    onOpenDebts: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenTransfer: () -> Unit = {},
    onOpenManageModules: () -> Unit = {},
    onOpenAddIncome: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenAddVault: () -> Unit,
    onOpenSetBudget: (String, Double) -> Unit,
    onOpenSettings: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onNavigateTab: (Int) -> Unit = {},
    onOpenCommitments: () -> Unit = { onNavigateTab(1) },
    modifier: Modifier = Modifier
) {
    val appStrings = LocalStrings.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("smart_vault_prefs", android.content.Context.MODE_PRIVATE) }
    var isSavingsCardVisible by rememberSaveable {
        mutableStateOf(prefs.getBoolean("dashboard_savings_card_visible", false))
    }
    var isBalanceVisible by remember { mutableStateOf(true) }
    var showAllInsightsDialog by remember { mutableStateOf(false) }
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

    val formattedCurrentMonth = remember(AppText.language) {
        val sdf = SimpleDateFormat("MMMM yyyy", AppText.locale)
        sdf.format(Date())
    }

    val badgesList = remember(AppText.language) {
        listOf(
            AchievementBadge(AppText.text(com.example.R.string.text_423c46d500b6), AppText.text(com.example.R.string.text_07573c6f7bf2), Icons.Default.TrendingDown),
            AchievementBadge(AppText.text(com.example.R.string.text_b1941eb051e1), AppText.text(com.example.R.string.text_ff701fdfe751), Icons.Default.AccountBalanceWallet),
            AchievementBadge(AppText.text(com.example.R.string.text_b0bbbdfb65cd), AppText.text(com.example.R.string.text_caf13cd7cfae), Icons.Default.EventNote),
            AchievementBadge(AppText.text(com.example.R.string.text_bb695066ad60), AppText.text(com.example.R.string.text_4cd4daac5f7a), Icons.Default.CheckCircle),
            AchievementBadge(AppText.text(com.example.R.string.text_e0b6c7065e6a), AppText.text(com.example.R.string.text_9c27d696138c), Icons.Default.Verified),
            AchievementBadge(AppText.text(com.example.R.string.text_3196303021cd), AppText.text(com.example.R.string.text_6316da350959), Icons.Default.InsertDriveFile)
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 64.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appStrings.titleHome,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedCurrentMonth,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

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
                            contentDescription = if (isSavingsCardVisible) AppText.text(com.example.R.string.text_e74cf8f88064) else AppText.text(com.example.R.string.text_c8b0cd8eb4b8),
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

        // 1.2. VAULT ENVELOPES SELECTOR (أظرف وخزائن الأسرة)
        item {
            VaultEnvelopesSelector(
                vaults = vaults,
                selectedVaultName = state.selectedVaultName,
                onSelectVault = onSelectVault,
                onOpenAddVault = onOpenAddVault,
                currency = state.currency,
                isBalanceVisible = isBalanceVisible
            )
        }

        // 1.5. QUICK SHORTCUTS ROW (صافي الثروة | الديون | التقويم | تحويل | تخصيص)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Net Worth
                QuickShortcutButton(
                    title = AppText.text(com.example.R.string.text_0b8ff6a5604b),
                    icon = Icons.Default.Security,
                    tint = EmeraldGreenPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenNetWorth
                )

                // Debts (if enabled)
                if (config.debts) {
                    QuickShortcutButton(
                        title = AppText.text(com.example.R.string.text_3beed403483e),
                        icon = Icons.Default.AccountBalance,
                        tint = Color(0xFF7C4DFF),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDebts
                    )
                }

                // Calendar
                QuickShortcutButton(
                    title = AppText.text(com.example.R.string.text_9555082e8961),
                    icon = Icons.Default.CalendarMonth,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCalendar
                )

                // Transfer
                QuickShortcutButton(
                    title = AppText.text(com.example.R.string.text_0326e4ed17db),
                    icon = Icons.Default.SwapHoriz,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTransfer
                )

                // Manage Modules
                QuickShortcutButton(
                    title = AppText.text(com.example.R.string.text_f6968545d1d8),
                    icon = Icons.Default.Tune,
                    tint = Color.Gray,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenManageModules
                )
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
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

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
                                        contentDescription = AppText.text(com.example.R.string.text_cec0e67fc3a9),
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = AppText.text(com.example.R.string.text_b5ddb3447714, state.dueBillsCount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
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
                                        contentDescription = AppText.text(com.example.R.string.text_877e2ea5f33a),
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isBalanceVisible)
                                "${String.format(Locale.US, "%,.0f", state.currentVaultBalance)} ${AppText.currency(state.currency)}"
                            else
                                "\u2022\u2022\u2022\u2022\u2022\u2022 ${AppText.currency(state.currency)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sub-Pills Row: دخل الشهر & مصروفات الشهر
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Monthly Income Pill (Right)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
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
                                                contentDescription = AppText.text(com.example.R.string.text_4654875cd068),
                                                tint = Color(0xFF4ADE80),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = appStrings.monthlyIncome,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBalanceVisible)
                                            "${String.format(Locale.US, "%,.0f", state.totalIncomeThisMonth)} ${AppText.currency(state.currency)}"
                                        else
                                            "\u2022\u2022\u2022\u2022 ${AppText.currency(state.currency)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Monthly Expense Pill (Left)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
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
                                                contentDescription = AppText.text(com.example.R.string.text_3e1877e91a6f),
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = appStrings.monthlyExpenses,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBalanceVisible)
                                            "${String.format(Locale.US, "%,.0f", state.totalExpenseThisMonth)} ${AppText.currency(state.currency)}"
                                        else
                                            "\u2022\u2022\u2022\u2022 ${AppText.currency(state.currency)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2.1. HOUSEHOLD BUDGET HEALTH CARD (صحة ميزانية الأسرة وضبط الإنفاق)
        item {
            HouseholdBudgetHealthCard(
                remainingSalary = state.remainingSalary,
                totalIncome = state.totalIncomeThisMonth,
                totalExpense = state.totalExpenseThisMonth,
                currency = state.currency,
                isBalanceVisible = isBalanceVisible
            )
        }

        // 2.15. URGENT FAMILY COMMITMENTS & KIDS LESSONS (التزامات ودروس الأسرة المستحقة)
        item {
            UrgentFamilyCommitmentsCard(
                commitments = commitments,
                lessons = lessons,
                currency = state.currency,
                onPayCommitment = onPayCommitment,
                onPayLesson = onPayLesson,
                onOpenCommitmentsTab = onOpenCommitments
            )
        }

        // 2.2. MY FINANCIAL SITUATION TODAY
        //
        // The primary number intentionally represents what the user currently
        // owns (cash/accounts + independent savings + gold + receivables).
        // Confirmed liabilities stay visible but are NOT subtracted from this
        // headline amount. The accounting position after liabilities remains
        // available on the detailed financial-position screen.
        item {
            val breakdown = financialSummary.netWorthBreakdown
            val availableFunds = breakdown.vaultsTotal + breakdown.cashSavingsTotal

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenNetWorth() },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreenPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = AppText.text(com.example.R.string.text_0b8ff6a5604b),
                                    tint = EmeraldGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_1b7441ca41de),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = AppText.text(com.example.R.string.text_a738ad2e9389),
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = AppText.text(com.example.R.string.text_e11353146505),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(EmeraldGreenPrimary.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = AppText.text(com.example.R.string.financial_overview_total_owned),
                                fontSize = 11.5.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBalanceVisible)
                                    "${String.format(Locale.US, "%,d", breakdown.totalAssets.toLong().coerceAtLeast(0L))} ${AppText.currency(state.currency)}"
                                else
                                    "\u2022\u2022\u2022\u2022 ${AppText.currency(state.currency)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp,
                                color = EmeraldGreenPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinancialOverviewMetricCard(
                            title = AppText.text(com.example.R.string.financial_overview_available_funds),
                            amount = availableFunds,
                            currency = state.currency,
                            isBalanceVisible = isBalanceVisible,
                            icon = Icons.Default.AccountBalanceWallet,
                            accent = EmeraldGreenPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        FinancialOverviewMetricCard(
                            title = AppText.text(com.example.R.string.financial_overview_gold_value),
                            amount = breakdown.goldEstimatedValue,
                            currency = state.currency,
                            isBalanceVisible = isBalanceVisible,
                            icon = Icons.Default.MonetizationOn,
                            accent = GoldAccent,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinancialOverviewMetricCard(
                            title = AppText.text(com.example.R.string.financial_overview_receivables),
                            amount = breakdown.moneyOwedToMe,
                            currency = state.currency,
                            isBalanceVisible = isBalanceVisible,
                            icon = Icons.Default.TrendingUp,
                            accent = Color(0xFF2563EB),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        FinancialOverviewMetricCard(
                            title = AppText.text(com.example.R.string.financial_overview_liabilities),
                            amount = breakdown.totalLiabilities,
                            currency = state.currency,
                            isBalanceVisible = isBalanceVisible,
                            icon = Icons.Default.ReceiptLong,
                            accent = ExpenseRed,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        // 2.3. SMART LOCAL FINANCIAL INSIGHTS (الرؤى والتحليلات الذكية المحلية)
        if (insights.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00897B).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = AppText.text(com.example.R.string.text_24d53def2103),
                                        tint = Color(0xFF00897B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = AppText.text(com.example.R.string.text_d383c633feff),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = AppText.text(com.example.R.string.text_0d4fdbffe901),
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = AppText.text(com.example.R.string.text_6e8c46e9d70b, insights.size),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreenPrimary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAllInsightsDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Display top 2 insights
                        insights.take(2).forEach { insight ->
                            val tint = when (insight.priority) {
                                InsightPriority.CRITICAL -> Color(0xFFEF4444)
                                InsightPriority.WARNING -> Color(0xFFFF9800)
                                InsightPriority.POSITIVE -> Color(0xFF22C55E)
                                InsightPriority.INFO -> EmeraldGreenPrimary
                            }
                            val bg = tint.copy(alpha = 0.1f)
                            val icon = when (insight.priority) {
                                InsightPriority.CRITICAL, InsightPriority.WARNING -> Icons.Default.Warning
                                InsightPriority.POSITIVE -> Icons.Default.CheckCircle
                                InsightPriority.INFO -> Icons.Default.Info
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(bg, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(insight.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = tint)
                                    Text(insight.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
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
                        .clickable { onNavigateTab(3) },
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
                            modifier = Modifier.weight(1f),
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
                                    contentDescription = AppText.text(com.example.R.string.text_76258e00f832),
                                    tint = Color(0xFFB8860B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_74885d00713f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = AppText.text(com.example.R.string.text_5814373b6d2e),
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = AppText.text(com.example.R.string.text_f7d9ae166049),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary,
                            maxLines = 1,
                            softWrap = false
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_f3ac2e6da42a),
                                    fontSize = 11.5.sp,
                                    color = EmeraldGreenDark,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBalanceVisible)
                                        "${String.format(Locale.US, "%,.0f", state.totalSavings)} ${AppText.currency(state.currency)}"
                                    else
                                        "\u2022\u2022\u2022\u2022 ${AppText.currency(state.currency)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldGreenDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldGreenPrimary)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_a35683da616b),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 Mini stats row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gold Value
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_a468321f66f1),
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBalanceVisible) "${String.format(Locale.US, "%,.0f", state.totalGoldValue)}" else "\u2022\u2022\u2022\u2022",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Cash Savings
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_c4321b7e4fed),
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBalanceVisible) "${String.format(Locale.US, "%,.0f", state.totalCashSavings)}" else "\u2022\u2022\u2022\u2022",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Gold Weight
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_35dc88643736),
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", state.totalGoldWeightGrams)}g",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Pieces Count
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_23e643d03dde),
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = AppText.text(com.example.R.string.text_fd835aa43312, state.totalGoldPiecesCount),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                contentDescription = AppText.text(com.example.R.string.text_7d83a31000b2),
                                tint = EmeraldGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = appStrings.smartAnalytics,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
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
                            text = AppText.text(com.example.R.string.text_28dae43e8dcf, String.format(Locale.US, "%,.0f", state.remainingSalary), AppText.currency(state.currency)),
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = LightBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = AppText.text(com.example.R.string.text_2bccc4bbf0d9),
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", state.totalExpenseLastMonth)} ${AppText.currency(state.currency)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = AppText.text(com.example.R.string.text_a39d9e5bcc98),
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = LightBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = AppText.text(com.example.R.string.text_4757b3b88509),
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", state.totalIncomeLastMonth)} ${AppText.currency(state.currency)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = AppText.text(com.example.R.string.text_a39d9e5bcc98),
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = LightBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = AppText.text(com.example.R.string.text_3431c146da7c),
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val dailyAvg = state.totalExpenseThisMonth / 30.0
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", dailyAvg)} ${AppText.currency(state.currency)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = LightBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = AppText.text(com.example.R.string.text_9aa255f3885d),
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val safeDaily = (state.remainingSalary / 23.0).coerceAtLeast(0.0)
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", safeDaily)} ${AppText.currency(state.currency)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onOpenAddIncome,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = AppText.text(com.example.R.string.text_f9aaa00729de),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = appStrings.addIncome,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onOpenAddExpense,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
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
                            Text(
                                text = appStrings.addExpense,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EmeraldGreenPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.salarySpentPercentage.toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldGreenDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AppText.text(com.example.R.string.text_150293b7792d),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_be1e41cbff16, String.format(Locale.US, "%,.0f", state.remainingSalary), AppText.currency(state.currency)),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreenDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AppText.text(com.example.R.string.text_501aabb98ee5, String.format(Locale.US, "%,.0f", state.totalExpenseThisMonth), AppText.currency(state.currency)),
                                    fontSize = 12.5.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
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
                                    contentDescription = AppText.text(com.example.R.string.text_2e3575a463d9),
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_4f7580d78f5f),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreenDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = AppText.text(com.example.R.string.text_d7e36702a621, state.salarySpentPercentage.toInt()),
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MintBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = AppText.text(com.example.R.string.text_ba828efad7d2),
                                    tint = EmeraldGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = AppText.text(com.example.R.string.text_d6ed50d80f68),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${badgesList.count { it.isUnlocked }} / 6",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            softWrap = false
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
                        text = AppText.text(com.example.R.string.text_f8518fa27a4a),
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
                        text = AppText.text(com.example.R.string.text_e50acc511afa),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = AppText.text(com.example.R.string.text_d36294e5abb3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenPrimary,
                        maxLines = 1,
                        softWrap = false,
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
                                text = AppText.text(com.example.R.string.text_c302118eabab),
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
                        .clickable { onOpenCommitments() },
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
                            text = AppText.text(com.example.R.string.text_cecfe96db0ad),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Left Shortcut Card: أين ذهب راتبي؟
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
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppText.text(com.example.R.string.text_2dffd674f9b7),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAllInsightsDialog) {
        AllInsightsDialog(
            insights = insights,
            onDismiss = { showAllInsightsDialog = false }
        )
    }
}

@Composable
private fun QuickShortcutButton(
    title: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FinancialOverviewMetricCard(
    title: String,
    amount: Double,
    currency: String,
    isBalanceVisible: Boolean,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.07f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isBalanceVisible)
                    "${String.format(Locale.US, "%,d", amount.toLong().coerceAtLeast(0L))} ${AppText.currency(currency)}"
                else
                    "\u2022\u2022\u2022\u2022 ${AppText.currency(currency)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = badge.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = badge.subtitle,
                fontSize = 10.sp,
                color = Color.Gray,
                lineHeight = 14.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
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
    val sdf = remember(AppText.language) { SimpleDateFormat("dd MMM, hh:mm a", AppText.locale) }
    val formattedDate = remember(AppText.language, tx.dateMillis) { sdf.format(Date(tx.dateMillis)) }

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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = AppStrings(AppText.language).translateCategory(tx.category),
                        fontSize = 11.sp,
                        color = iconColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = " \u2022 ", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = tx.vaultName,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(text = " \u2022 ", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${if (isIncome) "+" else "-"}${String.format(Locale.US, "%,.0f", tx.amount)} ${AppText.currency(currency)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = if (isIncome) IncomeGreen else ExpenseRed,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun VaultEnvelopesSelector(
    vaults: List<VaultEntity>,
    selectedVaultName: String,
    onSelectVault: (String) -> Unit,
    onOpenAddVault: () -> Unit,
    currency: String,
    isBalanceVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreenPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = EmeraldGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = AppText.text(com.example.R.string.label_dashboard_vault_envelopes_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AppText.text(com.example.R.string.label_dashboard_new_vault_action),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreenPrimary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenAddVault() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        if (vaults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(vaults) { vault ->
                    val isSelected = vault.name == selectedVaultName
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { onSelectVault(vault.name) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MintBackground else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) EmeraldGreenPrimary else CardBorderColor
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = vault.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = if (isSelected) EmeraldGreenDark else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldGreenPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isBalanceVisible)
                                    "${String.format(Locale.US, "%,.0f", vault.balance)} ${AppText.currency(currency)}"
                                else
                                    "\u2022\u2022\u2022\u2022 ${AppText.currency(currency)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) EmeraldGreenDark else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HouseholdBudgetHealthCard(
    remainingSalary: Double,
    totalIncome: Double,
    totalExpense: Double,
    currency: String,
    isBalanceVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val remainingDays = (maxDays - currentDay + 1).coerceAtLeast(1)

    val safeDailyLimit = (remainingSalary / remainingDays).coerceAtLeast(0.0)
    val spentPercentage = if (totalIncome > 0) (totalExpense / totalIncome) * 100.0 else if (totalExpense > 0) 100.0 else 0.0

    val statusColor = when {
        spentPercentage < 60.0 -> Color(0xFF10B981)
        spentPercentage <= 85.0 -> Color(0xFF0284C7)
        spentPercentage <= 100.0 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val statusBadgeText = when {
        spentPercentage < 60.0 -> AppText.text(com.example.R.string.label_dashboard_budget_status_safe)
        spentPercentage <= 85.0 -> AppText.text(com.example.R.string.label_dashboard_budget_status_balanced)
        spentPercentage <= 100.0 -> AppText.text(com.example.R.string.label_dashboard_budget_status_near_limit)
        else -> AppText.text(com.example.R.string.label_dashboard_budget_status_over_budget)
    }

    val adviceText = when {
        spentPercentage < 60.0 -> AppText.text(com.example.R.string.label_dashboard_budget_advice_safe)
        spentPercentage <= 85.0 -> AppText.text(com.example.R.string.label_dashboard_budget_advice_balanced)
        spentPercentage <= 100.0 -> AppText.text(com.example.R.string.label_dashboard_budget_advice_near_limit)
        else -> AppText.text(com.example.R.string.label_dashboard_budget_advice_over_budget)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppText.text(com.example.R.string.label_dashboard_budget_health_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = AppText.text(com.example.R.string.label_dashboard_budget_health_subtitle),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = statusBadgeText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppText.text(com.example.R.string.label_dashboard_income_spent_ratio),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${spentPercentage.toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                LinearProgressIndicator(
                    progress = { (spentPercentage / 100.0).coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = adviceText,
                    fontSize = 11.5.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LightBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = AppText.text(com.example.R.string.label_dashboard_remaining_this_month),
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBalanceVisible)
                                "${String.format(Locale.US, "%,.0f", remainingSalary)} ${AppText.currency(currency)}"
                            else "\u2022\u2022\u2022\u2022",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (remainingSalary >= 0) EmeraldGreenDark else ExpenseRed
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LightBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = AppText.text(com.example.R.string.label_dashboard_daily_spend_limit),
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBalanceVisible)
                                "${String.format(Locale.US, "%,.0f", safeDailyLimit)} ${AppText.currency(currency)}"
                            else "\u2022\u2022\u2022\u2022",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = statusColor
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LightBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = AppText.text(com.example.R.string.label_dashboard_days_remaining),
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AppText.text(com.example.R.string.label_dashboard_days_count, remainingDays),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UrgentFamilyCommitmentsCard(
    commitments: List<CommitmentEntity>,
    lessons: List<ChildLessonEntity>,
    currency: String,
    onPayCommitment: (CommitmentEntity) -> Unit,
    onPayLesson: (ChildLessonEntity) -> Unit,
    onOpenCommitmentsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unpaidCommitments = remember(commitments) { commitments.filter { !it.isPaid } }
    val unpaidLessons = remember(lessons) { lessons.filter { !it.isPaid } }
    val totalUnpaid = remember(unpaidCommitments, unpaidLessons) {
        unpaidCommitments.sumOf { it.amount } + unpaidLessons.sumOf { it.amount }
    }
    val dateFormat = remember(AppText.language) { SimpleDateFormat("dd MMM", AppText.locale) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (totalUnpaid > 0) Color(0xFFEF4444).copy(alpha = 0.12f) else EmeraldGreenPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (totalUnpaid > 0) Icons.Default.Alarm else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (totalUnpaid > 0) Color(0xFFEF4444) else EmeraldGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppText.text(com.example.R.string.label_dashboard_urgent_commitments_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (totalUnpaid > 0)
                                AppText.text(
                                    com.example.R.string.label_dashboard_due_amount,
                                    String.format(Locale.US, "%,.0f", totalUnpaid),
                                    AppText.currency(currency)
                                )
                            else AppText.text(com.example.R.string.label_dashboard_all_commitments_paid),
                            fontSize = 11.5.sp,
                            color = if (totalUnpaid > 0) ExpenseRed else EmeraldGreenPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = AppText.text(com.example.R.string.text_d36294e5abb3),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreenPrimary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenCommitmentsTab() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (unpaidCommitments.isEmpty() && unpaidLessons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MintBackground)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppText.text(com.example.R.string.label_dashboard_no_urgent_commitments),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = EmeraldGreenDark,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unpaidCommitments.take(2).forEach { item ->
                        val dateStr = remember(item.dueDateMillis, AppText.language) { dateFormat.format(Date(item.dueDateMillis)) }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", item.amount)} ${AppText.currency(currency)} \u2022 $dateStr",
                                        fontSize = 11.sp,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onPayCommitment(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = AppText.text(com.example.R.string.label_dashboard_pay_action),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    unpaidLessons.take(2).forEach { lesson ->
                        val dateStr = remember(lesson.dueDateMillis, AppText.language) { dateFormat.format(Date(lesson.dueDateMillis)) }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${lesson.subject} (${lesson.childName})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", lesson.amount)} ${AppText.currency(currency)} \u2022 $dateStr",
                                        fontSize = 11.sp,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onPayLesson(lesson) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = AppText.text(com.example.R.string.label_dashboard_pay_action),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
