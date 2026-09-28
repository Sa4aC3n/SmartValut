package com.example.ui.dialogs

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
                if (type == "OWED_TO_ME") "تسجيل أموال لي عند الآخرين" else "تسجيل دين عليّ للآخرين",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == "OWED_TO_ME",
                        onClick = { type = "OWED_TO_ME" },
                        label = { Text("أموال لي عند الغير") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = type == "I_OWE",
                        onClick = { type = "I_OWE" },
                        label = { Text("دين عليّ للغير") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        errorText = null
                    },
                    label = { Text(if (type == "OWED_TO_ME") "اسم الشخص المدين" else "اسم الشخص الدائن") },
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
                    label = { Text("مبلغ الدين") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات / موعد السداد المتوقع") },
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
                        errorText = "يرجى إدخال اسم الشخص"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorText = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                        return@Button
                    }
                    onConfirm(personName.trim(), type, amount, notes.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
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
                if (debt.type == "OWED_TO_ME") "تحصيل سداد من ${debt.personName}" else "سداد دين لـ ${debt.personName}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "المبلغ المتبقي: ${debt.remainingAmount.toInt()} $currency (من أصل ${debt.originalAmount.toInt()} $currency)",
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = {
                        paymentAmountText = it
                        errorText = null
                    },
                    label = { Text("المبلغ المدفوع / المحصل") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظة على الدفعة (اختياري)") },
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
                        errorText = "يرجى إدخال مبلغ سداد صحيح"
                        return@Button
                    }
                    if (amount > debt.remainingAmount) {
                        errorText = "مبلغ السداد أكبر من المتبقي (${debt.remainingAmount.toInt()} $currency)"
                        return@Button
                    }
                    onConfirm(amount, notes.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text("تأكيد السداد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
