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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.DebtEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransferEntity
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayFinancialSummary(
    val dayNumber: Int,
    val dateMillis: Long,
    val incomeTotal: Double = 0.0,
    val expenseTotal: Double = 0.0,
    val hasObligation: Boolean = false,
    val transactions: List<TransactionEntity> = emptyList(),
    val transfers: List<TransferEntity> = emptyList(),
    val commitments: List<CommitmentEntity> = emptyList(),
    val lessons: List<ChildLessonEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCalendarScreen(
    transactions: List<TransactionEntity>,
    transfers: List<TransferEntity>,
    commitments: List<CommitmentEntity>,
    lessons: List<ChildLessonEntity>,
    debts: List<DebtEntity>,
    currency: String = "ج.م",
    onBack: () -> Unit
) {
    var calendarYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-based
    var selectedDaySummary by remember { mutableStateOf<DayFinancialSummary?>(null) }

    // Calculate month metrics
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, calendarYear)
        set(Calendar.MONTH, calendarMonth)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 7=Saturday

    val monthStartMillis = cal.timeInMillis
    cal.set(Calendar.DAY_OF_MONTH, daysInMonth)
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    val monthEndMillis = cal.timeInMillis

    val monthTransactions = transactions.filter { it.dateMillis in monthStartMillis..monthEndMillis }
    val monthIncome = monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val monthExpense = monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val monthNet = monthIncome - monthExpense

    val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale("ar"))
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val monthTitle = monthNameFormat.format(cal.time)

    // Precalculate day summaries
    val dayMap = mutableMapOf<Int, DayFinancialSummary>()
    for (d in 1..daysInMonth) {
        val dayCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendarYear)
            set(Calendar.MONTH, calendarMonth)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startD = dayCal.timeInMillis
        val endD = startD + 86400000L - 1000L

        val dayTxs = transactions.filter { it.dateMillis in startD..endD }
        val dayTransfers = transfers.filter { it.dateMillis in startD..endD }
        val dayComm = commitments.filter { it.dueDateMillis in startD..endD }
        val dayLess = lessons.filter { it.dueDateMillis in startD..endD }
        val dayDebts = debts.filter { it.dueDateMillis != null && it.dueDateMillis in startD..endD }

        val inc = dayTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val exp = dayTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val hasOblig = dayComm.isNotEmpty() || dayLess.isNotEmpty() || dayDebts.isNotEmpty()

        dayMap[d] = DayFinancialSummary(
            dayNumber = d,
            dateMillis = startD,
            incomeTotal = inc,
            expenseTotal = exp,
            hasObligation = hasOblig,
            transactions = dayTxs,
            transfers = dayTransfers,
            commitments = dayComm,
            lessons = dayLess,
            debts = dayDebts
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقويم المالي", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Month Switcher Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (calendarMonth == 0) {
                        calendarMonth = 11
                        calendarYear -= 1
                    } else {
                        calendarMonth -= 1
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الشهر السابق")
                }

                Text(
                    text = monthTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = {
                    if (calendarMonth == 11) {
                        calendarMonth = 0
                        calendarYear += 1
                    } else {
                        calendarMonth += 1
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الشهر القادم")
                }
            }

            // Month Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("الدخل", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "+${String.format("%,d", monthIncome.toInt())} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = IncomeGreen
                        )
                    }
                    Column {
                        Text("المصروف", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "-${String.format("%,d", monthExpense.toInt())} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ExpenseRed
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("الصافي", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "${String.format("%,d", monthNet.toInt())} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (monthNet >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Days of week header (Sat to Fri)
            val weekDayNames = listOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                weekDayNames.forEach { dayName ->
                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            // Arabic week: Saturday is 7 in java Calendar (Sunday=1, Monday=2 ... Saturday=7)
            // Shift to Saturday-based index: Saturday=0, Sunday=1, ..., Friday=6
            val startingEmptySlots = (firstDayOfWeek % 7) // Sat=7%7=0, Sun=1%7=1, Mon=2%7=2 ...
            val totalSlots = startingEmptySlots + daysInMonth
            val totalRows = (totalSlots + 6) / 7

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until totalRows) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0..6) {
                            val slotIndex = row * 7 + col
                            val dayNum = slotIndex - startingEmptySlots + 1

                            if (dayNum in 1..daysInMonth) {
                                val summary = dayMap[dayNum] ?: DayFinancialSummary(dayNum, 0L)
                                DayCell(
                                    summary = summary,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedDaySummary = summary }
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f).height(54.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Day Details Dialog / Sheet
    if (selectedDaySummary != null) {
        val s = selectedDaySummary!!
        val dayFormat = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
        val dateTitle = dayFormat.format(Date(s.dateMillis))

        AlertDialog(
            onDismissRequest = { selectedDaySummary = null },
            title = {
                Text(dateTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الدخل: +${s.incomeTotal.toInt()} $currency", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("المصروف: -${s.expenseTotal.toInt()} $currency", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val allItemsCount = s.transactions.size + s.transfers.size + s.commitments.size + s.lessons.size + s.debts.size
                    if (allItemsCount == 0) {
                        Text("لا توجد حركات مالية أو التزامات مسجلة في هذا اليوم.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Transactions
                            items(s.transactions) { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tx.description.ifBlank { tx.category }, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text("${tx.category} • ${tx.vaultName}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text(
                                        "${if (tx.type == "INCOME") "+" else "-"}${tx.amount.toInt()} $currency",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (tx.type == "INCOME") IncomeGreen else ExpenseRed
                                    )
                                }
                            }

                            // Transfers
                            items(s.transfers) { tr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(EmeraldGreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("تحويل: ${tr.fromVaultName} ← ${tr.toVaultName}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        if (tr.notes.isNotBlank()) Text(tr.notes, fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Text("${tr.amount.toInt()} $currency", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldGreenPrimary)
                                }
                            }

                            // Commitments
                            items(s.commitments) { c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE91E63).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("التزام: ${c.title}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text("${c.amount.toInt()} $currency", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE91E63))
                                }
                            }

                            // Lessons
                            items(s.lessons) { l ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF3F51B5).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("درس: ${l.childName} - ${l.subject}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text("${l.amount.toInt()} $currency", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF3F51B5))
                                }
                            }

                            // Debts
                            items(s.debts) { d ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF7C4DFF).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("دين: ${d.personName} (${if (d.type == "OWED_TO_ME") "لي" else "عليّ"})", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text("${d.remainingAmount.toInt()} $currency", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF7C4DFF))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedDaySummary = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                ) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun DayCell(
    summary: DayFinancialSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val hasInc = summary.incomeTotal > 0
    val hasExp = summary.expenseTotal > 0

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "${summary.dayNumber}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hasInc) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(IncomeGreen))
                }
                if (hasExp) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(ExpenseRed))
                }
                if (summary.hasObligation) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFFF9800)))
                }
            }
        }
    }
}
