package com.example.ui.screens

import com.example.ui.utils.AppText

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.CloudSyncStatus
import com.example.data.model.CloudVaultItem
import com.example.data.model.VaultActivityLog
import com.example.data.model.VaultItemType
import com.example.ui.SmartVaultViewModel
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.utils.PasswordGenerator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudVaultScreen(
    viewModel: SmartVaultViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val items by viewModel.cloudVaultItems.collectAsStateWithLifecycle()
    val activityLogs by viewModel.cloudActivityLogs.collectAsStateWithLifecycle()
    val syncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val isLocked by viewModel.isCloudVaultLocked.collectAsStateWithLifecycle()

    // Periodically check for 2-minute inactivity lock
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L) // Check every 15 seconds
            viewModel.checkInactivityAutoLock()
        }
    }

    // Filter & Search states
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf<String?>(null) } // null = All

    // Dialog States
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<CloudVaultItem?>(null) }
    var showPasswordGenDialog by remember { mutableStateOf(false) }
    var showActivityLogDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    // Map of revealed plaintexts for security (item.id -> decrypted string)
    val revealedDataMap = remember { mutableStateMapOf<String, String>() }

    // Notify activity whenever user touches the screen
    val onTouchActivity = {
        viewModel.onUserInteracted()
    }

    if (isLocked) {
        // Biometric / PIN Lock Screen
        VaultLockScreen(
            onUnlock = {
                viewModel.unlockCloudVault()
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Cloud Firestore Sync & Encryption Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTouchActivity() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when (syncStatus) {
                                        CloudSyncStatus.CONNECTED -> EmeraldGreenPrimary.copy(alpha = 0.2f)
                                        CloudSyncStatus.SYNCING -> GoldAccent.copy(alpha = 0.2f)
                                        CloudSyncStatus.OFFLINE -> Color.Gray.copy(alpha = 0.2f)
                                        CloudSyncStatus.ERROR -> Color.Red.copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (syncStatus) {
                                    CloudSyncStatus.CONNECTED -> Icons.Default.CloudDone
                                    CloudSyncStatus.SYNCING -> Icons.Default.Sync
                                    CloudSyncStatus.OFFLINE -> Icons.Default.Cloud
                                    CloudSyncStatus.ERROR -> Icons.Default.Cloud
                                },
                                contentDescription = null,
                                tint = when (syncStatus) {
                                    CloudSyncStatus.CONNECTED -> EmeraldGreenPrimary
                                    CloudSyncStatus.SYNCING -> GoldAccent
                                    CloudSyncStatus.OFFLINE -> Color.Gray
                                    CloudSyncStatus.ERROR -> Color.Red
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = syncStatus.labelAr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = syncStatus.iconEmoji, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AppText.text(com.example.R.string.text_3306a9374e57),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Lock Now button
                    IconButton(
                        onClick = {
                            viewModel.lockCloudVault()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = AppText.text(com.example.R.string.text_fd14703a0a80),
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Action Buttons (Password Gen, Activity Log, Encrypted Backup)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Password Generator Button
                OutlinedButton(
                    onClick = {
                        onTouchActivity()
                        showPasswordGenDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmeraldGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = AppText.text(com.example.R.string.text_da6e6d26cdc6), fontSize = 11.sp, maxLines = 1)
                }

                // Activity Log Button
                OutlinedButton(
                    onClick = {
                        onTouchActivity()
                        showActivityLogDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GoldAccent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = AppText.text(com.example.R.string.text_c3e160350e30), fontSize = 11.sp, maxLines = 1)
                }

                // Encrypted Backup Button
                OutlinedButton(
                    onClick = {
                        onTouchActivity()
                        showBackupDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmeraldGreenDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = AppText.text(com.example.R.string.text_811cabca45ae), fontSize = 11.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Search & Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onTouchActivity()
                },
                placeholder = { Text(AppText.text(com.example.R.string.text_3e2d41206804), fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldGreenPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = AppText.text(com.example.R.string.text_0d97fd6e069e), modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterType == null,
                        onClick = {
                            selectedFilterType = null
                            onTouchActivity()
                        },
                        label = { Text(AppText.text(com.example.R.string.text_904779de8074, items.size), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreenPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = EmeraldGreenPrimary
                        )
                    )
                }
                items(VaultItemType.entries) { type ->
                    val count = items.count { it.type == type.rawType }
                    FilterChip(
                        selected = selectedFilterType == type.rawType,
                        onClick = {
                            selectedFilterType = if (selectedFilterType == type.rawType) null else type.rawType
                            onTouchActivity()
                        },
                        label = { Text("${type.iconEmoji} ${type.localizedTitle} ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreenPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = EmeraldGreenPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filtered Items
            val filteredItems = items.filter { item ->
                val matchesType = selectedFilterType == null || item.type.equals(selectedFilterType, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                        item.title.contains(searchQuery, ignoreCase = true) ||
                        item.category.contains(searchQuery, ignoreCase = true)
                matchesType && matchesSearch
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreenPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldGreenPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedFilterType != null)
                                AppText.text(com.example.R.string.text_f0d6b532fd55)
                            else
                                AppText.text(com.example.R.string.text_31dd7827ba4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = AppText.text(com.example.R.string.text_e39fb1c44b15),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isRevealed = revealedDataMap.containsKey(item.id)
                        val decryptedText = revealedDataMap[item.id] ?: ""

                        VaultItemCard(
                            item = item,
                            isRevealed = isRevealed,
                            decryptedText = decryptedText,
                            onToggleReveal = {
                                onTouchActivity()
                                if (isRevealed) {
                                    revealedDataMap.remove(item.id)
                                } else {
                                    val plain = viewModel.decryptVaultData(item.encryptedData)
                                    revealedDataMap[item.id] = plain
                                }
                            },
                            onCopy = { text ->
                                onTouchActivity()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("VaultData", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, AppText.text(com.example.R.string.text_1f7d44005382), Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                onTouchActivity()
                                editingItem = item
                                showAddEditDialog = true
                            },
                            onDelete = {
                                onTouchActivity()
                                viewModel.deleteCloudVaultItem(
                                    itemId = item.id,
                                    itemTitle = item.title,
                                    onSuccess = {
                                        Toast.makeText(context, AppText.text(com.example.R.string.text_271dc1c1ef0a), Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add New Item
        FloatingActionButton(
            onClick = {
                onTouchActivity()
                editingItem = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = EmeraldGreenPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = AppText.text(com.example.R.string.text_0be31b74e082))
        }
    }

    // --- Dialogs ---

    // 1. Add / Edit Vault Item Dialog
    if (showAddEditDialog) {
        AddEditVaultItemDialog(
            item = editingItem,
            onDismiss = {
                showAddEditDialog = false
                editingItem = null
            },
            onSave = { title, type, plainData, category ->
                viewModel.saveCloudVaultItem(
                    title = title,
                    type = type,
                    plaintext = plainData,
                    category = category,
                    existingId = editingItem?.id ?: "",
                    onSuccess = {
                        showAddEditDialog = false
                        editingItem = null
                        Toast.makeText(context, AppText.text(com.example.R.string.text_7c5f51aaf731), Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            decryptExisting = { encrypted ->
                viewModel.decryptVaultData(encrypted)
            }
        )
    }

    // 2. Strong Password Generator Dialog
    if (showPasswordGenDialog) {
        PasswordGeneratorDialog(
            onDismiss = { showPasswordGenDialog = false },
            onCopy = { pwd ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("GeneratedPassword", pwd)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, AppText.text(com.example.R.string.text_e532d79c9402), Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. Activity Log Dialog
    if (showActivityLogDialog) {
        ActivityLogDialog(
            logs = activityLogs,
            onDismiss = { showActivityLogDialog = false }
        )
    }

    // 4. Encrypted JSON Backup Dialog
    if (showBackupDialog) {
        com.example.ui.dialogs.LocalBackupDialog(
            viewModel = viewModel,
            onDismiss = { showBackupDialog = false }
        )
    }
}

@Composable
fun VaultItemCard(
    item: CloudVaultItem,
    isRevealed: Boolean,
    decryptedText: String,
    onToggleReveal: () -> Unit,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val typeEnum = VaultItemType.fromRaw(item.type)
    val dateFormat = remember(AppText.language) { SimpleDateFormat("yyyy/MM/dd HH:mm", AppText.locale) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = typeEnum.iconEmoji, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.category,
                                fontSize = 11.sp,
                                color = EmeraldGreenPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(text = " • ", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = dateFormat.format(Date(item.updatedAt)),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Action buttons (Edit & Delete)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = AppText.text(com.example.R.string.text_b4f76c3aa21e), tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = AppText.text(com.example.R.string.text_2d2bbdc2d694), tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Encrypted Data State Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isRevealed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0x10000000)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isRevealed) {
                        Text(
                            text = decryptedText,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onCopy(decryptedText) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = AppText.text(com.example.R.string.text_46e6841e2136), tint = EmeraldGreenPrimary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onToggleReveal, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = AppText.text(com.example.R.string.text_0804220142f6), tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "••••••••••••••••••••",
                                letterSpacing = 2.sp,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }

                        TextButton(
                            onClick = onToggleReveal,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = AppText.text(com.example.R.string.text_4bde76230633), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(AppText.text(com.example.R.string.text_e17b604a24c6)) },
            text = { Text(AppText.text(com.example.R.string.text_5d7a567d8dca, item.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(AppText.text(com.example.R.string.text_494feab789bf))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(AppText.text(com.example.R.string.text_e776b0209b50))
                }
            }
        )
    }
}

@Composable
fun AddEditVaultItemDialog(
    item: CloudVaultItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, type: String, plainData: String, category: String) -> Unit,
    decryptExisting: (String) -> String
) {
    var title by remember { mutableStateOf(item?.title ?: "") }
    var selectedType by remember { mutableStateOf(item?.type ?: "password") }
    var category by remember { mutableStateOf(item?.category ?: "عام") }
    var payload by remember {
        mutableStateOf(if (item != null) decryptExisting(item.encryptedData) else "")
    }
    var showSecret by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordGenInside by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (item == null) AppText.text(com.example.R.string.text_03d4f49a9f53) else AppText.text(com.example.R.string.text_2758eaf60da4),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = AppText.text(com.example.R.string.text_931f42b60f68), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VaultItemType.entries.forEach { type ->
                        val isSelected = selectedType == type.rawType
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type.rawType },
                            label = { Text("${type.iconEmoji} ${type.localizedTitle}", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreenPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = EmeraldGreenPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text(AppText.text(com.example.R.string.text_245ea7c50a6b)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category field
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(AppText.text(com.example.R.string.text_f746871b441d)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Secret Payload field
                OutlinedTextField(
                    value = payload,
                    onValueChange = {
                        payload = it
                        errorMessage = null
                    },
                    label = {
                        Text(
                            when (selectedType) {
                                "password" -> AppText.text(com.example.R.string.text_7f112b75b4d6)
                                "card" -> AppText.text(com.example.R.string.text_0b410d0448e6)
                                "note" -> AppText.text(com.example.R.string.text_cd86b470984d)
                                else -> AppText.text(com.example.R.string.text_a7c4e82d7f6c)
                            }
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedType == "password") {
                                IconButton(onClick = { showPasswordGenInside = true }) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = AppText.text(com.example.R.string.text_da6e6d26cdc6), tint = GoldAccent)
                                }
                            }
                            IconButton(onClick = { showSecret = !showSecret }) {
                                Icon(
                                    imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (selectedType == "note" || selectedType == "document") 3 else 1,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Encryption guarantee badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1000897B), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = EmeraldGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppText.text(com.example.R.string.text_54cdc9f60a75),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 15.sp
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = AppText.text(com.example.R.string.text_ae865794266f)
                        return@Button
                    }
                    if (payload.isBlank()) {
                        errorMessage = AppText.text(com.example.R.string.text_19d3d5df6a5e)
                        return@Button
                    }
                    onSave(title.trim(), selectedType, payload.trim(), category.trim().ifBlank { "عام" })
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text(AppText.text(com.example.R.string.text_a3a29271cc23))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppText.text(com.example.R.string.text_e776b0209b50))
            }
        }
    )

    if (showPasswordGenInside) {
        PasswordGeneratorDialog(
            onDismiss = { showPasswordGenInside = false },
            onCopy = { gen ->
                payload = gen
                showSecret = true
                showPasswordGenInside = false
            }
        )
    }
}

@Composable
fun PasswordGeneratorDialog(
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    var length by remember { mutableFloatStateOf(16f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }

    var generatedPassword by remember {
        mutableStateOf(
            PasswordGenerator.generate(
                length = length.toInt(),
                includeUppercase = includeUppercase,
                includeNumbers = includeNumbers,
                includeSymbols = includeSymbols
            )
        )
    }

    val strength = PasswordGenerator.evaluateStrength(generatedPassword)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppText.text(com.example.R.string.text_50b8de962ea0), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Generated Password Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = generatedPassword,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = EmeraldGreenPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                generatedPassword = PasswordGenerator.generate(
                                    length = length.toInt(),
                                    includeUppercase = includeUppercase,
                                    includeNumbers = includeNumbers,
                                    includeSymbols = includeSymbols
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = AppText.text(com.example.R.string.text_194a8a3e1619), tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Strength Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = AppText.text(com.example.R.string.text_9579eae2b253), fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = strength.labelAr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (strength) {
                            PasswordGenerator.PasswordStrength.VERY_STRONG -> EmeraldGreenPrimary
                            PasswordGenerator.PasswordStrength.STRONG -> EmeraldGreenDark
                            PasswordGenerator.PasswordStrength.MEDIUM -> GoldAccent
                            else -> Color.Red
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Length Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = AppText.text(com.example.R.string.text_0d4da760f02f), fontSize = 12.sp)
                    Text(text = AppText.text(com.example.R.string.text_f1a168373935, length.toInt()), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldGreenPrimary)
                }

                Slider(
                    value = length,
                    onValueChange = {
                        length = it
                        generatedPassword = PasswordGenerator.generate(
                            length = it.toInt(),
                            includeUppercase = includeUppercase,
                            includeNumbers = includeNumbers,
                            includeSymbols = includeSymbols
                        )
                    },
                    valueRange = 8f..32f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldGreenPrimary,
                        activeTrackColor = EmeraldGreenPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Checkboxes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeUppercase,
                        onCheckedChange = {
                            includeUppercase = it
                            generatedPassword = PasswordGenerator.generate(
                                length = length.toInt(),
                                includeUppercase = it,
                                includeNumbers = includeNumbers,
                                includeSymbols = includeSymbols
                            )
                        },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreenPrimary)
                    )
                    Text(text = AppText.text(com.example.R.string.text_a34b6b4ce659), fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeNumbers,
                        onCheckedChange = {
                            includeNumbers = it
                            generatedPassword = PasswordGenerator.generate(
                                length = length.toInt(),
                                includeUppercase = includeUppercase,
                                includeNumbers = it,
                                includeSymbols = includeSymbols
                            )
                        },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreenPrimary)
                    )
                    Text(text = AppText.text(com.example.R.string.text_b3b0acd2e3dc), fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeSymbols,
                        onCheckedChange = {
                            includeSymbols = it
                            generatedPassword = PasswordGenerator.generate(
                                length = length.toInt(),
                                includeUppercase = includeUppercase,
                                includeNumbers = includeNumbers,
                                includeSymbols = it
                            )
                        },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreenPrimary)
                    )
                    Text(text = AppText.text(com.example.R.string.text_d2e4d9585aa6), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopy(generatedPassword)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppText.text(com.example.R.string.text_1b3717f1298b))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppText.text(com.example.R.string.text_5bf826c5e57c))
            }
        }
    )
}

@Composable
fun ActivityLogDialog(
    logs: List<VaultActivityLog>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember(AppText.language) { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", AppText.locale) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = GoldAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppText.text(com.example.R.string.text_e20fd67d44a3), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = AppText.text(com.example.R.string.text_9c607d6922dd), color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (log.action) {
                                                "created" -> EmeraldGreenPrimary.copy(alpha = 0.2f)
                                                "updated" -> GoldAccent.copy(alpha = 0.2f)
                                                "deleted" -> Color.Red.copy(alpha = 0.2f)
                                                else -> Color.Gray.copy(alpha = 0.2f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (log.action) {
                                            "created" -> "➕"
                                            "updated" -> "✏️"
                                            "deleted" -> "🗑️"
                                            else -> "📋"
                                        },
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (log.action) {
                                            "created" -> AppText.text(com.example.R.string.text_bf83e7501261, log.itemTitle)
                                            "updated" -> AppText.text(com.example.R.string.text_f8b496281864, log.itemTitle)
                                            "deleted" -> AppText.text(com.example.R.string.text_75791690d672, log.itemTitle)
                                            else -> log.itemTitle
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)) {
                Text(AppText.text(com.example.R.string.text_5bf826c5e57c))
            }
        }
    )
}

@Composable
fun EncryptedBackupDialog(
    backupJson: String,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppText.text(com.example.R.string.text_8d8199888617), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = AppText.text(com.example.R.string.text_777ff5690379),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = backupJson,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onShare(backupJson) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppText.text(com.example.R.string.text_2c4618769704))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppText.text(com.example.R.string.text_5bf826c5e57c))
            }
        }
    )
}

@Composable
fun VaultLockScreen(
    onUnlock: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F201B), Color(0xFF06110D))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreenPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = EmeraldGreenPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = AppText.text(com.example.R.string.text_8a7c72902813),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = AppText.text(com.example.R.string.text_f9fab4694b1f),
                fontSize = 13.sp,
                color = Color(0xFFA0B5AA),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = AppText.text(com.example.R.string.text_e2326a8899ce),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
