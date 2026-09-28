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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DebtEntity
import com.example.ui.dialogs.AddDebtDialog
import com.example.ui.dialogs.RecordDebtPaymentDialog
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    debts: List<DebtEntity>,
    currency: String = "ج.م",
    onBack: () -> Unit,
    onAddDebt: (personName: String, type: String, amount: Double, notes: String) -> Unit,
    onRecordPayment: (debtId: Int, amount: Double, notes: String) -> Unit,
    onDeleteDebt: (debtId: Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: لي (OWED_TO_ME), 1: عليّ (I_OWE)
    var showAddDialog by remember { mutableStateOf(false) }
    var debtForPayment by remember { mutableStateOf<DebtEntity?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "PAID"

    val owedToMeList = debts.filter { it.type == "OWED_TO_ME" }
    val iOweList = debts.filter { it.type == "I_OWE" }

    val totalOwedToMe = owedToMeList.filter { it.status != "PAID" }.sumOf { it.remainingAmount }
    val totalIOwe = iOweList.filter { it.status != "PAID" }.sumOf { it.remainingAmount }
    val netDebt = totalOwedToMe - totalIOwe

    val currentList = if (selectedTab == 0) owedToMeList else iOweList
    val filteredList = currentList.filter {
        when (statusFilter) {
            "ACTIVE" -> it.status != "PAID"
            "PAID" -> it.status == "PAID"
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة الديون (لي / عليّ)", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldGreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة دين")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Summary Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("لي عند الغير", fontSize = 11.sp, color = IncomeGreen, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${String.format("%,d", totalOwedToMe.toInt())} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = IncomeGreen
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("عليّ للغير", fontSize = 11.sp, color = ExpenseRed, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${String.format("%,d", totalIOwe.toInt())} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ExpenseRed
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreenPrimary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("الصافي", fontSize = 11.sp, color = EmeraldGreenPrimary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${String.format("%,d", netDebt.toInt())} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (netDebt >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tabs: لي عند الآخرين vs أموال عليّ
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("لي عند الآخرين (${owedToMeList.count { it.status != "PAID" }})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("أموال عليّ (${iOweList.count { it.status != "PAID" }})") }
                )
            }

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" },
                    label = { Text("الكل") }
                )
                FilterChip(
                    selected = statusFilter == "ACTIVE",
                    onClick = { statusFilter = "ACTIVE" },
                    label = { Text("نشط / غير مسدد") }
                )
                FilterChip(
                    selected = statusFilter == "PAID",
                    onClick = { statusFilter = "PAID" },
                    label = { Text("تم السداد بالكامل") }
                )
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (selectedTab == 0) "لا توجد أموال مستحقة لك مسجلة حاليًا" else "لا توجد ديون مستحقة عليك مسجلة حاليًا",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                        ) {
                            Text("تسجيل دين جديد")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { debt ->
                        DebtCard(
                            debt = debt,
                            currency = currency,
                            onRecordPayment = { debtForPayment = debt },
                            onDelete = { onDeleteDebt(debt.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDebtDialog(
            initialType = if (selectedTab == 0) "OWED_TO_ME" else "I_OWE",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, amount, notes ->
                onAddDebt(name, type, amount, notes)
            }
        )
    }

    if (debtForPayment != null) {
        RecordDebtPaymentDialog(
            debt = debtForPayment!!,
            currency = currency,
            onDismiss = { debtForPayment = null },
            onConfirm = { paymentAmount, notes ->
                onRecordPayment(debtForPayment!!.id, paymentAmount, notes)
                debtForPayment = null
            }
        )
    }
}

@Composable
private fun DebtCard(
    debt: DebtEntity,
    currency: String,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("ar"))
    val isPaid = debt.status == "PAID"
    val progress = if (debt.originalAmount > 0) (debt.paidAmount / debt.originalAmount).toFloat().coerceIn(0f, 1f) else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = debt.personName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                // Status Badge
                val (badgeText, badgeColor) = when (debt.status) {
                    "PAID" -> Pair("مسدد بالكامل", IncomeGreen)
                    "PARTIALLY_PAID" -> Pair("مسدد جزئيًا", Color(0xFFFF9800))
                    else -> Pair("نشط", EmeraldGreenPrimary)
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("المبلغ الكلي", fontSize = 11.sp, color = Color.Gray)
                    Text("${String.format("%,d", debt.originalAmount.toInt())} $currency", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text("تم سداده", fontSize = 11.sp, color = Color.Gray)
                    Text("${String.format("%,d", debt.paidAmount.toInt())} $currency", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = IncomeGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("المتبقي", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        "${String.format("%,d", debt.remainingAmount.toInt())} $currency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isPaid) IncomeGreen else ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = IncomeGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (debt.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = debt.notes,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sdf.format(Date(debt.startDateMillis)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row {
                    if (!isPaid) {
                        Button(
                            onClick = onRecordPayment,
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسجيل سداد", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray)
                    }
                }
            }
        }
    }
}
