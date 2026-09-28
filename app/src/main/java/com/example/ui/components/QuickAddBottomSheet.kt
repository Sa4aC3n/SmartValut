package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.ModuleConfiguration
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
    val backgroundTint: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    sheetState: SheetState,
    config: ModuleConfiguration,
    onDismiss: () -> Unit,
    onOpenAddIncome: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenAddCashSaving: () -> Unit,
    onOpenAddGold: () -> Unit,
    onOpenAddDebt: () -> Unit,
    onOpenAddCommitment: () -> Unit,
    onOpenAddLesson: () -> Unit,
    onOpenAddOuting: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "مركز الإضافة السريعة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            val actions = mutableListOf<QuickActionItem>()

            // Income (Always enabled)
            actions.add(
                QuickActionItem(
                    title = "دخل جديد",
                    icon = Icons.Default.ArrowDownward,
                    tint = IncomeGreen,
                    backgroundTint = IncomeGreen.copy(alpha = 0.12f),
                    onClick = {
                        onDismiss()
                        onOpenAddIncome()
                    }
                )
            )

            // Expense (Always enabled)
            actions.add(
                QuickActionItem(
                    title = "مصروف جديد",
                    icon = Icons.Default.ArrowUpward,
                    tint = ExpenseRed,
                    backgroundTint = ExpenseRed.copy(alpha = 0.12f),
                    onClick = {
                        onDismiss()
                        onOpenAddExpense()
                    }
                )
            )

            // Transfer (Between Vaults)
            actions.add(
                QuickActionItem(
                    title = "تحويل خزن",
                    icon = Icons.Default.SwapHoriz,
                    tint = EmeraldGreenPrimary,
                    backgroundTint = EmeraldGreenPrimary.copy(alpha = 0.12f),
                    onClick = {
                        onDismiss()
                        onOpenTransfer()
                    }
                )
            )

            // Debts (If enabled)
            if (config.debts) {
                actions.add(
                    QuickActionItem(
                        title = "تسجيل دين",
                        icon = Icons.Default.AccountBalance,
                        tint = Color(0xFF7C4DFF),
                        backgroundTint = Color(0xFF7C4DFF).copy(alpha = 0.12f),
                        onClick = {
                            onDismiss()
                            onOpenAddDebt()
                        }
                    )
                )
            }

            // Cash Savings (If enabled)
            if (config.savings) {
                actions.add(
                    QuickActionItem(
                        title = "ادخار نقدي",
                        icon = Icons.Default.AccountBalanceWallet,
                        tint = Color(0xFF009688),
                        backgroundTint = Color(0xFF009688).copy(alpha = 0.12f),
                        onClick = {
                            onDismiss()
                            onOpenAddCashSaving()
                        }
                    )
                )
            }

            // Gold (If enabled)
            if (config.gold) {
                actions.add(
                    QuickActionItem(
                        title = "شراء ذهب",
                        icon = Icons.Default.MonetizationOn,
                        tint = GoldAccent,
                        backgroundTint = GoldAccent.copy(alpha = 0.15f),
                        onClick = {
                            onDismiss()
                            onOpenAddGold()
                        }
                    )
                )
            }

            // Commitments (If enabled)
            if (config.commitments) {
                actions.add(
                    QuickActionItem(
                        title = "التزام / فاتورة",
                        icon = Icons.Default.EventNote,
                        tint = Color(0xFFE91E63),
                        backgroundTint = Color(0xFFE91E63).copy(alpha = 0.12f),
                        onClick = {
                            onDismiss()
                            onOpenAddCommitment()
                        }
                    )
                )
            }

            // Lessons (If enabled)
            if (config.lessons) {
                actions.add(
                    QuickActionItem(
                        title = "درس طفل",
                        icon = Icons.Default.School,
                        tint = Color(0xFF3F51B5),
                        backgroundTint = Color(0xFF3F51B5).copy(alpha = 0.12f),
                        onClick = {
                            onDismiss()
                            onOpenAddLesson()
                        }
                    )
                )
            }

            // Outings (If enabled)
            if (config.outings) {
                actions.add(
                    QuickActionItem(
                        title = "خروجة جماعية",
                        icon = Icons.Default.Attractions,
                        tint = Color(0xFFFF9800),
                        backgroundTint = Color(0xFFFF9800).copy(alpha = 0.12f),
                        onClick = {
                            onDismiss()
                            onOpenAddOuting()
                        }
                    )
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(actions) { action ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { action.onClick() }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(action.backgroundTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.title,
                                tint = action.tint,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = action.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
