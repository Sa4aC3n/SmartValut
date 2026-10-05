package com.example.ui.dialogs

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
            message = "تم إلغاء الحفظ؛ يمكنك المحاولة مجدداً"
        } else {
            scope.launch {
                try {
                    check(viewModel.activeUserId.value == userId) { "تغير الحساب؛ افتح النافذة مجدداً" }
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri, "wt")
                            ?: error("تعذر فتح الملف للحفظ")
                        output.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    }
                    password = ""
                    message = "تم حفظ النسخة المشفرة بنجاح"
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message = e.message ?: "تعذر حفظ النسخة"
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
                    check(viewModel.activeUserId.value == userId) { "تغير الحساب؛ افتح النافذة مجدداً" }
                    val result = withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(uri) ?: error("تعذر فتح الملف")
                        val payload = input.use { stream ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = stream.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= 20 * 1024 * 1024) { "حجم النسخة يتجاوز 20 ميجابايت" }
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
                        message = result.errorMessage ?: "النسخة غير صالحة"
                    }
                    password = ""
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message = e.message ?: "تعذر قراءة النسخة"
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
            title = { Text("استبدال بيانات الحساب الحالي؟") },
            text = {
                Text("سيتم استبدال بيانات الحساب الحالي بالكامل. احفظ نسخة منها أولاً.\n" +
                    "الحركات: ${restore.transactionCount}، الذهب: ${restore.goldAssetCount}، المدخرات: ${restore.cashSavingCount}.\n" +
                    "صور الإيصالات والمرفقات غير مضمنة. لن تتغير بيانات الحسابات الأخرى.")
            },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val success = viewModel.restoreLocalBackup(userId, requireNotNull(restore.decryptedJson))
                            message = if (success) "تمت الاستعادة بنجاح" else
                                "فشلت الاستعادة ولم تتغير بياناتك. النسخ القديمة للأسرار تتطلب مفتاح الجهاز الأصلي."
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            message = e.message ?: "تعذرت الاستعادة"
                        } finally {
                            pendingRestore = null
                            busy = false
                        }
                    }
                }) { Text("استبدال واستعادة") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { pendingRestore = null }) { Text("إلغاء") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = { if (!busy) onDismiss() },
            title = { Text("النسخ الاحتياطي المشفر") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("احفظ بياناتك المالية وأسرار الخزنة بكلمة مرور. احتفظ بالكلمة لاستعادة النسخة على أي جهاز. الصور والمرفقات غير مضمنة.")
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, enabled = !busy,
                        label = { Text("كلمة مرور النسخة") }, singleLine = true,
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
                                message = e.message ?: "تعذر إنشاء النسخة"
                            }
                        }
                    }) { Text("حفظ نسخة") }
                    Button(enabled = !busy && password.isNotBlank(), onClick = {
                        busy = true
                        message = ""
                        try {
                            openFile.launch(arrayOf("*/*"))
                        } catch (e: Exception) {
                            busy = false
                            message = e.message ?: "تعذر فتح مستعرض الملفات"
                        }
                    }) { Text("فتح نسخة للاستعادة") }
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("إغلاق") } }
        )
    }
}
