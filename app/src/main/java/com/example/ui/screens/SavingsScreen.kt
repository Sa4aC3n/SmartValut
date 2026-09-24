package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.GoldAssetEntity
import com.example.ui.GoldAssetComputed
import com.example.ui.SavingsSummaryUiState
import com.example.ui.components.SavingsGrowthInteractiveCard
import com.example.ui.dialogs.AddEditCashSavingDialog
import com.example.ui.dialogs.AddEditGoldDialog
import com.example.ui.dialogs.GoldDetailsDialog
import com.example.ui.dialogs.LiveGoldApiDialog
import com.example.ui.dialogs.SellGoldDialog
import com.example.ui.dialogs.UpdateGoldPricesDialog
import com.example.data.api.LiveGoldPrices
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LightBackground
import com.example.ui.theme.MintBackground
import com.example.ui.utils.LocalStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavingsScreen(
    savingsState: SavingsSummaryUiState,
    goldAssets: List<GoldAssetEntity>,
    cashSavings: List<CashSavingEntity>,
    goldPriceMap: Map<Int, Double>,
    onAddGoldAsset: (name: String, goldType: String, karat: Int, weight: Double, purchasePrice: Double, purchaseDateMillis: Long, purpose: String, notes: String, imagePath: String?) -> Unit,
    onUpdateGoldAsset: (GoldAssetEntity) -> Unit,
    onDeleteGoldAsset: (Int) -> Unit,
    onSellGoldAsset: (id: Int, salePrice: Double, saleDateMillis: Long, saleNotes: String?) -> Unit,
    onAddCashSaving: (amount: Double, currency: String, notes: String, dateMillis: Long) -> Unit,
    onUpdateCashSaving: (CashSavingEntity) -> Unit,
    onDeleteCashSaving: (Int) -> Unit,
    onUpdateGoldPrices: (Map<Int, Double>) -> Unit,
    goldApiKey: String = "",
    isUpdatingLiveGoldPrice: Boolean = false,
    lastGoldPriceUpdateTimestamp: Long = 0L,
    liveGoldPriceError: String? = null,
    liveGoldPriceSuccess: String? = null,
    isDailyGoldPriceUpdateEnabled: Boolean = true,
    onToggleDailyGoldPriceUpdate: ((Boolean) -> Unit)? = null,
    onFetchLiveGoldPrices: ((customKey: String?, currency: String?, onSuccess: ((LiveGoldPrices) -> Unit)?, onError: ((String) -> Unit)?) -> Unit)? = null,
    onSaveGoldApiKey: ((String) -> Unit)? = null,
    onClearGoldPriceMessages: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val appStrings = LocalStrings.current
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var filterKarat by remember { mutableStateOf<Int?>(null) }
    var filterType by remember { mutableStateOf<String?>(null) }
    var filterPurpose by remember { mutableStateOf<String?>(null) }

    // Dialog state holders
    var showAddGoldDialog by remember { mutableStateOf(false) }
    var showAddCashDialog by remember { mutableStateOf(false) }
    var showUpdatePricesDialog by remember { mutableStateOf(false) }
    var showLiveGoldApiDialog by remember { mutableStateOf(false) }
    var assetToEdit by remember { mutableStateOf<GoldAssetEntity?>(null) }
    var assetToSell by remember { mutableStateOf<GoldAssetComputed?>(null) }
    var assetToViewDetails by remember { mutableStateOf<GoldAssetComputed?>(null) }
    var cashToEdit by remember { mutableStateOf<CashSavingEntity?>(null) }

    // Show Dialogs
    if (showAddGoldDialog || assetToEdit != null) {
        AddEditGoldDialog(
            initialAsset = assetToEdit,
            goldPriceMap = goldPriceMap,
            onDismiss = {
                showAddGoldDialog = false
                assetToEdit = null
            },
            onSave = { name, type, karat, weight, price, date, purpose, notes, img ->
                if (assetToEdit != null) {
                    onUpdateGoldAsset(
                        assetToEdit!!.copy(
                            name = name,
                            goldType = type,
                            karat = karat,
                            weight = weight,
                            purchasePrice = price,
                            purchaseDateMillis = date,
                            purpose = purpose,
                            notes = notes,
                            imagePath = img
                        )
                    )
                    assetToEdit = null
                } else {
                    onAddGoldAsset(name, type, karat, weight, price, date, purpose, notes, img)
                    showAddGoldDialog = false
                }
            }
        )
    }

    if (showAddCashDialog || cashToEdit != null) {
        AddEditCashSavingDialog(
            initialSaving = cashToEdit,
            onDismiss = {
                showAddCashDialog = false
                cashToEdit = null
            },
            onSave = { amount, currency, notes, date ->
                if (cashToEdit != null) {
                    onUpdateCashSaving(
                        cashToEdit!!.copy(
                            amount = amount,
                            currency = currency,
                            notes = notes,
                            dateMillis = date
                        )
                    )
                    cashToEdit = null
                } else {
                    onAddCashSaving(amount, currency, notes, date)
                    showAddCashDialog = false
                }
            }
        )
    }

    if (showUpdatePricesDialog) {
        UpdateGoldPricesDialog(
            currentPrices = goldPriceMap,
            onDismiss = { showUpdatePricesDialog = false },
            onSavePrices = { prices ->
                onUpdateGoldPrices(prices)
                showUpdatePricesDialog = false
            },
            onOpenLiveApi = {
                showLiveGoldApiDialog = true
            }
        )
    }

    if (showLiveGoldApiDialog) {
        LiveGoldApiDialog(
            initialApiKey = goldApiKey,
            currentCurrency = savingsState.currency,
            isLoading = isUpdatingLiveGoldPrice,
            errorMessage = liveGoldPriceError,
            successMessage = liveGoldPriceSuccess,
            isDailyAutoUpdateEnabled = isDailyGoldPriceUpdateEnabled,
            onToggleDailyAutoUpdate = onToggleDailyGoldPriceUpdate,
            onDismiss = {
                showLiveGoldApiDialog = false
                onClearGoldPriceMessages?.invoke()
            },
            onFetchLivePrices = { key, curr ->
                onFetchLiveGoldPrices?.invoke(key, curr, null, null)
            },
            onSaveApiKey = { key ->
                onSaveGoldApiKey?.invoke(key)
            }
        )
    }

    assetToSell?.let { assetComp ->
        SellGoldDialog(
            assetComputed = assetComp,
            onDismiss = { assetToSell = null },
            onConfirmSale = { salePrice, date, notes ->
                onSellGoldAsset(assetComp.asset.id, salePrice, date, notes)
                assetToSell = null
            }
        )
    }

    assetToViewDetails?.let { assetComp ->
        GoldDetailsDialog(
            assetComputed = assetComp,
            onDismiss = { assetToViewDetails = null },
            onEdit = {
                val asset = assetComp.asset
                assetToViewDetails = null
                assetToEdit = asset
            },
            onSell = {
                assetToViewDetails = null
                assetToSell = assetComp
            },
            onDelete = {
                val id = assetComp.asset.id
                assetToViewDetails = null
                onDeleteGoldAsset(id)
            }
        )
    }

    // Filter Active Gold Items
    val activeGoldComputed = remember(savingsState.activeGoldAssets, searchQuery, filterKarat, filterType, filterPurpose) {
        savingsState.activeGoldAssets.filter { computed ->
            val asset = computed.asset
            val matchesQuery = searchQuery.isBlank() ||
                    asset.name.contains(searchQuery, ignoreCase = true) ||
                    asset.notes.contains(searchQuery, ignoreCase = true) ||
                    asset.goldType.contains(searchQuery, ignoreCase = true)
            val matchesKarat = filterKarat == null || asset.karat == filterKarat
            val matchesType = filterType == null || asset.goldType == filterType
            val matchesPurpose = filterPurpose == null || asset.purpose == filterPurpose

            matchesQuery && matchesKarat && matchesType && matchesPurpose
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER & TOTAL SAVINGS HERO CARD
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = appStrings.titleSavings,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "محفظة الأصول والادخار بالذهب والنقد",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 1.5 LIVE GOLD MARKET TICKER (GoldAPI.io)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (goldApiKey.isBlank()) {
                            showLiveGoldApiDialog = true
                        } else {
                            onFetchLiveGoldPrices?.invoke(null, null, null, null)
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                    .background(if (lastGoldPriceUpdateTimestamp > 0) Color(0xFF4ADE80) else GoldAccent)
                            )
                            Text(
                                text = "أسعار الذهب اللحظية (GoldAPI.io)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isUpdatingLiveGoldPrice) {
                                Text(
                                    text = "جاري التحديث...",
                                    fontSize = 11.sp,
                                    color = EmeraldGreenPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                CircularProgressIndicator(
                                    color = EmeraldGreenPrimary,
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                val text = when {
                                    lastGoldPriceUpdateTimestamp > 0 -> {
                                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        "تحديث: ${sdf.format(Date(lastGoldPriceUpdateTimestamp))}"
                                    }
                                    isDailyGoldPriceUpdateEnabled -> "تحديث يومي 12:00 ظ ⏰"
                                    else -> "اضغط للتحديث اللحظي ⚡"
                                }
                                Text(
                                    text = text,
                                    fontSize = 11.sp,
                                    color = if (isDailyGoldPriceUpdateEnabled && lastGoldPriceUpdateTimestamp == 0L) EmeraldGreenDark else Color.Gray
                                )
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Karat price pills row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val karatsToShow = listOf(24, 21, 18)
                        items(karatsToShow) { k ->
                            val p = goldPriceMap[k] ?: 0.0
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MintBackground,
                                border = BorderStroke(0.5.dp, EmeraldGreenPrimary.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "عيار $k:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldGreenDark
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", p)} ${savingsState.currency}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldAccent.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable { showLiveGoldApiDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "إعدادات GoldAPI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB8860B)
                                    )
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(0.5.dp, CardBorderColor),
                                modifier = Modifier.clickable { showUpdatePricesDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "تعديل يدوي",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. LUXURY HERO CARD (إجمالي المدخرات)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0D5E42),
                                    Color(0xFF053B28),
                                    Color(0xFF1B382B)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // Title & Live Badge
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
                                        .background(GoldAccent)
                                )
                                Text(
                                    text = appStrings.totalSavings,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Profit / Growth Pill
                            if (savingsState.totalGoldUnrealizedProfit != 0.0) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (savingsState.totalGoldUnrealizedProfit >= 0) IncomeGreen.copy(alpha = 0.25f)
                                            else ExpenseRed.copy(alpha = 0.25f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (savingsState.totalGoldUnrealizedProfit >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = if (savingsState.totalGoldUnrealizedProfit >= 0) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${if (savingsState.totalGoldUnrealizedProfit >= 0) "+" else ""}${String.format(Locale.US, "%.1f", savingsState.totalGoldProfitPercentage)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (savingsState.totalGoldUnrealizedProfit >= 0) Color(0xFF4ADE80) else Color(0xFFFF8A80)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large Total Savings Amount
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", savingsState.totalSavings)} ${savingsState.currency}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4 Mini Summary Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Gold Value
                            StatMiniCard(
                                title = "💛 قيمة الذهب",
                                value = "${String.format(Locale.US, "%,.0f", savingsState.totalGoldCurrentValue)}",
                                unit = savingsState.currency,
                                modifier = Modifier.weight(1f)
                            )
                            // Cash Savings
                            StatMiniCard(
                                title = "💵 نقدية",
                                value = "${String.format(Locale.US, "%,.0f", savingsState.totalCashSavings)}",
                                unit = savingsState.currency,
                                modifier = Modifier.weight(1f)
                            )
                            // Gold Weight
                            StatMiniCard(
                                title = "⚖️ وزن الذهب",
                                value = String.format(Locale.US, "%.1f", savingsState.totalGoldWeightGrams),
                                unit = "جرام",
                                modifier = Modifier.weight(1f)
                            )
                            // Pieces Count
                            StatMiniCard(
                                title = "📦 القطع",
                                value = "${savingsState.totalGoldPiecesCount}",
                                unit = "قطعة",
                                modifier = Modifier.weight(0.85f)
                            )
                        }
                    }
                }
            }
        }

        // 3. ACTION BUTTONS ROW (إضافة ذهب / إضافة كاش)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showAddGoldDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إضافة ذهب / سبيكة",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = { showAddCashDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إضافة نقدية",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 4. SUB-TABS NAVIGATION
        item {
            val subTabs = listOf(
                Pair(appStrings.tabGold, "💛"),
                Pair(appStrings.tabCash, "💵"),
                Pair(appStrings.tabAnalytics, "📊"),
                Pair(appStrings.tabSoldHistory, "🏷️")
            )

            ScrollableTabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                        color = EmeraldGreenPrimary,
                        height = 3.dp
                    )
                }
            ) {
                subTabs.forEachIndexed { index, (title, emoji) ->
                    val isSelected = selectedSubTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSubTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(emoji, fontSize = 13.sp)
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) EmeraldGreenPrimary else Color.Gray
                                )
                            }
                        }
                    )
                }
            }
        }

        // 5. SUB-TAB CONTENT
        when (selectedSubTab) {
            0 -> {
                // GOLD & BULLIONS TAB
                item {
                    // Search and Filters Bar
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث بالاسم، الماركة، أو الملاحظات...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Filter Chips Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = filterKarat == null && filterType == null && filterPurpose == null,
                                    onClick = {
                                        filterKarat = null
                                        filterType = null
                                        filterPurpose = null
                                    },
                                    label = { Text("الكل", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterKarat == 24,
                                    onClick = { filterKarat = if (filterKarat == 24) null else 24 },
                                    label = { Text("عيار 24", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterKarat == 21,
                                    onClick = { filterKarat = if (filterKarat == 21) null else 21 },
                                    label = { Text("عيار 21", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterKarat == 18,
                                    onClick = { filterKarat = if (filterKarat == 18) null else 18 },
                                    label = { Text("عيار 18", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterType == "سبيكة",
                                    onClick = { filterType = if (filterType == "سبيكة") null else "سبيكة" },
                                    label = { Text("السبائك", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterType == "جنيه ذهب",
                                    onClick = { filterType = if (filterType == "جنيه ذهب") null else "جنيه ذهب" },
                                    label = { Text("جنيهات ذهب", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterPurpose == "SAVING",
                                    onClick = { filterPurpose = if (filterPurpose == "SAVING") null else "SAVING" },
                                    label = { Text("للادخار", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                if (activeGoldComputed.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = appStrings.noGoldYet,
                            subtitle = "اضغط على زر «إضافة ذهب / سبيكة» لتسجيل أول قطعة ومتابعة قيمتها لحظة بلحظة.",
                            buttonText = "إضافة سبيكة أو قطعة ذهب",
                            onButtonClick = { showAddGoldDialog = true }
                        )
                    }
                } else {
                    items(activeGoldComputed) { computed ->
                        GoldAssetCard(
                            assetComputed = computed,
                            currency = savingsState.currency,
                            onViewDetails = { assetToViewDetails = computed },
                            onSell = { assetToSell = computed },
                            onEdit = { assetToEdit = computed.asset },
                            onDelete = { onDeleteGoldAsset(computed.asset.id) }
                        )
                    }
                }
            }

            1 -> {
                // CASH SAVINGS TAB
                if (cashSavings.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.MonetizationOn,
                            title = appStrings.noCashSavingsYet,
                            subtitle = "سجّل مدخراتك النقدية وحسابات الطوارئ لمتابعة إجمالي ثروتك في مكان واحد.",
                            buttonText = "إضافة مدخرات نقدية",
                            onButtonClick = { showAddCashDialog = true }
                        )
                    }
                } else {
                    items(cashSavings) { cash ->
                        CashSavingCard(
                            saving = cash,
                            onEdit = { cashToEdit = cash },
                            onDelete = { onDeleteCashSaving(cash.id) }
                        )
                    }
                }
            }

            2 -> {
                // ANALYTICS & ALLOCATION TAB
                item {
                    AnalyticsAndAllocationSection(
                        savingsState = savingsState,
                        goldAssets = goldAssets,
                        cashSavings = cashSavings,
                        goldPriceMap = goldPriceMap
                    )
                }
            }

            3 -> {
                // SOLD GOLD HISTORY TAB
                val soldAssets = savingsState.soldGoldAssets
                if (soldAssets.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Sell,
                            title = appStrings.noSoldGoldYet,
                            subtitle = "عند بيع أي قطعة ذهب من الخزنة، ستظهر هنا تفاصيل البيع والأرباح المحققة وتاريخ العملية.",
                            buttonText = null,
                            onButtonClick = {}
                        )
                    }
                } else {
                    items(soldAssets) { computed ->
                        SoldGoldCard(
                            assetComputed = computed,
                            currency = savingsState.currency,
                            onViewDetails = { assetToViewDetails = computed },
                            onDelete = { onDeleteGoldAsset(computed.asset.id) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = unit,
                fontSize = 9.sp,
                color = GoldAccent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun GoldAssetCard(
    assetComputed: GoldAssetComputed,
    currency: String,
    onViewDetails: () -> Unit,
    onSell: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val asset = assetComputed.asset
    val formattedPurchaseDate = remember(asset.purchaseDateMillis) {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(asset.purchaseDateMillis))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Thumbnail + Name + Karat & Purpose Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Gold Coin / Photo Thumbnail
                    if (!asset.imagePath.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(asset.imagePath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = asset.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFFB8860B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = asset.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "عيار ${asset.karat}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Text("•", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = "${asset.weight} جرام",
                                fontSize = 11.5.sp,
                                color = Color.Gray
                            )
                            Text("•", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = asset.goldType,
                                fontSize = 11.sp,
                                color = EmeraldGreenPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Profit / Loss Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (assetComputed.profitLoss >= 0) IncomeGreen.copy(alpha = 0.15f)
                            else ExpenseRed.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${if (assetComputed.profitLoss >= 0) "+" else ""}${String.format(Locale.US, "%.1f", assetComputed.profitLossPercentage)}%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (assetComputed.profitLoss >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Values Grid Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Current Value
                    Column {
                        Text("القيمة الحالية بالخزنة", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", assetComputed.currentValue)} $currency",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Purchase Price
                    Column {
                        Text("سعر الشراء", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", asset.purchasePrice)} $currency",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }

                    // Net Profit
                    Column(horizontalAlignment = Alignment.End) {
                        Text("صافي الربح", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (assetComputed.profitLoss >= 0) "+" else ""}${String.format(Locale.US, "%,.0f", assetComputed.profitLoss)} $currency",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (assetComputed.profitLoss >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "شراء: $formattedPurchaseDate",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Sell Button
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldAccent.copy(alpha = 0.2f))
                            .clickable { onSell() }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤝 بيع", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB8860B))
                    }

                    // Edit
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = ExpenseRed.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun CashSavingCard(
    saving: CashSavingEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(saving.dateMillis) {
        SimpleDateFormat("dd MMMM yyyy", Locale("ar")).format(Date(saving.dateMillis))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreenPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = EmeraldGreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", saving.amount)} ${saving.currency}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (saving.notes.isNotBlank()) saving.notes else "مدخرات نقدية بالخزنة",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 10.5.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = ExpenseRed.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun SoldGoldCard(
    assetComputed: GoldAssetComputed,
    currency: String,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val asset = assetComputed.asset
    val saleDateStr = remember(asset.saleDateMillis) {
        if (asset.saleDateMillis != null) {
            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(asset.saleDateMillis))
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = asset.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "تاريخ البيع: $saleDateStr",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (assetComputed.profitLoss >= 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ربح: ${if (assetComputed.profitLoss >= 0) "+" else ""}${String.format(Locale.US, "%,.0f", assetComputed.profitLoss)} $currency",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (assetComputed.profitLoss >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("سعر الشراء: ${String.format(Locale.US, "%,.0f", asset.purchasePrice)} $currency", fontSize = 11.sp, color = Color.Gray)
                Text("سعر البيع: ${String.format(Locale.US, "%,.0f", asset.salePrice ?: 0.0)} $currency", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AnalyticsAndAllocationSection(
    savingsState: SavingsSummaryUiState,
    goldAssets: List<GoldAssetEntity>,
    cashSavings: List<CashSavingEntity>,
    goldPriceMap: Map<Int, Double>
) {
    val goldRatio = savingsState.goldRatioPercentage.toFloat() / 100f
    val cashRatio = savingsState.cashRatioPercentage.toFloat() / 100f

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Interactive Savings & Gold Growth Chart Card
        SavingsGrowthInteractiveCard(
            goldAssets = goldAssets,
            cashSavings = cashSavings,
            goldPriceMap = goldPriceMap,
            currency = savingsState.currency
        )

        // 2. Asset Allocation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MintBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = EmeraldGreenPrimary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "توزيع الأصول والمدخرات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dual progress indicator bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f))
                ) {
                    if (savingsState.goldRatioPercentage > 0) {
                        Box(
                            modifier = Modifier
                                .weight(if (goldRatio > 0) goldRatio else 0.001f)
                                .fillMaxSize()
                                .background(GoldAccent)
                        )
                    }
                    if (savingsState.cashRatioPercentage > 0) {
                        Box(
                            modifier = Modifier
                                .weight(if (cashRatio > 0) cashRatio else 0.001f)
                                .fillMaxSize()
                                .background(EmeraldGreenPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(GoldAccent))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الذهب والسبائك: ${String.format(Locale.US, "%.1f", savingsState.goldRatioPercentage)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(EmeraldGreenPrimary))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "السيولة النقدية: ${String.format(Locale.US, "%.1f", savingsState.cashRatioPercentage)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Karat Breakdown Card
        if (savingsState.karatBreakdown.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تفاصيل الذهب حسب العيار",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    for ((karat, weight) in savingsState.karatBreakdown) {
                        val piecesCount = savingsState.activeGoldAssets.count { it.asset.karat == karat }
                        val currentKaratValue = savingsState.activeGoldAssets
                            .filter { it.asset.karat == karat }
                            .sumOf { it.currentValue }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("عيار $karat:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("($piecesCount قطع)", fontSize = 11.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${String.format(Locale.US, "%.1f", weight)} جرام", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${String.format(Locale.US, "%,.0f", currentKaratValue)} ${savingsState.currency}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // 3. Smart Wealth Tips Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MintBackground),
            border = BorderStroke(1.dp, EmeraldGreenPrimary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = EmeraldGreenDark)
                    Text(
                        text = "إرشادات الخزنة الذكية لتنمية المدخرات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = EmeraldGreenDark
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                WealthTipItem("الادخار بالسبائك وجنيهات الذهب يقلل من خسارة المصنعية عند إعادة البيع مع إمكانية استرداد الكاش باك (Cashback).")
                WealthTipItem("الحفاظ على نسبة 30% إلى 50% من المدخرات في الذهب يحمي القوة الشرائية لأموالك على المدى الطويل ضد التضخم.")
                WealthTipItem("تجنب بيع الذهب إلا عند الضرورة أو تحويله إلى أصل استثماري ذو عائد أعلى.")
            }
        }
    }
}

@Composable
private fun WealthTipItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("•", color = EmeraldGreenPrimary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF1B382B),
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String?,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MintBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldGreenPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (buttonText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onButtonClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                ) {
                    Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }
    }
}
