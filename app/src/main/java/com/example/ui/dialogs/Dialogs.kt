package com.example.ui.dialogs

import com.example.ui.utils.AppText
import com.example.ui.utils.AppStrings

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.data.entity.VaultEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.ChildLessonEntity
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.MintBackground
import com.example.ui.theme.CardBorderColor
import com.example.ui.utils.CategoryUtils
import com.example.ui.utils.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    vaults: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, description: String, vaultName: String, dateMillis: Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CategoryUtils.incomeCategories.first()) }
    var description by remember { mutableStateOf("") }
    var selectedVault by remember { mutableStateOf(vaults.firstOrNull() ?: "الخزنة الرئيسية") }
    var selectedDateText by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date()))
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var vaultExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppText.text(com.example.R.string.text_2045dbe9ad39),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IncomeGreen
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(AppText.text(com.example.R.string.text_8399a8121b07)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IncomeGreen)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = AppStrings(AppText.language).translateCategory(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppText.text(com.example.R.string.text_9daaed987022)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        CategoryUtils.incomeCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(AppStrings(AppText.language).translateCategory(category)) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vault Dropdown
                if (vaults.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = vaultExpanded,
                        onExpandedChange = { vaultExpanded = !vaultExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedVault,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppText.text(com.example.R.string.text_6e200a34c778)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vaultExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = vaultExpanded,
                            onDismissRequest = { vaultExpanded = false }
                        ) {
                            vaults.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v) },
                                    onClick = {
                                        selectedVault = v
                                        vaultExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(AppText.text(com.example.R.string.text_d554c7b27da7)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && !isSubmitting) {
                        isSubmitting = true
                        val parsedDateMillis = try {
                            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(selectedDateText)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        onConfirm(amt, selectedCategory, description.ifBlank { selectedCategory }, selectedVault, parsedDateMillis)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(AppText.text(com.example.R.string.text_5e3a3fdfce20), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppText.text(com.example.R.string.text_e776b0209b50))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    vaults: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, description: String, vaultName: String, receiptPath: String?, dateMillis: Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appStrings = LocalStrings.current
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CategoryUtils.expenseCategories.firstOrNull() ?: "خضروات") }
    var description by remember { mutableStateOf("") }
    var selectedVault by remember { mutableStateOf(vaults.firstOrNull() ?: "الخزنة الرئيسية") }
    var selectedDateText by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date()))
    }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var receiptName by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        receiptUri = uri
        if (uri != null) {
            receiptName = if (appStrings.isEn) "Receipt Attached ✓" else AppText.text(com.example.R.string.text_5741b09082a3)
        }
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var vaultExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF5F8F6),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Title & Close 'X' Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appStrings.addExpense,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B241E)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = appStrings.close,
                            tint = Color(0xFF5A665E),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Field 1: Amount (المبلغ)
                Text(
                    text = appStrings.amount,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3931),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = {
                        Text(
                            text = "0",
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.End,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B241E)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFD4E0D7),
                        unfocusedBorderColor = Color(0xFFE2EBE4)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Field 2: Category (التصنيف)
                Text(
                    text = appStrings.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3931),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = appStrings.translateCategory(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = EmeraldGreenPrimary,
                            unfocusedBorderColor = EmeraldGreenPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        CategoryUtils.expenseCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(appStrings.translateCategory(category)) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Field 3: Description (الوصف)
                Text(
                    text = appStrings.notes,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3931),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            text = appStrings.optional,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.End,
                        fontSize = 15.sp,
                        color = Color(0xFF1B241E)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFD4E0D7),
                        unfocusedBorderColor = Color(0xFFE2EBE4)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Field 4: Date (التاريخ)
                Text(
                    text = appStrings.date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3931),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = selectedDateText,
                    onValueChange = { selectedDateText = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B241E)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFD4E0D7),
                        unfocusedBorderColor = Color(0xFFE2EBE4)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Field 5: Receipt Image (صورة الفاتورة)
                Text(
                    text = appStrings.receiptImage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3931),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2EBE4))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = if (receiptUri != null) EmeraldGreenPrimary else Color(0xFF2D3931),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = receiptName ?: appStrings.attachReceipt,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (receiptUri != null) EmeraldGreenPrimary else Color(0xFF2D3931)
                        )
                    }
                }

                // Vault dropdown if multiple vaults exist
                if (vaults.size > 1) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = appStrings.vault,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2D3931),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = vaultExpanded,
                        onExpandedChange = { vaultExpanded = !vaultExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedVault,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vaultExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFFD4E0D7),
                                unfocusedBorderColor = Color(0xFFE2EBE4)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = vaultExpanded,
                            onDismissRequest = { vaultExpanded = false }
                        ) {
                            vaults.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v) },
                                    onClick = {
                                        selectedVault = v
                                        vaultExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Save Button ("حفظ")
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && !isSubmitting) {
                            isSubmitting = true
                            val savedPath = receiptUri?.let { uri ->
                                com.example.ui.utils.ImageUtils.saveUriToInternalStorage(context, uri)
                            }
                            val parsedDateMillis = try {
                                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(selectedDateText)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            onConfirm(amt, selectedCategory, description.ifBlank { selectedCategory }, selectedVault, savedPath, parsedDateMillis)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = appStrings.save,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AddCommitmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, isRecurring: Boolean, notes: String, receiptPath: String?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appStrings = LocalStrings.current
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDayText by remember { mutableStateOf("1") }
    var isRecurring by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var receiptName by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        receiptUri = uri
        if (uri != null) {
            receiptName = if (appStrings.isEn) "Receipt Attached ✓" else AppText.text(com.example.R.string.text_5741b09082a3)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF5F8F6),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Title & Close 'X' Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appStrings.addCommitment,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B241E)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = appStrings.close,
                            tint = Color(0xFF5A665E),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Field 1: Name (الاسم)
                Text(
                    text = appStrings.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3931),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            text = appStrings.commitmentExample,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.End,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B241E)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = EmeraldGreenPrimary,
                        unfocusedBorderColor = EmeraldGreenPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Row for Due Day (يوم الاستحقاق) & Amount (المبلغ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Due Day (يوم الاستحقاق) - Left side in RTL
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appStrings.dueDay,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D3931),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = dueDayText,
                            onValueChange = { dueDayText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1B241E)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFFD4E0D7),
                                unfocusedBorderColor = Color(0xFFE2EBE4)
                            )
                        )
                    }

                    // Amount (المبلغ) - Right side in RTL
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appStrings.amount,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D3931),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.End,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1B241E)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFFD4E0D7),
                                unfocusedBorderColor = Color(0xFFE2EBE4)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Switch container: Repeats monthly (يتكرر شهرياً)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFEFF4F0)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreenPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray
                            )
                        )
                        Text(
                            text = appStrings.repeatMonthly,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B241E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Field 5: Notes (ملاحظات)
                Text(
                    text = appStrings.notes,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3931),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    minLines = 3,
                    maxLines = 5,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.End,
                        fontSize = 15.sp,
                        color = Color(0xFF1B241E)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFD4E0D7),
                        unfocusedBorderColor = Color(0xFFE2EBE4)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Receipt Image Attachment
                Text(
                    text = appStrings.receiptImage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3931),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2EBE4))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = if (receiptUri != null) EmeraldGreenPrimary else Color(0xFF2D3931),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = receiptName ?: appStrings.attachReceipt,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (receiptUri != null) EmeraldGreenPrimary else Color(0xFF2D3931)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Save Button ("حفظ")
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amt > 0) {
                            val savedPath = receiptUri?.let { uri ->
                                com.example.ui.utils.ImageUtils.saveUriToInternalStorage(context, uri)
                            }
                            val data = mapOf<String, Any>(
                                "id" to "",
                                "title" to title.trim(),
                                "amount" to amt,
                                "dueDateMillis" to System.currentTimeMillis(),
                                "isPaid" to false,
                                "isRecurringMonthly" to isRecurring,
                                "notes" to notes,
                                "createdAt" to "ServerTimestamp",
                                "updatedAt" to "ServerTimestamp"
                            ).let { m ->
                                if (savedPath != null) m + ("receiptImagePath" to savedPath) else m
                            }

                            val debugText = data.entries.joinToString("\n") { (key, value) ->
                                "$key = $value (${value?.javaClass?.simpleName ?: "null"})"
                            }
                            android.app.AlertDialog.Builder(context)
                                .setTitle(AppText.text(com.example.R.string.text_caf3f5fa344d))
                                .setMessage(debugText)
                                .setPositiveButton(AppText.text(com.example.R.string.text_b730937d1d5e)) { _, _ ->
                                    onConfirm(title, amt, isRecurring, notes, savedPath)
                                }
                                .show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = appStrings.save,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AddChildLessonDialog(
    onDismiss: () -> Unit,
    onConfirm: (childName: String, subject: String, teacherName: String, amount: Double, receiptPath: String?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appStrings = LocalStrings.current
    var childName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var receiptName by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        receiptUri = uri
        if (uri != null) {
            receiptName = if (appStrings.isEn) "Receipt Attached ✓" else AppText.text(com.example.R.string.text_5741b09082a3)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppText.text(com.example.R.string.text_a02759f37c7e),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreenPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = childName,
                    onValueChange = { childName = it },
                    label = { Text(AppText.text(com.example.R.string.text_c97c2377ad3c)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(AppText.text(com.example.R.string.text_d5681a90cfde)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text(AppText.text(com.example.R.string.text_0667a9bf31ce)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(AppText.text(com.example.R.string.text_5e42a81c0bdb)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2EBE4))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = if (receiptUri != null) EmeraldGreenPrimary else Color(0xFF2D3931),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = receiptName ?: appStrings.attachReceipt,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (receiptUri != null) EmeraldGreenPrimary else Color(0xFF2D3931)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (childName.isNotBlank() && amt > 0) {
                        val savedPath = receiptUri?.let { uri ->
                            com.example.ui.utils.ImageUtils.saveUriToInternalStorage(context, uri)
                        }
                        val data = mapOf<String, Any>(
                            "id" to "",
                            "childName" to childName.trim(),
                            "subject" to subject.trim(),
                            "teacherName" to teacherName.trim(),
                            "amount" to amt,
                            "dueDateMillis" to System.currentTimeMillis(),
                            "isPaid" to false,
                            "createdAt" to "ServerTimestamp",
                            "updatedAt" to "ServerTimestamp"
                        ).let { m ->
                            if (savedPath != null) m + ("receiptImagePath" to savedPath) else m
                        }

                        val debugText = data.entries.joinToString("\n") { (key, value) ->
                            "$key = $value (${value?.javaClass?.simpleName ?: "null"})"
                        }
                        android.app.AlertDialog.Builder(context)
                            .setTitle(AppText.text(com.example.R.string.text_caf3f5fa344d))
                            .setMessage(debugText)
                            .setPositiveButton(AppText.text(com.example.R.string.text_b730937d1d5e)) { _, _ ->
                                onConfirm(childName, subject, teacherName, amt, savedPath)
                            }
                            .show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text(AppText.text(com.example.R.string.text_c82c597a6f7d))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppText.text(com.example.R.string.text_e776b0209b50)) }
        }
    )
}

@Composable
fun AddVaultDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, initialBalance: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = AppText.text(com.example.R.string.text_4cdf4b9f0083), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppText.text(com.example.R.string.text_02f7925bc7b6)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text(AppText.text(com.example.R.string.text_d85b2adf0086)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, bal)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text(AppText.text(com.example.R.string.text_866cb767ecfb))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(AppText.text(com.example.R.string.text_e776b0209b50)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetDialog(
    category: String,
    currentLimit: Double,
    onDismiss: () -> Unit,
    onConfirm: (category: String, limit: Double) -> Unit,
    onDelete: ((category: String) -> Unit)? = null
) {
    var categoryText by remember { mutableStateOf(category) }
    var limitText by remember { mutableStateOf(if (currentLimit > 0) (if (currentLimit % 1.0 == 0.0) currentLimit.toLong().toString() else currentLimit.toString()) else "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val presetCategories = listOf("سوبر ماركت", "مطاعم", "بنزين", "كهرباء", "إيجار", "خضروات", "مواصلات", "فواتير", "علاج", "مصاريف عامة")

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(AppText.text(com.example.R.string.text_b0a9803991e9), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(AppText.text(com.example.R.string.text_0a6fb171955e, categoryText.ifEmpty { category })) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete?.invoke(categoryText.ifEmpty { category })
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppText.text(com.example.R.string.text_2d2bbdc2d694))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(AppText.text(com.example.R.string.text_e776b0209b50)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (category.isNotEmpty() && currentLimit > 0) AppText.text(com.example.R.string.text_166e9d1b4fe3, category) else AppText.text(com.example.R.string.text_32f1966a4cd5),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = AppText.text(com.example.R.string.text_e18899dad359))

                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    label = { Text(AppText.text(com.example.R.string.text_1a0f3b39aac5)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(text = AppText.text(com.example.R.string.text_7b4764ccb0f0), fontSize = 12.sp, color = Color.Gray)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presetCategories) { cat ->
                        FilterChip(
                            selected = categoryText == cat,
                            onClick = { categoryText = cat },
                            label = { Text(AppStrings(AppText.language).translateCategory(cat), fontSize = 11.sp) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text(AppText.text(com.example.R.string.text_0e2c15ffddf0)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null && (category.isNotEmpty() || currentLimit > 0)) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text(AppText.text(com.example.R.string.text_ef1c45607fc6))
                    }
                }

                Button(
                    onClick = {
                        val limit = limitText.toDoubleOrNull() ?: 0.0
                        val finalCat = categoryText.trim()
                        if (finalCat.isNotEmpty() && limit >= 0) {
                            onConfirm(finalCat, limit)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                ) {
                    Text(AppText.text(com.example.R.string.text_6d5aa037cf62))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(AppText.text(com.example.R.string.text_e776b0209b50)) } }
    )
}

@Composable
fun EditVaultDialog(
    vault: VaultEntity,
    onDismiss: () -> Unit,
    onConfirm: (id: Int, name: String, balance: Double) -> Unit,
    onDelete: (id: Int) -> Unit
) {
    var name by remember { mutableStateOf(vault.name) }
    var balanceText by remember { mutableStateOf(if (vault.balance % 1.0 == 0.0) vault.balance.toLong().toString() else vault.balance.toString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(AppText.text(com.example.R.string.text_53ccaef115ec), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(AppText.text(com.example.R.string.text_97fab5f15441, vault.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(vault.id)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppText.text(com.example.R.string.text_5ef55c0db740), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(AppText.text(com.example.R.string.text_98df46fbd83b)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = AppText.text(com.example.R.string.text_f55f825ec231), fontWeight = FontWeight.Bold)
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = AppText.text(com.example.R.string.text_7ce2b7078a8e),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppText.text(com.example.R.string.text_4555e916f59f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text(AppText.text(com.example.R.string.text_8e6dada6dcc2)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: vault.balance
                    if (name.isNotBlank()) {
                        onConfirm(vault.id, name, bal)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text(AppText.text(com.example.R.string.text_33081e44cb7c))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(AppText.text(com.example.R.string.text_e776b0209b50)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnpaidBillsDialog(
    commitments: List<CommitmentEntity>,
    lessons: List<ChildLessonEntity>,
    currency: String,
    onPayCommitment: (CommitmentEntity) -> Unit,
    onPayLesson: (ChildLessonEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val unpaidCommitments = remember(AppText.language, commitments) { commitments.filter { !it.isPaid } }
    val unpaidLessons = remember(AppText.language, lessons) { lessons.filter { !it.isPaid } }

    val totalUnpaidCount = unpaidCommitments.size + unpaidLessons.size
    val totalUnpaidAmount = unpaidCommitments.sumOf { it.amount } + unpaidLessons.sumOf { it.amount }

    val sdf = remember(AppText.language) { SimpleDateFormat("dd/MM/yyyy", AppText.locale) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ExpenseRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = AppText.text(com.example.R.string.text_a0b93eb47f97),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = AppText.text(com.example.R.string.text_b87ef5afd60a, totalUnpaidCount, String.format(Locale.US, "%,.0f", totalUnpaidAmount), AppText.currency(currency)),
                        fontSize = 11.5.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (totalUnpaidCount == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = AppText.text(com.example.R.string.text_1222aedee374),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = AppText.text(com.example.R.string.text_0d209f6617e0),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    // Commitments
                    if (unpaidCommitments.isNotEmpty()) {
                        Text(
                            text = AppText.text(com.example.R.string.text_f23d86570e8d),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )

                        unpaidCommitments.forEach { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MintBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = AppText.text(com.example.R.string.text_e359071b6d6c, String.format(Locale.US, "%,.0f", item.amount), AppText.currency(currency)),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ExpenseRed
                                        )
                                        val dateStr = try { sdf.format(Date(item.dueDateMillis)) } catch (e: Exception) { "" }
                                        if (dateStr.isNotEmpty()) {
                                            Text(
                                                text = AppText.text(com.example.R.string.text_82f89f773df0, dateStr),
                                                fontSize = 10.5.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        if (item.notes.isNotBlank()) {
                                            Text(
                                                text = AppText.text(com.example.R.string.text_d20206869e15, item.notes),
                                                fontSize = 10.5.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { onPayCommitment(item) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(AppText.text(com.example.R.string.text_00892ccddcd9), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Child Lessons
                    if (unpaidLessons.isNotEmpty()) {
                        Text(
                            text = AppText.text(com.example.R.string.text_0647a642ebee),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )

                        unpaidLessons.forEach { lesson ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MintBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${lesson.childName} • ${lesson.subject}",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = AppText.text(com.example.R.string.text_765b68c05b0d, lesson.teacherName),
                                            fontSize = 11.5.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = AppText.text(com.example.R.string.text_b0a5de0afd74, String.format(Locale.US, "%,.0f", lesson.amount), AppText.currency(currency)),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ExpenseRed
                                        )
                                        val dateStr = try { sdf.format(Date(lesson.dueDateMillis)) } catch (e: Exception) { "" }
                                        if (dateStr.isNotEmpty()) {
                                            Text(
                                                text = AppText.text(com.example.R.string.text_82f89f773df0, dateStr),
                                                fontSize = 10.5.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { onPayLesson(lesson) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(AppText.text(com.example.R.string.text_00892ccddcd9), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(AppText.text(com.example.R.string.text_5bf826c5e57c), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
