package com.example.ui.dialogs

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
            Text("إدارة وتخصيص الأقسام", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "يمكنك إخفاء أو إظهار أقسام التطبيق حسب احتياجاتك اليومية. لن يتم حذف أي بيانات مسجلة عند إخفاء القسم.",
                    fontSize = 12.sp,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        ModuleToggleRow(
                            title = "الدخل والمصروفات",
                            subtitle = "القسم الأساسي (مفعّل دائمًا)",
                            checked = true,
                            enabled = false,
                            onCheckedChange = {}
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = "صافي الثروة",
                            subtitle = "حساب الأصول والالتزامات",
                            checked = netWorth,
                            onCheckedChange = { netWorth = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = "الديون (لي / عليّ)",
                            subtitle = "إدارة الديون والأموال المستحقة",
                            checked = debts,
                            onCheckedChange = { debts = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = "التقويم المالي",
                            subtitle = "عرض العمليات والالتزامات على مدار الشهر",
                            checked = calendar,
                            onCheckedChange = { calendar = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = "المدخرات النقدية",
                            subtitle = "صناديق الادخار والعملات",
                            checked = savings,
                            onCheckedChange = { savings = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = "الذهب والسبائك",
                            subtitle = "المشغولات، السبائك، وقيمتها الحالية",
                            checked = gold,
                            onCheckedChange = { gold = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = "الالتزامات والفواتير",
                            subtitle = "الإيجار، الفواتير، والأقساط الشهرية",
                            checked = commitments,
                            onCheckedChange = { commitments = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = "دروس الأطفال",
                            subtitle = "حسابات المدرسين ومصروفات المواد",
                            checked = lessons,
                            onCheckedChange = { lessons = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = "الخروجات الجماعية",
                            subtitle = "حساب النزهات وتقسيم المصاريف",
                            checked = outings,
                            onCheckedChange = { outings = it }
                        )
                    }
                    item {
                        ModuleToggleRow(
                            title = "الميزانيات وسقف الإنفاق",
                            subtitle = "تحديد ميزانية لكل تصنيف",
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
                Text("حفظ التغييرات")
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
