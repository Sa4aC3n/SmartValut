package com.example.data.firestore

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.ui.utils.AppText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudAvailabilityTest {
    @Before fun initializeTranslations() {
        AppText.initialize(ApplicationProvider.getApplicationContext())
        AppText.setLanguage("ar")
    }

    @After fun resetLanguage() {
        AppText.setLanguage("ar")
    }

    @Test fun missingFirestoreDoesNotCacheFailureOrClaimPhotoUploadSuccess() = runBlocking {
        var attempts = 0
        val repository = FirestoreVaultRepository {
            attempts++
            throw IllegalStateException("Missing Firebase configuration")
        }

        for (language in listOf("ar", "en")) {
            AppText.setLanguage(language)
            val error = try {
                repository.uploadProfilePhoto("user", Uri.EMPTY)
                fail("Must not report an uploaded photo without saving its profile URL")
                return@runBlocking
            } catch (expected: IllegalStateException) {
                expected
            }
            assertEquals(AppText.text(com.example.R.string.label_cloud_unavailable), error.message)
            assertEquals(CloudSyncStatus.OFFLINE, repository.syncStatus.value)
        }
        assertEquals("Unavailable Firestore should be retried", 2, attempts)
    }
}