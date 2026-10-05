package com.example.ui.dialogs

import com.example.ui.utils.AppText

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.ModuleConfiguration
import com.example.ui.theme.EmeraldGreenPrimary

@Composable
fun ManageModulesDialog(
    currentConfig: ModuleConfiguration,
    onDismiss: () -> Unit,
    onSave: (ModuleConfiguration) -> Unit
) {
    var netWorth by remember { mutableStateOf(currentConfig.netWorth) }
    var debts by remember { mutableStateOf(currentConfig.debts) }
    var calendar by remember { mutableStateOf(currentConfig.calendar) }
    var savings by remember { mutableStateOf(currentConfig.savings) }
    var gold by remember { mutableStateOf(currentConfig.gold) }
    var commitments by remember { mutableStateOf(currentConfig.commitments) }
    var lessons by remember { mutableStateOf(currentConfig.lessons) }
    var outings by remember { mutableStateOf(currentConfig.outings) }
    var budgets by remember { mutableStateOf(currentConfig.budgets) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(AppText.text(com.example.R.string.text_d0e559035070), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = AppText.text(com.example.R.string.text_241c729c5948),
                    fontSize = 12.sp,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_ac5f8981223b),
                            subtitle = AppText.text(com.example.R.string.text_9eabacadf3bd),
                            checked = true,
                            enabled = false,
                            onCheckedChange = {}
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_0b8ff6a5604b),
                            subtitle = AppText.text(com.example.R.string.text_5053d7bf195f),
                            checked = netWorth,
                            onCheckedChange = { netWorth = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_3c7023c98558),
                            subtitle = AppText.text(com.example.R.string.text_630aff3fd3d6),
                            checked = debts,
                            onCheckedChange = { debts = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_3ac532621c03),
                            subtitle = AppText.text(com.example.R.string.text_4fc432f8f7b5),
                            checked = calendar,
                            onCheckedChange = { calendar = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_0911e2b39ae7),
                            subtitle = AppText.text(com.example.R.string.text_8203c5a07039),
                            checked = savings,
                            onCheckedChange = { savings = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_270cfe7362c3),
                            subtitle = AppText.text(com.example.R.string.text_78bca3b4e55d),
                            checked = gold,
                            onCheckedChange = { gold = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_bb5f9ffd9b6d),
                            subtitle = AppText.text(com.example.R.string.text_9e754462d4ab),
                            checked = commitments,
                            onCheckedChange = { commitments = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_6831249d5a13),
                            subtitle = AppText.text(com.example.R.string.text_25b22a8dc6ba),
                            checked = lessons,
                            onCheckedChange = { lessons = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_1e293181f8da),
                            subtitle = AppText.text(com.example.R.string.text_6eee8a54550a),
                            checked = outings,
                            onCheckedChange = { outings = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = AppText.text(com.example.R.string.text_73227b4300b7),
                            subtitle = AppText.text(com.example.R.string.text_b3e90b0100ae),
                            checked = budgets,
                            onCheckedChange = { budgets = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ModuleConfiguration(
                            incomeExpense = true,
                            netWorth = netWorth,
                            debts = debts,
                            calendar = calendar,
                            savings = savings,
                            gold = gold,
                            commitments = commitments,
                            lessons = lessons,
                            outings = outings,
                            budgets = budgets
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text(AppText.text(com.example.R.string.text_33081e44cb7c))
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
private fun ModuleToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreenPrimary)
        )
    }
}
