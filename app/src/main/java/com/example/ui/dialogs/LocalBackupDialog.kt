package com.example.ui.dialogs

import com.example.ui.utils.AppText

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.data.backup.BackupValidationResult
import com.example.data.backup.LocalBackupManager
import com.example.ui.SmartVaultViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Passwords and decrypted payloads are deliberately never stored in saved state or files. */
@Composable
fun LocalBackupDialog(viewModel: SmartVaultViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = remember { viewModel.activeUserId.value }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var encryptedExport by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<BackupValidationResult?>(null) }

    val saveFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val payload = encryptedExport
        encryptedExport = null
        if (uri == null || payload == null) {
            busy = false
            message = AppText.text(com.example.R.string.text_c12100c61b18)
        } else {
            scope.launch {
                try {
                    check(viewModel.activeUserId.value == userId) { AppText.text(com.example.R.string.text_1eb9aa242624) }
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri, "wt")
                            ?: error(AppText.text(com.example.R.string.text_a8793266c256))
                        output.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    }
                    password = ""
                    message = AppText.text(com.example.R.string.text_06f26e015e2c)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message = e.message ?: AppText.text(com.example.R.string.text_414e1e6dc40e)
                } finally {
                    busy = false
                }
            }
        }
    }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            busy = false
        } else {
            scope.launch {
                try {
                    check(viewModel.activeUserId.value == userId) { AppText.text(com.example.R.string.text_1eb9aa242624) }
                    val result = withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(uri) ?: error(AppText.text(com.example.R.string.text_a929b754604e))
                        val payload = input.use { stream ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = stream.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= 20 * 1024 * 1024) { AppText.text(com.example.R.string.text_e6804597584a) }
                                output.write(buffer, 0, count)
                            }
                            output.toString("UTF-8")
                        }
                        LocalBackupManager.validateBackup(payload, password)
                    }
                    if (result.isValid) {
                        pendingRestore = result
                        message = ""
                    } else {
                        message = result.errorMessage ?: AppText.text(com.example.R.string.text_d384371c7d76)
                    }
                    password = ""
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message = e.message ?: AppText.text(com.example.R.string.text_a9bd5dc954ba)
                } finally {
                    busy = false
                }
            }
        }
    }

    val restore = pendingRestore
    if (restore != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) pendingRestore = null },
            title = { Text(AppText.text(com.example.R.string.text_694abc983ec7)) },
            text = {
                Text(AppText.text(com.example.R.string.text_722dca4d5e9b) +
                    AppText.text(com.example.R.string.text_4df94446f435, restore.transactionCount, restore.goldAssetCount, restore.cashSavingCount) +
                    AppText.text(com.example.R.string.text_f82f083ba1cf))
            },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val success = viewModel.restoreLocalBackup(userId, requireNotNull(restore.decryptedJson))
                            message = if (success) AppText.text(com.example.R.string.text_7f40355588ef) else
                                AppText.text(com.example.R.string.text_82e454e3065d)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            message = e.message ?: AppText.text(com.example.R.string.text_b07a7cb3a1af)
                        } finally {
                            pendingRestore = null
                            busy = false
                        }
                    }
                }) { Text(AppText.text(com.example.R.string.text_5fc5f97d822a)) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { pendingRestore = null }) { Text(AppText.text(com.example.R.string.text_e776b0209b50)) } }
        )
    } else {
        AlertDialog(
            onDismissRequest = { if (!busy) onDismiss() },
            title = { Text(AppText.text(com.example.R.string.text_949fece4e9b3)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(AppText.text(com.example.R.string.text_8a0a96f7a552))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, enabled = !busy,
                        label = { Text(AppText.text(com.example.R.string.text_5aa636210f67)) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    if (message.isNotBlank()) Text(message)
                    Button(enabled = !busy && password.isNotBlank(), onClick = {
                        busy = true
                        message = ""
                        scope.launch {
                            try {
                                encryptedExport = viewModel.createLocalBackup(userId, password)
                                saveFile.launch("SmartVault-${System.currentTimeMillis()}.svbackup")
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                encryptedExport = null
                                busy = false
                                message = e.message ?: AppText.text(com.example.R.string.text_f29136956db6)
                            }
                        }
                    }) { Text(AppText.text(com.example.R.string.text_1019d9dd0ede)) }
                    Button(enabled = !busy && password.isNotBlank(), onClick = {
                        busy = true
                        message = ""
                        try {
                            openFile.launch(arrayOf("*/*"))
                        } catch (e: Exception) {
                            busy = false
                            message = e.message ?: AppText.text(com.example.R.string.text_0ba062d0f757)
                        }
                    }) { Text(AppText.text(com.example.R.string.text_90bab6e8c99d)) }
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(AppText.text(com.example.R.string.text_5bf826c5e57c)) } }
        )
    }
}
