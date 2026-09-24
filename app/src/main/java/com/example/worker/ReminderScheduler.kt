package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME_DAILY = "daily_expense_reminder_work"
    private const val WORK_NAME_TEST = "test_expense_reminder_work"
    private const val WORK_NAME_GOLD_DAILY = "daily_gold_price_update_work"
    private const val WORK_NAME_GOLD_IMMEDIATE = "immediate_gold_price_update_work"

    fun scheduleDailyReminder(context: Context, hourOfDay: Int = 20, minute: Int = 0) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyExpenseReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_DAILY,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_DAILY)
    }

    fun triggerTestNotificationNow(context: Context) {
        val testWorkRequest = OneTimeWorkRequestBuilder<DailyExpenseReminderWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_TEST,
            ExistingWorkPolicy.REPLACE,
            testWorkRequest
        )
    }

    /**
     * Schedules automatic daily live gold price update at 12:00 PM noon.
     */
    fun scheduleDailyGoldPriceUpdate(context: Context, hourOfDay: Int = 12, minute: Int = 0) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dailyGoldWorkRequest = PeriodicWorkRequestBuilder<DailyGoldPriceUpdateWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_GOLD_DAILY,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyGoldWorkRequest
        )
    }

    fun cancelDailyGoldPriceUpdate(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_GOLD_DAILY)
    }

    fun triggerImmediateGoldUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DailyGoldPriceUpdateWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_GOLD_IMMEDIATE,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
