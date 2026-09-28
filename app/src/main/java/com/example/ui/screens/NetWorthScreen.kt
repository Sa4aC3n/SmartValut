package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calculator.NetWorthBreakdown
import com.example.data.entity.NetWorthSnapshotEntity
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(
    breakdown: NetWorthBreakdown,
    snapshots: List<NetWorthSnapshotEntity>,
    currency: String = "ج.م",
    onBack: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("30d") } // "7d", "30d", "90d", "1y"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "صافي الثروة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Card: Net Worth Overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreenPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(EmeraldGreenPrimary, EmeraldGreenDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "صافي ثروتك",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${String.format("%,d", breakdown.netWorth.toInt())} $currency",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "إجمالي ما تملكه ناقص إجمالي ما عليك",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "إجمالي الأصول",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${String.format("%,d", breakdown.totalAssets.toInt())} $currency",
                                        color = IncomeGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "إجمالي الالتزامات",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${String.format("%,d", breakdown.totalLiabilities.toInt())} $currency",
                                        color = ExpenseRed,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Horizontal Assets vs Liabilities Comparison Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ما تملكه: ${breakdown.assetsPercentage.toInt()}%",
                                color = IncomeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "ما عليك: ${breakdown.liabilitiesPercentage.toInt()}%",
                                color = ExpenseRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val progress = (breakdown.assetsPercentage / 100.0).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape),
                            color = IncomeGreen,
                            trackColor = ExpenseRed.copy(alpha = 0.35f)
                        )
                    }
                }
            }

            // 3. Asset Distribution ("أين توجد أموالك؟")
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "أين توجد أموالك؟ (توزيع الأصول)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val totalAssetsSafe = breakdown.totalAssets.coerceAtLeast(1.0)

                        AssetCategoryRow(
                            name = "الخزن النقدية",
                            amount = breakdown.vaultsTotal,
                            percentage = (breakdown.vaultsTotal / totalAssetsSafe) * 100.0,
                            icon = Icons.Default.AccountBalanceWallet,
                            color = EmeraldGreenPrimary,
                            currency = currency
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        AssetCategoryRow(
                            name = "الذهب والسبائك",
                            amount = breakdown.goldEstimatedValue,
                            percentage = (breakdown.goldEstimatedValue / totalAssetsSafe) * 100.0,
                            icon = Icons.Default.MonetizationOn,
                            color = GoldAccent,
                            currency = currency
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        AssetCategoryRow(
                            name = "المدخرات النقدية",
                            amount = breakdown.cashSavingsTotal,
                            percentage = (breakdown.cashSavingsTotal / totalAssetsSafe) * 100.0,
                            icon = Icons.Default.AccountBalance,
                            color = Color(0xFF009688),
                            currency = currency
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        AssetCategoryRow(
                            name = "أموال لدى الآخرين (ديون لي)",
                            amount = breakdown.moneyOwedToMe,
                            percentage = (breakdown.moneyOwedToMe / totalAssetsSafe) * 100.0,
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFF7C4DFF),
                            currency = currency
                        )
                    }
                }
            }

            // 4. Net Worth History Snapshots
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "سجل صافي الثروة عبر الزمن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "7d" to "7 أيام",
                                "30d" to "30 يوم",
                                "90d" to "90 يوم",
                                "1y" to "سنة"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedPeriod == key,
                                    onClick = { selectedPeriod = key },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val periodDays = when (selectedPeriod) {
                            "7d" -> 7
                            "30d" -> 30
                            "90d" -> 90
                            else -> 365
                        }
                        val cutoff = System.currentTimeMillis() - (periodDays.toLong() * 86400000L)
                        val periodSnapshots = snapshots.filter { it.dateMillis >= cutoff }

                        if (periodSnapshots.isEmpty()) {
                            Text(
                                text = "يتم حفظ لقطات دورية لصافي الثروة لتتبع نمو أصولك عبر الزمن.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("ar"))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                periodSnapshots.takeLast(5).reversed().forEach { snap ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sdf.format(Date(snap.dateMillis)),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${String.format("%,d", snap.netWorth.toInt())} $currency",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = EmeraldGreenPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun AssetCategoryRow(
    name: String,
    amount: Double,
    percentage: Double,
    icon: ImageVector,
    color: Color,
    currency: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = "${percentage.toInt()}% من إجمالي الأصول",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Text(
            text = "${String.format("%,d", amount.toInt())} $currency",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
