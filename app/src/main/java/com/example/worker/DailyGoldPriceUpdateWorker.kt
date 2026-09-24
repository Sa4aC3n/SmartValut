package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.SmartVaultRepository
import com.example.data.api.GoldApiClient
import java.util.Locale

class DailyGoldPriceUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("smart_vault_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gold_api_key", GoldApiClient.DEFAULT_API_KEY)
            ?.ifBlank { GoldApiClient.DEFAULT_API_KEY } ?: GoldApiClient.DEFAULT_API_KEY

        val rawCurrency = prefs.getString("currency", "EGP") ?: "EGP"
        val mappedCurrency = when (rawCurrency.trim()) {
            "ج.م", "EGP", "جنيه", "LE" -> "EGP"
            "$", "USD", "دولار" -> "USD"
            "ر.س", "SAR", "ريال" -> "SAR"
            "د.إ", "AED", "درهم" -> "AED"
            "€", "EUR", "يورو" -> "EUR"
            "د.ك", "KWD" -> "KWD"
            "د.ب", "BHD" -> "BHD"
            "ر.ع", "OMR" -> "OMR"
            "ر.ق", "QAR" -> "QAR"
            else -> "EGP"
        }

        val result = GoldApiClient.fetchGoldPrices(
            apiKey = apiKey,
            currency = mappedCurrency,
            symbol = "XAU"
        )

        return result.fold(
            onSuccess = { livePrices ->
                val db = AppDatabase.getInstance(context)
                val repository = SmartVaultRepository(db)
                repository.updateAllGoldPrices(livePrices.toKaratMap())

                val now = System.currentTimeMillis()
                prefs.edit().putLong("gold_price_last_update_ts", now).apply()

                showGoldUpdatedNotification(livePrices.priceGram21k, livePrices.priceGram24k, mappedCurrency)
                Result.success()
            },
            onFailure = {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        )
    }

    private fun showGoldUpdatedNotification(price21: Double, price24: Double, currency: String) {
        val channelId = "daily_gold_price_channel"
        val notificationId = 2002

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "أسعار الذهب اليومية",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات التحديث اليومي لأسعار الذهب (12:00 ظهراً)"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val p21Formatted = String.format(Locale.US, "%,.0f", price21)
        val p24Formatted = String.format(Locale.US, "%,.0f", price24)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("تحديث أسعار الذهب (12:00 ظهراً) 🪙")
            .setContentText("عيار 21: $p21Formatted $currency | عيار 24: $p24Formatted $currency")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("تم تحديث أسعار الذهب لحظياً من GoldAPI.io بنجاح:\n• عيار 21: $p21Formatted $currency\n• عيار 24: $p24Formatted $currency\nتمت إعادة تقييم محفظة الذهب وسبائكك تلقائياً.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
