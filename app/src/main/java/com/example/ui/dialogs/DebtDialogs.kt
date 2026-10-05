package com.example.ui.dialogs

import com.example.ui.utils.AppText

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import com.example.data.entity.DebtEntity
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed

@Composable
fun AddDebtDialog(
    initialType: String = "OWED_TO_ME", // "OWED_TO_ME" (لي عنده) or "I_OWE" (عليّ له)
    onDismiss: () -> Unit,
    onConfirm: (personName: String, type: String, amount: Double, notes: String) -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (type == "OWED_TO_ME") AppText.text(com.example.R.string.text_bf8d7bd53353) else AppText.text(com.example.R.string.text_a37cd3567d4d),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == "OWED_TO_ME",
                        onClick = { type = "OWED_TO_ME" },
                        label = { Text(AppText.text(com.example.R.string.text_7612847d2233)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = type == "I_OWE",
                        onClick = { type = "I_OWE" },
                        label = { Text(AppText.text(com.example.R.string.text_dd169bf23200)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        errorText = null
                    },
                    label = { Text(if (type == "OWED_TO_ME") AppText.text(com.example.R.string.text_7201473b2591) else AppText.text(com.example.R.string.text_63feb3bb804e)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorText = null
                    },
                    label = { Text(AppText.text(com.example.R.string.text_bc44a773ac92)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppText.text(com.example.R.string.text_c8a0cdd5189a)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorText!!, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (personName.isBlank()) {
                        errorText = AppText.text(com.example.R.string.text_19af7a0f2ad8)
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorText = AppText.text(com.example.R.string.text_268c5b73a9b0)
                        return@Button
                    }
                    onConfirm(personName.trim(), type, amount, notes.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text(AppText.text(com.example.R.string.text_56ee6e0d206b))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppText.text(com.example.R.string.text_e776b0209b50))
            }
        }
    )
}

@Composable
fun RecordDebtPaymentDialog(
    debt: DebtEntity,
    currency: String = "ج.م",
    onDismiss: () -> Unit,
    onConfirm: (paymentAmount: Double, notes: String) -> Unit
) {
    var paymentAmountText by remember { mutableStateOf(debt.remainingAmount.toInt().toString()) }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (debt.type == "OWED_TO_ME") AppText.text(com.example.R.string.text_b1635a0b10cd, debt.personName) else AppText.text(com.example.R.string.text_56d7f604af7f, debt.personName),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = AppText.text(com.example.R.string.text_082322fac562, debt.remainingAmount.toInt(), AppText.currency(currency), debt.originalAmount.toInt(), AppText.currency(currency)),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = {
                        paymentAmountText = it
                        errorText = null
                    },
                    label = { Text(AppText.text(com.example.R.string.text_f88f6cb033cb)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppText.text(com.example.R.string.text_d40d75caab23)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorText!!, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = paymentAmountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorText = AppText.text(com.example.R.string.text_c77f84848501)
                        return@Button
                    }
                    if (amount > debt.remainingAmount) {
                        errorText = AppText.text(com.example.R.string.text_9ec43fbe481b, debt.remainingAmount.toInt(), AppText.currency(currency))
                        return@Button
                    }
                    onConfirm(amount, notes.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text(AppText.text(com.example.R.string.text_11b0cc7ecdb7))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppText.text(com.example.R.string.text_e776b0209b50))
            }
        }
    )
}
