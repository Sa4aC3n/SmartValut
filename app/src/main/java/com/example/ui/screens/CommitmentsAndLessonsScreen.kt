package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.utils.CategoryUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.utils.LocalStrings

@Composable
fun CommitmentsAndLessonsScreen(
    commitments: List<CommitmentEntity>,
    lessons: List<ChildLessonEntity>,
    currency: String,
    onPayCommitment: (CommitmentEntity) -> Unit,
    onPayLesson: (ChildLessonEntity) -> Unit,
    onDeleteCommitment: (Int) -> Unit,
    onDeleteLesson: (Int) -> Unit,
    onOpenAddCommitment: () -> Unit,
    onOpenAddLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appStrings = LocalStrings.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(appStrings.commitmentsTab, appStrings.lessonsTab)
    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }

    viewingReceiptPath?.let { path ->
        com.example.ui.utils.ReceiptImageViewerDialog(
            imagePathOrUri = path,
            onDismiss = { viewingReceiptPath = null }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) onOpenAddCommitment() else onOpenAddLesson()
                },
                containerColor = EmeraldGreenPrimary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة جديد")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) EmeraldGreenPrimary else Color.Gray
                            )
                        }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // MONTHLY COMMITMENTS TAB
                val totalCommitments = commitments.sumOf { it.amount }
                val paidCommitments = commitments.filter { it.isPaid }.sumOf { it.amount }
                val remainingCommitments = totalCommitments - paidCommitments

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "إجمالي الالتزامات", fontSize = 11.sp, color = Color.Gray)
                                    Text(text = "${String.format(Locale.US, "%.0f", totalCommitments)} $currency", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "تم دفعه", fontSize = 11.sp, color = Color.Gray)
                                    Text(text = "${String.format(Locale.US, "%.0f", paidCommitments)} $currency", fontWeight = FontWeight.Bold, color = IncomeGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "المتبقي", fontSize = 11.sp, color = Color.Gray)
                                    Text(text = "${String.format(Locale.US, "%.0f", remainingCommitments)} $currency", fontWeight = FontWeight.Bold, color = ExpenseRed)
                                }
                            }
                        }
                    }

                    if (commitments.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(text = "لا توجد التزامات شهرية مسجلة. اضغط + لإضافة إيجار أو فواتير.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(commitments) { item ->
                            CommitmentCardItem(
                                item = item,
                                currency = currency,
                                onPay = { onPayCommitment(item) },
                                onDelete = { onDeleteCommitment(item.id) },
                                onViewReceipt = { path -> viewingReceiptPath = path }
                            )
                        }
                    }
                }
            } else {
                // CHILDREN LESSONS TAB
                val totalLessons = lessons.sumOf { it.amount }
                val paidLessons = lessons.filter { it.isPaid }.sumOf { it.amount }
                val remainingLessons = totalLessons - paidLessons

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "إجمالي الدروس", fontSize = 11.sp, color = Color.Gray)
                                    Text(text = "${String.format(Locale.US, "%.0f", totalLessons)} $currency", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "تم دفعه", fontSize = 11.sp, color = Color.Gray)
                                    Text(text = "${String.format(Locale.US, "%.0f", paidLessons)} $currency", fontWeight = FontWeight.Bold, color = IncomeGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "المتبقي للدروس", fontSize = 11.sp, color = Color.Gray)
                                    Text(text = "${String.format(Locale.US, "%.0f", remainingLessons)} $currency", fontWeight = FontWeight.Bold, color = ExpenseRed)
                                }
                            }
                        }
                    }

                    if (lessons.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(text = "لا توجد دروس أطفال مسجلة. اضغط + لإضافة درس جديدة.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(lessons) { lesson ->
                            LessonCardItem(
                                lesson = lesson,
                                currency = currency,
                                onPay = { onPayLesson(lesson) },
                                onDelete = { onDeleteLesson(lesson.id) },
                                onViewReceipt = { path -> viewingReceiptPath = path }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommitmentCardItem(
    item: CommitmentEntity,
    currency: String,
    onPay: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: ((String) -> Unit)? = null
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale("ar")) }
    val formattedDate = remember(item.dueDateMillis) { sdf.format(Date(item.dueDateMillis)) }
    val icon = CategoryUtils.getCategoryIcon(item.title)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (item.isPaid) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.title,
                        tint = if (item.isPaid) IncomeGreen else ExpenseRed
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "تاريخ الاستحقاق: $formattedDate", fontSize = 12.sp, color = Color.Gray)
                    if (!item.receiptImagePath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        com.example.ui.utils.ReceiptBadgeButton(
                            imagePathOrUri = item.receiptImagePath!!,
                            onClick = { onViewReceipt?.invoke(item.receiptImagePath!!) }
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", item.amount)} $currency",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (item.isPaid) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تم الدفع",
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "تم الدفع",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "غير مدفوع",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = onPay,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تم الدفع",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تم الدفع",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.notes.isNotEmpty()) {
                    Text(
                        text = item.notes,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun LessonCardItem(
    lesson: ChildLessonEntity,
    currency: String,
    onPay: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: ((String) -> Unit)? = null
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale("ar")) }
    val formattedDate = remember(lesson.dueDateMillis) { sdf.format(Date(lesson.dueDateMillis)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(EmeraldGreenPrimary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "درس",
                        tint = EmeraldGreenPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${lesson.childName} • ${lesson.subject}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "المدرس: ${lesson.teacherName} | $formattedDate", fontSize = 12.sp, color = Color.Gray)
                    if (!lesson.receiptImagePath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        com.example.ui.utils.ReceiptBadgeButton(
                            imagePathOrUri = lesson.receiptImagePath!!,
                            onClick = { onViewReceipt?.invoke(lesson.receiptImagePath!!) }
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", lesson.amount)} $currency",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (lesson.isPaid) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تم الدفع",
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "تم الدفع",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "غير مدفوع",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = onPay,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تم الدفع",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تم الدفع",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray)
                }
            }
        }
    }
}
