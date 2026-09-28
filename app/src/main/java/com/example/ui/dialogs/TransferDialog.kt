package com.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.VaultEntity
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    vaults: List<VaultEntity>,
    onDismiss: () -> Unit,
    onConfirm: (fromVault: String, toVault: String, amount: Double, notes: String) -> String? // returns error if any
) {
    if (vaults.size < 2) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("تحويل بين الخزن", fontWeight = FontWeight.Bold) },
            text = { Text("تحتاج إلى وجود خزنتين على الأقل لتتمكن من إجراء التحويل.") },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                ) {
                    Text("حسناً")
                }
            }
        )
        return
    }

    var fromVaultName by remember { mutableStateOf(vaults.firstOrNull()?.name ?: "") }
    var toVaultName by remember { mutableStateOf(vaults.getOrNull(1)?.name ?: "") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }

    val fromVault = vaults.find { it.name == fromVaultName }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحويل بين الخزن", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Source Vault
                ExposedDropdownMenuBox(
                    expanded = expandedFrom,
                    onExpandedChange = { expandedFrom = it }
                ) {
                    OutlinedTextField(
                        value = "$fromVaultName (الرصيد: ${fromVault?.balance?.toInt() ?: 0})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("من خزنة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFrom,
                        onDismissRequest = { expandedFrom = false }
                    ) {
                        vaults.forEach { vault ->
                            DropdownMenuItem(
                                text = { Text("${vault.name} (رصيد: ${vault.balance.toInt()})") },
                                onClick = {
                                    fromVaultName = vault.name
                                    expandedFrom = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Destination Vault
                ExposedDropdownMenuBox(
                    expanded = expandedTo,
                    onExpandedChange = { expandedTo = it }
                ) {
                    OutlinedTextField(
                        value = toVaultName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("إلى خزنة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTo,
                        onDismissRequest = { expandedTo = false }
                    ) {
                        vaults.filter { it.name != fromVaultName }.forEach { vault ->
                            DropdownMenuItem(
                                text = { Text("${vault.name} (رصيد: ${vault.balance.toInt()})") },
                                onClick = {
                                    toVaultName = vault.name
                                    expandedTo = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("المبلغ المحول") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظة (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = ExpenseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "يرجى إدخال مبلغ تحويل صحيح"
                        return@Button
                    }
                    if (fromVaultName == toVaultName) {
                        errorMessage = "لا يمكن التحويل من الخزنة إلى نفسها"
                        return@Button
                    }
                    if (fromVault != null && fromVault.balance < amount) {
                        errorMessage = "رصيد الخزنة (${fromVault.balance.toInt()}) لا يكفي للتحويل"
                        return@Button
                    }

                    val err = onConfirm(fromVaultName, toVaultName, amount, notes)
                    if (err != null) {
                        errorMessage = err
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text("تنفيذ التحويل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
