package com.example.data.firestore

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

enum class CloudSyncStatus(val labelAr: String, val iconEmoji: String) {
    CONNECTED("متصل بالسحابة", "🟢"),
    SYNCING("جارِ المزامنة...", "🔄"),
    OFFLINE("وضع دون اتصال (مخزن محلياً)", "🟡"),
    ERROR("تعذر الاتصال بالسحابة", "🔴")
}

/**
 * Cloud Account Profile & Auth Repository ONLY.
 *
 * All financial data (income, expenses, vaults, savings, gold, outings, budgets)
 * has been permanently removed from Cloud Firestore and is stored exclusively
 * in the local Room database (Local-First architecture).
 */
class FirestoreVaultRepository {

    private val tag = "FirestoreVaultRepo"

    private val firestore: FirebaseFirestore by lazy {
        val db = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
        } catch (e: Exception) {
            Log.w(tag, "Firestore settings already applied or error: ${e.message}")
        }
        db
    }

    private val _syncStatus = MutableStateFlow(CloudSyncStatus.CONNECTED)
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
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
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
        firestore.collection("users").document(uid)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(tag, "User profile document updated successfully for UID: $uid")
            }
            .addOnFailureListener { e ->
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
     * and saves photoUrl in users/{uid} document in Firestore.
     */
    suspend fun uploadProfilePhoto(uid: String, imageUri: android.net.Uri): String {
        val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
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

        // Update Firestore document users/{uid} with photoUrl
        kotlinx.coroutines.suspendCancellableCoroutine<Void?> { cont ->
            firestore.collection("users").document(uid)
                .set(
                    mapOf(
                        "photoUrl" to downloadUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener { cont.resume(null) }
                .addOnFailureListener { exc -> cont.resumeWith(Result.failure(exc)) }
        }

        return downloadUrl
    }
}
