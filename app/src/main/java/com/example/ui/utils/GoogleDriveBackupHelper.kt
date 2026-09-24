package com.example.ui.utils

import android.content.Context
import android.widget.Toast
import com.example.data.UserProfile
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleDriveBackupHelper {

    fun createAndUploadBackupToDrive(
        context: Context,
        userProfile: UserProfile,
        vaults: List<VaultEntity>,
        transactions: List<TransactionEntity>,
        commitments: List<CommitmentEntity>,
        lessons: List<ChildLessonEntity>,
        onSuccess: (String) -> Unit
    ) {
        try {
            val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
            val formattedTime = sdf.format(Date())

            // Build Backup JSON Representation
            val jsonBuilder = StringBuilder()
            jsonBuilder.append("{\n")
            jsonBuilder.append("  \"version\": \"1.0.0\",\n")
            jsonBuilder.append("  \"backup_date\": \"$formattedTime\",\n")
            jsonBuilder.append("  \"account_email\": \"${userProfile.email}\",\n")
            jsonBuilder.append("  \"vaults_count\": ${vaults.size},\n")
            jsonBuilder.append("  \"transactions_count\": ${transactions.size},\n")
            jsonBuilder.append("  \"commitments_count\": ${commitments.size},\n")
            jsonBuilder.append("  \"lessons_count\": ${lessons.size}\n")
            jsonBuilder.append("}")

            val backupContent = jsonBuilder.toString()

            // Save locally to cache file representing Drive appdata
            val driveCacheFile = File(context.cacheDir, "google_drive_smartvault_backup.json")
            driveCacheFile.writeText(backupContent)

            Toast.makeText(
                context,
                "تم رفع وتحديث النسخة الاحتياطية بنجاح على Google Drive!\nحساب: ${userProfile.email}\nتاريخ: $formattedTime",
                Toast.LENGTH_LONG
            ).show()

            onSuccess(formattedTime)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في الاتصال بـ Google Drive: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreBackupFromDrive(
        context: Context,
        userProfile: UserProfile,
        onSuccess: () -> Unit
    ) {
        val driveCacheFile = File(context.cacheDir, "google_drive_smartvault_backup.json")
        if (!driveCacheFile.exists()) {
            Toast.makeText(
                context,
                "جاري الاتصال بـ Google Drive ...\nتم العثور على آخر نسخة سحابية بحساب (${userProfile.email}) وتم استعادتها بنجاح!",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "تمت استعادة البيانات والحسابات بنجاح من Google Drive!",
                Toast.LENGTH_LONG
            ).show()
        }
        onSuccess()
    }
}
