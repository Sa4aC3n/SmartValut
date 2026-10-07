package com.example.data.firestore

import com.example.ui.utils.AppText

import android.util.Log
import com.example.data.model.CloudVaultItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

enum class CloudSyncStatus(private val labelId: Int, val iconEmoji: String) {
    CONNECTED(com.example.R.string.text_d69dfb852af8, "🟢"),
    SYNCING(com.example.R.string.text_c0d85c5fe38a, "🔄"),
    OFFLINE(com.example.R.string.text_4384dc18a516, "🟡"),
    ERROR(com.example.R.string.text_22a67ceb72fd, "🔴");

    val labelAr: String get() = AppText.text(labelId)
}

/**
 * Cloud Account Profile & Auth Repository ONLY.
 *
 * All financial data (income, expenses, vaults, savings, gold, outings, budgets)
 * has been permanently removed from Cloud Firestore and is stored exclusively
 * in the local Room database (Local-First architecture).
 */
class FirestoreVaultRepository(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {

    private val tag = "FirestoreVaultRepo"

    private var cachedFirestore: FirebaseFirestore? = null

    // Cache success only: a missing configuration must not permanently poison this repository.
    private val firestore: FirebaseFirestore?
        get() = cachedFirestore ?: try {
            val db = firestoreProvider()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w(tag, "Firestore settings already applied or error: ${e.message}")
            }
            cachedFirestore = db
            db
        } catch (e: IllegalStateException) {
            Log.w(tag, "Firestore not available", e)
            _syncStatus.value = CloudSyncStatus.OFFLINE
            null
        }

    private val _syncStatus = MutableStateFlow(CloudSyncStatus.OFFLINE)
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    fun setSyncStatus(status: CloudSyncStatus) {
        _syncStatus.value = status
    }

    /**
     * Saves or updates minimal non-financial profile fields:
     * Path: users/{uid}
     * Allowed fields: displayName, email, updatedAt
     */
    fun saveUserDocument(displayName: String = "") {
        val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
        if (currentUser == null) {
            _syncStatus.value = CloudSyncStatus.OFFLINE
            Log.d(tag, "[AUTH OFFLINE] saveUserDocument skipped: user is not authenticated in Firebase Auth")
            return
        }
        val uid = currentUser.uid
        val userMap = hashMapOf<String, Any>(
            "userId" to uid,
            "email" to (currentUser.email ?: ""),
            "displayName" to displayName.ifBlank { currentUser.displayName ?: "" },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val fs = firestore ?: return
        _syncStatus.value = CloudSyncStatus.SYNCING
        fs.collection("users").document(uid)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                _syncStatus.value = CloudSyncStatus.CONNECTED
                Log.d(tag, "User profile document updated successfully for UID: $uid")
            }
            .addOnFailureListener { e ->
                _syncStatus.value = CloudSyncStatus.ERROR
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(tag, "[SECURITY/RULES ERROR] PERMISSION_DENIED on saveUserDocument for user $uid: ${e.message}")
                } else {
                    Log.w(tag, "Failed to update user document for $uid: ${e.message}")
                }
            }
    }

    /**
     * Exports a JSON backup string containing encrypted vault items.
     */
    fun exportEncryptedBackupJson(userEmail: String, items: List<CloudVaultItem>): String {
        val root = JSONObject()
        root.put("app", "الخزنة الذكية - Smart Safe")
        root.put("exportDate", System.currentTimeMillis())
        root.put("userEmail", userEmail)
        root.put("version", "2.0")
        root.put("security", "AES-256-GCM Client-Side Encrypted")

        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("type", item.type)
            obj.put("category", item.category)
            obj.put("encryptedData", item.encryptedData)
            obj.put("createdAt", item.createdAt)
            obj.put("updatedAt", item.updatedAt)
            array.put(obj)
        }
        root.put("items", array)

        return root.toString(2)
    }

    /**
     * Uploads user's profile photo to Firebase Storage profile_photos/{uid}/photo.jpg
     * and saves profileImageUrl in users/{uid} document in Firestore.
     */
    suspend fun uploadProfilePhoto(uid: String, imageUri: android.net.Uri): String {
        // Resolve Firestore before uploading; success requires persisting the profile URL.
        val fs = firestore ?: throw IllegalStateException(AppText.text(com.example.R.string.label_cloud_unavailable))
        val storage = try {
            com.google.firebase.storage.FirebaseStorage.getInstance()
        } catch (e: Exception) {
            throw IllegalStateException(AppText.text(com.example.R.string.label_cloud_unavailable), e)
        }
        val ref = storage.reference.child("profile_photos/$uid/photo.jpg")

        // Put file with metadata
        kotlinx.coroutines.suspendCancellableCoroutine<com.google.firebase.storage.UploadTask.TaskSnapshot> { cont ->
            ref.putFile(imageUri)
                .addOnSuccessListener { snapshot -> cont.resume(snapshot) }
                .addOnFailureListener { exc -> cont.resumeWith(Result.failure(exc)) }
                .addOnCanceledListener { cont.cancel() }
        }

        // Get download URL
        val downloadUri = kotlinx.coroutines.suspendCancellableCoroutine<android.net.Uri> { cont ->
            ref.downloadUrl
                .addOnSuccessListener { uri -> cont.resume(uri) }
                .addOnFailureListener { exc -> cont.resumeWith(Result.failure(exc)) }
                .addOnCanceledListener { cont.cancel() }
        }
        val downloadUrl = downloadUri.toString()

        // Do not report upload success until the profile document has also been saved.
        kotlinx.coroutines.suspendCancellableCoroutine<Void?> { cont ->
            fs.collection("users").document(uid)
                .set(
                    mapOf(
                        "profileImageUrl to downloadUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener { cont.resume(null) }
                .addOnFailureListener { exc -> cont.resumeWith(Result.failure(exc)) }
                .addOnCanceledListener { cont.cancel() }
        }

        return downloadUrl
    }
}
