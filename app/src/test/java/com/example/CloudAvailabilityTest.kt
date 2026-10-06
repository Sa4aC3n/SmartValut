package com.example

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.example.data.firestore.CloudSyncStatus
import com.example.data.firestore.FirestoreVaultRepository
import com.example.ui.SmartVaultViewModel
import com.example.ui.utils.AppText
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudAvailabilityTest {
    @Test fun missingFirebaseKeepsGuestModeAndReportsAllAuthFailures() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        // Only the isolated Robolectric process is affected; no real account is contacted.
        FirebaseApp.getApps(application).toList().forEach { it.delete() }
        val store = ViewModelStore()
        try {
            val vm = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory(application))
                .get(SmartVaultViewModel::class.java)
            assertNull(vm.firebaseAuth)
            assertEquals("local_guest", vm.activeUserId.value)
            assertFalse(vm.userProfile.value.isLoggedIn)
            assertEquals(CloudSyncStatus.OFFLINE, vm.cloudSyncStatus.value)
            assertNotNull(WorkManager.getInstance(application))
            for (language in listOf("ar", "en")) {
                AppText.setLanguage(language)
                val errors = mutableListOf<String>()
                val error: (String) -> Unit = { errors.add(it) }
                vm.registerWithFirebase("test@example.com", "password123", "Test",
                    { fail("Must not report verification sent") }, error)
                vm.loginWithFirebase("test@example.com", "password123",
                    { fail("Must not authenticate") }, { fail("No request should be sent") }, error)
                vm.resendVerificationEmail("test@example.com", "password123",
                    { fail("Must not report email sent") }, error)
                vm.sendPasswordReset("test@example.com", { fail("Must not report reset sent") }, error)
                assertEquals(List(4) { AppText.text(R.string.label_cloud_unavailable) }, errors)
            }
            assertTrue("No fallback Firebase app may be created", FirebaseApp.getApps(application).isEmpty())
        } finally {
            store.clear()
            AppText.setLanguage("ar")
        }
    }

    @Test fun unavailableFirestoreRejectsPhotoUploadAndRetriesInitialization() = runTest {
        var attempts = 0
        val repository = FirestoreVaultRepository {
            attempts++
            throw IllegalStateException("Missing Firebase configuration")
        }
        try {
            for (language in listOf("ar", "en")) {
                AppText.setLanguage(language)
                try {
                    repository.uploadProfilePhoto("user", Uri.EMPTY)
                    fail("Must not upload or return a URL without Firestore")
                } catch (error: IllegalStateException) {
                    assertEquals(AppText.text(R.string.label_cloud_unavailable), error.message)
                }
                assertEquals(CloudSyncStatus.OFFLINE, repository.syncStatus.value)
            }
            assertEquals("Do not permanently cache unavailable Firestore", 2, attempts)
        } finally {
            AppText.setLanguage("ar")
        }
    }
}
