package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.GoldAccent
import java.util.Locale

@Composable
fun PieChartComposable(
    categoryData: Map<String, Double>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val total = categoryData.values.sum()
    val defaultColors = listOf(
        Color(0xFF1B5E20), Color(0xFF00695C), Color(0xFF1565C0),
        Color(0xFF6A1B9A), Color(0xFFAD1457), Color(0xFFC62828),
        Color(0xFFD84315), Color(0xFFEF6C00), Color(0xFF283593)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "توزيع الإنفاق حسب التصنيف (Pie Chart)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (total <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مصروفات مسجلة هذا الشهر حتى الآن",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Pie Canvas
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                        Canvas(modifier = Modifier.size(160.dp)) {
                            var startAngle = -90f
                            var index = 0
                            categoryData.forEach { (_, value) ->
                                val sweepAngle = ((value / total) * 360f).toFloat()
                                val color = defaultColors[index % defaultColors.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    size = Size(size.width, size.height)
                                )
                                startAngle += sweepAngle
                                index++
                            }
                            // Inner circle for donut feel
                            drawCircle(
                                color = Color.White,
                                radius = size.width * 0.28f
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "الإجمالي",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = String.format(Locale.US, "%.0f", total),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Legend
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        var idx = 0
                        categoryData.entries.take(5).forEach { (cat, valAmount) ->
                            val color = defaultColors[idx % defaultColors.size]
                            val pct = (valAmount / total * 100).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                            idx++
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartComposable(
    monthlyData: Map<String, Double>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val maxVal = (monthlyData.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "الإنفاق لكل شهر (Bar Chart)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyData.forEach { (monthLabel, amount) ->
                    val barHeightFactor = (amount / maxVal).toFloat().coerceIn(0.05f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f", amount),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(
                            modifier = Modifier
                                .width(20.dp)
                                .height(120.dp * barHeightFactor)
                        ) {
                            drawRoundRect(
                                color = EmeraldGreenPrimary,
                                size = Size(size.width, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = monthLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LineChartComposable(
    balanceHistory: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "تطور الرصيد مع الوقت (Line Chart)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (balanceHistory.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "سيظهر منحنى التطور مع زيادة العمليات", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                val maxVal = (balanceHistory.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
                val minVal = (balanceHistory.minOfOrNull { it.second } ?: 0.0).coerceAtLeast(0.0)
                val range = (maxVal - minVal).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val points = balanceHistory.mapIndexed { idx, item ->
                        val x = (idx.toFloat() / (balanceHistory.size - 1)) * width
                        val y = height - (((item.second - minVal) / range) * height).toFloat()
                        Offset(x, y.coerceIn(10f, height - 10f))
                    }

                    // Stroke Path
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    // Fill Path
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                EmeraldGreenPrimary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )

                    drawPath(
                        path = path,
                        color = EmeraldGreenPrimary,
                        style = Stroke(width = 5f)
                    )

                    points.forEach { pt ->
                        drawCircle(
                            color = GoldAccent,
                            radius = 6f,
                            center = pt
                        )
                        drawCircle(
                            color = EmeraldGreenPrimary,
                            radius = 4f,
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
