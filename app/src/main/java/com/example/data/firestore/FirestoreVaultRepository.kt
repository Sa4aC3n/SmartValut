package com.example.data.firestore

import android.util.Log
import com.example.data.model.CloudBudgetLimit
import com.example.data.model.CloudCashSaving
import com.example.data.model.CloudChildLesson
import com.example.data.model.CloudCommitment
import com.example.data.model.CloudGoldAsset
import com.example.data.model.CloudGoldPrice
import com.example.data.model.CloudOuting
import com.example.data.model.CloudOutingExpense
import com.example.data.model.CloudTransaction
import com.example.data.model.CloudVault
import com.example.data.model.CloudVaultItem
import com.example.data.model.VaultActivityLog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CloudSyncStatus(val labelAr: String, val iconEmoji: String) {
    CONNECTED("متصل بالسحابة", "🟢"),
    SYNCING("جارِ المزامنة...", "🔄"),
    OFFLINE("وضع دون اتصال (مخزن محلياً)", "🟡"),
    ERROR("تعذر الاتصال بالسحابة", "🔴")
}

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
     * Creates or updates the user profile document in Firestore:
     * users/{FirebaseAuth.currentUser.uid}
     *
     * Strict requirement: email is set directly and strictly from currentUser.email.
     * updatedAt is set via FieldValue.serverTimestamp().
     */
    fun saveUserDocument(displayName: String = "") {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(tag, "[AUTH OFFLINE] saveUserDocument skipped: user is not authenticated in Firebase Auth")
            return
        }
        val uid = currentUser.uid
        val userMap = hashMapOf<String, Any>(
            "email" to (currentUser.email ?: ""),
            "displayName" to displayName.ifBlank { currentUser.displayName ?: "" },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        // Path: users/{FirebaseAuth.currentUser.uid}
        firestore.collection("users").document(uid)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(tag, "User document updated successfully for UID: $uid, email: ${currentUser.email}")
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
     * Real-time listener for user's encrypted vault items:
     * users/{FirebaseAuth.currentUser.uid}/vault_items
     *
     * Differentiates strictly between:
     * - OFFLINE (no auth or network down) -> CloudSyncStatus.OFFLINE
     * - PERMISSION_DENIED (rules/data problem for authenticated user) -> CloudSyncStatus.ERROR with error log
     */
    fun getVaultItemsFlow(): Flow<List<CloudVaultItem>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(tag, "[AUTH OFFLINE] No user authenticated in Firebase Auth. Cloud vault offline.")
            _syncStatus.value = CloudSyncStatus.OFFLINE
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = currentUser.uid
        _syncStatus.value = CloudSyncStatus.SYNCING

        // Path: users/{FirebaseAuth.currentUser.uid}/vault_items
        val query = firestore.collection("users")
            .document(uid)
            .collection("vault_items")
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    // Authenticated user but denied: Security rules issue or schema issue!
                    Log.e(tag, "[SECURITY/RULES ERROR] PERMISSION_DENIED on vault_items for authenticated UID $uid: ${error.message}")
                    _syncStatus.value = CloudSyncStatus.ERROR
                } else if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    // True network / offline disconnect
                    Log.i(tag, "[OFFLINE] Network/Firestore unavailable for user $uid: ${error.message}")
                    _syncStatus.value = CloudSyncStatus.OFFLINE
                } else {
                    Log.e(tag, "[FIRESTORE ERROR] Vault items snapshot error for user $uid (Code: ${error.code}): ${error.message}")
                    _syncStatus.value = CloudSyncStatus.ERROR
                }
                return@addSnapshotListener
            }

            val hasPendingWrites = snapshot?.metadata?.hasPendingWrites() ?: false
            val isFromCache = snapshot?.metadata?.isFromCache ?: false

            _syncStatus.value = when {
                hasPendingWrites -> CloudSyncStatus.SYNCING
                isFromCache -> CloudSyncStatus.OFFLINE
                else -> CloudSyncStatus.CONNECTED
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val title = doc.getString("title") ?: ""
                    val type = doc.getString("type") ?: "password"
                    val encryptedData = doc.getString("encryptedData") ?: ""
                    val category = doc.getString("category") ?: "عام"
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: doc.getLong("createdAt")
                        ?: System.currentTimeMillis()
                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
                        ?: doc.getLong("updatedAt")
                        ?: System.currentTimeMillis()
                    CloudVaultItem(
                        id = id,
                        title = title,
                        type = type,
                        encryptedData = encryptedData,
                        category = category,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                } catch (e: Exception) {
                    Log.w(tag, "Failed to parse item doc ${doc.id}: ${e.message}")
                    null
                }
            } ?: emptyList()

            trySend(items)
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Real-time listener for user's audit log:
     * users/{FirebaseAuth.currentUser.uid}/activity_log
     */
    fun getActivityLogsFlow(): Flow<List<VaultActivityLog>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(tag, "[AUTH OFFLINE] No user authenticated. Activity log offline.")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = currentUser.uid

        // Path: users/{FirebaseAuth.currentUser.uid}/activity_log
        val query = firestore.collection("users")
            .document(uid)
            .collection("activity_log")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(tag, "[SECURITY/RULES ERROR] PERMISSION_DENIED on activity_log for authenticated UID $uid: ${error.message}")
                } else if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    Log.i(tag, "[OFFLINE] Activity log snapshot unavailable for user $uid: ${error.message}")
                } else {
                    Log.e(tag, "[FIRESTORE ERROR] Activity log snapshot error for user $uid (Code: ${error.code}): ${error.message}")
                }
                return@addSnapshotListener
            }

            val logs = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val action = doc.getString("action") ?: "created"
                    val itemId = doc.getString("itemId") ?: ""
                    val itemTitle = doc.getString("itemTitle") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time
                        ?: doc.getLong("timestamp")
                        ?: System.currentTimeMillis()
                    VaultActivityLog(
                        id = id,
                        action = action,
                        itemId = itemId,
                        itemTitle = itemTitle,
                        timestamp = timestamp
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            trySend(logs)
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Saves or updates an encrypted item in Firestore, and records the event in activity_log:
     * users/{FirebaseAuth.currentUser.uid}/vault_items/{itemId}
     * users/{FirebaseAuth.currentUser.uid}/activity_log/{logId}
     *
     * Uses FieldValue.serverTimestamp() for createdAt (if new) and updatedAt.
     */
    fun saveVaultItem(
        item: CloudVaultItem,
        isNew: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(tag, "[AUTH OFFLINE] Attempted saveVaultItem without authentication")
            onError("يجب تسجيل الدخول بالبريد الإلكتروني للوصول إلى الخزنة السحابية")
            return
        }
        val uid = currentUser.uid

        val itemId = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id
        val finalItem = item.copy(
            id = itemId,
            updatedAt = System.currentTimeMillis()
        )

        val itemMap = hashMapOf<String, Any>(
            "id" to finalItem.id,
            "title" to finalItem.title,
            "type" to finalItem.type,
            "encryptedData" to finalItem.encryptedData,
            "category" to finalItem.category,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isNew) {
            itemMap["createdAt"] = FieldValue.serverTimestamp()
        }

        // Path: users/{FirebaseAuth.currentUser.uid}
        val userDoc = firestore.collection("users").document(uid)

        // Path: users/{FirebaseAuth.currentUser.uid}/vault_items/{itemId}
        userDoc.collection("vault_items").document(itemId)
            .set(itemMap, SetOptions.merge())
            .addOnSuccessListener {
                // Path: users/{FirebaseAuth.currentUser.uid}/activity_log/{logId}
                val actionType = if (isNew) "created" else "updated"
                val logId = UUID.randomUUID().toString()
                val logMap = hashMapOf<String, Any>(
                    "id" to logId,
                    "action" to actionType,
                    "itemId" to itemId,
                    "itemTitle" to finalItem.title,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                userDoc.collection("activity_log").document(logId).set(logMap)
                onSuccess()
            }
            .addOnFailureListener { e ->
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(tag, "[SECURITY/RULES ERROR] PERMISSION_DENIED on saveVaultItem for user $uid: ${e.message}")
                }
                onError(e.localizedMessage ?: "فشل حفظ العنصر في السحابة")
            }
    }

    /**
     * Deletes an encrypted item from Firestore, and records the delete event in activity_log:
     * users/{FirebaseAuth.currentUser.uid}/vault_items/{itemId}
     * users/{FirebaseAuth.currentUser.uid}/activity_log/{logId}
     */
    fun deleteVaultItem(
        itemId: String,
        itemTitle: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null || itemId.isBlank()) {
            onError("يجب تسجيل الدخول لحذف العنصر من الخزنة السحابية")
            return
        }
        val uid = currentUser.uid

        // Path: users/{FirebaseAuth.currentUser.uid}
        val userDoc = firestore.collection("users").document(uid)

        // Path: users/{FirebaseAuth.currentUser.uid}/vault_items/{itemId}
        userDoc.collection("vault_items").document(itemId)
            .delete()
            .addOnSuccessListener {
                // Path: users/{FirebaseAuth.currentUser.uid}/activity_log/{logId}
                val logId = UUID.randomUUID().toString()
                val logMap = hashMapOf<String, Any>(
                    "id" to logId,
                    "action" to "deleted",
                    "itemId" to itemId,
                    "itemTitle" to itemTitle,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                userDoc.collection("activity_log").document(logId).set(logMap)
                onSuccess()
            }
            .addOnFailureListener { e ->
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(tag, "[SECURITY/RULES ERROR] PERMISSION_DENIED on deleteVaultItem for user $uid: ${e.message}")
                }
                onError(e.localizedMessage ?: "فشل حذف العنصر من السحابة")
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

    // ==========================================
    // FINANCIAL VAULTS & TRANSACTIONS (FIRESTORE)
    // ==========================================

    /**
     * Creates a new financial vault in:
     * users/{FirebaseAuth.currentUser.uid}/vaults/{vaultId}
     *
     * Initial balance defaults to 0.0 with server timestamps.
     */
    fun createVault(
        name: String,
        initialBalance: Double = 0.0,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لإنشاء خزنة جديدة")
            return
        }
        val uid = currentUser.uid
        val vaultId = UUID.randomUUID().toString()
        val vaultName = name.trim().ifBlank { "الخزنة الرئيسية" }
        val vaultDoc = firestore.collection("users").document(uid).collection("vaults").document(vaultId)

        val vaultData = hashMapOf<String, Any>(
            "id" to vaultId,
            "name" to vaultName,
            "balance" to initialBalance,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        vaultDoc.set(vaultData)
            .addOnSuccessListener {
                Log.d(tag, "Vault created successfully: $vaultName ($vaultId)")
                onSuccess(vaultId)
            }
            .addOnFailureListener { e ->
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    val detail = "[PERMISSION_DENIED] تم رفض الإذن بإنشاء الخزنة '$vaultName' (uid: $uid, balance: $initialBalance): ${e.message}. تأكد من استيفاء شروط isValidVault() في قواعد Firestore."
                    Log.e(tag, detail)
                    onError(detail)
                } else {
                    onError(e.localizedMessage ?: "فشل إنشاء الخزنة")
                }
            }
    }

    /**
     * Real-time listener for user's financial vaults:
     * users/{FirebaseAuth.currentUser.uid}/vaults
     */
    fun getVaultsFlow(): Flow<List<CloudVault>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = currentUser.uid
        val query = firestore.collection("users")
            .document(uid)
            .collection("vaults")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(tag, "[SECURITY/RULES ERROR] PERMISSION_DENIED on vaults for user $uid: ${error.message}")
                } else if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    Log.i(tag, "[OFFLINE] Vaults unavailable for user $uid: ${error.message}")
                } else {
                    Log.e(tag, "[FIRESTORE ERROR] Vaults snapshot error for user $uid: ${error.message}")
                }
                return@addSnapshotListener
            }

            val vaults = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: ""
                    val balance = doc.getDouble("balance") ?: 0.0
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: doc.getLong("createdAt")
                        ?: System.currentTimeMillis()
                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
                        ?: doc.getLong("updatedAt")
                        ?: System.currentTimeMillis()
                    CloudVault(
                        id = id,
                        name = name,
                        balance = balance,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                } catch (e: Exception) {
                    Log.w(tag, "Failed to parse vault doc ${doc.id}: ${e.message}")
                    null
                }
            } ?: emptyList()

            trySend(vaults)
        }

        awaitClose {
            registration.remove()
        }
    }

    private fun diagnoseTransactionPermissionError(
        uid: String,
        vaultId: String,
        type: String,
        amount: Double,
        category: String,
        description: String,
        originalError: String,
        onDiagnosisComplete: (detailedError: String) -> Unit
    ) {
        val userDoc = firestore.collection("users").document(uid)
        val vaultDoc = userDoc.collection("vaults").document(vaultId)

        vaultDoc.get().addOnCompleteListener { task ->
            val reasons = mutableListOf<String>()

            if (!task.isSuccessful) {
                reasons.add("❌ تعذر الاستعلام عن مستند الخزنة: ${task.exception?.message}")
            } else {
                val snapshot = task.result
                if (snapshot == null || !snapshot.exists()) {
                    reasons.add("❌ مستند الخزنة (vaultId: '$vaultId') غير موجود في مسار users/$uid/vaults/$vaultId! هذا هو سبب فشل شرط existsVault(userId, vaultId).")
                } else {
                    val vaultName = snapshot.getString("name") ?: ""
                    reasons.add("✔️ مستند الخزنة موجود في Firestore باسم: '$vaultName' (شرط existsVault سليم).")
                }
            }

            if (type != "INCOME" && type != "EXPENSE") {
                reasons.add("❌ نوع المعاملة '$type' غير مطابق (القواعد تشترط INCOME أو EXPENSE فقط).")
            } else {
                reasons.add("✔️ نوع المعاملة سليم: $type.")
            }

            if (amount.isNaN() || amount < 0) {
                reasons.add("❌ المبلغ $amount غير صالح (القواعد تشترط رقم غير سالب).")
            } else {
                reasons.add("✔️ المبلغ سليم: $amount.")
            }

            if (category.isBlank()) {
                reasons.add("❌ الفئة فارغة (القواعد تشترط نص غير فارغ).")
            } else {
                reasons.add("✔️ الفئة سليمة: '$category'.")
            }

            val detailedMessage = buildString {
                appendLine("[تقرير تشخيص خطأ الصلاحيات PERMISSION_DENIED]:")
                appendLine("المستخدم: $uid")
                appendLine("المعاملة: type=$type, amount=$amount, category='$category', vaultId='$vaultId'")
                appendLine("الخطأ الأصلي من Firestore: $originalError")
                appendLine("نتائج التحقق مقابل القواعد الصارمة:")
                reasons.forEach { appendLine("  - $it") }
            }

            Log.e(tag, detailedMessage)
            onDiagnosisComplete(detailedMessage)
        }
    }

    /**
     * Adds an income or expense transaction atomically via WriteBatch:
     * 1. Creates document in users/{uid}/transactions/{transactionId}
     * 2. Atomically increments or decrements balance in users/{uid}/vaults/{vaultId} via FieldValue.increment()
     *
     * No manual read-before-write race conditions.
     */
    fun addTransaction(
        type: String, // "INCOME" or "EXPENSE"
        amount: Double,
        category: String,
        description: String,
        vaultId: String,
        dateMillis: Long = System.currentTimeMillis(),
        receiptImagePath: String? = null,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحفظ المعاملة في السحابة")
            return
        }
        val uid = currentUser.uid
        if (vaultId.isBlank()) {
            onError("معرف الخزنة غير صالح")
            return
        }

        val userDoc = firestore.collection("users").document(uid)
        val txId = UUID.randomUUID().toString()
        val txDoc = userDoc.collection("transactions").document(txId)
        val vaultDoc = userDoc.collection("vaults").document(vaultId)

        val absAmount = Math.abs(amount)
        val balanceDelta = if (type == "INCOME") absAmount else -absAmount
        val safeCategory = category.trim().ifBlank { "عام" }
        val safeDescription = description.trim()

        val txMap = hashMapOf<String, Any>(
            "id" to txId,
            "type" to type,
            "amount" to absAmount,
            "category" to safeCategory,
            "description" to safeDescription,
            "vaultId" to vaultId,
            "date" to Timestamp(dateMillis / 1000, ((dateMillis % 1000) * 1_000_000).toInt()),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!receiptImagePath.isNullOrBlank()) {
            txMap["receiptImagePath"] = receiptImagePath
        }

        val batch = firestore.batch()
        batch.set(txDoc, txMap)
        batch.update(
            vaultDoc,
            "balance", FieldValue.increment(balanceDelta),
            "updatedAt", FieldValue.serverTimestamp()
        )

        batch.commit()
            .addOnSuccessListener {
                Log.d(tag, "Transaction $txId ($type $absAmount) and vault $vaultId balance committed atomically")
                onSuccess(txId)
            }
            .addOnFailureListener { e ->
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(tag, "[SECURITY/RULES ERROR] PERMISSION_DENIED on addTransaction for user $uid: ${e.message}")
                    diagnoseTransactionPermissionError(
                        uid = uid,
                        vaultId = vaultId,
                        type = type,
                        amount = absAmount,
                        category = safeCategory,
                        description = safeDescription,
                        originalError = e.message ?: "PERMISSION_DENIED"
                    ) { diagnosis ->
                        onError("رفض الإذن [PERMISSION_DENIED]:\n$diagnosis")
                    }
                } else {
                    onError(e.localizedMessage ?: "فشل حفظ المعاملة وتحديث الرصيد")
                }
            }
    }

    /**
     * Real-time listener for user's financial transactions:
     * users/{FirebaseAuth.currentUser.uid}/transactions
     *
     * Optionally filters by vaultId and orders descending by date.
     */
    fun getTransactionsFlow(vaultId: String? = null): Flow<List<CloudTransaction>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = currentUser.uid
        var query: Query = firestore.collection("users")
            .document(uid)
            .collection("transactions")

        if (!vaultId.isNullOrBlank()) {
            query = query.whereEqualTo("vaultId", vaultId)
        }

        query = query.orderBy("date", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(tag, "[SECURITY/RULES ERROR] PERMISSION_DENIED on transactions for user $uid: ${error.message}")
                } else if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    Log.i(tag, "[OFFLINE] Transactions unavailable for user $uid: ${error.message}")
                } else {
                    Log.e(tag, "[FIRESTORE ERROR] Transactions snapshot error for user $uid: ${error.message}")
                }
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val type = doc.getString("type") ?: "INCOME"
                    val amount = doc.getDouble("amount") ?: 0.0
                    val category = doc.getString("category") ?: ""
                    val description = doc.getString("description") ?: ""
                    val vId = doc.getString("vaultId") ?: ""
                    val date = doc.getTimestamp("date")?.toDate()?.time
                        ?: doc.getLong("date")
                        ?: System.currentTimeMillis()
                    val receiptImagePath = doc.getString("receiptImagePath")
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: doc.getLong("createdAt")
                        ?: System.currentTimeMillis()
                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
                        ?: doc.getLong("updatedAt")
                        ?: System.currentTimeMillis()
                    CloudTransaction(
                        id = id,
                        type = type,
                        amount = amount,
                        category = category,
                        description = description,
                        vaultId = vId,
                        date = date,
                        receiptImagePath = receiptImagePath,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                } catch (e: Exception) {
                    Log.w(tag, "Failed to parse transaction doc ${doc.id}: ${e.message}")
                    null
                }
            } ?: emptyList()

            trySend(list)
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Deletes a transaction and safely adjusts the vault balance if available.
     * Guaranteed never to fail deletion if the vault document does not exist.
     */
    fun deleteTransaction(
        transactionId: String,
        vaultId: String,
        amount: Double,
        type: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحذف المعاملة")
            return
        }
        val uid = currentUser.uid
        if (transactionId.isBlank()) {
            onSuccess()
            return
        }

        val userDoc = firestore.collection("users").document(uid)
        val txDoc = userDoc.collection("transactions").document(transactionId)
        val vaultDoc = if (vaultId.isNotBlank()) userDoc.collection("vaults").document(vaultId) else null

        val absAmount = Math.abs(amount)
        val reverseDelta = if (type == "INCOME") -absAmount else absAmount

        fun performDelete(targetDocRef: com.google.firebase.firestore.DocumentReference) {
            targetDocRef.delete()
                .addOnSuccessListener {
                    Log.d(tag, "Transaction ${targetDocRef.id} deleted successfully from Firestore")
                    if (vaultDoc != null) {
                        vaultDoc.get().addOnSuccessListener { vSnap ->
                            if (vSnap.exists()) {
                                vaultDoc.update(
                                    "balance", FieldValue.increment(reverseDelta),
                                    "updatedAt", FieldValue.serverTimestamp()
                                ).addOnFailureListener { err ->
                                    Log.w(tag, "Could not adjust vault balance after deletion: ${err.message}")
                                }
                            }
                        }.addOnFailureListener { err ->
                            Log.w(tag, "Could not verify vault document: ${err.message}")
                        }
                    }
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to delete transaction doc ${targetDocRef.id}: ${e.message}")
                    onError(e.localizedMessage ?: "فشل حذف المعاملة")
                }
        }

        // Check if doc exists by ID directly
        txDoc.get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                performDelete(txDoc)
            } else {
                // If not found by document ID, search if an internal "id" field matches
                userDoc.collection("transactions").whereEqualTo("id", transactionId).get()
                    .addOnSuccessListener { querySnap ->
                        if (!querySnap.isEmpty) {
                            for (doc in querySnap.documents) {
                                performDelete(doc.reference)
                            }
                        } else {
                            performDelete(txDoc)
                        }
                    }
                    .addOnFailureListener {
                        performDelete(txDoc)
                    }
            }
        }.addOnFailureListener {
            performDelete(txDoc)
        }
    }

    /**
     * Deletes a financial vault document from users/{uid}/vaults/{vaultId}.
     * If transactions are linked to this vault and a fallbackVaultId is available,
     * reassigns those transactions so they maintain a valid vault reference.
     */
    fun deleteVault(
        vaultId: String,
        fallbackVaultId: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحذف الخزنة")
            return
        }
        val uid = currentUser.uid
        if (vaultId.isBlank()) return

        val userDoc = firestore.collection("users").document(uid)
        val vaultDoc = userDoc.collection("vaults").document(vaultId)

        userDoc.collection("transactions").whereEqualTo("vaultId", vaultId).get()
            .addOnSuccessListener { txSnapshots ->
                val batch = firestore.batch()
                if (!fallbackVaultId.isNullOrBlank() && fallbackVaultId != vaultId && !txSnapshots.isEmpty) {
                    for (doc in txSnapshots.documents) {
                        batch.update(doc.reference, "vaultId", fallbackVaultId)
                    }
                }
                batch.delete(vaultDoc)
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(tag, "Vault $vaultId deleted successfully from Firestore")
                        logActivity(uid, "DELETE_VAULT", "حذف الخزنة / الحساب المالي ($vaultId)")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Error deleting vault $vaultId in batch: ${e.message}")
                        // Direct delete fallback
                        vaultDoc.delete()
                            .addOnSuccessListener {
                                logActivity(uid, "DELETE_VAULT", "حذف الخزنة ($vaultId)")
                                onSuccess()
                            }
                            .addOnFailureListener { err ->
                                onError(err.localizedMessage ?: "فشل حذف الخزنة من السحابة")
                            }
                    }
            }
            .addOnFailureListener {
                vaultDoc.delete()
                    .addOnSuccessListener {
                        logActivity(uid, "DELETE_VAULT", "حذف الخزنة ($vaultId)")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError(e.localizedMessage ?: "فشل حذف الخزنة من السحابة")
                    }
            }
    }

    /**
     * Updates an existing financial vault's name and balance in Firestore.
     */
    fun updateVault(
        vaultId: String,
        name: String,
        balance: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لتعديل الخزنة")
            return
        }
        val uid = currentUser.uid
        if (vaultId.isBlank()) return

        val vaultDoc = firestore.collection("users").document(uid).collection("vaults").document(vaultId)
        val updates = hashMapOf<String, Any>(
            "name" to name.trim().ifBlank { "الخزنة" },
            "balance" to balance,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        vaultDoc.update(updates)
            .addOnSuccessListener {
                Log.d(tag, "Vault $vaultId updated successfully in Firestore: $name, $balance")
                logActivity(uid, "UPDATE_VAULT", "تعديل بيانات الخزنة / الحساب ($name)")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error updating vault $vaultId: ${e.message}")
                onError(e.localizedMessage ?: "فشل تعديل الخزنة")
            }
    }

    /**
     * Real-time listener for user's gold assets (الذهب والسبائك):
     * users/{FirebaseAuth.currentUser.uid}/gold_assets
     */
    fun getGoldAssetsFlow(): Flow<List<CloudGoldAsset>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = currentUser.uid
        val query = firestore.collection("users")
            .document(uid)
            .collection("gold_assets")
            .orderBy("purchaseDateMillis", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Gold assets snapshot error for $uid: ${error.message}")
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: ""
                    val goldType = doc.getString("goldType") ?: "سبيكة"
                    val karat = doc.getLong("karat")?.toInt() ?: 24
                    val weight = doc.getDouble("weight") ?: 0.0
                    val purchasePrice = doc.getDouble("purchasePrice") ?: 0.0
                    val purchaseDate = doc.getLong("purchaseDateMillis") ?: System.currentTimeMillis()
                    val purpose = doc.getString("purpose") ?: "SAVING"
                    val imagePath = doc.getString("imagePath")
                    val status = doc.getString("status") ?: "ACTIVE"
                    val salePrice = doc.getDouble("salePrice")
                    val saleDate = doc.getLong("saleDateMillis")
                    val saleNotes = doc.getString("saleNotes")
                    val notes = doc.getString("notes") ?: ""
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
                        ?: doc.getLong("updatedAt") ?: System.currentTimeMillis()

                    CloudGoldAsset(
                        id = id,
                        name = name,
                        goldType = goldType,
                        karat = karat,
                        weight = weight,
                        purchasePrice = purchasePrice,
                        purchaseDateMillis = purchaseDate,
                        purpose = purpose,
                        imagePath = imagePath,
                        status = status,
                        salePrice = salePrice,
                        saleDateMillis = saleDate,
                        saleNotes = saleNotes,
                        notes = notes,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                } catch (e: Exception) {
                    Log.w(tag, "Failed to parse gold asset doc ${doc.id}: ${e.message}")
                    null
                }
            } ?: emptyList()

            trySend(list)
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Adds a new gold asset document to users/{uid}/gold_assets/{assetId}
     */
    fun addGoldAsset(
        asset: CloudGoldAsset,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لتسجيل قطعة ذهب")
            return
        }
        val uid = currentUser.uid
        val assetId = asset.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = firestore.collection("users").document(uid).collection("gold_assets").document(assetId)

        val map = hashMapOf<String, Any>(
            "id" to assetId,
            "name" to asset.name.trim(),
            "goldType" to asset.goldType,
            "karat" to asset.karat,
            "weight" to asset.weight,
            "purchasePrice" to asset.purchasePrice,
            "purchaseDateMillis" to asset.purchaseDateMillis,
            "purpose" to asset.purpose,
            "status" to asset.status,
            "notes" to asset.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (asset.imagePath != null) map["imagePath"] = asset.imagePath
        if (asset.salePrice != null) map["salePrice"] = asset.salePrice
        if (asset.saleDateMillis != null) map["saleDateMillis"] = asset.saleDateMillis
        if (asset.saleNotes != null) map["saleNotes"] = asset.saleNotes

        docRef.set(map)
            .addOnSuccessListener {
                Log.d(tag, "Gold asset saved to Firestore: ${asset.name} ($assetId)")
                logActivity(uid, "ADD_GOLD", "إضافة قطعة ذهب (${asset.name})")
                onSuccess(assetId)
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to save gold asset: ${e.message}")
                onError(e.localizedMessage ?: "فشل حفظ قطعة الذهب في السحابة")
            }
    }

    /**
     * Updates an existing gold asset document in users/{uid}/gold_assets/{asset.id}
     */
    fun updateGoldAsset(
        asset: CloudGoldAsset,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لتعديل قطعة الذهب")
            return
        }
        val uid = currentUser.uid
        if (asset.id.isBlank()) return
        val docRef = firestore.collection("users").document(uid).collection("gold_assets").document(asset.id)

        val updates = hashMapOf<String, Any>(
            "name" to asset.name.trim(),
            "goldType" to asset.goldType,
            "karat" to asset.karat,
            "weight" to asset.weight,
            "purchasePrice" to asset.purchasePrice,
            "purchaseDateMillis" to asset.purchaseDateMillis,
            "purpose" to asset.purpose,
            "status" to asset.status,
            "notes" to asset.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (asset.imagePath != null) updates["imagePath"] = asset.imagePath
        if (asset.salePrice != null) updates["salePrice"] = asset.salePrice
        if (asset.saleDateMillis != null) updates["saleDateMillis"] = asset.saleDateMillis
        if (asset.saleNotes != null) updates["saleNotes"] = asset.saleNotes

        docRef.update(updates)
            .addOnSuccessListener {
                Log.d(tag, "Gold asset updated in Firestore: ${asset.name} (${asset.id})")
                logActivity(uid, "UPDATE_GOLD", "تعديل قطعة الذهب (${asset.name})")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to update gold asset: ${e.message}")
                onError(e.localizedMessage ?: "فشل تعديل قطعة الذهب")
            }
    }

    /**
     * Deletes a gold asset document from users/{uid}/gold_assets/{assetId}
     */
    fun deleteGoldAsset(
        assetId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحذف قطعة الذهب")
            return
        }
        val uid = currentUser.uid
        if (assetId.isBlank()) return

        firestore.collection("users").document(uid).collection("gold_assets").document(assetId)
            .delete()
            .addOnSuccessListener {
                Log.d(tag, "Gold asset deleted from Firestore: $assetId")
                logActivity(uid, "DELETE_GOLD", "حذف قطعة الذهب ($assetId)")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to delete gold asset: ${e.message}")
                onError(e.localizedMessage ?: "فشل حذف قطعة الذهب")
            }
    }

    /**
     * Marks a gold asset as SOLD in Firestore
     */
    fun sellGoldAsset(
        assetId: String,
        salePrice: Double,
        saleDateMillis: Long,
        saleNotes: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لبيع قطعة الذهب")
            return
        }
        val uid = currentUser.uid
        if (assetId.isBlank()) return

        val updates = hashMapOf<String, Any>(
            "status" to "SOLD",
            "salePrice" to salePrice,
            "saleDateMillis" to saleDateMillis,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (saleNotes != null) updates["saleNotes"] = saleNotes

        firestore.collection("users").document(uid).collection("gold_assets").document(assetId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(tag, "Gold asset marked as SOLD: $assetId, price: $salePrice")
                logActivity(uid, "SELL_GOLD", "بيع قطعة ذهب بمبلغ $salePrice")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to sell gold asset: ${e.message}")
                onError(e.localizedMessage ?: "فشل تسجيل بيع قطعة الذهب")
            }
    }

    /**
     * Real-time listener for cash savings (المدخرات النقدية):
     * users/{FirebaseAuth.currentUser.uid}/cash_savings
     */
    fun getCashSavingsFlow(): Flow<List<CloudCashSaving>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = currentUser.uid
        val query = firestore.collection("users")
            .document(uid)
            .collection("cash_savings")
            .orderBy("dateMillis", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Cash savings snapshot error for $uid: ${error.message}")
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val amount = doc.getDouble("amount") ?: 0.0
                    val currency = doc.getString("currency") ?: "EGP"
                    val notes = doc.getString("notes") ?: ""
                    val dateMillis = doc.getLong("dateMillis") ?: System.currentTimeMillis()
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
                        ?: doc.getLong("updatedAt") ?: System.currentTimeMillis()

                    CloudCashSaving(
                        id = id,
                        amount = amount,
                        currency = currency,
                        notes = notes,
                        dateMillis = dateMillis,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                } catch (e: Exception) {
                    Log.w(tag, "Failed to parse cash saving doc ${doc.id}: ${e.message}")
                    null
                }
            } ?: emptyList()

            trySend(list)
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Adds a new cash saving document to users/{uid}/cash_savings/{savingId}
     */
    fun addCashSaving(
        saving: CloudCashSaving,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لتسجيل مدخرات نقدية")
            return
        }
        val uid = currentUser.uid
        val savingId = saving.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = firestore.collection("users").document(uid).collection("cash_savings").document(savingId)

        val map = hashMapOf<String, Any>(
            "id" to savingId,
            "amount" to saving.amount,
            "currency" to saving.currency,
            "notes" to saving.notes,
            "dateMillis" to saving.dateMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(map)
            .addOnSuccessListener {
                Log.d(tag, "Cash saving saved to Firestore: ${saving.amount} ${saving.currency} ($savingId)")
                logActivity(uid, "ADD_CASH_SAVING", "إضافة مدخرات نقدية بمبلغ ${saving.amount} ${saving.currency}")
                onSuccess(savingId)
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to save cash saving: ${e.message}")
                onError(e.localizedMessage ?: "فشل حفظ المدخرات النقدية في السحابة")
            }
    }

    /**
     * Updates an existing cash saving document in users/{uid}/cash_savings/{saving.id}
     */
    fun updateCashSaving(
        saving: CloudCashSaving,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لتعديل المدخرات النقدية")
            return
        }
        val uid = currentUser.uid
        if (saving.id.isBlank()) return
        val docRef = firestore.collection("users").document(uid).collection("cash_savings").document(saving.id)

        val updates = hashMapOf<String, Any>(
            "amount" to saving.amount,
            "currency" to saving.currency,
            "notes" to saving.notes,
            "dateMillis" to saving.dateMillis,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.update(updates)
            .addOnSuccessListener {
                Log.d(tag, "Cash saving updated in Firestore: ${saving.amount} (${saving.id})")
                logActivity(uid, "UPDATE_CASH_SAVING", "تعديل مدخرات نقدية بمبلغ ${saving.amount}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to update cash saving: ${e.message}")
                onError(e.localizedMessage ?: "فشل تعديل المدخرات النقدية")
            }
    }

    /**
     * Deletes a cash saving document from users/{uid}/cash_savings/{savingId}
     */
    fun deleteCashSaving(
        savingId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحذف المدخرات النقدية")
            return
        }
        val uid = currentUser.uid
        if (savingId.isBlank()) return

        firestore.collection("users").document(uid).collection("cash_savings").document(savingId)
            .delete()
            .addOnSuccessListener {
                Log.d(tag, "Cash saving deleted from Firestore: $savingId")
                logActivity(uid, "DELETE_CASH_SAVING", "حذف مدخرات نقدية ($savingId)")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to delete cash saving: ${e.message}")
                onError(e.localizedMessage ?: "فشل حذف المدخرات النقدية")
            }
    }

    /**
     * Commitments in Firestore (users/{uid}/commitments)
     */
    fun addCommitment(
        commitment: CloudCommitment,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحفظ الالتزام في السحابة")
            return
        }
        val uid = currentUser.uid
        val docId = commitment.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = firestore.collection("users").document(uid).collection("commitments").document(docId)

        val map = hashMapOf<String, Any>(
            "id" to docId,
            "title" to commitment.title.trim(),
            "amount" to commitment.amount,
            "dueDateMillis" to commitment.dueDateMillis,
            "isPaid" to commitment.isPaid,
            "isRecurringMonthly" to commitment.isRecurringMonthly,
            "notes" to commitment.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (commitment.receiptImagePath != null) {
            map["receiptImagePath"] = commitment.receiptImagePath
        }

        Log.d("FirestoreDebug", "سيتم إرسال: $map")
        map.forEach { (key, value) ->
            Log.d("FirestoreDebug", "  $key = $value (${value?.javaClass?.simpleName})")
        }

        try {
            docRef.set(map)
                .addOnSuccessListener {
                    Log.d(tag, "Commitment saved to Firestore: ${commitment.title} ($docId)")
                    logActivity(uid, "ADD_COMMITMENT", "إضافة التزام: ${commitment.title}")
                    onSuccess(docId)
                }
                .addOnFailureListener { e ->
                    Log.e("SaveDebug", "فشل الحفظ: ${e.message}", e)
                    onError(e.message ?: "فشل حفظ الالتزام في السحابة")
                }
        } catch (e: Exception) {
            Log.e("SaveDebug", "فشل الحفظ: ${e.message}", e)
            onError(e.message ?: "فشل حفظ الالتزام في السحابة")
        }
    }

    fun updateCommitment(
        commitment: CloudCommitment,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لتعديل الالتزام")
            return
        }
        val uid = currentUser.uid
        if (commitment.id.isBlank()) return
        val docRef = firestore.collection("users").document(uid).collection("commitments").document(commitment.id)

        val updates = hashMapOf<String, Any>(
            "title" to commitment.title.trim(),
            "amount" to commitment.amount,
            "dueDateMillis" to commitment.dueDateMillis,
            "isPaid" to commitment.isPaid,
            "isRecurringMonthly" to commitment.isRecurringMonthly,
            "notes" to commitment.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (commitment.receiptImagePath != null) {
            updates["receiptImagePath"] = commitment.receiptImagePath
        }

        docRef.update(updates)
            .addOnSuccessListener {
                Log.d(tag, "Commitment updated in Firestore: ${commitment.title} (${commitment.id})")
                logActivity(uid, "UPDATE_COMMITMENT", "تعديل التزام: ${commitment.title}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to update commitment: ${e.message}")
                onError(e.localizedMessage ?: "فشل تعديل الالتزام")
            }
    }

    fun deleteCommitment(
        commitmentId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحذف الالتزام")
            return
        }
        val uid = currentUser.uid
        if (commitmentId.isBlank()) return

        firestore.collection("users").document(uid).collection("commitments").document(commitmentId)
            .delete()
            .addOnSuccessListener {
                Log.d(tag, "Commitment deleted from Firestore: $commitmentId")
                logActivity(uid, "DELETE_COMMITMENT", "حذف التزام ($commitmentId)")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to delete commitment: ${e.message}")
                onError(e.localizedMessage ?: "فشل حذف الالتزام")
            }
    }

    /**
     * Child Lessons in Firestore (users/{uid}/child_lessons)
     */
    fun addChildLesson(
        lesson: CloudChildLesson,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحفظ الدرس في السحابة")
            return
        }
        val uid = currentUser.uid
        val docId = lesson.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = firestore.collection("users").document(uid).collection("child_lessons").document(docId)

        val map = hashMapOf<String, Any>(
            "id" to docId,
            "childName" to lesson.childName.trim(),
            "subject" to lesson.subject.trim(),
            "teacherName" to lesson.teacherName.trim(),
            "amount" to lesson.amount,
            "dueDateMillis" to lesson.dueDateMillis,
            "isPaid" to lesson.isPaid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (lesson.receiptImagePath != null) {
            map["receiptImagePath"] = lesson.receiptImagePath
        }

        Log.d("FirestoreDebug", "سيتم إرسال: $map")
        map.forEach { (key, value) ->
            Log.d("FirestoreDebug", "  $key = $value (${value?.javaClass?.simpleName})")
        }

        try {
            docRef.set(map)
                .addOnSuccessListener {
                    Log.d(tag, "Child lesson saved to Firestore: ${lesson.childName} - ${lesson.subject} ($docId)")
                    logActivity(uid, "ADD_LESSON", "إضافة درس: ${lesson.childName} - ${lesson.subject}")
                    onSuccess(docId)
                }
                .addOnFailureListener { e ->
                    Log.e("SaveDebug", "فشل الحفظ: ${e.message}", e)
                    onError(e.message ?: "فشل حفظ الدرس في السحابة")
                }
        } catch (e: Exception) {
            Log.e("SaveDebug", "فشل الحفظ: ${e.message}", e)
            onError(e.message ?: "فشل حفظ الدرس في السحابة")
        }
    }

    fun updateChildLesson(
        lesson: CloudChildLesson,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لتعديل الدرس")
            return
        }
        val uid = currentUser.uid
        if (lesson.id.isBlank()) return
        val docRef = firestore.collection("users").document(uid).collection("child_lessons").document(lesson.id)

        val updates = hashMapOf<String, Any>(
            "childName" to lesson.childName.trim(),
            "subject" to lesson.subject.trim(),
            "teacherName" to lesson.teacherName.trim(),
            "amount" to lesson.amount,
            "dueDateMillis" to lesson.dueDateMillis,
            "isPaid" to lesson.isPaid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (lesson.receiptImagePath != null) {
            updates["receiptImagePath"] = lesson.receiptImagePath
        }

        docRef.update(updates)
            .addOnSuccessListener {
                Log.d(tag, "Child lesson updated in Firestore: ${lesson.childName} - ${lesson.subject}")
                logActivity(uid, "UPDATE_LESSON", "تعديل درس: ${lesson.childName} - ${lesson.subject}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to update child lesson: ${e.message}")
                onError(e.localizedMessage ?: "فشل تعديل الدرس")
            }
    }

    fun deleteChildLesson(
        lessonId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحذف الدرس")
            return
        }
        val uid = currentUser.uid
        if (lessonId.isBlank()) return

        firestore.collection("users").document(uid).collection("child_lessons").document(lessonId)
            .delete()
            .addOnSuccessListener {
                Log.d(tag, "Child lesson deleted from Firestore: $lessonId")
                logActivity(uid, "DELETE_LESSON", "حذف درس ($lessonId)")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to delete child lesson: ${e.message}")
                onError(e.localizedMessage ?: "فشل حذف الدرس")
            }
    }

    /**
     * Budget Limits in Firestore (users/{uid}/budget_limits)
     */
    fun setBudgetLimit(
        category: String,
        monthlyLimit: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحفظ حد الميزانية")
            return
        }
        val uid = currentUser.uid
        val docId = category.trim()
        if (docId.isBlank()) return

        val docRef = firestore.collection("users").document(uid).collection("budget_limits").document(docId)
        val map = hashMapOf<String, Any>(
            "category" to docId,
            "monthlyLimit" to monthlyLimit,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(map, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(tag, "Budget limit saved to Firestore: $category -> $monthlyLimit")
                logActivity(uid, "SET_BUDGET_LIMIT", "تحديد ميزانية $category بمبلغ $monthlyLimit")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to set budget limit: ${e.message}")
                onError(e.localizedMessage ?: "فشل حفظ حد الميزانية")
            }
    }

    fun deleteBudgetLimit(
        category: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("يجب تسجيل الدخول لحذف حد الميزانية")
            return
        }
        val uid = currentUser.uid
        val docId = category.trim()
        if (docId.isBlank()) return

        firestore.collection("users").document(uid).collection("budget_limits").document(docId)
            .delete()
            .addOnSuccessListener {
                Log.d(tag, "Budget limit deleted from Firestore: $category")
                logActivity(uid, "DELETE_BUDGET_LIMIT", "حذف ميزانية $category")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to delete budget limit: ${e.message}")
                onError(e.localizedMessage ?: "فشل حذف حد الميزانية")
            }
    }

    /**
     * Real-time listener for gold prices:
     * users/{FirebaseAuth.currentUser.uid}/gold_prices
     */
    fun getGoldPricesFlow(): Flow<List<CloudGoldPrice>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = currentUser.uid
        val query = firestore.collection("users")
            .document(uid)
            .collection("gold_prices")

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Gold prices snapshot error for $uid: ${error.message}")
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val karat = doc.getLong("karat")?.toInt() ?: return@mapNotNull null
                    val price = doc.getDouble("pricePerGram") ?: 0.0
                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
                        ?: doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    CloudGoldPrice(karat = karat, pricePerGram = price, updatedAt = updatedAt)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            trySend(list)
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Saves/updates a specific karat price in Firestore
     */
    fun saveGoldPrice(karat: Int, pricePerGram: Double) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
        val docRef = firestore.collection("users").document(uid).collection("gold_prices").document("karat_$karat")
        val data = hashMapOf<String, Any>(
            "karat" to karat,
            "pricePerGram" to pricePerGram,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(tag, "Gold price for karat $karat updated to $pricePerGram in Firestore")
            }
            .addOnFailureListener { e ->
                Log.w(tag, "Failed to update gold price for karat $karat: ${e.message}")
            }
    }

    /**
     * Saves/updates all karat prices in Firestore
     */
    fun saveAllGoldPrices(prices: Map<Int, Double>) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
        val batch = firestore.batch()
        for ((karat, price) in prices) {
            val docRef = firestore.collection("users").document(uid).collection("gold_prices").document("karat_$karat")
            val data = hashMapOf<String, Any>(
                "karat" to karat,
                "pricePerGram" to price,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(docRef, data, SetOptions.merge())
        }
        batch.commit()
            .addOnSuccessListener {
                Log.d(tag, "All gold prices synced to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.w(tag, "Failed to sync all gold prices to Firestore: ${e.message}")
            }
    }

    private fun logActivity(uid: String, action: String, details: String) {
        val logId = UUID.randomUUID().toString()
        val logMap = hashMapOf<String, Any>(
            "id" to logId,
            "action" to action,
            "itemTitle" to details,
            "timestamp" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid).collection("activity_log").document(logId).set(logMap)
    }

    /**
     * Creates a new Outing in Firestore under users/{uid}/outings/{outingId}
     */
    fun createOuting(
        name: String,
        participantNames: List<String>,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            onComplete?.invoke(false, "لم يتم تسجيل الدخول")
            return
        }
        val uid = currentUser.uid
        val outingId = UUID.randomUUID().toString()
        val data = hashMapOf<String, Any>(
            "id" to outingId,
            "name" to name,
            "participantNames" to participantNames,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid).collection("outings").document(outingId)
            .set(data)
            .addOnSuccessListener {
                onComplete?.invoke(true, outingId)
            }
            .addOnFailureListener { e ->
                onComplete?.invoke(false, e.message)
            }
    }

    /**
     * Updates an Outing's name and participants in Firestore
     */
    fun updateOuting(
        outingId: String,
        name: String,
        participantNames: List<String>,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            onComplete?.invoke(false)
            return
        }
        val uid = currentUser.uid
        val data = hashMapOf<String, Any>(
            "name" to name,
            "participantNames" to participantNames,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid).collection("outings").document(outingId)
            .update(data)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    /**
     * Deletes an Outing and its expenses subcollection
     */
    fun deleteOuting(outingId: String, onComplete: ((Boolean) -> Unit)? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            onComplete?.invoke(false)
            return
        }
        val uid = currentUser.uid
        val outingRef = firestore.collection("users").document(uid).collection("outings").document(outingId)

        outingRef.collection("expenses").get().addOnSuccessListener { snapshot ->
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.delete(outingRef)
            batch.commit()
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        }.addOnFailureListener {
            outingRef.delete()
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        }
    }

    /**
     * Adds an expense to an Outing: users/{uid}/outings/{outingId}/expenses/{expenseId}
     */
    fun addOutingExpense(
        outingId: String,
        description: String,
        amount: Double,
        category: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            onComplete?.invoke(false)
            return
        }
        val uid = currentUser.uid
        val expenseId = UUID.randomUUID().toString()
        val data = hashMapOf<String, Any>(
            "id" to expenseId,
            "description" to description,
            "amount" to amount,
            "category" to category,
            "date" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid)
            .collection("outings").document(outingId)
            .collection("expenses").document(expenseId)
            .set(data)
            .addOnSuccessListener {
                firestore.collection("users").document(uid).collection("outings").document(outingId)
                    .update("updatedAt", FieldValue.serverTimestamp())
                onComplete?.invoke(true)
            }
            .addOnFailureListener {
                onComplete?.invoke(false)
            }
    }

    /**
     * Deletes an expense from an Outing
     */
    fun deleteOutingExpense(
        outingId: String,
        expenseId: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            onComplete?.invoke(false)
            return
        }
        val uid = currentUser.uid
        firestore.collection("users").document(uid)
            .collection("outings").document(outingId)
            .collection("expenses").document(expenseId)
            .delete()
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    /**
     * Real-time listener for user's outings: users/{uid}/outings
     */
    fun getOutingsFlow(): Flow<List<CloudOuting>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val uid = currentUser.uid
        val query = firestore.collection("users")
            .document(uid)
            .collection("outings")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(tag, "Outings snapshot error for user $uid: ${error.message}")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    CloudOuting(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        participantNames = (doc.get("participantNames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) { null }
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time listener for expenses in an outing: users/{uid}/outings/{outingId}/expenses
     */
    fun getOutingExpensesFlow(outingId: String): Flow<List<CloudOutingExpense>> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null || outingId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val uid = currentUser.uid
        val query = firestore.collection("users")
            .document(uid)
            .collection("outings")
            .document(outingId)
            .collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(tag, "Outing expenses snapshot error for user $uid, outing $outingId: ${error.message}")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    CloudOutingExpense(
                        id = doc.getString("id") ?: doc.id,
                        description = doc.getString("description") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        category = doc.getString("category") ?: "",
                        date = doc.getTimestamp("date")?.toDate()?.time ?: doc.getLong("date") ?: System.currentTimeMillis(),
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) { null }
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Uploads user profile photo to Firebase Storage: profile_photos/{uid}/photo.jpg
     * and saves photoUrl in users/{uid} document in Firestore.
     */
    suspend fun uploadProfilePhoto(uid: String, imageUri: android.net.Uri): String {
        val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
        val ref = storage.reference.child("profile_photos/$uid/photo.jpg")

        // Put file with metadata
        kotlinx.coroutines.suspendCancellableCoroutine<com.google.firebase.storage.UploadTask.TaskSnapshot> { cont ->
            ref.putFile(imageUri)
                .addOnSuccessListener { snapshot -> cont.resume(snapshot) {} }
                .addOnFailureListener { exc -> cont.resumeWith(Result.failure(exc)) }
                .addOnCanceledListener { cont.cancel() }
        }

        // Get download URL
        val downloadUri = kotlinx.coroutines.suspendCancellableCoroutine<android.net.Uri> { cont ->
            ref.downloadUrl
                .addOnSuccessListener { uri -> cont.resume(uri) {} }
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
                .addOnSuccessListener { cont.resume(null) {} }
                .addOnFailureListener { exc -> cont.resumeWith(Result.failure(exc)) }
        }

        return downloadUrl
    }
}
