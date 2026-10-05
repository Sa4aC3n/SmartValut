package com.example.ui.dialogs

import com.example.ui.utils.AppText

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.data.api.GoldApiClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.GoldAssetEntity
import com.example.ui.GoldAssetComputed
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MintBackground
import com.example.ui.utils.LocalStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoldDialog(
    initialAsset: GoldAssetEntity? = null,
    goldPriceMap: Map<Int, Double>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        goldType: String,
        karat: Int,
        weight: Double,
        purchasePrice: Double,
        purchaseDateMillis: Long,
        purpose: String,
        notes: String,
        imagePath: String?
    ) -> Unit
) {
    val appStrings = LocalStrings.current
    var name by remember { mutableStateOf(initialAsset?.name ?: "") }
    var goldType by remember { mutableStateOf(initialAsset?.goldType ?: "سبيكة") }
    var karat by remember { mutableIntStateOf(initialAsset?.karat ?: 24) }
    var weightText by remember { mutableStateOf(if (initialAsset != null) initialAsset.weight.toString() else "") }
    var purchasePriceText by remember { mutableStateOf(if (initialAsset != null) initialAsset.purchasePrice.toLong().toString() else "") }
    var purpose by remember { mutableStateOf(initialAsset?.purpose ?: "SAVING") }
    var notes by remember { mutableStateOf(initialAsset?.notes ?: "") }
    var imageUriString by remember { mutableStateOf<String?>(initialAsset?.imagePath) }
    var purchaseDateMillis by remember { mutableStateOf(initialAsset?.purchaseDateMillis ?: System.currentTimeMillis()) }

    var expandedTypeDropdown by remember { mutableStateOf(false) }
    var expandedKaratDropdown by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { imageUriString = it.toString() }
        }
    )

    val weightDouble = weightText.toDoubleOrNull() ?: 0.0
    val priceDouble = purchasePriceText.toDoubleOrNull() ?: 0.0
    val currentGramPrice = goldPriceMap[karat] ?: 5250.0
    val calculatedCurrentValue = weightDouble * currentGramPrice
    val profitLoss = calculatedCurrentValue - priceDouble
    val profitPct = if (priceDouble > 0) (profitLoss / priceDouble) * 100.0 else 0.0

    val goldTypes = listOf("سبيكة", "مشغولات", "عملة ذهبية", "جنيه ذهب", "أخرى")
    val standardKarats = listOf(24, 22, 21, 18, 14, 12)
    val weightPresets = listOf(1.0, 2.5, 5.0, 8.0, 10.0, 20.0, 31.1, 50.0, 100.0)

    val formattedDate = remember(AppText.language, purchaseDateMillis) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        sdf.format(Date(purchaseDateMillis))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialAsset == null) appStrings.addGoldPiece else AppText.text(com.example.R.string.text_7bf909688837),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = appStrings.close,
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(appStrings.goldPieceName) },
                    placeholder = { Text(AppText.text(com.example.R.string.text_30d402720db8)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Type & Karat Dropdowns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gold Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedTypeDropdown,
                        onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = AppText.goldType(goldType),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(appStrings.goldType) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTypeDropdown,
                            onDismissRequest = { expandedTypeDropdown = false }
                        ) {
                            goldTypes.forEach { typeOption ->
                                DropdownMenuItem(
                                    text = { Text(AppText.goldType(typeOption)) },
                                    onClick = {
                                        goldType = typeOption
                                        if (typeOption == "جنيه ذهب") {
                                            karat = 21
                                            weightText = "8.0"
                                        }
                                        expandedTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Karat Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedKaratDropdown,
                        onExpandedChange = { expandedKaratDropdown = !expandedKaratDropdown },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = AppText.text(com.example.R.string.text_2c3d2ad75181, karat),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(appStrings.karat) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKaratDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedKaratDropdown,
                            onDismissRequest = { expandedKaratDropdown = false }
                        ) {
                            standardKarats.forEach { k ->
                                DropdownMenuItem(
                                    text = { Text(AppText.text(com.example.R.string.text_9595409f46b5, k)) },
                                    onClick = {
                                        karat = k
                                        expandedKaratDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weight Input & Quick Presets
                Text(
                    text = "${appStrings.weightInGrams}:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    placeholder = { Text(AppText.text(com.example.R.string.text_0651503e203d)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text(
                            text = AppText.text(com.example.R.string.text_ae71934c1102),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Preset weights chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(weightPresets) { w ->
                        val isSelected = weightDouble == w
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                weightText = if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
                            },
                            label = {
                                Text(
                                    text = when (w) {
                                        8.0 -> AppText.text(com.example.R.string.text_09355e9347df)
                                        31.1 -> AppText.text(com.example.R.string.text_73e9421c8325)
                                        else -> "${w}g"
                                    },
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Purchase Price Input
                OutlinedTextField(
                    value = purchasePriceText,
                    onValueChange = { purchasePriceText = it },
                    label = { Text(appStrings.purchasePrice) },
                    placeholder = { Text(AppText.text(com.example.R.string.text_7bb800f48f49)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text(
                            text = AppText.text(com.example.R.string.text_5c54bb48fcff),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time valuation banner
                if (weightDouble > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (profitLoss >= 0) MintBackground else Color(0xFFFFECEC)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (profitLoss >= 0) EmeraldGreenPrimary.copy(alpha = 0.3f) else ExpenseRed.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_af339c9bb6d8),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = AppText.text(com.example.R.string.text_fb74677123fd, String.format(Locale.US, "%,.0f", calculatedCurrentValue)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = AppText.text(com.example.R.string.text_02d2dfdd5f2b, karat),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = AppText.text(com.example.R.string.text_938d3e255411, String.format(Locale.US, "%,.0f", currentGramPrice)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (priceDouble > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = AppText.text(com.example.R.string.text_5736e8751e12),
                                        fontSize = 12.sp,
                                        color = if (profitLoss >= 0) IncomeGreen else ExpenseRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = AppText.text(com.example.R.string.text_542bbcfa90cb, if (profitLoss >= 0) "+" else "", String.format(Locale.US, "%,.0f", profitLoss), String.format(Locale.US, "%.1f", profitPct)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profitLoss >= 0) IncomeGreen else ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Purpose Selection (Saving vs Adornment)
                Text(
                    text = "${appStrings.purpose}:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { purpose = "SAVING" }
                            .padding(end = 16.dp)
                    ) {
                        RadioButton(
                            selected = purpose == "SAVING",
                            onClick = { purpose = "SAVING" },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                        )
                        Text(appStrings.purposeSaving, fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { purpose = "ADORNMENT" }
                    ) {
                        RadioButton(
                            selected = purpose == "ADORNMENT",
                            onClick = { purpose = "ADORNMENT" },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                        )
                        Text(appStrings.purposeAdornment, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppText.text(com.example.R.string.text_940eb631311c)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Photo Attach Button & Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EmeraldGreenPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (imageUriString == null) appStrings.piecePhoto else AppText.text(com.example.R.string.text_f12384b96e33),
                            color = EmeraldGreenPrimary,
                            fontSize = 12.sp
                        )
                    }

                    if (imageUriString != null) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUriString)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Gold Image Preview",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(appStrings.cancel)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && weightDouble > 0) {
                                onSave(
                                    name.trim(),
                                    goldType,
                                    karat,
                                    weightDouble,
                                    priceDouble,
                                    purchaseDateMillis,
                                    purpose,
                                    notes.trim(),
                                    imageUriString
                                )
                            }
                        },
                        enabled = name.isNotBlank() && weightDouble > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Text(appStrings.save, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SellGoldDialog(
    assetComputed: GoldAssetComputed,
    onDismiss: () -> Unit,
    onConfirmSale: (salePrice: Double, saleDateMillis: Long, saleNotes: String?) -> Unit
) {
    val appStrings = LocalStrings.current
    val asset = assetComputed.asset
    var salePriceText by remember {
        mutableStateOf(String.format(Locale.US, "%.0f", assetComputed.currentValue))
    }
    var saleNotes by remember { mutableStateOf("") }
    var saleDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val salePriceDouble = salePriceText.toDoubleOrNull() ?: 0.0
    val realizedProfit = salePriceDouble - asset.purchasePrice
    val realizedPct = if (asset.purchasePrice > 0) (realizedProfit / asset.purchasePrice) * 100.0 else 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appStrings.sellGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = appStrings.close)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Piece Summary Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = asset.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(AppText.text(com.example.R.string.text_5fa7c4ce5839, asset.karat), fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            Text("•", fontSize = 12.sp, color = Color.Gray)
                            Text(AppText.text(com.example.R.string.text_953e21c4f2c5, asset.weight), fontSize = 12.sp, color = Color.Gray)
                            Text("•", fontSize = 12.sp, color = Color.Gray)
                            Text(AppText.text(com.example.R.string.text_fff19b8abb41, String.format(Locale.US, "%,.0f", asset.purchasePrice)), fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actual Sale Price Input
                OutlinedTextField(
                    value = salePriceText,
                    onValueChange = { salePriceText = it },
                    label = { Text(appStrings.salePrice) },
                    placeholder = { Text(AppText.text(com.example.R.string.text_92291ba269c7)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text(
                            text = AppText.text(com.example.R.string.text_5c54bb48fcff),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Realized Profit / Loss Preview
                if (salePriceDouble > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (realizedProfit >= 0) MintBackground else Color(0xFFFFECEC)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (realizedProfit >= 0) EmeraldGreenPrimary.copy(alpha = 0.3f) else ExpenseRed.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (realizedProfit >= 0) AppText.text(com.example.R.string.text_3459448d9802) else AppText.text(com.example.R.string.text_bb392991a17f),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = AppText.text(com.example.R.string.text_ae55a78e16f4, if (realizedProfit >= 0) "+" else "", String.format(Locale.US, "%,.0f", realizedProfit)),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (realizedProfit >= 0) IncomeGreen else ExpenseRed
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (realizedProfit >= 0) IncomeGreen else ExpenseRed)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${if (realizedProfit >= 0) "+" else ""}${String.format(Locale.US, "%.1f", realizedPct)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Sale Notes
                OutlinedTextField(
                    value = saleNotes,
                    onValueChange = { saleNotes = it },
                    label = { Text(AppText.text(com.example.R.string.text_8c2840bd04e1)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(appStrings.cancel)
                    }

                    Button(
                        onClick = {
                            if (salePriceDouble > 0) {
                                onConfirmSale(salePriceDouble, saleDateMillis, saleNotes.ifBlank { null })
                            }
                        },
                        enabled = salePriceDouble > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Text(AppText.text(com.example.R.string.text_bd5b1d4d50d9), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GoldDetailsDialog(
    assetComputed: GoldAssetComputed,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSell: () -> Unit,
    onDelete: () -> Unit
) {
    val appStrings = LocalStrings.current
    val asset = assetComputed.asset
    val isSold = asset.status == "SOLD"

    val purchaseDateStr = remember(AppText.language, asset.purchaseDateMillis) {
        SimpleDateFormat("dd MMMM yyyy", AppText.locale).format(Date(asset.purchaseDateMillis))
    }
    val saleDateStr = remember(AppText.language, asset.saleDateMillis) {
        if (asset.saleDateMillis != null) {
            SimpleDateFormat("dd MMMM yyyy", AppText.locale).format(Date(asset.saleDateMillis))
        } else ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = asset.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isSold) AppText.text(com.example.R.string.text_8f3cc63d292c, saleDateStr) else AppText.text(com.example.R.string.text_0619e9cfa1ee, asset.karat),
                                fontSize = 12.sp,
                                color = if (isSold) Color.Gray else EmeraldGreenPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = appStrings.close)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Image if available
                if (!asset.imagePath.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(asset.imagePath)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Gold asset image",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Grid of Specs
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailRow(AppText.text(com.example.R.string.text_2a5f152b8551), AppText.goldType(asset.goldType))
                        DetailRow(AppText.text(com.example.R.string.text_6e31d9bdf7b4), AppText.text(com.example.R.string.text_5fa7c4ce5839, asset.karat))
                        DetailRow(AppText.text(com.example.R.string.text_20218fd4bf04), AppText.text(com.example.R.string.text_953e21c4f2c5, asset.weight))
                        DetailRow(AppText.text(com.example.R.string.text_fc4d767fab55), if (asset.purpose == "SAVING") AppText.text(com.example.R.string.text_f4bce26fcbd7) else AppText.text(com.example.R.string.text_0694745f849b))
                        DetailRow(AppText.text(com.example.R.string.text_21fc46ec23cd), purchaseDateStr)
                        DetailRow(AppText.text(com.example.R.string.text_6d3b8a57de85), AppText.text(com.example.R.string.text_d03ed058b768, String.format(Locale.US, "%,.0f", asset.purchasePrice)))
                        if (!isSold) {
                            DetailRow(AppText.text(com.example.R.string.text_574c4d101909), AppText.text(com.example.R.string.text_e9af10cebbd5, String.format(Locale.US, "%,.0f", assetComputed.currentPricePerGram)))
                            DetailRow(AppText.text(com.example.R.string.text_29f8a247485e), AppText.text(com.example.R.string.text_97c39d12f919, String.format(Locale.US, "%,.0f", assetComputed.currentValue)), isHighlighted = true)
                            DetailRow(
                                label = AppText.text(com.example.R.string.text_ec746491fb2e),
                                value = AppText.text(com.example.R.string.text_6f6b054fcaf6, if (assetComputed.profitLoss >= 0) "+" else "", String.format(Locale.US, "%,.0f", assetComputed.profitLoss), String.format(Locale.US, "%.1f", assetComputed.profitLossPercentage)),
                                valueColor = if (assetComputed.profitLoss >= 0) IncomeGreen else ExpenseRed
                            )
                        } else {
                            DetailRow(AppText.text(com.example.R.string.text_37e9486a6817), AppText.text(com.example.R.string.text_044936796b55, String.format(Locale.US, "%,.0f", asset.salePrice ?: 0.0)), isHighlighted = true)
                            DetailRow(AppText.text(com.example.R.string.text_6045f88b80a0), saleDateStr)
                            DetailRow(
                                label = AppText.text(com.example.R.string.text_378584de934b),
                                value = AppText.text(com.example.R.string.text_6f6b054fcaf6, if (assetComputed.profitLoss >= 0) "+" else "", String.format(Locale.US, "%,.0f", assetComputed.profitLoss), String.format(Locale.US, "%.1f", assetComputed.profitLossPercentage)),
                                valueColor = if (assetComputed.profitLoss >= 0) IncomeGreen else ExpenseRed
                            )
                            if (!asset.saleNotes.isNullOrBlank()) {
                                DetailRow(AppText.text(com.example.R.string.text_267dda293317), asset.saleNotes)
                            }
                        }

                        if (asset.notes.isNotBlank()) {
                            DetailRow(AppText.text(com.example.R.string.text_f05125201dc0), asset.notes)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isSold) {
                        Button(
                            onClick = {
                                onDismiss()
                                onSell()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            Text(AppText.text(com.example.R.string.text_40ca4ab3b62c), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEdit()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(AppText.text(com.example.R.string.text_b4f76c3aa21e), fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onDelete()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.5.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = if (isHighlighted) 14.sp else 13.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) EmeraldGreenPrimary else valueColor
        )
    }
}

@Composable
fun UpdateGoldPricesDialog(
    currentPrices: Map<Int, Double>,
    onDismiss: () -> Unit,
    onSavePrices: (Map<Int, Double>) -> Unit,
    onOpenLiveApi: (() -> Unit)? = null
) {
    val appStrings = LocalStrings.current
    var price24 by remember { mutableStateOf(String.format(Locale.US, "%.0f", currentPrices[24] ?: 5250.0)) }
    var price22 by remember { mutableStateOf(String.format(Locale.US, "%.0f", currentPrices[22] ?: 4810.0)) }
    var price21 by remember { mutableStateOf(String.format(Locale.US, "%.0f", currentPrices[21] ?: 4590.0)) }
    var price18 by remember { mutableStateOf(String.format(Locale.US, "%.0f", currentPrices[18] ?: 3935.0)) }
    var price14 by remember { mutableStateOf(String.format(Locale.US, "%.0f", currentPrices[14] ?: 3060.0)) }
    var price12 by remember { mutableStateOf(String.format(Locale.US, "%.0f", currentPrices[12] ?: 2625.0)) }

    // Preset standard egyptian market prices
    fun resetToDefaults() {
        price24 = "5250"
        price22 = "4810"
        price21 = "4590"
        price18 = "3935"
        price14 = "3060"
        price12 = "2625"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appStrings.updateGoldPrices,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = appStrings.close)
                    }
                }

                Text(
                    text = AppText.text(com.example.R.string.text_32ef45f6c4dd),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )

                if (onOpenLiveApi != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenLiveApi()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(
                                        text = AppText.text(com.example.R.string.text_c733eb2d663a),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = AppText.text(com.example.R.string.text_f6141002d56d),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                PriceInputField(label = AppText.text(com.example.R.string.text_e06f61ff041d), value = price24, onValueChange = { price24 = it })
                Spacer(modifier = Modifier.height(10.dp))
                PriceInputField(label = AppText.text(com.example.R.string.text_33c43892b786), value = price22, onValueChange = { price22 = it })
                Spacer(modifier = Modifier.height(10.dp))
                PriceInputField(label = AppText.text(com.example.R.string.text_bf390a0524ba), value = price21, onValueChange = { price21 = it })
                Spacer(modifier = Modifier.height(10.dp))
                PriceInputField(label = AppText.text(com.example.R.string.text_af3b35eeda19), value = price18, onValueChange = { price18 = it })
                Spacer(modifier = Modifier.height(10.dp))
                PriceInputField(label = AppText.text(com.example.R.string.text_e69a15a0a8d1), value = price14, onValueChange = { price14 = it })
                Spacer(modifier = Modifier.height(10.dp))
                PriceInputField(label = AppText.text(com.example.R.string.text_a49055f064e4), value = price12, onValueChange = { price12 = it })

                Spacer(modifier = Modifier.height(14.dp))

                // Reset defaults button
                TextButton(
                    onClick = { resetToDefaults() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(AppText.text(com.example.R.string.text_29dc89ac3e1d), color = EmeraldGreenPrimary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(appStrings.cancel)
                    }

                    Button(
                        onClick = {
                            val newPrices = mapOf(
                                24 to (price24.toDoubleOrNull() ?: 5250.0),
                                22 to (price22.toDoubleOrNull() ?: 4810.0),
                                21 to (price21.toDoubleOrNull() ?: 4590.0),
                                18 to (price18.toDoubleOrNull() ?: 3935.0),
                                14 to (price14.toDoubleOrNull() ?: 3060.0),
                                12 to (price12.toDoubleOrNull() ?: 2625.0)
                            )
                            onSavePrices(newPrices)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Text(AppText.text(com.example.R.string.text_0b97ec532c0e), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            Text(
                text = AppText.text(com.example.R.string.text_5c54bb48fcff),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreenPrimary,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveGoldApiDialog(
    initialApiKey: String,
    currentCurrency: String,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    isDailyAutoUpdateEnabled: Boolean = true,
    onToggleDailyAutoUpdate: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
    onFetchLivePrices: (apiKey: String, currency: String) -> Unit,
    onSaveApiKey: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(initialApiKey.ifBlank { GoldApiClient.DEFAULT_API_KEY }) }
    var selectedCurr by remember {
        mutableStateOf(
            when (currentCurrency.trim()) {
                "$", "USD" -> "USD"
                "ر.س", "SAR" -> "SAR"
                "د.إ", "AED" -> "AED"
                "€", "EUR" -> "EUR"
                else -> "EGP"
            }
        )
    }
    var showKey by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = AppText.text(com.example.R.string.text_0cb303cf3d30),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = AppText.text(com.example.R.string.text_d9e689d6c7ee),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = AppText.text(com.example.R.string.text_5bf826c5e57c))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Info banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MintBackground)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreenPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            text = AppText.text(com.example.R.string.text_64667411274d),
                            fontSize = 11.5.sp,
                            color = Color(0xFF1B382B),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Daily Auto-update at 12:00 PM
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = AppText.text(com.example.R.string.text_62aa66740379),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = AppText.text(com.example.R.string.text_d0b12b2cc73d),
                                    fontSize = 10.5.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Switch(
                            checked = isDailyAutoUpdateEnabled,
                            onCheckedChange = { onToggleDailyAutoUpdate?.invoke(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreenPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Token Input Field
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        onSaveApiKey(it)
                    },
                    label = { Text(AppText.text(com.example.R.string.text_c5f5d253fe51), fontSize = 12.sp) },
                    placeholder = { Text(AppText.text(com.example.R.string.text_753686b7d08a), fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = GoldAccent)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppText.text(com.example.R.string.text_05f0a0d376f8),
                        fontSize = 11.sp,
                        color = EmeraldGreenDark,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    if (apiKey != GoldApiClient.DEFAULT_API_KEY) {
                        TextButton(
                            onClick = {
                                apiKey = GoldApiClient.DEFAULT_API_KEY
                                onSaveApiKey(apiKey)
                            }
                        ) {
                            Text(AppText.text(com.example.R.string.text_e3629ac272ce), fontSize = 11.sp, color = EmeraldGreenPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Currency selector
                Text(
                    text = AppText.text(com.example.R.string.text_5136d2baf9c5),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                val currencies = listOf("EGP" to AppText.text(com.example.R.string.text_a70ee3c6a8cd), "USD" to AppText.text(com.example.R.string.text_98ca59bfadc9), "SAR" to AppText.text(com.example.R.string.text_1d09a9c7dfb6), "AED" to AppText.text(com.example.R.string.text_1288bee92f4e))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(currencies) { (code, label) ->
                        val isSelected = selectedCurr == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCurr = code },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Success Message
                if (successMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
                            Text(text = successMessage, fontSize = 12.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Error Message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                            Text(text = errorMessage, fontSize = 12.sp, color = ExpenseRed, lineHeight = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(AppText.text(com.example.R.string.text_5bf826c5e57c))
                    }

                    Button(
                        onClick = {
                            onFetchLivePrices(apiKey, selectedCurr)
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(AppText.text(com.example.R.string.text_51ccc930475c), color = Color.White, fontSize = 12.5.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppText.text(com.example.R.string.text_aa669a2ec51f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCashSavingDialog(
    initialSaving: CashSavingEntity? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Double, currency: String, notes: String, dateMillis: Long) -> Unit
) {
    val appStrings = LocalStrings.current
    var amountText by remember { mutableStateOf(if (initialSaving != null) initialSaving.amount.toLong().toString() else "") }
    var notes by remember { mutableStateOf(initialSaving?.notes ?: "") }
    var dateMillis by remember { mutableStateOf(initialSaving?.dateMillis ?: System.currentTimeMillis()) }

    val amountDouble = amountText.toDoubleOrNull() ?: 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialSaving == null) appStrings.addCashSaving else AppText.text(com.example.R.string.text_342321732127),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = appStrings.close)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(AppText.text(com.example.R.string.text_a6a99774a971)) },
                    placeholder = { Text(AppText.text(com.example.R.string.text_dd178e8508b9)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text(
                            text = AppText.text(com.example.R.string.text_5c54bb48fcff),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppText.text(com.example.R.string.text_a7cc4e694ddc)) },
                    placeholder = { Text(AppText.text(com.example.R.string.text_ebab6415c56f)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(appStrings.cancel)
                    }

                    Button(
                        onClick = {
                            if (amountDouble > 0) {
                                onSave(amountDouble, "EGP", notes.trim(), dateMillis)
                            }
                        },
                        enabled = amountDouble > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Text(appStrings.save, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
