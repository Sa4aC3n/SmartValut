package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.data.entity.TransferEntity
import com.example.data.entity.VaultEntity
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MintBackground
import com.example.ui.utils.CategoryUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class UnifiedFinanceItem {
    abstract val id: Int
    abstract val dateMillis: Long
    abstract val amount: Double

    data class TxItem(val tx: TransactionEntity) : UnifiedFinanceItem() {
        override val id: Int get() = tx.id
        override val dateMillis: Long get() = tx.dateMillis
        override val amount: Double get() = tx.amount
    }

    data class TransferItem(val transfer: TransferEntity) : UnifiedFinanceItem() {
        override val id: Int get() = transfer.id
        override val dateMillis: Long get() = transfer.dateMillis
        override val amount: Double get() = transfer.amount
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesAndSearchScreen(
    transactions: List<TransactionEntity>,
    transfers: List<TransferEntity> = emptyList(),
    vaults: List<VaultEntity> = emptyList(),
    searchQuery: String,
    selectedCategory: String?,
    selectedType: String?,
    currency: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectType: (String?) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onDeleteTransfer: (TransferEntity) -> Unit = {},
    onOpenAddExpense: () -> Unit,
    onOpenAddIncome: () -> Unit = {},
    onOpenTransfer: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Type Filter: "ALL", "EXPENSE", "INCOME", "TRANSFER"
    var activeFilterType by remember { mutableStateOf(selectedType ?: "ALL") }
    var selectedDateRange by remember { mutableStateOf("ALL") } // "ALL", "THIS_MONTH", "7D", "30D"
    var selectedSortOrder by remember { mutableStateOf("NEWEST") } // "NEWEST", "OLDEST", "HIGHEST", "LOWEST"
    var filterVault by remember { mutableStateOf<String?>(null) }
    var showAdvancedFilters by remember { mutableStateOf(false) }

    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }

    viewingReceiptPath?.let { path ->
        com.example.ui.utils.ReceiptImageViewerDialog(
            imagePathOrUri = path,
            onDismiss = { viewingReceiptPath = null }
        )
    }

    // Combine transactions and transfers into unified list
    val unifiedItems = remember(transactions, transfers, activeFilterType, searchQuery, selectedDateRange, selectedSortOrder, selectedCategory, filterVault) {
        val now = System.currentTimeMillis()
        val minDate = when (selectedDateRange) {
            "7D" -> now - (7 * 86400000L)
            "30D" -> now - (30 * 86400000L)
            "THIS_MONTH" -> {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            else -> 0L
        }

        val txItems = if (activeFilterType == "TRANSFER") emptyList() else {
            transactions.filter { tx ->
                val typeMatch = when (activeFilterType) {
                    "INCOME" -> tx.type == "INCOME"
                    "EXPENSE" -> tx.type == "EXPENSE"
                    else -> true
                }
                val dateMatch = tx.dateMillis >= minDate
                val catMatch = selectedCategory == null || tx.category == selectedCategory
                val vaultMatch = filterVault == null || tx.vaultName == filterVault
                val searchMatch = if (searchQuery.isBlank()) true else {
                    tx.description.contains(searchQuery, ignoreCase = true) ||
                            tx.category.contains(searchQuery, ignoreCase = true) ||
                            tx.amount.toString().contains(searchQuery) ||
                            tx.vaultName.contains(searchQuery, ignoreCase = true)
                }
                typeMatch && dateMatch && catMatch && vaultMatch && searchMatch
            }.map { UnifiedFinanceItem.TxItem(it) }
        }

        val transferItems = if (activeFilterType == "INCOME" || activeFilterType == "EXPENSE") emptyList() else {
            transfers.filter { tr ->
                val dateMatch = tr.dateMillis >= minDate
                val vaultMatch = filterVault == null || tr.fromVaultName == filterVault || tr.toVaultName == filterVault
                val searchMatch = if (searchQuery.isBlank()) true else {
                    tr.fromVaultName.contains(searchQuery, ignoreCase = true) ||
                            tr.toVaultName.contains(searchQuery, ignoreCase = true) ||
                            tr.notes.contains(searchQuery, ignoreCase = true) ||
                            tr.amount.toString().contains(searchQuery)
                }
                dateMatch && vaultMatch && searchMatch
            }.map { UnifiedFinanceItem.TransferItem(it) }
        }

        val all = txItems + transferItems
        when (selectedSortOrder) {
            "OLDEST" -> all.sortedBy { it.dateMillis }
            "HIGHEST" -> all.sortedByDescending { it.amount }
            "LOWEST" -> all.sortedBy { it.amount }
            else -> all.sortedByDescending { it.dateMillis }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. TOP HEADER
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
                        text = "العمليات والبحث",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "سجل الحركات المالية المتقدم",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. TYPE FILTER BAR (الكل | مصروفات | دخل | تحويلات)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MintBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "ALL" to "الكل",
                        "EXPENSE" to "مصروفات",
                        "INCOME" to "دخل",
                        "TRANSFER" to "تحويلات"
                    ).forEach { (typeKey, label) ->
                        val isSelected = activeFilterType == typeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .then(
                                    if (isSelected) Modifier.border(1.dp, CardBorderColor, RoundedCornerShape(20.dp))
                                    else Modifier
                                )
                                .clickable {
                                    activeFilterType = typeKey
                                    onSelectType(if (typeKey == "ALL") null else typeKey)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // 3. SEARCH BAR + ADVANCED FILTERS TOGGLE
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(23.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "بحث بالوصف، الخزنة، التصنيف، أو المبلغ...",
                                        fontSize = 12.sp,
                                        color = Color.Gray.copy(alpha = 0.75f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Filter Toggle Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (showAdvancedFilters) EmeraldGreenPrimary else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (showAdvancedFilters) EmeraldGreenPrimary else CardBorderColor, CircleShape)
                        .clickable { showAdvancedFilters = !showAdvancedFilters },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "فلاتر متقدمة",
                        tint = if (showAdvancedFilters) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Quick Add (+) Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreenPrimary)
                        .clickable {
                            when (activeFilterType) {
                                "INCOME" -> onOpenAddIncome()
                                "TRANSFER" -> onOpenTransfer()
                                else -> onOpenAddExpense()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 4. ADVANCED FILTERS SECTION (Collapsible)
        if (showAdvancedFilters) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("النطاق الزمني", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "ALL" to "كل الأوقات",
                                "THIS_MONTH" to "هذا الشهر",
                                "7D" to "آخر 7 أيام",
                                "30D" to "آخر 30 يوم"
                            ).forEach { (k, label) ->
                                FilterChip(
                                    selected = selectedDateRange == k,
                                    onClick = { selectedDateRange = k },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("الترتيب حسب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "NEWEST" to "الأحدث",
                                "OLDEST" to "الأقدم",
                                "HIGHEST" to "الأعلى قيمة",
                                "LOWEST" to "الأقل قيمة"
                            ).forEach { (k, label) ->
                                FilterChip(
                                    selected = selectedSortOrder == k,
                                    onClick = { selectedSortOrder = k },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        if (vaults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("الخزنة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = filterVault == null,
                                    onClick = { filterVault = null },
                                    label = { Text("جميع الخزن", fontSize = 11.sp) }
                                )
                                vaults.forEach { vault ->
                                    FilterChip(
                                        selected = filterVault == vault.name,
                                        onClick = { filterVault = if (filterVault == vault.name) null else vault.name },
                                        label = { Text(vault.name, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. UNIFIED ITEMS LIST AREA
        if (unifiedItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد نتائج مطابقة لبحثك أو الفلاتر المحددة",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(unifiedItems) { item ->
                when (item) {
                    is UnifiedFinanceItem.TxItem -> {
                        TransactionRowItemLocal(
                            tx = item.tx,
                            currency = currency,
                            onDelete = { onDeleteTransaction(item.tx) },
                            onViewReceipt = { path -> viewingReceiptPath = path }
                        )
                    }
                    is UnifiedFinanceItem.TransferItem -> {
                        TransferRowItemLocal(
                            transfer = item.transfer,
                            currency = currency,
                            onDelete = { onDeleteTransfer(item.transfer) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description.ifEmpty { tx.category },
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
                fontSize = 13.5.sp,
                color = if (isIncome) IncomeGreen else ExpenseRed
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TransferRowItemLocal(
    transfer: TransferEntity,
    currency: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(transfer.dateMillis) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            sdf.format(Date(transfer.dateMillis))
        } catch (e: Exception) {
            ""
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    .background(EmeraldGreenPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "تحويل",
                    tint = EmeraldGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${transfer.fromVaultName} ← ${transfer.toVaultName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "تحويل بين الخزن",
                        fontSize = 11.sp,
                        color = EmeraldGreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (transfer.notes.isNotBlank()) {
                        Text(text = " • ", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = transfer.notes,
                            fontSize = 10.5.sp,
                            color = Color.Gray
                        )
                    }
                    if (formattedDate.isNotEmpty()) {
                        Text(text = " • ", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = formattedDate,
                            fontSize = 10.5.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Text(
                text = "${String.format(Locale.US, "%,.0f", transfer.amount)} $currency",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                color = EmeraldGreenPrimary
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
