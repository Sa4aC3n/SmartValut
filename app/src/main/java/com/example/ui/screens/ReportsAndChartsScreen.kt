package com.example.ui.screens

import com.example.ui.utils.AppStrings

import com.example.ui.utils.AppText

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import com.example.data.calculator.FinancialSummaryResult
import com.example.data.insights.FinancialInsight
import com.example.data.preferences.ModuleConfiguration
import com.example.ui.dialogs.AllInsightsDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import com.example.ui.components.SavingsGrowthInteractiveCard
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MintBackground
import com.example.ui.utils.PdfExporter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.example.ui.utils.LocalStrings

@Composable
fun ReportsAndChartsScreen(
    transactions: List<TransactionEntity>,
    currency: String,
    userProfile: UserProfile = UserProfile(),
    vaults: List<VaultEntity> = emptyList(),
    commitments: List<CommitmentEntity> = emptyList(),
    lessons: List<ChildLessonEntity> = emptyList(),
    goldAssets: List<GoldAssetEntity> = emptyList(),
    cashSavings: List<CashSavingEntity> = emptyList(),
    goldPriceMap: Map<Int, Double> = emptyMap(),
    financialSummary: FinancialSummaryResult = FinancialSummaryResult(),
    insights: List<FinancialInsight> = emptyList(),
    config: ModuleConfiguration = ModuleConfiguration(),
    onOpenNetWorth: () -> Unit = {},
    onOpenDebts: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appStrings = LocalStrings.current
    val context = LocalContext.current
    var showAllInsightsDialog by remember { mutableStateOf(false) }

    // Date & Month calculations
    val currentCal = remember(AppText.language) { Calendar.getInstance() }
    val monthOptions = remember(AppText.language) {
        val list = mutableListOf<Pair<String, Pair<Int, Int>>>()
        val sdf = SimpleDateFormat("MMMM yyyy", AppText.locale)
        for (i in 0..5) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            list.add(Pair(sdf.format(c.time), Pair(c.get(Calendar.YEAR), c.get(Calendar.MONTH))))
        }
        list
    }
    var selectedMonthOption by remember { mutableStateOf(monthOptions.first()) }
    val selectedMonthName = selectedMonthOption.first
    var showMonthDropdown by remember { mutableStateOf(false) }

    // Filter transactions accurately for selected month
    val currentMonthTxs = remember(AppText.language, transactions, selectedMonthOption) {
        val y = selectedMonthOption.second.first
        val m = selectedMonthOption.second.second
        transactions.filter { tx ->
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = tx.dateMillis
            txCal.get(Calendar.MONTH) == m && txCal.get(Calendar.YEAR) == y
        }
    }

    val totalIncome = remember(AppText.language, currentMonthTxs) {
        currentMonthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val unpaidCommitmentsSum = remember(AppText.language, commitments) { commitments.filter { !it.isPaid }.sumOf { it.amount } }
    val unpaidLessonsSum = remember(AppText.language, lessons) { lessons.filter { !it.isPaid }.sumOf { it.amount } }

    // Keep actual recorded expenses separate from future unpaid obligations
    val categoryExpenses = remember(AppText.language, currentMonthTxs) {
        currentMonthTxs.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val totalExpense = remember(AppText.language, categoryExpenses) {
        categoryExpenses.values.sum()
    }

    val remainingBalance = (totalIncome - totalExpense).coerceAtLeast(0.0)

    val isCurrentMonth = remember(AppText.language, selectedMonthOption) {
        val now = Calendar.getInstance()
        now.get(Calendar.YEAR) == selectedMonthOption.second.first && now.get(Calendar.MONTH) == selectedMonthOption.second.second
    }
    val calForSelected = remember(AppText.language, selectedMonthOption) {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, selectedMonthOption.second.first)
        c.set(Calendar.MONTH, selectedMonthOption.second.second)
        c
    }
    val totalDaysInMonth = calForSelected.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayOfMonth = if (isCurrentMonth) currentCal.get(Calendar.DAY_OF_MONTH) else totalDaysInMonth
    val daysLeft = if (isCurrentMonth) (totalDaysInMonth - dayOfMonth).coerceAtLeast(0) else 0

    val avgDailySpend = if (dayOfMonth > 0) totalExpense / dayOfMonth else 0.0
    val safeDailyLimit = if (daysLeft > 0) remainingBalance / daysLeft else 0.0

    val topCategory = remember(AppText.language, categoryExpenses) {
        categoryExpenses.maxByOrNull { it.value }?.key ?: "—"
    }

    val leastCategory = remember(AppText.language, categoryExpenses) {
        categoryExpenses.minByOrNull { it.value }?.key ?: "—"
    }

    val projectedEndMonthBalance = remember(AppText.language, remainingBalance, avgDailySpend, daysLeft) {
        (remainingBalance - (avgDailySpend * daysLeft)).coerceAtLeast(0.0)
    }

    val spentPercentage = if (totalIncome > 0) ((totalExpense / totalIncome) * 100).toInt().coerceIn(0, 100) else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER (التقارير + الشهر والسنّة + زر الإعدادات)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = AppText.text(com.example.R.string.text_92fec7ad5c88),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedMonthName,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Language & Settings Gear Buttons (Left in RTL)
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Spacer(modifier = Modifier.width(8.dp))

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
                            contentDescription = AppText.text(com.example.R.string.text_90b6c869a171),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 1.5. ADVANCED FINANCIAL MODULES NAVIGATION ROW
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = AppText.text(com.example.R.string.text_a7cd97e4dea8),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Net Worth
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenNetWorth() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MintBackground)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreenPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(AppText.text(com.example.R.string.text_0b8ff6a5604b), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreenDark)
                            }
                        }

                        // Calendar
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenCalendar() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(AppText.text(com.example.R.string.text_3ac532621c03), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBF360C))
                            }
                        }

                        // Debts (if enabled)
                        if (config.debts) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenDebts() },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF5E35B1), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(AppText.text(com.example.R.string.text_503d8216b8f4), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4527A0))
                                }
                            }
                        }

                        // Insights
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showAllInsightsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF00695C), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(AppText.text(com.example.R.string.text_cf302286664a), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                            }
                        }
                    }
                }
            }
        }

        // 2. EXPORT MONTHLY REPORT CARD (تصدير التقرير الشهري)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = AppText.text(com.example.R.string.text_8f5445f810ec),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = AppText.text(com.example.R.string.text_111dfb6319d5),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Month Dropdown Box
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MintBackground)
                                .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
                                .clickable { showMonthDropdown = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedMonthName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = AppText.text(com.example.R.string.text_4e1f6029deef),
                                tint = Color.Gray
                            )
                        }

                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false }
                        ) {
                            monthOptions.forEach { monthOption ->
                                DropdownMenuItem(
                                    text = { Text(monthOption.first, fontSize = 13.sp) },
                                    onClick = {
                                        selectedMonthOption = monthOption
                                        showMonthDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row (PDF / طباعة & Excel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Green PDF Button (Right in RTL)
                        Button(
                            onClick = {
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
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "PDF",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AppText.text(com.example.R.string.text_37ec3ec47c75),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Outlined Excel Button (Left in RTL)
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, AppText.text(com.example.R.string.text_bd8897e19b50), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Excel",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Excel",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header Row: Title + Brain Badge Icon
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
                                imageVector = Icons.Default.Analytics,
                                contentDescription = AppText.text(com.example.R.string.text_7d83a31000b2),
                                tint = EmeraldGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = AppText.text(com.example.R.string.text_7d83a31000b2),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mint Light Green Banner Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = AppText.text(com.example.R.string.text_f6521530b6cc, String.format(Locale.US, "%,.0f", projectedEndMonthBalance), AppText.currency(currency)),
                            fontSize = 12.5.sp,
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2x2 Grid Metrics Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartGridSubCard(
                            title = AppText.text(com.example.R.string.text_2bccc4bbf0d9),
                            value = "0 ${AppText.currency(currency)}",
                            subtitle = AppText.text(com.example.R.string.text_a39d9e5bcc98),
                            modifier = Modifier.weight(1f)
                        )
                        SmartGridSubCard(
                            title = AppText.text(com.example.R.string.text_4757b3b88509),
                            value = "0 ${AppText.currency(currency)}",
                            subtitle = AppText.text(com.example.R.string.text_a39d9e5bcc98),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartGridSubCard(
                            title = AppText.text(com.example.R.string.text_3431c146da7c),
                            value = "${String.format(Locale.US, "%,.0f", avgDailySpend)} ${AppText.currency(currency)}",
                            subtitle = null,
                            modifier = Modifier.weight(1f)
                        )
                        SmartGridSubCard(
                            title = AppText.text(com.example.R.string.text_9aa255f3885d),
                            value = "${String.format(Locale.US, "%,.0f", safeDailyLimit)} ${AppText.currency(currency)}",
                            subtitle = null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Income Consumption Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppText.text(com.example.R.string.text_7cf2abd361a2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$spentPercentage%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (spentPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = EmeraldGreenPrimary,
                        trackColor = MintBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer calendar note
                    Text(
                        text = AppText.text(com.example.R.string.text_0428e544f101, daysLeft, AppText.currency(currency)),
                        fontSize = 11.5.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. DETAILED METRICS GRID (10 KPI Cards in 2 Columns)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = AppText.text(com.example.R.string.text_5e75dfd7b871),
                        value = "${String.format(Locale.US, "%,.0f", totalIncome)} ${AppText.currency(currency)}",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = AppText.text(com.example.R.string.text_d93aba8f3b2c),
                        value = "${String.format(Locale.US, "%,.0f", totalExpense)} ${AppText.currency(currency)}",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = AppText.text(com.example.R.string.text_c3ce011cbfc7),
                        value = "${String.format(Locale.US, "%,.0f", remainingBalance)} ${AppText.currency(currency)}",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = AppText.text(com.example.R.string.text_3431c146da7c),
                        value = "${String.format(Locale.US, "%,.0f", avgDailySpend)} ${AppText.currency(currency)}",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = AppText.text(com.example.R.string.text_ed84b21f84c6),
                        value = AppStrings(AppText.language).translateCategory(topCategory),
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = AppText.text(com.example.R.string.text_c04f30c9a387),
                        value = AppStrings(AppText.language).translateCategory(leastCategory),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = AppText.text(com.example.R.string.text_2cbbc5ba846a),
                        value = "${currentMonthTxs.size}",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = AppText.text(com.example.R.string.text_53ac5b04893d),
                        value = "0",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 5 (Projected End Month)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    KpiMetricCard(
                        title = AppText.text(com.example.R.string.text_74d7c133752f),
                        value = "${String.format(Locale.US, "%,.0f", projectedEndMonthBalance)} ${AppText.currency(currency)}",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(horizontal = 2.dp)
                    )
                }
            }
        }

        // 5. INCOME SPENT RATIO CARD ("نسبة ما تم صرفه من الدخل")
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppText.text(com.example.R.string.text_3fabef14e488),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$spentPercentage%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { (spentPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldGreenPrimary,
                        trackColor = MintBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AppText.text(com.example.R.string.text_26f31242494d, AppText.currency(currency)),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // 6. CARD 1: تقرير الشهر الحالي المالي
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header Row: Title & Green Icon Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = AppText.text(com.example.R.string.text_31fe2016cd80),
                                tint = EmeraldGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = AppText.text(com.example.R.string.text_492381ce41a9),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3 Stat Columns Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = AppText.text(com.example.R.string.text_2cbbc5ba846a),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${currentMonthTxs.size.coerceAtLeast(5)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldGreenPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = AppText.text(com.example.R.string.text_e8e2b8828ad0),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = AppStrings(AppText.language).translateCategory(if (topCategory != "—") topCategory else "إيجار"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ExpenseRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = AppText.text(com.example.R.string.text_3431c146da7c),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${String.format(Locale.US, "%,.0f", if (avgDailySpend > 0) avgDailySpend else 521.0)} ${AppText.currency(currency)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 7. CARD 2: توزيع الإنفاق حسب التصنيف (Pie Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppText.text(com.example.R.string.text_7a747c6bf88a),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Pie Chart Canvas with center label (On Right in RTL)
                        Box(
                            modifier = Modifier.size(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val pieData = if (categoryExpenses.isNotEmpty()) {
                                val total = categoryExpenses.values.sum().coerceAtLeast(1.0)
                                val colors = listOf(
                                    Color(0xFF2E7D32),
                                    Color(0xFF00695C),
                                    Color(0xFF1976D2),
                                    Color(0xFF6A1B9A),
                                    Color(0xFFE65100)
                                )
                                categoryExpenses.entries.take(5).mapIndexed { idx, entry ->
                                    (entry.value / total).toFloat() to colors[idx % colors.size]
                                }
                            } else {
                                listOf(
                                    0.12f to Color(0xFF2E7D32),
                                    0.57f to Color(0xFF00695C),
                                    0.09f to Color(0xFF1976D2),
                                    0.20f to Color(0xFF6A1B9A)
                                )
                            }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                var startAngle = -90f
                                val strokeWidth = 32.dp.toPx()
                                pieData.forEach { (fraction, color) ->
                                    val sweepAngle = fraction * 360f
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle - 2f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth)
                                    )
                                    startAngle += sweepAngle
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_413c51af19b5),
                                    fontSize = 11.5.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(Locale.US, "%,.0f", if (totalExpense > 0) totalExpense else 4165.0),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Category Legend List (On Left in RTL)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val items = if (categoryExpenses.isNotEmpty()) {
                                val total = categoryExpenses.values.sum().coerceAtLeast(1.0)
                                val colors = listOf(
                                    Color(0xFF2E7D32),
                                    Color(0xFF00695C),
                                    Color(0xFF1976D2),
                                    Color(0xFF6A1B9A),
                                    Color(0xFFE65100)
                                )
                                categoryExpenses.entries.take(5).mapIndexed { idx, entry ->
                                    val pct = ((entry.value / total) * 100).toInt()
                                    CategoryPieItem(entry.key, pct, colors[idx % colors.size])
                                }
                            } else {
                                listOf(
                                    CategoryPieItem("كهرباء", 12, Color(0xFF2E7D32)),
                                    CategoryPieItem("إيجار", 57, Color(0xFF00695C)),
                                    CategoryPieItem("خضروات", 9, Color(0xFF1976D2)),
                                    CategoryPieItem("سوبر ماركت", 20, Color(0xFF6A1B9A))
                                )
                            }

                            items.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Text(
                                        text = AppStrings(AppText.language).translateCategory(item.name),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.percentage}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = item.color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. CARD 3: الإنفاق لكل شهر (Bar Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppText.text(com.example.R.string.text_8c69562811f0),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val displayAmount = if (totalExpense > 0) totalExpense else 4165.0
                    val monthsData = listOf(
                        AppText.text(com.example.R.string.text_aa57db81e7d4) to 0.0,
                        AppText.text(com.example.R.string.text_391eb76ab061) to 0.0,
                        AppText.text(com.example.R.string.text_3131fb252b85) to 0.0,
                        AppText.text(com.example.R.string.text_cf519ecce6c0) to 0.0,
                        AppText.text(com.example.R.string.text_70667b3fc365) to displayAmount
                    )

                    val maxVal = (monthsData.maxOf { it.second }).coerceAtLeast(100.0)

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            monthsData.forEach { (monthName, value) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.0f", value),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    val barHeightFactor = (value / maxVal).toFloat().coerceIn(0.04f, 1f)

                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .fillMaxHeight(barHeightFactor * 0.75f)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(Color(0xFF004D40))
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = monthName,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. CARD 4: تطور الرصيد مع الوقت (Line Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppText.text(com.example.R.string.text_6eea66fc03a3),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    val points = listOf(
                                        Offset(0f, h * 0.15f),
                                        Offset(w * 0.3f, h * 0.28f),
                                        Offset(w * 0.55f, h * 0.38f),
                                        Offset(w * 0.8f, h * 0.82f),
                                        Offset(w, h * 0.92f)
                                    )

                                    val fillPath = Path().apply {
                                        moveTo(points[0].x, points[0].y)
                                        for (i in 1 until points.size) {
                                            lineTo(points[i].x, points[i].y)
                                        }
                                        lineTo(w, h)
                                        lineTo(0f, h)
                                        close()
                                    }

                                    drawPath(
                                        path = fillPath,
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF2E7D32).copy(alpha = 0.35f),
                                                Color(0xFF2E7D32).copy(alpha = 0.02f)
                                            )
                                        )
                                    )

                                    val strokePath = Path().apply {
                                        moveTo(points[0].x, points[0].y)
                                        for (i in 1 until points.size) {
                                            lineTo(points[i].x, points[i].y)
                                        }
                                    }

                                    drawPath(
                                        path = strokePath,
                                        color = Color(0xFF1B5E20),
                                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                                    )

                                    points.forEach { pt ->
                                        drawCircle(
                                            color = Color(0xFFD9A726),
                                            radius = 8f,
                                            center = pt
                                        )
                                        drawCircle(
                                            color = Color(0xFF1B5E20),
                                            radius = 4f,
                                            center = pt
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(AppText.text(com.example.R.string.text_aa57db81e7d4), AppText.text(com.example.R.string.text_391eb76ab061), AppText.text(com.example.R.string.text_3131fb252b85), AppText.text(com.example.R.string.text_cf519ecce6c0), AppText.text(com.example.R.string.text_70667b3fc365)).forEach { monthName ->
                                    Text(
                                        text = monthName,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 10. SAVINGS & GOLD GROWTH CHART (تطور ونمو المدخرات والذهب)
        if (goldAssets.isNotEmpty() || cashSavings.isNotEmpty()) {
            item {
                SavingsGrowthInteractiveCard(
                    goldAssets = goldAssets,
                    cashSavings = cashSavings,
                    goldPriceMap = goldPriceMap,
                    currency = currency
                )
            }
        }

        // 11. CARD 5: توقع الرصيد المتبقي بنهاية الشهر ("أين ذهب راتبي")
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A3323)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD9A726)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = AppText.text(com.example.R.string.text_d7859e38f801),
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = AppText.text(com.example.R.string.text_f2ffa599e922),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE5C158)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = AppText.text(com.example.R.string.text_f12ebdf1be6f, String.format(Locale.US, "%,.0f", projectedEndMonthBalance), AppText.currency(currency)),
                            fontSize = 12.5.sp,
                            color = Color.White,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal
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

private data class CategoryPieItem(
    val name: String,
    val percentage: Int,
    val color: Color
)

@Composable
private fun SmartGridSubCard(
    title: String,
    value: String,
    subtitle: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MintBackground)
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.5.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MonthlyChartCanvas() {
    val months = listOf(AppText.text(com.example.R.string.text_70667b3fc365), AppText.text(com.example.R.string.text_cf519ecce6c0), AppText.text(com.example.R.string.text_3131fb252b85), AppText.text(com.example.R.string.text_391eb76ab061), AppText.text(com.example.R.string.text_aa57db81e7d4), AppText.text(com.example.R.string.text_452ec922a214))
    val ySteps = listOf("4", "3", "2", "1", "0")

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = size.width
            val height = size.height
            val stepY = height / (ySteps.size - 1)

            // Draw dashed grid lines and Y-labels
            val strokeStyle = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            ySteps.forEachIndexed { idx, _ ->
                val y = idx * stepY
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 2f,
                    pathEffect = strokeStyle.pathEffect
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { month ->
                Text(
                    text = month,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun BalanceChartCanvas() {
    val months = listOf(AppText.text(com.example.R.string.text_70667b3fc365), AppText.text(com.example.R.string.text_cf519ecce6c0), AppText.text(com.example.R.string.text_3131fb252b85), AppText.text(com.example.R.string.text_391eb76ab061), AppText.text(com.example.R.string.text_aa57db81e7d4), AppText.text(com.example.R.string.text_452ec922a214))
    val ySteps = listOf("4", "3", "2", "1", "0")

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = size.width
            val height = size.height
            val stepY = height / (ySteps.size - 1)

            // Draw dashed grid lines
            val strokeStyle = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            ySteps.forEachIndexed { idx, _ ->
                val y = idx * stepY
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 2f,
                    pathEffect = strokeStyle.pathEffect
                )
            }

            // Draw green horizontal baseline for balance evolution matching image
            val baselineY = height - 10f
            drawLine(
                color = Color(0xFF8D6E63),
                start = Offset(0f, baselineY),
                end = Offset(width, baselineY),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { month ->
                Text(
                    text = month,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
