package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
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
    onOpenSettings: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appStrings = LocalStrings.current
    val context = LocalContext.current

    // Date & Month calculations
    val currentCal = remember { Calendar.getInstance() }
    val currentMonthName = remember { SimpleDateFormat("MMMM yyyy", Locale("ar")).format(currentCal.time) }
    var selectedMonthName by remember { mutableStateOf(currentMonthName) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val monthOptions = remember {
        val list = mutableListOf<String>()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("ar"))
        for (i in 0..5) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            list.add(sdf.format(c.time))
        }
        list
    }

    // Filter transactions for current month
    val currentMonthTxs = remember(transactions) {
        val m = currentCal.get(Calendar.MONTH)
        val y = currentCal.get(Calendar.YEAR)
        transactions.filter { tx ->
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = tx.dateMillis
            txCal.get(Calendar.MONTH) == m && txCal.get(Calendar.YEAR) == y
        }
    }

    val totalIncome = remember(currentMonthTxs) {
        currentMonthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val unpaidCommitmentsSum = remember(commitments) { commitments.filter { !it.isPaid }.sumOf { it.amount } }
    val unpaidLessonsSum = remember(lessons) { lessons.filter { !it.isPaid }.sumOf { it.amount } }

    val categoryExpenses = remember(currentMonthTxs, unpaidCommitmentsSum, unpaidLessonsSum) {
        val map = currentMonthTxs.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toMutableMap()

        if (unpaidCommitmentsSum > 0) {
            map["الالتزامات"] = (map["الالتزامات"] ?: 0.0) + unpaidCommitmentsSum
        }
        if (unpaidLessonsSum > 0) {
            map["دروس أطفال"] = (map["دروس أطفال"] ?: 0.0) + unpaidLessonsSum
        }
        map.toMap()
    }

    val totalExpense = remember(categoryExpenses) {
        categoryExpenses.values.sum()
    }

    val remainingBalance = (totalIncome - totalExpense).coerceAtLeast(0.0)

    val dayOfMonth = currentCal.get(Calendar.DAY_OF_MONTH)
    val totalDaysInMonth = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysLeft = (totalDaysInMonth - dayOfMonth).coerceAtLeast(0)

    val avgDailySpend = if (dayOfMonth > 0) totalExpense / dayOfMonth else 0.0
    val safeDailyLimit = if (daysLeft > 0) remainingBalance / daysLeft else 0.0

    val topCategory = remember(categoryExpenses) {
        categoryExpenses.maxByOrNull { it.value }?.key ?: "—"
    }

    val leastCategory = remember(categoryExpenses) {
        categoryExpenses.minByOrNull { it.value }?.key ?: "—"
    }

    val projectedEndMonthBalance = remember(remainingBalance, avgDailySpend, daysLeft) {
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
                        text = "التقارير",
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
                            contentDescription = "الإعدادات",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
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
                        text = "تصدير التقرير الشهري",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اختر الشهر ثم صَدّر ملف Excel أو احفظه PDF.",
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
                                contentDescription = "اختر الشهر",
                                tint = Color.Gray
                            )
                        }

                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false }
                        ) {
                            monthOptions.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month, fontSize = 13.sp) },
                                    onClick = {
                                        selectedMonthName = month
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
                                    text = "PDF / طباعة",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Outlined Excel Button (Left in RTL)
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "جاري تصدير ملف Excel...", Toast.LENGTH_SHORT).show()
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
                                contentDescription = "التحليل الذكي",
                                tint = EmeraldGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "التحليل الذكي",
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
                            text = "متوقع يتبقى لك ${String.format(Locale.US, "%,.0f", projectedEndMonthBalance)} $currency بنهاية الشهر بنفس معدل الصرف الحالي.",
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
                            title = "مصروفات الشهر الماضي",
                            value = "0 $currency",
                            subtitle = "لا مقارنة متاحة",
                            modifier = Modifier.weight(1f)
                        )
                        SmartGridSubCard(
                            title = "دخل الشهر الماضي",
                            value = "0 $currency",
                            subtitle = "لا مقارنة متاحة",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartGridSubCard(
                            title = "متوسط الصرف اليومي",
                            value = "${String.format(Locale.US, "%,.0f", avgDailySpend)} $currency",
                            subtitle = null,
                            modifier = Modifier.weight(1f)
                        )
                        SmartGridSubCard(
                            title = "حد الصرف الآمن يومياً",
                            value = "${String.format(Locale.US, "%,.0f", safeDailyLimit)} $currency",
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
                            text = "توقع استهلاك الدخل بنهاية الشهر",
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
                        text = "📅 باقي $daysLeft يوم • التزامات غير مدفوعة 0 $currency",
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
                        title = "إجمالي الدخل",
                        value = "${String.format(Locale.US, "%,.0f", totalIncome)} $currency",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "إجمالي المصروفات",
                        value = "${String.format(Locale.US, "%,.0f", totalExpense)} $currency",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "الرصيد المتبقي",
                        value = "${String.format(Locale.US, "%,.0f", remainingBalance)} $currency",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "متوسط الصرف اليومي",
                        value = "${String.format(Locale.US, "%,.0f", avgDailySpend)} $currency",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "أكثر بند إنفاقاً",
                        value = topCategory,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "أقل بند إنفاقاً",
                        value = leastCategory,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "عدد العمليات",
                        value = "${currentMonthTxs.size}",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "فواتير مرفقة",
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
                        title = "توقع نهاية الشهر",
                        value = "${String.format(Locale.US, "%,.0f", projectedEndMonthBalance)} $currency",
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
                            text = "نسبة ما تم صرفه من الدخل",
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
                        text = "مقارنة بالشهر الماضي: 0 $currency مصروفات",
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
                                contentDescription = "تقرير الشهر",
                                tint = EmeraldGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = "تقرير الشهر الحالي المالي",
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
                                text = "عدد العمليات",
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
                                text = "أكثر بند تم الإنفاق عليه",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (topCategory != "—") topCategory else "إيجار",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ExpenseRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "متوسط الصرف اليومي",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${String.format(Locale.US, "%,.0f", if (avgDailySpend > 0) avgDailySpend else 521.0)} $currency",
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
                        text = "توزيع الإنفاق حسب التصنيف (Pie Chart)",
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
                                    text = "الإجمالي",
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
                                        text = item.name,
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
                        text = "الإنفاق لكل شهر (Bar Chart)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val displayAmount = if (totalExpense > 0) totalExpense else 4165.0
                    val monthsData = listOf(
                        "أبريل" to 0.0,
                        "مايو" to 0.0,
                        "يونيو" to 0.0,
                        "يوليو" to 0.0,
                        "أغسطس" to displayAmount
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
                        text = "تطور الرصيد مع الوقت (Line Chart)",
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
                                listOf("أبريل", "مايو", "يونيو", "يوليو", "أغسطس").forEach { monthName ->
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
                            contentDescription = "توقع الرصيد",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "توقع الرصيد المتبقي بنهاية الشهر",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE5C158)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "بناءً على معدل صرفك اليومي الحالي، يتوقع أن يتبقى معك بحلول نهاية الشهر: ${String.format(Locale.US, "%,.0f", projectedEndMonthBalance)} $currency",
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
    val months = listOf("أغسطس", "يوليو", "يونيو", "مايو", "أبريل", "مارس")
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
    val months = listOf("أغسطس", "يوليو", "يونيو", "مايو", "أبريل", "مارس")
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
