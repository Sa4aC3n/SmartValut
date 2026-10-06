package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.worker.ReminderScheduler
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkManagerInitializationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun testCallingScheduleDailyReminderTwiceDoesNotCrash() {
        ReminderScheduler.scheduleDailyReminder(context)
        ReminderScheduler.scheduleDailyReminder(context)
    }

    @Test
    fun testCallingScheduleDailyGoldPriceUpdateTwiceDoesNotCrash() {
        ReminderScheduler.scheduleDailyGoldPriceUpdate(context)
        ReminderScheduler.scheduleDailyGoldPriceUpdate(context)
    }

    @Test
    fun testCancelAndRescheduleWorks() {
        ReminderScheduler.scheduleDailyReminder(context)
        ReminderScheduler.cancelDailyReminder(context)
        ReminderScheduler.scheduleDailyReminder(context)

        ReminderScheduler.scheduleDailyGoldPriceUpdate(context)
        ReminderScheduler.cancelDailyGoldPriceUpdate(context)
        ReminderScheduler.scheduleDailyGoldPriceUpdate(context)
    }

    @Test
    fun testNoManualWorkManagerInitializeInProductionSource() {
        val srcDir = File("src/main/java")
        val prodFiles = if (srcDir.exists()) {
            srcDir.walkTopDown().filter { it.extension == "kt" }.toList()
        } else {
            File("app/src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()
        }
        for (file in prodFiles) {
            val content = file.readText()
            assertFalse(
                "File ${file.name} must not manually call WorkManager.initialize()",
                content.contains("WorkManager.initialize(")
            )
            assertFalse(
                "File ${file.name} must not implement Configuration.Provider",
                content.contains("Configuration.Provider")
            )
        }
    }
}
