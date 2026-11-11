package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransferEntity
import com.example.data.entity.VaultEntity
import com.example.data.preferences.ModuleConfiguration
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LightBackground
import com.example.ui.theme.MintBackground
import com.example.ui.utils.AppStrings
import com.example.ui.utils.AppText
import com.example.ui.utils.CategoryUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ExpenseSubTabs {
    const val EXPENSES_LOG = "EXPENSES_LOG"
    const val COMMITMENTS = "COMMITMENTS"
    const val LESSONS = "LESSONS"
}

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

private data class ExpenseSubTabSpec(
    val key: String,
    val label: String,
    val badgeCount: Int,
    val icon: ImageVector
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesAndSearchScreen(
    transactions: List<TransactionEntity>,
    transfers: List<TransferEntity> = emptyList(),
    vaults: List<VaultEntity> = emptyList(),
    commitments: List<CommitmentEntity> = emptyList(),
    lessons: List<ChildLessonEntity> = emptyList(),
    config: ModuleConfiguration = ModuleConfiguration(),
    searchQuery: String,
    selectedCategory: String?,
    selectedType: String?,
    selectedExpenseSubTab: String = ExpenseSubTabs.EXPENSES_LOG,
    currency: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectType: (String?) -> Unit,
    onSelectExpenseSubTab: (String) -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onDeleteTransfer: (TransferEntity) -> Unit = {},
    onPayCommitment: (CommitmentEntity) -> Unit = {},
    onPayLesson: (ChildLessonEntity) -> Unit = {},
    onDeleteCommitment: (Int) -> Unit = {},
    onDeleteLesson: (Int) -> Unit = {},
    onOpenAddExpense: () -> Unit,
    onOpenAddIncome: () -> Unit = {},
    onOpenTransfer: () -> Unit = {},
    onOpenAddCommitment: () -> Unit = {},
    onOpenAddLesson: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Type Filter: "ALL", "EXPENSE", "INCOME", "TRANSFER"
    var activeFilterType by remember(selectedType) { mutableStateOf(selectedType ?: "ALL") }
    var activeSubTabState by remember(selectedExpenseSubTab) { mutableStateOf(selectedExpenseSubTab) }

    // Ensure selected sub-tab stays valid when module flags change
    val resolvedExpenseSubTab = when {
        activeSubTabState == ExpenseSubTabs.COMMITMENTS && config.commitments -> ExpenseSubTabs.COMMITMENTS
        activeSubTabState == ExpenseSubTabs.LESSONS && config.lessons -> ExpenseSubTabs.LESSONS
        else -> ExpenseSubTabs.EXPENSES_LOG
    }

    var selectedDateRange by remember { mutableStateOf("ALL") } // "ALL", "THIS_MONTH", "7D", "30D"
    var selectedSortOrder by remember { mutableStateOf("NEWEST") } // "NEWEST", "OLDEST", "HIGHEST", "LOWEST"
    var filterVault by remember { mutableStateOf<String?>(null) }
    var showAdvancedFilters by remember { mutableStateOf(false) }

    // Sub-tab status filters ("ALL", "UNPAID", "PAID")
    var commitmentsStatusFilter by remember { mutableStateOf("ALL") }
    var lessonsStatusFilter by remember { mutableStateOf("ALL") }

    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }

    viewingReceiptPath?.let { path ->
        com.example.ui.utils.ReceiptImageViewerDialog(
            imagePathOrUri = path,
            onDismiss = { viewingReceiptPath = null }
        )
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val softContainerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else MintBackground
    val subtleCardColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else LightBackground
    val subtleBorderColor = if (isDarkTheme) MaterialTheme.colorScheme.outlineVariant else CardBorderColor
    val selectedSegmentTextColor = if (isDarkTheme) Color.White else EmeraldGreenDark

    val unpaidCommitmentsCount = remember(commitments, config.commitments) {
        if (config.commitments) commitments.count { !it.isPaid } else 0
    }
    val unpaidLessonsCount = remember(lessons, config.lessons) {
        if (config.lessons) lessons.count { !it.isPaid } else 0
    }

    val unpaidCommitmentsSum = remember(commitments, config.commitments) {
        if (config.commitments) commitments.filter { !it.isPaid }.sumOf { it.amount } else 0.0
    }
    val unpaidLessonsSum = remember(lessons, config.lessons) {
        if (config.lessons) lessons.filter { !it.isPaid }.sumOf { it.amount } else 0.0
    }

    val recordedExpensesThisMonth = remember(transactions) {
        val nowCal = Calendar.getInstance()
        val curMonth = nowCal.get(Calendar.MONTH)
        val curYear = nowCal.get(Calendar.YEAR)
        transactions.filter { tx ->
            if (tx.type != "EXPENSE") return@filter false
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            cal.get(Calendar.MONTH) == curMonth && cal.get(Calendar.YEAR) == curYear
        }.sumOf { it.amount }
    }

    val minDateMillis = remember(selectedDateRange) {
        val now = System.currentTimeMillis()
        when (selectedDateRange) {
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
    }

    // Unified transactions/transfers list for All / Expenses Log / Income / Transfers
    val unifiedItems = remember(
        AppText.language,
        transactions,
        transfers,
        activeFilterType,
        searchQuery,
        minDateMillis,
        selectedSortOrder,
        selectedCategory,
        filterVault
    ) {
        val txItems = if (activeFilterType == "TRANSFER") emptyList() else {
            transactions.filter { tx ->
                val typeMatch = when (activeFilterType) {
                    "INCOME" -> tx.type == "INCOME"
                    "EXPENSE" -> tx.type == "EXPENSE"
                    else -> true
                }
                val dateMatch = tx.dateMillis >= minDateMillis
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
                val dateMatch = tr.dateMillis >= minDateMillis
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

    val filteredCommitments = remember(commitments, commitmentsStatusFilter, searchQuery, selectedSortOrder, minDateMillis) {
        val base = commitments.filter { item ->
            val statusMatch = when (commitmentsStatusFilter) {
                "UNPAID" -> !item.isPaid
                "PAID" -> item.isPaid
                else -> true
            }
            val dateMatch = item.dueDateMillis >= minDateMillis
            val queryMatch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                    item.notes.contains(searchQuery, ignoreCase = true) ||
                    item.amount.toString().contains(searchQuery)
            }
            statusMatch && dateMatch && queryMatch
        }
        when (selectedSortOrder) {
            "OLDEST" -> base.sortedBy { it.dueDateMillis }
            "HIGHEST" -> base.sortedByDescending { it.amount }
            "LOWEST" -> base.sortedBy { it.amount }
            else -> base.sortedWith(compareBy<CommitmentEntity> { it.isPaid }.thenByDescending { it.dueDateMillis })
        }
    }

    val filteredLessons = remember(lessons, lessonsStatusFilter, searchQuery, selectedSortOrder, minDateMillis) {
        val base = lessons.filter { lesson ->
            val statusMatch = when (lessonsStatusFilter) {
                "UNPAID" -> !lesson.isPaid
                "PAID" -> lesson.isPaid
                else -> true
            }
            val dateMatch = lesson.dueDateMillis >= minDateMillis
            val queryMatch = if (searchQuery.isBlank()) true else {
                lesson.childName.contains(searchQuery, ignoreCase = true) ||
                    lesson.subject.contains(searchQuery, ignoreCase = true) ||
                    lesson.teacherName.contains(searchQuery, ignoreCase = true) ||
                    lesson.amount.toString().contains(searchQuery)
            }
            statusMatch && dateMatch && queryMatch
        }
        when (selectedSortOrder) {
            "OLDEST" -> base.sortedBy { it.dueDateMillis }
            "HIGHEST" -> base.sortedByDescending { it.amount }
            "LOWEST" -> base.sortedBy { it.amount }
            else -> base.sortedWith(compareBy<ChildLessonEntity> { it.isPaid }.thenByDescending { it.dueDateMillis })
        }
    }

    val expenseSubTabs = buildList {
        add(
            ExpenseSubTabSpec(
                key = ExpenseSubTabs.EXPENSES_LOG,
                label = AppText.text(com.example.R.string.label_ops_subtab_expenses),
                badgeCount = 0,
                icon = Icons.AutoMirrored.Filled.ReceiptLong
            )
        )
        if (config.commitments) {
            add(
                ExpenseSubTabSpec(
                    key = ExpenseSubTabs.COMMITMENTS,
                    label = AppText.text(com.example.R.string.label_ops_subtab_commitments),
                    badgeCount = unpaidCommitmentsCount,
                    icon = Icons.AutoMirrored.Filled.EventNote
                )
            )
        }
        if (config.lessons) {
            add(
                ExpenseSubTabSpec(
                    key = ExpenseSubTabs.LESSONS,
                    label = AppText.text(com.example.R.string.label_ops_subtab_lessons),
                    badgeCount = unpaidLessonsCount,
                    icon = Icons.Default.School
                )
            )
        }
    }

    val searchPlaceholderText = when {
        activeFilterType == "EXPENSE" && resolvedExpenseSubTab == ExpenseSubTabs.COMMITMENTS ->
            AppText.text(com.example.R.string.label_ops_search_commitments)
        activeFilterType == "EXPENSE" && resolvedExpenseSubTab == ExpenseSubTabs.LESSONS ->
            AppText.text(com.example.R.string.label_ops_search_lessons)
        else -> AppText.text(com.example.R.string.label_ops_search_expenses)
    }

    val addButtonLabel = when {
        activeFilterType == "EXPENSE" && resolvedExpenseSubTab == ExpenseSubTabs.COMMITMENTS ->
            AppText.text(com.example.R.string.label_ops_add_commitment)
        activeFilterType == "EXPENSE" && resolvedExpenseSubTab == ExpenseSubTabs.LESSONS ->
            AppText.text(com.example.R.string.label_ops_add_lesson)
        activeFilterType == "INCOME" -> AppText.text(com.example.R.string.label_ops_add_income)
        activeFilterType == "TRANSFER" -> AppText.text(com.example.R.string.label_ops_add_transfer)
        else -> AppText.text(com.example.R.string.label_ops_add_expense)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. TOP HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppText.text(com.example.R.string.label_ops_screen_title),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = AppText.text(com.example.R.string.label_ops_screen_subtitle),
                            fontSize = 11.5.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, subtleBorderColor, CircleShape)
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, subtleBorderColor, CircleShape)
                                .clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = AppText.text(com.example.R.string.text_90b6c869a171),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. PRIMARY TYPE SEGMENT BAR (الكل | مصاريف | دخل | تحويلات)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = softContainerColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "ALL" to AppText.text(com.example.R.string.label_ops_tab_all),
                            "EXPENSE" to AppText.text(com.example.R.string.label_ops_tab_expenses),
                            "INCOME" to AppText.text(com.example.R.string.label_ops_tab_income),
                            "TRANSFER" to AppText.text(com.example.R.string.label_ops_tab_transfers)
                        ).forEach { (typeKey, label) ->
                            val isSelected = activeFilterType == typeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(19.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .then(
                                        if (isSelected) Modifier.border(1.dp, subtleBorderColor, RoundedCornerShape(19.dp))
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
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) selectedSegmentTextColor else Color.Gray,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // 3. EXPENSES SUB-NAVIGATION (سجل المصاريف | الالتزامات | دروس الأطفال)
            if (activeFilterType == "EXPENSE" && expenseSubTabs.size > 1) {
                item {
                    AdaptivePillRow(gap = 6.dp) {
                        expenseSubTabs.forEach { spec ->
                            val selected = resolvedExpenseSubTab == spec.key
                            Card(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        activeSubTabState = spec.key
                                        onSelectExpenseSubTab(spec.key)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) EmeraldGreenPrimary else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) EmeraldGreenPrimary else subtleBorderColor
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = spec.icon,
                                        contentDescription = null,
                                        tint = if (selected) Color.White else EmeraldGreenPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = spec.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    if (spec.badgeCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .sizeIn(minWidth = 17.dp, minHeight = 17.dp)
                                                .clip(CircleShape)
                                                .background(if (selected) Color.White.copy(alpha = 0.25f) else ExpenseRed.copy(alpha = 0.14f))
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${spec.badgeCount}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (selected) Color.White else ExpenseRed,
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

            // 4. SEARCH BAR + FILTER TOGGLE + CONTEXTUAL ADD BUTTON
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, subtleBorderColor, RoundedCornerShape(21.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    cursorBrush = SolidColor(EmeraldGreenPrimary),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = searchPlaceholderText,
                                                    fontSize = 11.5.sp,
                                                    color = Color.Gray.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (showAdvancedFilters) EmeraldGreenPrimary else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (showAdvancedFilters) EmeraldGreenPrimary else subtleBorderColor, CircleShape)
                            .clickable { showAdvancedFilters = !showAdvancedFilters },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = AppText.text(com.example.R.string.text_fb8387b2d570),
                            tint = if (showAdvancedFilters) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                activeFilterType == "EXPENSE" && resolvedExpenseSubTab == ExpenseSubTabs.COMMITMENTS ->
                                    onOpenAddCommitment()
                                activeFilterType == "EXPENSE" && resolvedExpenseSubTab == ExpenseSubTabs.LESSONS ->
                                    onOpenAddLesson()
                                activeFilterType == "INCOME" -> onOpenAddIncome()
                                activeFilterType == "TRANSFER" -> onOpenTransfer()
                                else -> onOpenAddExpense()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(21.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = AppText.text(com.example.R.string.text_5e3a3fdfce20),
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = addButtonLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Active category chip indicator when filtered externally
            if (selectedCategory != null && !(activeFilterType == "EXPENSE" && resolvedExpenseSubTab != ExpenseSubTabs.EXPENSES_LOG)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = true,
                            onClick = { onSelectCategory(null) },
                            label = {
                                Text(
                                    text = AppStrings(AppText.language).translateCategory(selectedCategory),
                                    fontSize = 11.sp
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            // 5. COLLAPSIBLE ADVANCED FILTERS
            if (showAdvancedFilters) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, subtleBorderColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = AppText.text(com.example.R.string.text_8d23c3965527),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    "ALL" to AppText.text(com.example.R.string.text_d38aa97bb96e),
                                    "THIS_MONTH" to AppText.text(com.example.R.string.text_b7523863599a),
                                    "7D" to AppText.text(com.example.R.string.text_349de9cf3c2d),
                                    "30D" to AppText.text(com.example.R.string.text_c94c6c1e0911)
                                ).forEach { (k, label) ->
                                    FilterChip(
                                        selected = selectedDateRange == k,
                                        onClick = { selectedDateRange = k },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = AppText.text(com.example.R.string.text_cc23678b3cbc),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    "NEWEST" to AppText.text(com.example.R.string.text_36299595d69c),
                                    "OLDEST" to AppText.text(com.example.R.string.text_e69ddb8fafca),
                                    "HIGHEST" to AppText.text(com.example.R.string.text_42b76568a25e),
                                    "LOWEST" to AppText.text(com.example.R.string.text_81ae1f96d293)
                                ).forEach { (k, label) ->
                                    FilterChip(
                                        selected = selectedSortOrder == k,
                                        onClick = { selectedSortOrder = k },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }

                            val isOnBillsOrLessons = activeFilterType == "EXPENSE" &&
                                (resolvedExpenseSubTab == ExpenseSubTabs.COMMITMENTS || resolvedExpenseSubTab == ExpenseSubTabs.LESSONS)

                            if (vaults.isNotEmpty() && !isOnBillsOrLessons) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = AppText.text(com.example.R.string.text_9650779e9a22),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilterChip(
                                        selected = filterVault == null,
                                        onClick = { filterVault = null },
                                        label = { Text(AppText.text(com.example.R.string.text_5db167239353), fontSize = 11.sp) }
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

            // 6. MAIN TAB CONTENT
            if (activeFilterType == "EXPENSE" && resolvedExpenseSubTab == ExpenseSubTabs.COMMITMENTS) {
                val totalCommitments = commitments.sumOf { it.amount }
                val paidCommitments = commitments.filter { it.isPaid }.sumOf { it.amount }
                val remCommitments = totalCommitments - paidCommitments
                val progress = if (totalCommitments > 0.0) (paidCommitments / totalCommitments).toFloat().coerceIn(0f, 1f) else 0f
                val paidCount = commitments.count { it.isPaid }
                val unpaidCount = commitments.count { !it.isPaid }

                if (isWideScreen) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(0.42f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OperationsProgressSummaryCard(
                                    title = AppText.text(com.example.R.string.label_ops_commitments_summary),
                                    totalVal = "${String.format(Locale.US, "%,.0f", totalCommitments)} ${AppText.currency(currency)}",
                                    paidVal = "${String.format(Locale.US, "%,.0f", paidCommitments)} ${AppText.currency(currency)}",
                                    remVal = "${String.format(Locale.US, "%,.0f", remCommitments)} ${AppText.currency(currency)}",
                                    progress = progress,
                                    miniCardColor = subtleCardColor,
                                    trackColor = softContainerColor,
                                    borderColor = subtleBorderColor
                                )
                                OperationsStatusChipsRow(
                                    selectedStatus = commitmentsStatusFilter,
                                    unpaidCount = unpaidCount,
                                    paidCount = paidCount,
                                    onSelectStatus = { commitmentsStatusFilter = it },
                                    softContainerColor = softContainerColor,
                                    selectedTextColor = selectedSegmentTextColor,
                                    borderColor = subtleBorderColor
                                )
                            }
                            Column(
                                modifier = Modifier.weight(0.58f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (filteredCommitments.isEmpty()) {
                                    OperationsEmptyActionCard(
                                        message = AppText.text(com.example.R.string.label_ops_no_commitments),
                                        actionLabel = if (commitments.isEmpty()) AppText.text(com.example.R.string.label_ops_add_commitment) else null,
                                        onAction = onOpenAddCommitment,
                                        borderColor = subtleBorderColor
                                    )
                                } else {
                                    filteredCommitments.forEach { commitment ->
                                        CommitmentOperationCard(
                                            item = commitment,
                                            currency = currency,
                                            softContainerColor = softContainerColor,
                                            selectedTextColor = selectedSegmentTextColor,
                                            borderColor = subtleBorderColor,
                                            onPay = { onPayCommitment(commitment) },
                                            onDelete = { onDeleteCommitment(commitment.id) },
                                            onViewReceipt = { viewingReceiptPath = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        OperationsProgressSummaryCard(
                            title = AppText.text(com.example.R.string.label_ops_commitments_summary),
                            totalVal = "${String.format(Locale.US, "%,.0f", totalCommitments)} ${AppText.currency(currency)}",
                            paidVal = "${String.format(Locale.US, "%,.0f", paidCommitments)} ${AppText.currency(currency)}",
                            remVal = "${String.format(Locale.US, "%,.0f", remCommitments)} ${AppText.currency(currency)}",
                            progress = progress,
                            miniCardColor = subtleCardColor,
                            trackColor = softContainerColor,
                            borderColor = subtleBorderColor
                        )
                    }
                    item {
                        OperationsStatusChipsRow(
                            selectedStatus = commitmentsStatusFilter,
                            unpaidCount = unpaidCount,
                            paidCount = paidCount,
                            onSelectStatus = { commitmentsStatusFilter = it },
                            softContainerColor = softContainerColor,
                            selectedTextColor = selectedSegmentTextColor,
                            borderColor = subtleBorderColor
                        )
                    }
                    if (filteredCommitments.isEmpty()) {
                        item {
                            OperationsEmptyActionCard(
                                message = AppText.text(com.example.R.string.label_ops_no_commitments),
                                actionLabel = if (commitments.isEmpty()) AppText.text(com.example.R.string.label_ops_add_commitment) else null,
                                onAction = onOpenAddCommitment,
                                borderColor = subtleBorderColor
                            )
                        }
                    } else {
                        items(filteredCommitments, key = { "commitment_${it.id}" }) { commitment ->
                            CommitmentOperationCard(
                                item = commitment,
                                currency = currency,
                                softContainerColor = softContainerColor,
                                selectedTextColor = selectedSegmentTextColor,
                                borderColor = subtleBorderColor,
                                onPay = { onPayCommitment(commitment) },
                                onDelete = { onDeleteCommitment(commitment.id) },
                                onViewReceipt = { viewingReceiptPath = it }
                            )
                        }
                    }
                }
            } else if (activeFilterType == "EXPENSE" && resolvedExpenseSubTab == ExpenseSubTabs.LESSONS) {
                val totalLessons = lessons.sumOf { it.amount }
                val paidLessons = lessons.filter { it.isPaid }.sumOf { it.amount }
                val remLessons = totalLessons - paidLessons
                val progress = if (totalLessons > 0.0) (paidLessons / totalLessons).toFloat().coerceIn(0f, 1f) else 0f
                val paidCount = lessons.count { it.isPaid }
                val unpaidCount = lessons.count { !it.isPaid }

                if (isWideScreen) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(0.42f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OperationsProgressSummaryCard(
                                    title = AppText.text(com.example.R.string.label_ops_lessons_summary),
                                    totalVal = "${String.format(Locale.US, "%,.0f", totalLessons)} ${AppText.currency(currency)}",
                                    paidVal = "${String.format(Locale.US, "%,.0f", paidLessons)} ${AppText.currency(currency)}",
                                    remVal = "${String.format(Locale.US, "%,.0f", remLessons)} ${AppText.currency(currency)}",
                                    progress = progress,
                                    miniCardColor = subtleCardColor,
                                    trackColor = softContainerColor,
                                    borderColor = subtleBorderColor
                                )
                                OperationsStatusChipsRow(
                                    selectedStatus = lessonsStatusFilter,
                                    unpaidCount = unpaidCount,
                                    paidCount = paidCount,
                                    onSelectStatus = { lessonsStatusFilter = it },
                                    softContainerColor = softContainerColor,
                                    selectedTextColor = selectedSegmentTextColor,
                                    borderColor = subtleBorderColor
                                )
                            }
                            Column(
                                modifier = Modifier.weight(0.58f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (filteredLessons.isEmpty()) {
                                    OperationsEmptyActionCard(
                                        message = AppText.text(com.example.R.string.label_ops_no_lessons),
                                        actionLabel = if (lessons.isEmpty()) AppText.text(com.example.R.string.label_ops_add_lesson) else null,
                                        onAction = onOpenAddLesson,
                                        borderColor = subtleBorderColor
                                    )
                                } else {
                                    filteredLessons.forEach { lesson ->
                                        LessonOperationCard(
                                            lesson = lesson,
                                            currency = currency,
                                            borderColor = subtleBorderColor,
                                            onPay = { onPayLesson(lesson) },
                                            onDelete = { onDeleteLesson(lesson.id) },
                                            onViewReceipt = { viewingReceiptPath = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        OperationsProgressSummaryCard(
                            title = AppText.text(com.example.R.string.label_ops_lessons_summary),
                            totalVal = "${String.format(Locale.US, "%,.0f", totalLessons)} ${AppText.currency(currency)}",
                            paidVal = "${String.format(Locale.US, "%,.0f", paidLessons)} ${AppText.currency(currency)}",
                            remVal = "${String.format(Locale.US, "%,.0f", remLessons)} ${AppText.currency(currency)}",
                            progress = progress,
                            miniCardColor = subtleCardColor,
                            trackColor = softContainerColor,
                            borderColor = subtleBorderColor
                        )
                    }
                    item {
                        OperationsStatusChipsRow(
                            selectedStatus = lessonsStatusFilter,
                            unpaidCount = unpaidCount,
                            paidCount = paidCount,
                            onSelectStatus = { lessonsStatusFilter = it },
                            softContainerColor = softContainerColor,
                            selectedTextColor = selectedSegmentTextColor,
                            borderColor = subtleBorderColor
                        )
                    }
                    if (filteredLessons.isEmpty()) {
                        item {
                            OperationsEmptyActionCard(
                                message = AppText.text(com.example.R.string.label_ops_no_lessons),
                                actionLabel = if (lessons.isEmpty()) AppText.text(com.example.R.string.label_ops_add_lesson) else null,
                                onAction = onOpenAddLesson,
                                borderColor = subtleBorderColor
                            )
                        }
                    } else {
                        items(filteredLessons, key = { "lesson_${it.id}" }) { lesson ->
                            LessonOperationCard(
                                lesson = lesson,
                                currency = currency,
                                borderColor = subtleBorderColor,
                                onPay = { onPayLesson(lesson) },
                                onDelete = { onDeleteLesson(lesson.id) },
                                onViewReceipt = { viewingReceiptPath = it }
                            )
                        }
                    }
                }
            } else {
                // EXPENSES LOG (or ALL / INCOME / TRANSFER)
                if (activeFilterType == "EXPENSE") {
                    val curStr = AppText.currency(currency)
                    val totalMonthlyOutflow = recordedExpensesThisMonth + unpaidCommitmentsSum + unpaidLessonsSum
                    val unpaidCountTotal = unpaidCommitmentsCount + unpaidLessonsCount
                    val unpaidSumTotal = unpaidCommitmentsSum + unpaidLessonsSum

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, subtleBorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = AppText.text(com.example.R.string.label_ops_monthly_expenses_summary),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", totalMonthlyOutflow)} $curStr",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = ExpenseRed
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OperationsSummaryMiniCard(
                                        label = AppText.text(com.example.R.string.label_ops_recorded_expenses),
                                        value = "${String.format(Locale.US, "%,.0f", recordedExpensesThisMonth)} $curStr",
                                        tint = ExpenseRed,
                                        containerColor = subtleCardColor,
                                        borderColor = subtleBorderColor,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                    if (config.commitments) {
                                        OperationsSummaryMiniCard(
                                            label = AppText.text(com.example.R.string.label_ops_due_commitments),
                                            value = "${String.format(Locale.US, "%,.0f", unpaidCommitmentsSum)} $curStr",
                                            tint = Color(0xFFD97706),
                                            containerColor = subtleCardColor,
                                            borderColor = subtleBorderColor,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    activeSubTabState = ExpenseSubTabs.COMMITMENTS
                                                    onSelectExpenseSubTab(ExpenseSubTabs.COMMITMENTS)
                                                }
                                        )
                                    }
                                    if (config.lessons) {
                                        OperationsSummaryMiniCard(
                                            label = AppText.text(com.example.R.string.label_ops_due_lessons),
                                            value = "${String.format(Locale.US, "%,.0f", unpaidLessonsSum)} $curStr",
                                            tint = Color(0xFF4F46E5),
                                            containerColor = subtleCardColor,
                                            borderColor = subtleBorderColor,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    activeSubTabState = ExpenseSubTabs.LESSONS
                                                    onSelectExpenseSubTab(ExpenseSubTabs.LESSONS)
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (unpaidCountTotal > 0) {
                        item {
                            val bannerBg = if (isDarkTheme) Color(0xFF3B2F13) else Color(0xFFFFF8E6)
                            val bannerBorder = if (isDarkTheme) Color(0xFF6B5320) else Color(0xFFF3E2B3)
                            val bannerFg = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFF92400E)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        val target = if (unpaidCommitmentsCount > 0 && config.commitments) {
                                            ExpenseSubTabs.COMMITMENTS
                                        } else {
                                            ExpenseSubTabs.LESSONS
                                        }
                                        activeSubTabState = target
                                        onSelectExpenseSubTab(target)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = bannerBg),
                                border = BorderStroke(1.dp, bannerBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = AppText.text(
                                                com.example.R.string.label_ops_unpaid_banner,
                                                unpaidCountTotal,
                                                String.format(Locale.US, "%,.0f", unpaidSumTotal),
                                                curStr
                                            ),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = bannerFg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppText.text(com.example.R.string.label_ops_view_action),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreenPrimary,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // UNIFIED TRANSACTIONS / TRANSFERS ITEMS
                if (unifiedItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, subtleBorderColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_93fe23db91f1),
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    items(
                        unifiedItems,
                        key = { item ->
                            when (item) {
                                is UnifiedFinanceItem.TxItem -> "tx_${item.id}"
                                is UnifiedFinanceItem.TransferItem -> "tr_${item.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is UnifiedFinanceItem.TxItem -> {
                                TransactionRowItemLocal(
                                    tx = item.tx,
                                    currency = currency,
                                    borderColor = subtleBorderColor,
                                    onDelete = { onDeleteTransaction(item.tx) },
                                    onViewReceipt = { path -> viewingReceiptPath = path }
                                )
                            }
                            is UnifiedFinanceItem.TransferItem -> {
                                TransferRowItemLocal(
                                    transfer = item.transfer,
                                    currency = currency,
                                    borderColor = subtleBorderColor,
                                    onDelete = { onDeleteTransfer(item.transfer) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdaptivePillRow(
    gap: Dp = 6.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidthPx = constraints.maxWidth
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Layout(content = content) { measurables, _ ->
                val gapPx = gap.roundToPx()
                val naturalWidths = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
                val totalGapPx = gapPx * (measurables.size - 1).coerceAtLeast(0)
                val sumNaturalWidth = naturalWidths.sum() + totalGapPx

                val placeables = if (sumNaturalWidth < availableWidthPx && measurables.isNotEmpty()) {
                    val extraPerItem = (availableWidthPx - sumNaturalWidth) / measurables.size
                    val remainder = (availableWidthPx - sumNaturalWidth) % measurables.size
                    measurables.mapIndexed { idx, measurable ->
                        val w = naturalWidths[idx] + extraPerItem + if (idx < remainder) 1 else 0
                        measurable.measure(Constraints.fixedWidth(w))
                    }
                } else {
                    val loose = Constraints(
                        minWidth = 0,
                        maxWidth = Constraints.Infinity,
                        minHeight = 0,
                        maxHeight = Constraints.Infinity
                    )
                    measurables.map { it.measure(loose) }
                }

                val layoutWidth = maxOf(
                    availableWidthPx,
                    placeables.sumOf { it.width } + totalGapPx
                )
                val layoutHeight = placeables.maxOfOrNull { it.height } ?: 0

                layout(layoutWidth, layoutHeight) {
                    var x = 0
                    placeables.forEach { placeable ->
                        placeable.placeRelative(x, 0)
                        x += placeable.width + gapPx
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationsSummaryMiniCard(
    label: String,
    value: String,
    tint: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OperationsProgressSummaryCard(
    title: String,
    totalVal: String,
    paidVal: String,
    remVal: String,
    progress: Float,
    miniCardColor: Color,
    trackColor: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = EmeraldGreenPrimary
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = EmeraldGreenPrimary,
                trackColor = trackColor
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OperationsSummaryMiniCard(
                    label = AppText.text(com.example.R.string.label_ops_summary_total),
                    value = totalVal,
                    tint = MaterialTheme.colorScheme.onSurface,
                    containerColor = miniCardColor,
                    borderColor = borderColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                OperationsSummaryMiniCard(
                    label = AppText.text(com.example.R.string.label_ops_summary_paid),
                    value = paidVal,
                    tint = IncomeGreen,
                    containerColor = miniCardColor,
                    borderColor = borderColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                OperationsSummaryMiniCard(
                    label = AppText.text(com.example.R.string.label_ops_summary_remaining),
                    value = remVal,
                    tint = ExpenseRed,
                    containerColor = miniCardColor,
                    borderColor = borderColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun OperationsStatusChipsRow(
    selectedStatus: String,
    unpaidCount: Int,
    paidCount: Int,
    onSelectStatus: (String) -> Unit,
    softContainerColor: Color,
    selectedTextColor: Color,
    borderColor: Color
) {
    val total = unpaidCount + paidCount
    val options = listOf(
        "ALL" to AppText.text(com.example.R.string.label_ops_filter_all_count, total),
        "UNPAID" to AppText.text(com.example.R.string.label_ops_filter_unpaid_count, unpaidCount),
        "PAID" to AppText.text(com.example.R.string.label_ops_filter_paid_count, paidCount)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val sel = selectedStatus == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) softContainerColor else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (sel) EmeraldGreenPrimary else borderColor, RoundedCornerShape(16.dp))
                    .clickable { onSelectStatus(key) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    color = if (sel) selectedTextColor else Color.Gray,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun OperationsEmptyActionCard(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    borderColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            if (!actionLabel.isNullOrBlank()) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReceiptShortChip(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EmeraldGreenPrimary.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            tint = EmeraldGreenPrimary,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = AppText.text(com.example.R.string.label_ops_receipt_short),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldGreenPrimary,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommitmentOperationCard(
    item: CommitmentEntity,
    currency: String,
    softContainerColor: Color,
    selectedTextColor: Color,
    borderColor: Color,
    onPay: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: (String) -> Unit
) {
    val sdf = remember(AppText.language) { SimpleDateFormat("dd MMM yyyy", AppText.locale) }
    val formattedDate = remember(AppText.language, item.dueDateMillis) { sdf.format(Date(item.dueDateMillis)) }
    val icon = CategoryUtils.getCategoryIcon(item.title)
    val accent = if (item.isPaid) IncomeGreen else ExpenseRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
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
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.title,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = AppText.text(com.example.R.string.text_cf950597f86b, formattedDate),
                    fontSize = 10.5.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.isPaid) {
                                AppText.text(com.example.R.string.text_a49bd76b1c26)
                            } else {
                                AppText.text(com.example.R.string.text_a30287e12410)
                            },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            softWrap = false
                        )
                    }
                    if (item.isRecurringMonthly) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(softContainerColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = AppText.text(com.example.R.string.label_ops_recurring_monthly),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = selectedTextColor,
                                softWrap = false
                            )
                        }
                    }
                    if (!item.receiptImagePath.isNullOrBlank()) {
                        ReceiptShortChip(
                            onClick = { onViewReceipt(item.receiptImagePath!!) }
                        )
                    }
                }
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", item.amount)} ${AppText.currency(currency)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = AppText.text(com.example.R.string.text_2d2bbdc2d694),
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (!item.isPaid) {
                    Button(
                        onClick = onPay,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = AppText.text(com.example.R.string.label_ops_pay_action),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonOperationCard(
    lesson: ChildLessonEntity,
    currency: String,
    borderColor: Color,
    onPay: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: (String) -> Unit
) {
    val sdf = remember(AppText.language) { SimpleDateFormat("dd MMM yyyy", AppText.locale) }
    val formattedDate = remember(AppText.language, lesson.dueDateMillis) { sdf.format(Date(lesson.dueDateMillis)) }
    val iconAccent = if (lesson.isPaid) IncomeGreen else EmeraldGreenPrimary
    val statusAccent = if (lesson.isPaid) IncomeGreen else ExpenseRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
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
                    .background(iconAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = AppText.text(com.example.R.string.text_edc05b12eab0),
                    tint = iconAccent,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "${lesson.childName} • ${lesson.subject}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = AppText.text(com.example.R.string.text_0b892ba3c6ad, lesson.teacherName, formattedDate),
                    fontSize = 10.5.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusAccent.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (lesson.isPaid) {
                                AppText.text(com.example.R.string.text_a49bd76b1c26)
                            } else {
                                AppText.text(com.example.R.string.text_a30287e12410)
                            },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusAccent,
                            softWrap = false
                        )
                    }
                    if (!lesson.receiptImagePath.isNullOrBlank()) {
                        ReceiptShortChip(
                            onClick = { onViewReceipt(lesson.receiptImagePath!!) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", lesson.amount)} ${AppText.currency(currency)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = AppText.text(com.example.R.string.text_2d2bbdc2d694),
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (!lesson.isPaid) {
                    Button(
                        onClick = onPay,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = AppText.text(com.example.R.string.label_ops_pay_action),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRowItemLocal(
    tx: TransactionEntity,
    currency: String,
    borderColor: Color,
    onDelete: () -> Unit,
    onViewReceipt: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isIncome = tx.type == "INCOME"
    val icon = CategoryUtils.getCategoryIcon(tx.category)
    val iconColor = if (isIncome) IncomeGreen else ExpenseRed

    val formattedDate = remember(AppText.language, tx.dateMillis) {
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
        border = BorderStroke(1.dp, borderColor),
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
                    .size(38.dp)
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
                    text = tx.description.ifEmpty { AppStrings(AppText.language).translateCategory(tx.category) },
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
                    Text(text = " • ", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = tx.vaultName,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (formattedDate.isNotEmpty()) {
                        Text(text = " • ", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = formattedDate,
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            softWrap = false
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

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "\u2066${if (isIncome) "+" else "-"}${String.format(Locale.US, "%,.0f", tx.amount)}\u2069 ${AppText.currency(currency)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = if (isIncome) IncomeGreen else ExpenseRed,
                softWrap = false
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = AppText.text(com.example.R.string.text_2d2bbdc2d694),
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
    borderColor: Color,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(AppText.language, transfer.dateMillis) {
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
        border = BorderStroke(1.dp, borderColor),
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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreenPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = AppText.text(com.example.R.string.text_0326e4ed17db),
                    tint = EmeraldGreenPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${transfer.fromVaultName} ← ${transfer.toVaultName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = AppText.text(com.example.R.string.text_2621b907b936),
                        fontSize = 11.sp,
                        color = EmeraldGreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        softWrap = false
                    )
                    if (transfer.notes.isNotBlank()) {
                        Text(text = " • ", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = transfer.notes,
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (formattedDate.isNotEmpty()) {
                        Text(text = " • ", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = formattedDate,
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "${String.format(Locale.US, "%,.0f", transfer.amount)} ${AppText.currency(currency)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = EmeraldGreenPrimary,
                softWrap = false
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = AppText.text(com.example.R.string.text_2d2bbdc2d694),
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
