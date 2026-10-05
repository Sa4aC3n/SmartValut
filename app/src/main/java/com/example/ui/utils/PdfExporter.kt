package com.example.ui.utils

import com.example.ui.utils.AppStrings

import com.example.ui.utils.AppText

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.UserProfile
import com.example.data.entity.OutingExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateAndOpenFinancialPdf(
        context: Context,
        userProfile: UserProfile,
        vaults: List<VaultEntity>,
        transactions: List<TransactionEntity>,
        currency: String,
        totalIncome: Double,
        totalExpense: Double
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 page size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Header Green Banner Background
        paint.color = Color.parseColor("#0C3B2E") // Emerald dark
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Header Accent Gold Line
        paint.color = Color.parseColor("#FFD700") // Gold
        canvas.drawRect(0f, 96f, 595f, 100f, paint)

        // Header Title Text
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(AppText.text(com.example.R.string.text_f93caee4745f), 297f, 48f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Smart Vault Financial Statement & Analytics", 297f, 72f, paint)

        var y = 130f

        // Date and User Profile Info Card Area
        paint.color = Color.parseColor("#F4F6F5")
        canvas.drawRoundRect(20f, y, 575f, y + 65f, 12f, 12f, paint)

        paint.color = Color.parseColor("#0C3B2E")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_23ff5bd3cda6, userProfile.name), 550f, y + 25f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText(AppText.text(com.example.R.string.text_af62bc118661, userProfile.email, userProfile.phone), 550f, y + 48f, paint)

        val sdfDate = SimpleDateFormat("yyyy/MM/dd - hh:mm a", AppText.locale)
        val currentDateStr = sdfDate.format(Date())
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(AppText.text(com.example.R.string.text_5b5d51e492a1, currentDateStr), 35f, y + 25f, paint)

        y += 85f

        // Financial Summary Box Section
        paint.color = Color.parseColor("#0C3B2E")
        paint.textSize = 15f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_598b3013c93a), 575f, y, paint)

        y += 15f

        // Total Income Card (Green tint)
        paint.color = Color.parseColor("#E8F5E9")
        canvas.drawRoundRect(20f, y, 195f, y + 55f, 10f, 10f, paint)
        paint.color = Color.parseColor("#2E7D32")
        paint.textSize = 11f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(AppText.text(com.example.R.string.text_5e75dfd7b871), 107f, y + 20f, paint)
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("${String.format(Locale.US, "%,.0f", totalIncome)} ${AppText.currency(currency)}", 107f, y + 40f, paint)

        // Total Expense Card (Red tint)
        paint.color = Color.parseColor("#FFEBEE")
        canvas.drawRoundRect(210f, y, 385f, y + 55f, 10f, 10f, paint)
        paint.color = Color.parseColor("#C62828")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText(AppText.text(com.example.R.string.text_d93aba8f3b2c), 297f, y + 20f, paint)
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("${String.format(Locale.US, "%,.0f", totalExpense)} ${AppText.currency(currency)}", 297f, y + 40f, paint)

        // Net Balance Card (Gold tint)
        val netBalance = (totalIncome - totalExpense).coerceAtLeast(0.0)
        paint.color = Color.parseColor("#FFF8E1")
        canvas.drawRoundRect(400f, y, 575f, y + 55f, 10f, 10f, paint)
        paint.color = Color.parseColor("#F57F17")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText(AppText.text(com.example.R.string.text_2df6cb78d320), 487f, y + 20f, paint)
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("${String.format(Locale.US, "%,.0f", netBalance)} ${AppText.currency(currency)}", 487f, y + 40f, paint)

        y += 75f

        // Vaults & Accounts Summary Section
        paint.color = Color.parseColor("#0C3B2E")
        paint.textSize = 15f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_a07daa050122), 575f, y, paint)

        y += 15f

        // Vaults Table Header
        paint.color = Color.parseColor("#6D9773")
        canvas.drawRect(20f, y, 575f, y + 25f, paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_292aa105e256), 550f, y + 17f, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(AppText.text(com.example.R.string.text_19c61065a80f), 35f, y + 17f, paint)

        y += 25f

        vaults.forEachIndexed { idx, vault ->
            paint.color = if (idx % 2 == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#F9F9F9")
            canvas.drawRect(20f, y, 575f, y + 24f, paint)

            paint.color = Color.BLACK
            paint.textSize = 11f
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(vault.name, 550f, y + 16f, paint)

            paint.color = Color.parseColor("#0C3B2E")
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("${String.format(Locale.US, "%,.2f", vault.balance)} ${AppText.currency(currency)}", 35f, y + 16f, paint)

            y += 24f
        }

        y += 20f

        // Recent Transactions Section
        paint.color = Color.parseColor("#0C3B2E")
        paint.textSize = 15f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_06aea1a4b6cb), 575f, y, paint)

        y += 15f

        // Transactions Table Header
        paint.color = Color.parseColor("#0C3B2E")
        canvas.drawRect(20f, y, 575f, y + 25f, paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_70c980e32ac0), 550f, y + 17f, paint)
        canvas.drawText(AppText.text(com.example.R.string.text_7c75fec5c0f8), 350f, y + 17f, paint)
        canvas.drawText(AppText.text(com.example.R.string.text_0fd1ce3ab3e4), 230f, y + 17f, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(AppText.text(com.example.R.string.text_9c3c9f280b7e, AppText.currency(currency)), 35f, y + 17f, paint)

        y += 25f

        val recentTxs = transactions.take(12)
        recentTxs.forEachIndexed { idx, tx ->
            if (y > 780f) return@forEachIndexed // avoid overflowing A4 page

            paint.color = if (idx % 2 == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#F7F9F8")
            canvas.drawRect(20f, y, 575f, y + 22f, paint)

            paint.color = Color.BLACK
            paint.textSize = 10f
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.RIGHT
            val desc = if (tx.description.length > 25) tx.description.take(23) + ".." else tx.description
            canvas.drawText(desc, 550f, y + 15f, paint)
            canvas.drawText(AppStrings(AppText.language).translateCategory(tx.category), 350f, y + 15f, paint)
            canvas.drawText(tx.vaultName, 230f, y + 15f, paint)

            val isIncome = tx.type == "INCOME"
            paint.color = if (isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.LEFT
            val prefix = if (isIncome) "+" else "-"
            canvas.drawText("$prefix${String.format(Locale.US, "%,.0f", tx.amount)}", 35f, y + 15f, paint)

            y += 22f
        }

        // Footer Banner
        paint.color = Color.parseColor("#0C3B2E")
        canvas.drawRect(0f, 815f, 595f, 842f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(AppText.text(com.example.R.string.text_c2de3368a38f), 297f, 832f, paint)

        pdfDocument.finishPage(page)

        try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir != null && !docsDir.exists()) {
                docsDir.mkdirs()
            }
            val pdfFile = File(docsDir, "SmartVault_Financial_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            Toast.makeText(context, AppText.text(com.example.R.string.text_84ea95ad551f, pdfFile.name), Toast.LENGTH_LONG).show()

            // Launch View / Share Intent using FileProvider
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, AppText.text(com.example.R.string.text_6a24d4f395ba)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, AppText.text(com.example.R.string.text_b5ff5ea46c44, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    fun generateAndShareOutingPdf(
        context: Context,
        outingExpenses: List<OutingExpenseEntity>,
        participantsCount: Int,
        currency: String
    ) {
        val totalExpenses = outingExpenses.sumOf { it.amount }
        val validParticipants = if (participantsCount > 0) participantsCount else 1
        val sharePerPerson = totalExpenses / validParticipants

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 page size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Header Green Banner Background
        paint.color = Color.parseColor("#0C3B2E") // Emerald dark
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Header Accent Gold Line
        paint.color = Color.parseColor("#FFD700") // Gold
        canvas.drawRect(0f, 96f, 595f, 100f, paint)

        // Header Title Text
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(AppText.text(com.example.R.string.text_882e9abff232), 297f, 48f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Smart Vault - Outing Expenses & Group Split Statement", 297f, 72f, paint)

        var y = 130f

        // Date and Info Card
        paint.color = Color.parseColor("#F4F6F5")
        canvas.drawRoundRect(20f, y, 575f, y + 50f, 12f, 12f, paint)

        val sdfDate = SimpleDateFormat("yyyy/MM/dd - hh:mm a", AppText.locale)
        val currentDateStr = sdfDate.format(Date())

        paint.color = Color.parseColor("#0C3B2E")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_5b5d51e492a1, currentDateStr), 550f, y + 30f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(AppText.text(com.example.R.string.text_835815ab36b4, outingExpenses.size), 35f, y + 30f, paint)

        y += 70f

        // Summary Statistics Header
        paint.color = Color.parseColor("#0C3B2E")
        paint.textSize = 15f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_7a38397f0000), 575f, y, paint)

        y += 18f

        // Total Expenses Card (Red tint)
        paint.color = Color.parseColor("#FFEBEE")
        canvas.drawRoundRect(20f, y, 195f, y + 60f, 10f, 10f, paint)
        paint.color = Color.parseColor("#C62828")
        paint.textSize = 11f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = false
        canvas.drawText(AppText.text(com.example.R.string.text_eca3d1ae0873), 107f, y + 22f, paint)
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("${String.format(Locale.US, "%,.0f", totalExpenses)} ${AppText.currency(currency)}", 107f, y + 44f, paint)

        // Participants Count Card (Blue tint)
        paint.color = Color.parseColor("#E3F2FD")
        canvas.drawRoundRect(210f, y, 385f, y + 60f, 10f, 10f, paint)
        paint.color = Color.parseColor("#1565C0")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText(AppText.text(com.example.R.string.text_552ed370d702), 297f, y + 22f, paint)
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText(AppText.text(com.example.R.string.text_30d31f5738c7, validParticipants), 297f, y + 44f, paint)

        // Per-person Share Card (Green highlight tint)
        paint.color = Color.parseColor("#E8F5E9")
        canvas.drawRoundRect(400f, y, 575f, y + 60f, 10f, 10f, paint)
        paint.color = Color.parseColor("#2E7D32")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText(AppText.text(com.example.R.string.text_ee9024498f3d), 487f, y + 22f, paint)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("${String.format(Locale.US, "%,.1f", sharePerPerson)} ${AppText.currency(currency)}", 487f, y + 44f, paint)

        y += 85f

        // Expenses List Section
        paint.color = Color.parseColor("#0C3B2E")
        paint.textSize = 15f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_f27a59f61ba0), 575f, y, paint)

        y += 18f

        // Table Header
        paint.color = Color.parseColor("#0C3B2E")
        canvas.drawRect(20f, y, 575f, y + 26f, paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(AppText.text(com.example.R.string.text_b0a293b3248f), 550f, y + 18f, paint)
        canvas.drawText(AppText.text(com.example.R.string.text_6462ae41af9e), 320f, y + 18f, paint)
        canvas.drawText(AppText.text(com.example.R.string.text_a7bc8a435179), 200f, y + 18f, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(AppText.text(com.example.R.string.text_9c3c9f280b7e, AppText.currency(currency)), 35f, y + 18f, paint)

        y += 26f

        val itemSdf = SimpleDateFormat("dd/MM hh:mm a", AppText.locale)

        outingExpenses.forEachIndexed { idx, expense ->
            if (y > 780f) return@forEachIndexed // page bound check

            paint.color = if (idx % 2 == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#F7F9F8")
            canvas.drawRect(20f, y, 575f, y + 24f, paint)

            paint.color = Color.BLACK
            paint.textSize = 10f
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.RIGHT
            val title = if (expense.title.length > 28) expense.title.take(26) + ".." else expense.title
            canvas.drawText(title, 550f, y + 16f, paint)

            val payer = if (expense.payerName.isNotBlank()) expense.payerName else "-"
            canvas.drawText(payer, 320f, y + 16f, paint)

            val dateStr = itemSdf.format(Date(expense.dateMillis))
            canvas.drawText(dateStr, 200f, y + 16f, paint)

            paint.color = Color.parseColor("#C62828")
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("${String.format(Locale.US, "%,.0f", expense.amount)}", 35f, y + 16f, paint)

            y += 24f
        }

        // Footer Banner
        paint.color = Color.parseColor("#0C3B2E")
        canvas.drawRect(0f, 815f, 595f, 842f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(AppText.text(com.example.R.string.text_d1780ad75e2f), 297f, 832f, paint)

        pdfDocument.finishPage(page)

        try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir != null && !docsDir.exists()) {
                docsDir.mkdirs()
            }
            val pdfFile = File(docsDir, "Outing_Expenses_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            Toast.makeText(context, AppText.text(com.example.R.string.text_a8441ff2295f), Toast.LENGTH_SHORT).show()

            // Launch Share Intent using FileProvider
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    AppText.text(com.example.R.string.text_7a1f1081fa08, String.format(Locale.US, "%.0f", totalExpenses), AppText.currency(currency), validParticipants, String.format(Locale.US, "%.1f", sharePerPerson), AppText.currency(currency))
                )
                putExtra(Intent.EXTRA_SUBJECT, AppText.text(com.example.R.string.text_b1b890b5a0eb))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, AppText.text(com.example.R.string.text_c45468e5611d)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, AppText.text(com.example.R.string.text_952072da576d, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exports local transactions and vaults to an Excel-compatible CSV file with UTF-8 BOM.
     * Fully works offline and reads only from local Room entities.
     */
    fun exportFinancialExcelCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        vaults: List<VaultEntity>
    ) {
        try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir != null && !docsDir.exists()) {
                docsDir.mkdirs()
            }
            val csvFile = File(docsDir, "SmartVault_Transactions_${System.currentTimeMillis()}.csv")
            val outputStream = FileOutputStream(csvFile)
            // Write UTF-8 BOM so Microsoft Excel opens Arabic text cleanly
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", AppText.locale)
            val writer = outputStream.bufferedWriter(Charsets.UTF_8)

            writer.write(AppText.text(com.example.R.string.text_9835a8feba07))
            writer.write(AppText.text(com.example.R.string.text_9e3a72a661ba, sdf.format(Date())))

            // Vaults summary
            writer.write(AppText.text(com.example.R.string.text_3e3c9dfb2caf))
            vaults.forEach { v ->
                writer.write("\"${v.name.replace("\"", "\"\"")}\",${v.balance}\n")
            }
            writer.write("\n")

            // Column headers
            writer.write(AppText.text(com.example.R.string.text_8a2cbefb87da))

            // Transactions rows
            transactions.forEach { tx ->
                val typeAr = if (tx.type == "INCOME") AppText.text(com.example.R.string.text_6daff27f6fa5) else AppText.text(com.example.R.string.text_ffed616f0b7b)
                val formattedDate = sdf.format(Date(tx.dateMillis))
                val cleanDesc = tx.description.replace("\"", "\"\"")
                val cleanCat = tx.category.replace("\"", "\"\"")
                val cleanVault = tx.vaultName.replace("\"", "\"\"")
                writer.write("${tx.id},\"$typeAr\",${tx.amount},\"$cleanCat\",\"$cleanDesc\",\"$cleanVault\",$formattedDate\n")
            }

            writer.flush()
            writer.close()
            outputStream.close()

            Toast.makeText(context, AppText.text(com.example.R.string.text_98cf936bc34d, csvFile.name), Toast.LENGTH_SHORT).show()

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, AppText.text(com.example.R.string.text_7429f5a831b5))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, AppText.text(com.example.R.string.text_4d57eda7fba5)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, AppText.text(com.example.R.string.text_c3f85c6d57fd, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }
}
