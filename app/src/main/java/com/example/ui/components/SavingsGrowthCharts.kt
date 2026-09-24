package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.GoldAssetEntity
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LightBackground
import com.example.ui.theme.MintBackground
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data representation of savings status for a single monthly point
 */
data class MonthlySavingsPoint(
    val monthIndex: Int,
    val monthLabel: String,
    val shortLabel: String,
    val timestampMillis: Long,
    val cumulativeGoldValue: Double,
    val cumulativeCashValue: Double,
    val totalCumulativeValue: Double,
    val monthlyGoldAddition: Double,
    val monthlyCashAddition: Double,
    val totalMonthlyAddition: Double,
    val growthRatePercentage: Double
)

@Composable
fun SavingsGrowthInteractiveCard(
    goldAssets: List<GoldAssetEntity>,
    cashSavings: List<CashSavingEntity>,
    goldPriceMap: Map<Int, Double>,
    currency: String,
    modifier: Modifier = Modifier
) {
    var selectedMonthsRange by remember { mutableIntStateOf(6) } // 3, 6, 12
    var selectedChartType by remember { mutableIntStateOf(0) } // 0: Cumulative Area Trend, 1: Monthly Additions Bar
    var selectedPointIndex by remember { mutableIntStateOf(-1) }

    // Generate monthly timeline points
    val monthlyPoints = remember(goldAssets, cashSavings, goldPriceMap, selectedMonthsRange) {
        generateMonthlyDataPoints(goldAssets, cashSavings, goldPriceMap, selectedMonthsRange)
    }

    // Default selected point to latest point
    val activePoint = if (selectedPointIndex in monthlyPoints.indices) {
        monthlyPoints[selectedPointIndex]
    } else {
        monthlyPoints.lastOrNull()
    }

    // Overall trend metrics
    val firstPoint = monthlyPoints.firstOrNull()
    val lastPoint = monthlyPoints.lastOrNull()
    val totalPeriodGrowth = if (firstPoint != null && lastPoint != null && firstPoint.totalCumulativeValue > 0) {
        ((lastPoint.totalCumulativeValue - firstPoint.totalCumulativeValue) / firstPoint.totalCumulativeValue) * 100.0
    } else {
        0.0
    }

    val avgMonthlyInflow = if (monthlyPoints.isNotEmpty()) {
        monthlyPoints.map { it.totalMonthlyAddition }.average()
    } else 0.0

    val bestMonth = monthlyPoints.maxByOrNull { it.totalMonthlyAddition }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Header with Title & Live Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MintBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "النمو",
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "تطور نمو المدخرات والأصول",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تحليل بياني تفاعلي للذهب والنقدية",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Interactive Hint Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFFB8860B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "اسحب للمعاينة",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB8860B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Control Bar: View Type Selector & Timeframe Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chart Type Switcher (Area Trend vs Bar Chart)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightBackground)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedChartType == 0) EmeraldGreenPrimary else Color.Transparent)
                            .clickable {
                                selectedChartType = 0
                                selectedPointIndex = -1
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = if (selectedChartType == 0) Color.White else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "تراكمي",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedChartType == 0) Color.White else Color.Gray
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedChartType == 1) EmeraldGreenPrimary else Color.Transparent)
                            .clickable {
                                selectedChartType = 1
                                selectedPointIndex = -1
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = if (selectedChartType == 1) Color.White else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "شهري",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedChartType == 1) Color.White else Color.Gray
                            )
                        }
                    }
                }

                // Month Duration Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(3 to "3M", 6 to "6M", 12 to "1Y").forEach { (months, label) ->
                        val isSelected = selectedMonthsRange == months
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) EmeraldGreenDark else LightBackground)
                                .clickable {
                                    selectedMonthsRange = months
                                    selectedPointIndex = -1
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Dynamic Interactive Tooltip Card (Updates upon touch / drag)
            if (activePoint != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MintBackground.copy(alpha = 0.8f)),
                    border = BorderStroke(1.dp, EmeraldGreenPrimary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGreenPrimary)
                                )
                                Text(
                                    text = activePoint.monthLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = EmeraldGreenDark
                                )
                            }

                            // Growth percentage badge
                            if (activePoint.growthRatePercentage != 0.0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (activePoint.growthRatePercentage >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = if (activePoint.growthRatePercentage >= 0) IncomeGreen else ExpenseRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${if (activePoint.growthRatePercentage >= 0) "+" else ""}${String.format(Locale.US, "%.1f", activePoint.growthRatePercentage)}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activePoint.growthRatePercentage >= 0) IncomeGreen else ExpenseRed
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Values breakdown in the active point
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Total for Month
                            Column {
                                Text(
                                    text = if (selectedChartType == 0) "إجمالي الرصيد التراكمي" else "إجمالي إضافات الشهر",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%,.0f", if (selectedChartType == 0) activePoint.totalCumulativeValue else activePoint.totalMonthlyAddition)} $currency",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Gold Value
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoldAccent))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("الذهب", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%,.0f", if (selectedChartType == 0) activePoint.cumulativeGoldValue else activePoint.monthlyGoldAddition)} $currency",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB8860B)
                                )
                            }

                            // Cash Value
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreenPrimary))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("النقدية", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%,.0f", if (selectedChartType == 0) activePoint.cumulativeCashValue else activePoint.monthlyCashAddition)} $currency",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreenPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. MAIN INTERACTIVE CANVAS CHART
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                if (selectedChartType == 0) {
                    // Cumulative Multi-Layer Smooth Area Curve Chart
                    CumulativeSavingsAreaChart(
                        points = monthlyPoints,
                        selectedIndex = selectedPointIndex,
                        onSelectIndex = { selectedPointIndex = it },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Monthly Inflow Bar Comparison Chart
                    MonthlyAdditionsBarChart(
                        points = monthlyPoints,
                        selectedIndex = selectedPointIndex,
                        onSelectIndex = { selectedPointIndex = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Legend & Color Markers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(GoldAccent))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مدخرات الذهب والسبائك", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(EmeraldGreenPrimary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("المدخرات والسيولة النقدية", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Growth Insights Summary Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Growth Rate Card
                GrowthMetricMiniCard(
                    title = "معدل النمو للفترة",
                    value = "${if (totalPeriodGrowth >= 0) "+" else ""}${String.format(Locale.US, "%.1f", totalPeriodGrowth)}%",
                    subtitle = if (totalPeriodGrowth >= 0) "نمو إيجابي مستمر" else "معدل مستقر",
                    isPositive = totalPeriodGrowth >= 0,
                    modifier = Modifier.weight(1f)
                )

                // Avg Monthly Addition
                GrowthMetricMiniCard(
                    title = "متوسط الادخار الشهري",
                    value = "${String.format(Locale.US, "%,.0f", avgMonthlyInflow)}",
                    subtitle = currency,
                    isPositive = true,
                    modifier = Modifier.weight(1f)
                )

                // Peak Month
                GrowthMetricMiniCard(
                    title = "الشهر الأعلى ادخاراً",
                    value = bestMonth?.shortLabel ?: "-",
                    subtitle = if (bestMonth != null) "${String.format(Locale.US, "%,.0f", bestMonth.totalMonthlyAddition)} $currency" else "-",
                    isPositive = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GrowthMetricMiniCard(
    title: String,
    value: String,
    subtitle: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = LightBackground,
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPositive) EmeraldGreenDark else ExpenseRed,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = Color.Gray,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Interactive Cumulative Savings Area Chart with Bezier smoothing and scrub detection
 */
@Composable
private fun CumulativeSavingsAreaChart(
    points: List<MonthlySavingsPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxValue = remember(points) {
        val max = points.maxOfOrNull { it.totalCumulativeValue } ?: 100.0
        if (max <= 0.0) 100.0 else max * 1.15 // headroom
    }

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val spacing = size.width / points.size.coerceAtLeast(1)
                    val clickedIdx = (offset.x / spacing).toInt().coerceIn(0, points.size - 1)
                    onSelectIndex(clickedIdx)
                }
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val spacing = size.width / points.size.coerceAtLeast(1)
                        val draggedIdx = (offset.x / spacing).toInt().coerceIn(0, points.size - 1)
                        onSelectIndex(draggedIdx)
                    },
                    onDrag = { change, _ ->
                        val spacing = size.width / points.size.coerceAtLeast(1)
                        val draggedIdx = (change.position.x / spacing).toInt().coerceIn(0, points.size - 1)
                        onSelectIndex(draggedIdx)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val bottomPadding = 32.dp.toPx()
        val topPadding = 16.dp.toPx()
        val chartHeight = height - bottomPadding - topPadding

        val stepX = width / (points.size - 1).coerceAtLeast(1)

        // 1. Draw horizontal grid lines & Y values
        val gridLines = 4
        val paintText = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 26f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }

        for (i in 0..gridLines) {
            val y = topPadding + (chartHeight / gridLines) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        // 2. Build Paths for Total, Gold, and Cash Curves
        val totalPath = Path()
        val totalAreaPath = Path()
        val goldPath = Path()

        val totalPointsOffsets = mutableListOf<Offset>()
        val goldPointsOffsets = mutableListOf<Offset>()

        points.forEachIndexed { i, p ->
            val x = i * stepX
            val yTotal = topPadding + chartHeight - ((p.totalCumulativeValue / maxValue) * chartHeight).toFloat()
            val yGold = topPadding + chartHeight - ((p.cumulativeGoldValue / maxValue) * chartHeight).toFloat()

            totalPointsOffsets.add(Offset(x, yTotal))
            goldPointsOffsets.add(Offset(x, yGold))
        }

        // Connect Total points with smooth cubic Bezier
        if (totalPointsOffsets.isNotEmpty()) {
            totalPath.moveTo(totalPointsOffsets[0].x, totalPointsOffsets[0].y)
            totalAreaPath.moveTo(totalPointsOffsets[0].x, height - bottomPadding)
            totalAreaPath.lineTo(totalPointsOffsets[0].x, totalPointsOffsets[0].y)

            for (i in 0 until totalPointsOffsets.size - 1) {
                val p0 = totalPointsOffsets[i]
                val p1 = totalPointsOffsets[i + 1]
                val controlX1 = p0.x + (p1.x - p0.x) / 2
                val controlY1 = p0.y
                val controlX2 = p0.x + (p1.x - p0.x) / 2
                val controlY2 = p1.y

                totalPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                totalAreaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }

            totalAreaPath.lineTo(totalPointsOffsets.last().x, height - bottomPadding)
            totalAreaPath.close()

            // Draw Area Gradient for Total Savings
            drawPath(
                path = totalAreaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        EmeraldGreenPrimary.copy(alpha = 0.35f),
                        EmeraldGreenPrimary.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = topPadding,
                    endY = height - bottomPadding
                )
            )

            // Draw Stroke for Total
            drawPath(
                path = totalPath,
                color = EmeraldGreenPrimary,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Connect Gold points
        if (goldPointsOffsets.isNotEmpty()) {
            goldPath.moveTo(goldPointsOffsets[0].x, goldPointsOffsets[0].y)
            for (i in 0 until goldPointsOffsets.size - 1) {
                val p0 = goldPointsOffsets[i]
                val p1 = goldPointsOffsets[i + 1]
                val controlX1 = p0.x + (p1.x - p0.x) / 2
                val controlY1 = p0.y
                val controlX2 = p0.x + (p1.x - p0.x) / 2
                val controlY2 = p1.y

                goldPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }

            drawPath(
                path = goldPath,
                color = GoldAccent,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
            )
        }

        // 3. Draw Points & Month Labels
        val labelPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        points.forEachIndexed { i, p ->
            val offsetTotal = totalPointsOffsets[i]
            val offsetGold = goldPointsOffsets[i]
            val isSelected = selectedIndex == i

            // X-Axis Month label
            drawContext.canvas.nativeCanvas.drawText(
                p.shortLabel,
                offsetTotal.x,
                height - 6.dp.toPx(),
                labelPaint
            )

            // Gold dot
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = offsetGold
            )
            drawCircle(
                color = GoldAccent,
                radius = 3.dp.toPx(),
                center = offsetGold
            )

            // Total dot
            if (isSelected) {
                // Glow ring
                drawCircle(
                    color = EmeraldGreenPrimary.copy(alpha = 0.3f),
                    radius = 11.dp.toPx(),
                    center = offsetTotal
                )
                // Selected vertical guide line
                drawLine(
                    color = EmeraldGreenDark.copy(alpha = 0.6f),
                    start = Offset(offsetTotal.x, topPadding),
                    end = Offset(offsetTotal.x, height - bottomPadding),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }

            drawCircle(
                color = Color.White,
                radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                center = offsetTotal
            )
            drawCircle(
                color = EmeraldGreenPrimary,
                radius = if (isSelected) 4.5.dp.toPx() else 3.5.dp.toPx(),
                center = offsetTotal
            )
        }
    }
}

/**
 * Monthly Inflow / Additions Dual Bar Comparison Chart
 */
@Composable
private fun MonthlyAdditionsBarChart(
    points: List<MonthlySavingsPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxAddition = remember(points) {
        val max = points.maxOfOrNull { it.totalMonthlyAddition } ?: 100.0
        if (max <= 0.0) 100.0 else max * 1.2
    }

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val barGroupWidth = size.width / points.size.coerceAtLeast(1)
                    val clickedIdx = (offset.x / barGroupWidth).toInt().coerceIn(0, points.size - 1)
                    onSelectIndex(clickedIdx)
                }
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val barGroupWidth = size.width / points.size.coerceAtLeast(1)
                        val draggedIdx = (offset.x / barGroupWidth).toInt().coerceIn(0, points.size - 1)
                        onSelectIndex(draggedIdx)
                    },
                    onDrag = { change, _ ->
                        val barGroupWidth = size.width / points.size.coerceAtLeast(1)
                        val draggedIdx = (change.position.x / barGroupWidth).toInt().coerceIn(0, points.size - 1)
                        onSelectIndex(draggedIdx)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val bottomPadding = 32.dp.toPx()
        val topPadding = 16.dp.toPx()
        val chartHeight = height - bottomPadding - topPadding

        val groupWidth = width / points.size
        val barWidth = (groupWidth * 0.32f).coerceAtMost(22.dp.toPx())

        // Grid lines
        for (i in 0..3) {
            val y = topPadding + (chartHeight / 3) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        points.forEachIndexed { i, p ->
            val isSelected = selectedIndex == i
            val groupCenterX = i * groupWidth + (groupWidth / 2)

            // Selection background highlight
            if (isSelected) {
                drawRoundRect(
                    color = EmeraldGreenPrimary.copy(alpha = 0.10f),
                    topLeft = Offset(i * groupWidth + 4.dp.toPx(), topPadding - 4.dp.toPx()),
                    size = Size(groupWidth - 8.dp.toPx(), chartHeight + 8.dp.toPx()),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
            }

            val goldBarHeight = ((p.monthlyGoldAddition / maxAddition) * chartHeight).toFloat().coerceAtLeast(0f)
            val cashBarHeight = ((p.monthlyCashAddition / maxAddition) * chartHeight).toFloat().coerceAtLeast(0f)

            // Gold Bar
            val goldX = groupCenterX - barWidth - 2.dp.toPx()
            val goldY = topPadding + chartHeight - goldBarHeight
            if (goldBarHeight > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(GoldAccent, Color(0xFFD4AF37)),
                        startY = goldY,
                        endY = topPadding + chartHeight
                    ),
                    topLeft = Offset(goldX, goldY),
                    size = Size(barWidth, goldBarHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            } else {
                // Mini indicator baseline dot
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.5f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(goldX + barWidth / 2, topPadding + chartHeight - 2.dp.toPx())
                )
            }

            // Cash Bar
            val cashX = groupCenterX + 2.dp.toPx()
            val cashY = topPadding + chartHeight - cashBarHeight
            if (cashBarHeight > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(EmeraldGreenPrimary, EmeraldGreenDark),
                        startY = cashY,
                        endY = topPadding + chartHeight
                    ),
                    topLeft = Offset(cashX, cashY),
                    size = Size(barWidth, cashBarHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            } else {
                drawCircle(
                    color = EmeraldGreenPrimary.copy(alpha = 0.5f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(cashX + barWidth / 2, topPadding + chartHeight - 2.dp.toPx())
                )
            }

            // Month Label
            drawContext.canvas.nativeCanvas.drawText(
                p.shortLabel,
                groupCenterX,
                height - 6.dp.toPx(),
                labelPaint
            )
        }
    }
}

/**
 * Utility function to compute past N months data points from gold assets and cash savings
 */
private fun generateMonthlyDataPoints(
    goldAssets: List<GoldAssetEntity>,
    cashSavings: List<CashSavingEntity>,
    goldPriceMap: Map<Int, Double>,
    monthsCount: Int
): List<MonthlySavingsPoint> {
    val result = mutableListOf<MonthlySavingsPoint>()
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale("ar"))
    val sdfShort = SimpleDateFormat("MMM", Locale("ar"))

    val cal = Calendar.getInstance()

    // Determine target month calendars
    val monthsList = mutableListOf<Calendar>()
    for (i in (monthsCount - 1) downTo 0) {
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, -i)
        // set to end of month for cumulative evaluation
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        monthsList.add(c)
    }

    var prevTotalCumulative = 0.0

    monthsList.forEachIndexed { index, monthCal ->
        val monthEndMillis = monthCal.timeInMillis

        // Month start millis
        val startCal = monthCal.clone() as Calendar
        startCal.set(Calendar.DAY_OF_MONTH, 1)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        val monthStartMillis = startCal.timeInMillis

        // 1. Cumulative Gold on or before this month end
        val cumulativeGold = goldAssets
            .filter { it.purchaseDateMillis <= monthEndMillis && (it.status != "SOLD" || (it.saleDateMillis ?: Long.MAX_VALUE) > monthEndMillis) }
            .sumOf { asset ->
                val currentPricePerGram = goldPriceMap[asset.karat] ?: 0.0
                if (currentPricePerGram > 0) asset.weight * currentPricePerGram else asset.purchasePrice
            }

        // 2. Cumulative Cash on or before this month end
        val cumulativeCash = cashSavings
            .filter { it.dateMillis <= monthEndMillis }
            .sumOf { it.amount }

        val totalCumulative = cumulativeGold + cumulativeCash

        // 3. Monthly Inflow (purchased / saved strictly in this month)
        val goldAddedThisMonth = goldAssets
            .filter { it.purchaseDateMillis in monthStartMillis..monthEndMillis }
            .sumOf { asset ->
                val currentPricePerGram = goldPriceMap[asset.karat] ?: 0.0
                if (currentPricePerGram > 0) asset.weight * currentPricePerGram else asset.purchasePrice
            }

        val cashAddedThisMonth = cashSavings
            .filter { it.dateMillis in monthStartMillis..monthEndMillis }
            .sumOf { it.amount }

        val totalMonthlyAddition = goldAddedThisMonth + cashAddedThisMonth

        // 4. Growth percentage compared to previous month point
        val growthRate = if (index > 0 && prevTotalCumulative > 0) {
            ((totalCumulative - prevTotalCumulative) / prevTotalCumulative) * 100.0
        } else {
            0.0
        }

        prevTotalCumulative = totalCumulative

        result.add(
            MonthlySavingsPoint(
                monthIndex = index,
                monthLabel = sdfMonth.format(monthCal.time),
                shortLabel = sdfShort.format(monthCal.time),
                timestampMillis = monthEndMillis,
                cumulativeGoldValue = cumulativeGold,
                cumulativeCashValue = cumulativeCash,
                totalCumulativeValue = totalCumulative,
                monthlyGoldAddition = goldAddedThisMonth,
                monthlyCashAddition = cashAddedThisMonth,
                totalMonthlyAddition = totalMonthlyAddition,
                growthRatePercentage = growthRate
            )
        )
    }

    return result
}
