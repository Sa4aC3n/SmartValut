package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.TransactionEntity
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MintBackground
import com.example.ui.utils.CategoryUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesAndSearchScreen(
    transactions: List<TransactionEntity>,
    searchQuery: String,
    selectedCategory: String?,
    selectedType: String?,
    currency: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectType: (String?) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenAddIncome: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Effective type: default to "EXPENSE" if null to match screenshot
    val activeType = selectedType ?: "EXPENSE"
    var viewingReceiptPath by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    viewingReceiptPath?.let { path ->
        com.example.ui.utils.ReceiptImageViewerDialog(
            imagePathOrUri = path,
            onDismiss = { viewingReceiptPath = null }
        )
    }

    // Filter transactions based on activeType and searchQuery
    val filteredList = remember(transactions, activeType, searchQuery) {
        transactions.filter { tx ->
            val matchesType = tx.type == activeType
            val matchesSearch = if (searchQuery.isBlank()) true else {
                tx.description.contains(searchQuery, ignoreCase = true) ||
                        tx.category.contains(searchQuery, ignoreCase = true) ||
                        tx.amount.toString().contains(searchQuery) ||
                        tx.vaultName.contains(searchQuery, ignoreCase = true)
            }
            matchesType && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER (العمليات + سجل الدخل والمصروفات + زر الإعدادات)
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
                        text = "العمليات",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "سجل الدخل والمصروفات",
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

        // 2. SEGMENTED SWITCHER (المصروفات | الدخل)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MintBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "المصروفات" Button (Right side in RTL)
                    val isExpenseActive = activeType == "EXPENSE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isExpenseActive) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .then(
                                if (isExpenseActive) Modifier.border(1.dp, CardBorderColor, RoundedCornerShape(22.dp))
                                else Modifier
                            )
                            .clickable { onSelectType("EXPENSE") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "المصروفات",
                            fontSize = 14.sp,
                            fontWeight = if (isExpenseActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isExpenseActive) MaterialTheme.colorScheme.onSurface else Color.Gray
                        )
                    }

                    // "الدخل" Button (Left side in RTL)
                    val isIncomeActive = activeType == "INCOME"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isIncomeActive) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .then(
                                if (isIncomeActive) Modifier.border(1.dp, CardBorderColor, RoundedCornerShape(22.dp))
                                else Modifier
                            )
                            .clickable { onSelectType("INCOME") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "الدخل",
                            fontSize = 14.sp,
                            fontWeight = if (isIncomeActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isIncomeActive) MaterialTheme.colorScheme.onSurface else Color.Gray
                        )
                    }
                }
            }
        }

        // 3. SEARCH BAR + QUICK ADD BUTTON (+)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Box (Right side in RTL)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "ابحث بالاسم أو التصنيف أو المبلغ أو التاريخ",
                                        fontSize = 12.5.sp,
                                        color = Color.Gray.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Green (+) Action Button (Left side in RTL)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreenPrimary)
                        .clickable {
                            if (activeType == "INCOME") onOpenAddIncome() else onOpenAddExpense()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة عملية",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 4. TRANSACTION LIST AREA
        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد عمليات",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(filteredList) { tx ->
                TransactionRowItemLocal(
                    tx = tx,
                    currency = currency,
                    onDelete = { onDeleteTransaction(tx) },
                    onViewReceipt = { path -> viewingReceiptPath = path }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TransactionRowItemLocal(
    tx: TransactionEntity,
    currency: String,
    onDelete: () -> Unit,
    onViewReceipt: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isIncome = tx.type == "INCOME"
    val icon = CategoryUtils.getCategoryIcon(tx.category)
    val iconColor = if (isIncome) IncomeGreen else ExpenseRed

    val formattedDate = remember(tx.dateMillis) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            sdf.format(Date(tx.dateMillis))
        } catch (e: Exception) {
            ""
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
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

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description.ifEmpty { tx.category },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
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
                    if (formattedDate.isNotEmpty()) {
                        Text(text = " • ", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = formattedDate,
                            fontSize = 10.5.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (!tx.receiptImagePath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    com.example.ui.utils.ReceiptBadgeButton(
                        imagePathOrUri = tx.receiptImagePath!!,
                        onClick = { onViewReceipt?.invoke(tx.receiptImagePath!!) }
                    )
                }
            }

            Text(
                text = "${if (isIncome) "+" else "-"}${String.format(Locale.US, "%,.0f", tx.amount)} $currency",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (isIncome) IncomeGreen else ExpenseRed
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
