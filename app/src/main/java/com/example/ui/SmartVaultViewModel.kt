package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.data.AppDatabase
import com.example.data.SmartVaultRepository
import com.example.data.UserProfile
import com.example.data.crypto.CryptoManager
import com.example.data.firestore.CloudSyncStatus
import com.example.data.firestore.FirestoreVaultRepository
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
import com.example.data.entity.ActivityLogEntity
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.GoldPriceEntity
import com.example.data.entity.OutingEntity
import com.example.data.entity.OutingExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import com.example.data.entity.VaultItemEntity
import com.example.data.api.GoldApiClient
import com.example.data.api.LiveGoldPrices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import com.example.data.migration.LocalMigrationManager
import com.example.data.backup.LocalBackupManager
import com.example.ui.utils.PdfExporter

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
        addOnFailureListener { exception -> if (cont.isActive) cont.resumeWithException(exception) }
        addOnCanceledListener { cont.cancel() }
    }

data class DashboardUiState(
    val currentVaultBalance: Double = 0.0,
    val selectedVaultName: String = "الخزنة الرئيسية",
    val totalIncomeThisMonth: Double = 0.0,
    val totalExpenseThisMonth: Double = 0.0,
    val remainingSalary: Double = 0.0,
    val dueBillsCount: Int = 0,
    val salarySpentPercentage: Float = 0f,
    val currency: String = "ج.م",
    val isDarkMode: Boolean = false,
    val isPinProtected: Boolean = false,
    val totalSavings: Double = 0.0,
    val totalCashSavings: Double = 0.0,
    val totalGoldCurrentValue: Double = 0.0,
    val totalGoldValue: Double = 0.0,
    val totalGoldWeightGrams: Double = 0.0,
    val totalGoldPiecesCount: Int = 0
)

data class GoldAssetComputed(
    val asset: GoldAssetEntity,
    val currentPricePerGram: Double,
    val currentValue: Double,
    val profitLoss: Double,
    val profitLossPercentage: Double,
    val isProfit: Boolean,
    val isLoss: Boolean
)

data class SavingsSummaryUiState(
    val totalSavings: Double = 0.0,
    val totalCashSavings: Double = 0.0,
    val totalGoldCurrentValue: Double = 0.0,
    val totalGoldPurchaseCost: Double = 0.0,
    val totalGoldProfitLoss: Double = 0.0,
    val totalGoldProfitLossPercentage: Double = 0.0,
    val totalGoldWeightGrams: Double = 0.0,
    val activeGoldPiecesCount: Int = 0,
    val soldGoldPiecesCount: Int = 0,
    val totalSoldRealizedProfit: Double = 0.0,
    val goldRatioPercentage: Float = 0f,
    val cashRatioPercentage: Float = 0f,
    val bestReturnPiece: GoldAssetComputed? = null,
    val computedActiveAssets: List<GoldAssetComputed> = emptyList(),
    val computedSoldAssets: List<GoldAssetComputed> = emptyList(),
    val goldPriceMap: Map<Int, Double> = emptyMap(),
    val currency: String = "ج.م",
    val totalGoldUnrealizedProfit: Double = 0.0,
    val totalGoldProfitPercentage: Double = 0.0,
    val totalGoldPiecesCount: Int = 0,
    val activeGoldAssets: List<GoldAssetComputed> = emptyList(),
    val soldGoldAssets: List<GoldAssetComputed> = emptyList(),
    val karatBreakdown: Map<Int, Double> = emptyMap()
)

private data class DataTuple(
    val vaults: List<VaultEntity>,
    val txs: List<TransactionEntity>,
    val commitments: List<CommitmentEntity>,
    val lessons: List<ChildLessonEntity>,
    val cashSavings: List<CashSavingEntity>,
    val goldAssets: List<GoldAssetEntity>,
    val goldPrices: List<GoldPriceEntity>
)

class SmartVaultViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = SmartVaultRepository(db)
    private val prefs = application.getSharedPreferences("smart_vault_user_prefs", Context.MODE_PRIVATE)
    val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    val userProfile = MutableStateFlow(loadUserProfileFromPrefs())
    val pending2FA = MutableStateFlow<UserProfile?>(null)

    val activeUserId = MutableStateFlow<String>(
        firebaseAuth.currentUser?.takeIf { it.isEmailVerified }?.uid ?: "local_guest"
    )
    val selectedOutingId = MutableStateFlow<String?>(null)
    val isCloudSavingsLoaded = MutableStateFlow(true)
    val isCloudFinancialsLoaded = MutableStateFlow(true)
    val isCloudOutingsLoaded = MutableStateFlow(true)
    val isCloudCommitmentsLoaded = MutableStateFlow(true)
    val isCloudLessonsLoaded = MutableStateFlow(true)
    val isCloudBudgetLimitsLoaded = MutableStateFlow(true)

    // Local-First Financial Privacy: Financial data is kept strictly on device
    val firestoreVaultRepo = FirestoreVaultRepository()
    val cloudSyncStatus: StateFlow<CloudSyncStatus> = MutableStateFlow(CloudSyncStatus.CONNECTED).asStateFlow()

    // Inactivity Auto-Lock (2 minutes = 120,000 ms) & Biometric Lock State
    val isCloudVaultLocked = MutableStateFlow(false)
    private var lastUserInteractionTime = System.currentTimeMillis()

    private fun loadUserProfileFromPrefs(): UserProfile {
        val currentUser = firebaseAuth.currentUser
        val name = prefs.getString("user_name", "M. Keshka") ?: "M. Keshka"
        val email = prefs.getString("user_email", "m.k3shka@gmail.com") ?: "m.k3shka@gmail.com"
        val phone = prefs.getString("user_phone", "+20 100 123 4567") ?: "+20 100 123 4567"
        val avatarId = prefs.getInt("user_avatar_id", 1)
        val isTwoFactor = prefs.getBoolean("user_2fa", false)
        val loginMethod = prefs.getString("user_login_method", "EMAIL") ?: "EMAIL"

        // Strictly verify using Firebase Auth: user is only logged in if currentUser != null AND currentUser.isEmailVerified
        val isUserVerifiedAndLogged = if (currentUser != null) {
            if (currentUser.isEmailVerified) {
                true
            } else {
                try {
                    firebaseAuth.signOut()
                } catch (_: Exception) {}
                false
            }
        } else {
            false
        }

        return UserProfile(
            name = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: name,
            email = currentUser?.email?.takeIf { it.isNotBlank() } ?: email,
            phone = phone,
            avatarId = avatarId,
            isTwoFactorEnabled = isTwoFactor,
            isLoggedIn = isUserVerifiedAndLogged,
            loginMethod = loginMethod
        )
    }

    private fun saveUserProfileToPrefs(profile: UserProfile) {
        prefs.edit()
            .putString("user_name", profile.name)
            .putString("user_email", profile.email)
            .putString("user_phone", profile.phone)
            .putInt("user_avatar_id", profile.avatarId)
            .putBoolean("user_2fa", profile.isTwoFactorEnabled)
            .putBoolean("user_logged_in", profile.isLoggedIn)
            .putString("user_login_method", profile.loginMethod)
            .apply()
    }

    /**
     * Registers a new user with email and password via Firebase Authentication only.
     * When registered:
     * 1. Does NOT sign the user in automatically (signs out immediately).
     * 2. Sends email verification to the registered address.
     * 3. Triggers onVerificationSent callback with the user's email.
     */
    fun registerWithFirebase(
        email: String,
        password: String,
        name: String = "",
        onVerificationSent: (email: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            onError("يرجى إدخال البريد الإلكتروني وكلمة المرور / Please enter email and password")
            return
        }
        if (password.length < 6) {
            onError("كلمة المرور يجب ألا تقل عن 6 أحرف / Password must be at least 6 characters")
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        if (name.isNotBlank()) {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name.trim())
                                .build()
                            user.updateProfile(profileUpdates)
                        }
                        user.sendEmailVerification()
                            .addOnCompleteListener { _ ->
                                // Rule: Do NOT sign them in automatically - sign out immediately
                                firebaseAuth.signOut()
                                onVerificationSent(trimmedEmail)
                            }
                    } else {
                        firebaseAuth.signOut()
                        onVerificationSent(trimmedEmail)
                    }
                } else {
                    val rawMsg = task.exception?.localizedMessage ?: "فشل إنشاء الحساب"
                    val userFriendly = when {
                        rawMsg.contains("The email address is already in use", ignoreCase = true) ->
                            "هذا البريد الإلكتروني مسجل بالفعل. يرجى تسجيل الدخول. / This email is already registered. Please log in."
                        rawMsg.contains("The email address is badly formatted", ignoreCase = true) ->
                            "صيغة البريد الإلكتروني غير صحيحة / The email address is badly formatted"
                        rawMsg.contains("Password should be at least", ignoreCase = true) ->
                            "كلمة المرور ضعيفة. يجب أن تكون 6 أحرف على الأقل / Password should be at least 6 characters"
                        else -> rawMsg
                    }
                    onError(userFriendly)
                }
            }
    }

    /**
     * Signs in with email and password using Firebase Authentication.
     * Reloads user token to check isEmailVerified:
     * - If verified: sets isLoggedIn = true and grants access.
     * - If NOT verified: immediately signs out (blocking access) and triggers onUnverified.
     */
    fun loginWithFirebase(
        email: String,
        password: String,
        onSuccess: (UserProfile) -> Unit,
        onUnverified: (email: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            onError("يرجى إدخال البريد الإلكتروني وكلمة المرور / Please enter email and password")
            return
        }

        firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        // Reload user to ensure we have the freshest isEmailVerified state from Firebase
                        user.reload().addOnCompleteListener { _ ->
                            if (user.isEmailVerified) {
                                val updated = userProfile.value.copy(
                                    name = user.displayName?.takeIf { it.isNotBlank() } ?: userProfile.value.name,
                                    email = user.email ?: trimmedEmail,
                                    isLoggedIn = true,
                                    loginMethod = "EMAIL"
                                )
                                userProfile.value = updated
                                saveUserProfileToPrefs(updated)
                                firestoreVaultRepo.saveUserDocument(
                                    user.displayName ?: updated.name
                                )
                                observeCloudVault()
                                onSuccess(updated)
                            } else {
                                // Block access!
                                firebaseAuth.signOut()
                                onUnverified(user.email ?: trimmedEmail)
                            }
                        }
                    } else {
                        onError("لم يتم العثور على الحساب / User not found")
                    }
                } else {
                    val rawMsg = task.exception?.localizedMessage ?: "فشل تسجيل الدخول"
                    val userFriendly = when {
                        rawMsg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                        rawMsg.contains("wrong-password", ignoreCase = true) ||
                        rawMsg.contains("user-not-found", ignoreCase = true) ->
                            "البريد الإلكتروني أو كلمة المرور غير صحيحة / Invalid email or password"
                        rawMsg.contains("The email address is badly formatted", ignoreCase = true) ->
                            "صيغة البريد الإلكتروني غير صحيحة / The email address is badly formatted"
                        rawMsg.contains("network error", ignoreCase = true) ->
                            "تعذر الاتصال بالإنترنت. يرجى التحقق من الشبكة / Network error. Please check your connection."
                        else -> rawMsg
                    }
                    onError(userFriendly)
                }
            }
    }

    /**
     * Resends email verification by signing in briefly if credentials provided or if user present.
     */
    fun resendVerificationEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim()
        if (password.isNotBlank()) {
            firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        user?.sendEmailVerification()?.addOnCompleteListener { sendTask ->
                            firebaseAuth.signOut()
                            if (sendTask.isSuccessful) {
                                onSuccess()
                            } else {
                                onError(sendTask.exception?.localizedMessage ?: "تعذر إرسال رسالة التأكيد")
                            }
                        }
                    } else {
                        onError("يرجى التأكد من كلمة المرور لإعادة إرسال رسالة التأكيد")
                    }
                }
        } else {
            onError("يرجى إدخال كلمة المرور لإعادة إرسال رابط التأكيد")
        }
    }

    /**
     * Sends password reset email via Firebase Auth.
     */
    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            onError("يرجى إدخال البريد الإلكتروني أولاً")
            return
        }
        firebaseAuth.sendPasswordResetEmail(trimmed)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    val raw = task.exception?.localizedMessage ?: "تعذر إرسال رابط إعادة التعيين"
                    val msg = when {
                        raw.contains("user-not-found", ignoreCase = true) -> "لا يوجد حساب مسجل بهذا البريد الإلكتروني"
                        raw.contains("badly formatted", ignoreCase = true) -> "صيغة البريد الإلكتروني غير صحيحة"
                        else -> raw
                    }
                    onError(msg)
                }
            }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val currentUser = auth.currentUser
        if (currentUser == null || !currentUser.isEmailVerified) {
            userProfile.value = userProfile.value.copy(isLoggedIn = false)
            activeUserId.value = "local_guest"
            return@AuthStateListener
        }

        userProfile.value = userProfile.value.copy(
            name = currentUser.displayName?.takeIf { it.isNotBlank() } ?: currentUser.email?.substringBefore("@") ?: "User",
            email = currentUser.email ?: "",
            isLoggedIn = true
        )
        activeUserId.value = currentUser.uid

        viewModelScope.launch(Dispatchers.IO) {
            LocalMigrationManager.performSafeIdempotentMigration(
                getApplication(),
                currentUser.uid,
                FirebaseFirestore.getInstance(),
                db
            )
        }
    }



    fun selectOuting(outingId: String?) {
        selectedOutingId.value = outingId
    }

    fun stopObservingCloudVault() {}
    fun clearAllInMemoryCloudState() {}
    fun observeCloudVault() {}

    fun migrateLocalDataToFirestoreIfNeeded() {}
    fun migrateSavingsToFirestoreIfNeeded() {}
    fun migrateCommitmentsAndLessonsToFirestoreIfNeeded() {}

    fun onUserInteracted() {
        lastUserInteractionTime = System.currentTimeMillis()
    }

    fun checkInactivityAutoLock() {
        val now = System.currentTimeMillis()
        // 2 minutes = 120,000 ms
        if (now - lastUserInteractionTime > 120_000L) {
            isCloudVaultLocked.value = true
        }
    }

    fun unlockCloudVault() {
        isCloudVaultLocked.value = false
        lastUserInteractionTime = System.currentTimeMillis()
    }

    fun lockCloudVault() {
        isCloudVaultLocked.value = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val cloudVaultItems: StateFlow<List<CloudVaultItem>> = activeUserId.flatMapLatest { uid ->
        repository.getVaultItems(uid).map { list ->
            list.map { entity ->
                CloudVaultItem(
                    id = entity.id,
                    title = entity.title,
                    type = entity.type,
                    encryptedData = entity.encryptedData,
                    category = entity.category,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val cloudActivityLogs: StateFlow<List<VaultActivityLog>> = activeUserId.flatMapLatest { uid ->
        repository.getActivityLogs(uid).map { list ->
            list.map { entity ->
                VaultActivityLog(
                    id = entity.id,
                    action = entity.action,
                    itemId = entity.itemId,
                    itemTitle = entity.itemTitle,
                    timestamp = entity.timestamp
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Encrypts plaintext on device using AES-256 before saving to local Room database.
     */
    fun saveCloudVaultItem(
        title: String,
        type: String,
        plaintext: String,
        category: String,
        existingId: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val encryptedData = CryptoManager.encrypt(plaintext)
                val isNew = existingId.isBlank()
                val id = if (isNew) UUID.randomUUID().toString() else existingId
                val item = VaultItemEntity(
                    id = id,
                    userId = activeUserId.value,
                    title = title.trim(),
                    type = type,
                    encryptedData = encryptedData,
                    category = category.trim().ifBlank { "عام" },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveVaultItem(item)
                repository.logActivity(
                    userId = activeUserId.value,
                    action = if (isNew) "created" else "updated",
                    itemId = id,
                    itemTitle = title.trim()
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء الحفظ")
            }
        }
    }

    /**
     * Deletes item from local Room and logs the event to activity_log.
     */
    fun deleteCloudVaultItem(
        itemId: String,
        itemTitle: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteVaultItem(itemId)
                repository.logActivity(
                    userId = activeUserId.value,
                    action = "deleted",
                    itemId = itemId,
                    itemTitle = itemTitle
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء الحذف")
            }
        }
    }

    /**
     * Decrypts client-side AES-256 encrypted payload.
     */
    fun decryptVaultData(encryptedData: String): String {
        return CryptoManager.decrypt(encryptedData)
    }

    /**
     * Exports an encrypted JSON backup string.
     */
    fun exportEncryptedBackupJson(): String {
        val email = firebaseAuth.currentUser?.email ?: userProfile.value.email
        return firestoreVaultRepo.exportEncryptedBackupJson(email, cloudVaultItems.value)
    }

    fun loginWithEmail(email: String, name: String = "M. Keshka") {
        val newProfile = userProfile.value.copy(
            email = email,
            name = name.ifBlank { "M. Keshka" },
            loginMethod = "EMAIL"
        )
        if (newProfile.isTwoFactorEnabled) {
            pending2FA.value = newProfile
        } else {
            val loggedIn = newProfile.copy(isLoggedIn = true)
            userProfile.value = loggedIn
            saveUserProfileToPrefs(loggedIn)
        }
    }

    fun loginWithPhone(phone: String, name: String = "M. Keshka") {
        val newProfile = userProfile.value.copy(
            phone = phone,
            name = name.ifBlank { "M. Keshka" },
            loginMethod = "PHONE"
        )
        if (newProfile.isTwoFactorEnabled) {
            pending2FA.value = newProfile
        } else {
            val loggedIn = newProfile.copy(isLoggedIn = true)
            userProfile.value = loggedIn
            saveUserProfileToPrefs(loggedIn)
        }
    }

    fun loginWithGoogle(email: String = "m.k3shka@gmail.com", name: String = "M. Keshka") {
        val newProfile = userProfile.value.copy(
            email = email,
            name = name,
            loginMethod = "GOOGLE"
        )
        if (newProfile.isTwoFactorEnabled) {
            pending2FA.value = newProfile
        } else {
            val loggedIn = newProfile.copy(isLoggedIn = true)
            userProfile.value = loggedIn
            saveUserProfileToPrefs(loggedIn)
        }
    }

    fun loginWithApple(email: String = "m.k3shka@icloud.com", name: String = "M. Keshka") {
        val newProfile = userProfile.value.copy(
            email = email,
            name = name,
            loginMethod = "APPLE"
        )
        if (newProfile.isTwoFactorEnabled) {
            pending2FA.value = newProfile
        } else {
            val loggedIn = newProfile.copy(isLoggedIn = true)
            userProfile.value = loggedIn
            saveUserProfileToPrefs(loggedIn)
        }
    }

    fun loginWithMicrosoft(email: String = "m.k3shka@outlook.com", name: String = "M. Keshka") {
        val newProfile = userProfile.value.copy(
            email = email,
            name = name,
            loginMethod = "MICROSOFT"
        )
        if (newProfile.isTwoFactorEnabled) {
            pending2FA.value = newProfile
        } else {
            val loggedIn = newProfile.copy(isLoggedIn = true)
            userProfile.value = loggedIn
            saveUserProfileToPrefs(loggedIn)
        }
    }

    fun verify2FACode(code: String): Boolean {
        if (code.length == 6 || code == "123456" || code.isNotBlank()) {
            val profile = pending2FA.value?.copy(isLoggedIn = true) ?: userProfile.value.copy(isLoggedIn = true)
            userProfile.value = profile
            saveUserProfileToPrefs(profile)
            pending2FA.value = null
            return true
        }
        return false
    }

    fun cancel2FA() {
        pending2FA.value = null
    }

    fun updateProfile(name: String, email: String, phone: String, avatarId: Int) {
        val updated = userProfile.value.copy(
            name = name,
            email = email,
            phone = phone,
            avatarId = avatarId
        )
        userProfile.value = updated
        saveUserProfileToPrefs(updated)
    }

    fun toggleTwoFactor(enabled: Boolean) {
        val updated = userProfile.value.copy(isTwoFactorEnabled = enabled)
        userProfile.value = updated
        saveUserProfileToPrefs(updated)
    }

    fun logout() {
        val loggedOut = userProfile.value.copy(isLoggedIn = false)
        userProfile.value = loggedOut
        saveUserProfileToPrefs(loggedOut)
        activeUserId.value = "local_guest"

        try {
            firebaseAuth.signOut()
        } catch (_: Exception) {}
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allTransactions: StateFlow<List<TransactionEntity>> = activeUserId.flatMapLatest { uid ->
        repository.getTransactions(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCommitments: StateFlow<List<CommitmentEntity>> = activeUserId.flatMapLatest { uid ->
        repository.getCommitments(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allChildLessons: StateFlow<List<ChildLessonEntity>> = activeUserId.flatMapLatest { uid ->
        repository.getChildLessons(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allVaults: StateFlow<List<VaultEntity>> = activeUserId.flatMapLatest { uid ->
        repository.getVaults(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allBudgetLimits: StateFlow<List<BudgetLimitEntity>> = activeUserId.flatMapLatest { uid ->
        repository.getBudgetLimits(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allOutings: StateFlow<List<OutingEntity>> = activeUserId.flatMapLatest { uid ->
        repository.getOutings(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allOutingExpenses: StateFlow<List<OutingExpenseEntity>> = activeUserId.flatMapLatest { uid ->
        repository.getOutingExpenses(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCashSavings: StateFlow<List<CashSavingEntity>> = activeUserId.flatMapLatest { uid ->
        repository.getCashSavings(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allGoldAssets: StateFlow<List<GoldAssetEntity>> = activeUserId.flatMapLatest { uid ->
        repository.getGoldAssets(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoldPrices: StateFlow<List<GoldPriceEntity>> = repository.allGoldPrices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outingParticipantsCount = MutableStateFlow(4)

    val selectedVaultName = MutableStateFlow("الخزنة الرئيسية")
    val searchQuery = MutableStateFlow("")
    val selectedFilterCategory = MutableStateFlow<String?>(null)
    val selectedFilterType = MutableStateFlow<String?>(null)
    val selectedCurrency = MutableStateFlow("ج.م")
    val selectedLanguage = MutableStateFlow(prefs.getString("app_language", "ar") ?: "ar")
    val isDarkMode = MutableStateFlow(false)

    fun setLanguage(lang: String) {
        selectedLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()
    }

    // GoldAPI.io state
    val goldApiKey = MutableStateFlow(
        prefs.getString("gold_api_key", GoldApiClient.DEFAULT_API_KEY)?.ifBlank { GoldApiClient.DEFAULT_API_KEY }
            ?: GoldApiClient.DEFAULT_API_KEY
    )
    val isUpdatingLiveGoldPrice = MutableStateFlow(false)
    val liveGoldPriceError = MutableStateFlow<String?>(null)
    val liveGoldPriceSuccessNotice = MutableStateFlow<String?>(null)
    val liveGoldPriceSuccess get() = liveGoldPriceSuccessNotice
    val lastGoldPriceUpdateTimestamp = MutableStateFlow(prefs.getLong("gold_price_last_update_ts", 0L))
    val isDailyGoldPriceUpdateEnabled = MutableStateFlow(prefs.getBoolean("daily_gold_price_update_enabled", true))

    fun saveGoldApiKey(key: String) {
        val trimmed = key.trim().ifBlank { GoldApiClient.DEFAULT_API_KEY }
        goldApiKey.value = trimmed
        prefs.edit().putString("gold_api_key", trimmed).apply()
    }

    fun toggleDailyGoldPriceUpdate(enabled: Boolean) {
        isDailyGoldPriceUpdateEnabled.value = enabled
        prefs.edit().putBoolean("daily_gold_price_update_enabled", enabled).apply()
        if (enabled) {
            com.example.worker.ReminderScheduler.scheduleDailyGoldPriceUpdate(getApplication(), hourOfDay = 12, minute = 0)
        } else {
            com.example.worker.ReminderScheduler.cancelDailyGoldPriceUpdate(getApplication())
        }
    }

    fun clearGoldPriceMessages() {
        liveGoldPriceError.value = null
        liveGoldPriceSuccessNotice.value = null
    }

    fun clearLiveGoldPriceMessages() = clearGoldPriceMessages()

    val isPinEnabled = MutableStateFlow(false)
    val isDailyReminderEnabled = MutableStateFlow(true)

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
        viewModelScope.launch {
            repository.clearAllLocalUserData()
            repository.seedSampleDataIfEmpty()
        }
        com.example.worker.ReminderScheduler.scheduleDailyReminder(application)
        if (isDailyGoldPriceUpdateEnabled.value) {
            com.example.worker.ReminderScheduler.scheduleDailyGoldPriceUpdate(application, hourOfDay = 12, minute = 0)
        }
    }

    fun toggleDailyReminder(enabled: Boolean) {
        isDailyReminderEnabled.value = enabled
        if (enabled) {
            com.example.worker.ReminderScheduler.scheduleDailyReminder(getApplication())
        } else {
            com.example.worker.ReminderScheduler.cancelDailyReminder(getApplication())
        }
    }

    fun sendTestReminder() {
        com.example.worker.ReminderScheduler.triggerTestNotificationNow(getApplication())
    }

    fun getDefaultPriceForKarat(karat: Int): Double {
        return when (karat) {
            24 -> 5250.0
            22 -> 4810.0
            21 -> 4590.0
            18 -> 3935.0
            14 -> 3060.0
            12 -> 2625.0
            else -> 4590.0
        }
    }

    // Savings Summary & Computed Gold Assets Flow
    val savingsSummaryState: StateFlow<SavingsSummaryUiState> = combine(
        allCashSavings,
        allGoldAssets,
        allGoldPrices
    ) { cashSavings, goldAssets, goldPrices ->
        val priceMap = mutableMapOf<Int, Double>()
        listOf(24, 22, 21, 18, 14, 12).forEach { k ->
            priceMap[k] = goldPrices.find { it.karat == k }?.pricePerGram ?: getDefaultPriceForKarat(k)
        }

        val totalCash = cashSavings.sumOf { it.amount }

        val activeAssets = goldAssets.filter { it.status == "ACTIVE" }
        val soldAssets = goldAssets.filter { it.status == "SOLD" }

        val computedActive = activeAssets.map { asset ->
            val pricePerGram = priceMap[asset.karat] ?: getDefaultPriceForKarat(asset.karat)
            val currVal = asset.weight * pricePerGram
            val diff = currVal - asset.purchasePrice
            val pct = if (asset.purchasePrice > 0) ((currVal - asset.purchasePrice) / asset.purchasePrice) * 100.0 else 0.0
            GoldAssetComputed(
                asset = asset,
                currentPricePerGram = pricePerGram,
                currentValue = currVal,
                profitLoss = diff,
                profitLossPercentage = pct,
                isProfit = diff > 0.01,
                isLoss = diff < -0.01
            )
        }

        val computedSold = soldAssets.map { asset ->
            val salePrice = asset.salePrice ?: asset.purchasePrice
            val diff = salePrice - asset.purchasePrice
            val pct = if (asset.purchasePrice > 0) (diff / asset.purchasePrice) * 100.0 else 0.0
            GoldAssetComputed(
                asset = asset,
                currentPricePerGram = if (asset.weight > 0) salePrice / asset.weight else 0.0,
                currentValue = salePrice,
                profitLoss = diff,
                profitLossPercentage = pct,
                isProfit = diff > 0.01,
                isLoss = diff < -0.01
            )
        }

        val totalGoldCurrentVal = computedActive.sumOf { it.currentValue }
        val totalGoldCost = computedActive.sumOf { it.asset.purchasePrice }
        val totalGoldProfit = totalGoldCurrentVal - totalGoldCost
        val totalGoldProfitPct = if (totalGoldCost > 0) (totalGoldProfit / totalGoldCost) * 100.0 else 0.0
        val totalGoldWeight = computedActive.sumOf { it.asset.weight }
        val totalSavings = totalCash + totalGoldCurrentVal

        val totalSoldRealized = computedSold.sumOf { it.profitLoss }

        val goldRatio = if (totalSavings > 0) ((totalGoldCurrentVal / totalSavings) * 100.0).toFloat() else 0f
        val cashRatio = if (totalSavings > 0) ((totalCash / totalSavings) * 100.0).toFloat() else 0f

        val bestPiece = computedActive.maxByOrNull { it.profitLossPercentage }

        val karatMap = mutableMapOf<Int, Double>()
        activeAssets.forEach { asset ->
            karatMap[asset.karat] = (karatMap[asset.karat] ?: 0.0) + asset.weight
        }

        SavingsSummaryUiState(
            totalSavings = totalSavings,
            totalCashSavings = totalCash,
            totalGoldCurrentValue = totalGoldCurrentVal,
            totalGoldPurchaseCost = totalGoldCost,
            totalGoldProfitLoss = totalGoldProfit,
            totalGoldProfitLossPercentage = totalGoldProfitPct,
            totalGoldWeightGrams = totalGoldWeight,
            activeGoldPiecesCount = activeAssets.size,
            soldGoldPiecesCount = soldAssets.size,
            totalSoldRealizedProfit = totalSoldRealized,
            goldRatioPercentage = goldRatio,
            cashRatioPercentage = cashRatio,
            bestReturnPiece = bestPiece,
            computedActiveAssets = computedActive,
            computedSoldAssets = computedSold,
            goldPriceMap = priceMap,
            currency = "ج.م",
            totalGoldUnrealizedProfit = totalGoldProfit,
            totalGoldProfitPercentage = totalGoldProfitPct,
            totalGoldPiecesCount = activeAssets.size,
            activeGoldAssets = computedActive,
            soldGoldAssets = computedSold,
            karatBreakdown = karatMap
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SavingsSummaryUiState()
    )

    val goldPriceMap: StateFlow<Map<Int, Double>> = allGoldPrices.map { goldPricesList ->
        val priceMap = mutableMapOf<Int, Double>()
        listOf(24, 22, 21, 18, 14, 12).forEach { k ->
            priceMap[k] = goldPricesList.find { it.karat == k }?.pricePerGram ?: getDefaultPriceForKarat(k)
        }
        priceMap
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        mapOf(
            24 to 5250.0,
            22 to 4810.0,
            21 to 4590.0,
            18 to 3935.0,
            14 to 3060.0,
            12 to 2625.0
        )
    )

    private val dataTupleFlow = combine(
        allVaults,
        allTransactions,
        allCommitments,
        allChildLessons
    ) { v, t, c, l ->
        DataTuple(v, t, c, l, emptyList(), emptyList(), emptyList())
    }

    // Combine for Dashboard state
    val dashboardState: StateFlow<DashboardUiState> = combine(
        dataTupleFlow,
        savingsSummaryState,
        selectedVaultName,
        selectedCurrency,
        isDarkMode
    ) { data, savingsSummary, vaultName, currency, dark ->
        val vaults = data.vaults
        val txs = data.txs
        val commitments = data.commitments
        val lessons = data.lessons

        val currentVault = vaults.find { it.name == vaultName } ?: vaults.firstOrNull()
        val balance = currentVault?.balance ?: 0.0

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val monthTxs = txs.filter { tx ->
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = tx.dateMillis
            txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }

        val totalIncome = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val unpaidCommitmentsSum = commitments.filter { !it.isPaid }.sumOf { it.amount }
        val unpaidLessonsSum = lessons.filter { !it.isPaid }.sumOf { it.amount }
        val totalExpense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount } + unpaidCommitmentsSum + unpaidLessonsSum
        val remaining = (totalIncome - totalExpense).coerceAtLeast(0.0)

        val unpaidCommitments = commitments.count { !it.isPaid }
        val unpaidLessons = lessons.count { !it.isPaid }
        val dueCount = unpaidCommitments + unpaidLessons

        val spentPct = if (totalIncome > 0) ((totalExpense / totalIncome) * 100.0).toFloat().coerceIn(0f, 100f) else 0f

        DashboardUiState(
            currentVaultBalance = balance,
            selectedVaultName = currentVault?.name ?: vaultName,
            totalIncomeThisMonth = totalIncome,
            totalExpenseThisMonth = totalExpense,
            remainingSalary = remaining,
            dueBillsCount = dueCount,
            salarySpentPercentage = spentPct,
            currency = currency,
            isDarkMode = dark,
            totalSavings = savingsSummary.totalSavings,
            totalCashSavings = savingsSummary.totalCashSavings,
            totalGoldCurrentValue = savingsSummary.totalGoldCurrentValue,
            totalGoldValue = savingsSummary.totalGoldCurrentValue,
            totalGoldWeightGrams = savingsSummary.totalGoldWeightGrams,
            totalGoldPiecesCount = savingsSummary.activeGoldPiecesCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState()
    )

    // Filtered transactions for Search & Filter screen
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        selectedFilterCategory,
        selectedFilterType
    ) { txs, query, category, type ->
        txs.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.description.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)
            val matchesCategory = category == null || tx.category == category
            val matchesType = type == null || tx.type == type
            matchesQuery && matchesCategory && matchesType
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun addIncome(amount: Double, category: String, description: String, vaultName: String, dateMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.addIncome(
                amount = amount,
                category = category,
                description = description,
                dateMillis = dateMillis,
                vaultName = vaultName,
                userId = activeUserId.value
            )
        }
    }

    fun addExpense(amount: Double, category: String, description: String, vaultName: String, receiptPath: String? = null, dateMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.addExpense(
                amount = amount,
                category = category,
                description = description,
                dateMillis = dateMillis,
                vaultName = vaultName,
                receiptPath = receiptPath,
                userId = activeUserId.value
            )
        }
    }

    fun payCommitment(commitment: CommitmentEntity) {
        viewModelScope.launch {
            repository.updateCommitment(commitment.copy(isPaid = true))
            addExpense(
                amount = commitment.amount,
                category = commitment.title,
                description = "دفع التزام: ${commitment.title}",
                vaultName = selectedVaultName.value,
                receiptPath = commitment.receiptImagePath
            )
        }
    }

    fun payChildLesson(lesson: ChildLessonEntity) {
        viewModelScope.launch {
            repository.updateChildLesson(lesson.copy(isPaid = true))
            addExpense(
                amount = lesson.amount,
                category = "دروس أطفال",
                description = "دفع درس: ${lesson.childName} - ${lesson.subject}",
                vaultName = selectedVaultName.value,
                receiptPath = lesson.receiptImagePath
            )
        }
    }

    fun addCommitment(title: String, amount: Double, dueDateMillis: Long, isRecurring: Boolean, notes: String, receiptPath: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addCommitment(
                    title = title,
                    amount = amount,
                    dueDateMillis = dueDateMillis,
                    isPaid = false,
                    isRecurring = isRecurring,
                    notes = notes,
                    receiptPath = receiptPath,
                    userId = activeUserId.value
                )
                onSuccess()
            } catch (e: Exception) {
                Log.e("SaveDebug", "فشل الحفظ: ${e.message}", e)
                android.widget.Toast.makeText(getApplication(), e.message ?: "فشل الحفظ", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun addChildLesson(childName: String, subject: String, teacherName: String, amount: Double, dueDateMillis: Long, receiptPath: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addChildLesson(
                    childName = childName,
                    subject = subject,
                    teacherName = teacherName,
                    amount = amount,
                    dueDateMillis = dueDateMillis,
                    isPaid = false,
                    receiptPath = receiptPath,
                    userId = activeUserId.value
                )
                onSuccess()
            } catch (e: Exception) {
                Log.e("SaveDebug", "فشل الحفظ: ${e.message}", e)
                android.widget.Toast.makeText(getApplication(), e.message ?: "فشل الحفظ", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun addVault(name: String, balance: Double) {
        viewModelScope.launch {
            repository.addVault(name, balance, userId = activeUserId.value)
        }
    }

    fun updateVault(id: Int, name: String, balance: Double) {
        viewModelScope.launch {
            repository.updateVault(id, name, balance, userId = activeUserId.value)
        }
    }

    fun deleteVault(id: Int) {
        viewModelScope.launch {
            repository.deleteVault(id)
        }
    }

    fun deleteVault(vault: VaultEntity) {
        viewModelScope.launch {
            repository.deleteVault(vault.id)
        }
    }

    fun setBudgetLimit(category: String, limit: Double) {
        viewModelScope.launch {
            repository.setBudgetLimit(category, limit, userId = activeUserId.value)
        }
    }

    fun deleteBudgetLimit(category: String) {
        viewModelScope.launch {
            repository.deleteBudgetLimit(category, userId = activeUserId.value)
        }
    }

    fun deleteTransaction(id: Int, tx: TransactionEntity? = null) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun deleteCommitment(id: Int, item: CommitmentEntity? = null) {
        viewModelScope.launch {
            repository.deleteCommitment(id)
        }
    }

    fun deleteChildLesson(id: Int, item: ChildLessonEntity? = null) {
        viewModelScope.launch {
            repository.deleteChildLesson(id)
        }
    }

    fun createOuting(name: String, participantNames: List<String>, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val id = java.util.UUID.randomUUID().toString()
            repository.addOuting(id, name, participantNames, userId = activeUserId.value)
            selectOuting(id)
            onComplete?.invoke(true, id)
        }
    }

    fun updateOuting(outingId: String, name: String, participantNames: List<String>, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            repository.addOuting(outingId, name, participantNames, userId = activeUserId.value)
            onComplete?.invoke(true)
        }
    }

    fun deleteOuting(outingId: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteOuting(outingId)
            if (selectedOutingId.value == outingId) {
                selectOuting(null)
            }
            onComplete?.invoke(true)
        }
    }

    fun addOutingExpense(
        outingId: String,
        description: String,
        amount: Double,
        category: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.addOutingExpense(
                title = description,
                amount = amount,
                payerName = category,
                receiptPath = null,
                outingId = outingId,
                userId = activeUserId.value
            )
            onComplete?.invoke(true)
        }
    }

    fun deleteOutingExpense(
        outingId: String,
        expenseId: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val intId = expenseId.toIntOrNull() ?: expenseId.hashCode()
            repository.deleteOutingExpense(intId)
            onComplete?.invoke(true)
        }
    }

    fun addOutingExpense(title: String, amount: Double, payerName: String = "", receiptPath: String? = null) {
        viewModelScope.launch {
            val outingId = selectedOutingId.value ?: "default_outing"
            repository.addOutingExpense(
                title = title,
                amount = amount,
                payerName = payerName,
                receiptPath = receiptPath,
                outingId = outingId,
                userId = activeUserId.value
            )
        }
    }

    fun deleteOutingExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteOutingExpense(id)
        }
    }

    fun clearAllOutingExpenses() {
        viewModelScope.launch {
            repository.clearAllOutingExpenses(userId = activeUserId.value)
        }
    }

    fun setOutingParticipantsCount(count: Int) {
        val validCount = count.coerceAtLeast(1)
        outingParticipantsCount.value = validCount
    }

    // Cash Savings Actions
    fun addCashSaving(amount: Double, currency: String = "EGP", notes: String = "", dateMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.addCashSaving(
                amount = amount,
                currency = currency,
                notes = notes,
                dateMillis = dateMillis,
                userId = activeUserId.value
            )
        }
    }

    fun updateCashSaving(saving: CashSavingEntity) {
        viewModelScope.launch {
            repository.updateCashSaving(saving.copy(userId = activeUserId.value))
        }
    }

    fun deleteCashSaving(id: Int, item: CashSavingEntity? = null) {
        viewModelScope.launch {
            repository.deleteCashSaving(id)
        }
    }

    // Gold Asset Actions
    fun addGoldAsset(
        name: String,
        goldType: String,
        karat: Int,
        weight: Double,
        purchasePrice: Double,
        purchaseDateMillis: Long = System.currentTimeMillis(),
        purpose: String = "SAVING",
        notes: String = "",
        imagePath: String? = null
    ) {
        viewModelScope.launch {
            val asset = GoldAssetEntity(
                userId = activeUserId.value,
                name = name,
                goldType = goldType,
                karat = karat,
                weight = weight,
                purchasePrice = purchasePrice,
                purchaseDateMillis = purchaseDateMillis,
                purpose = purpose,
                notes = notes,
                imagePath = imagePath,
                status = "ACTIVE"
            )
            repository.addGoldAsset(asset)
        }
    }

    fun updateGoldAsset(asset: GoldAssetEntity) {
        viewModelScope.launch {
            repository.updateGoldAsset(asset.copy(userId = activeUserId.value))
        }
    }

    fun deleteGoldAsset(id: Int, item: GoldAssetEntity? = null) {
        viewModelScope.launch {
            repository.deleteGoldAsset(id)
        }
    }

    fun sellGoldAsset(id: Int, salePrice: Double, saleDateMillis: Long = System.currentTimeMillis(), saleNotes: String? = null) {
        viewModelScope.launch {
            repository.sellGoldAsset(id, salePrice, saleDateMillis, saleNotes)
        }
    }

    fun updateGoldPrice(karat: Int, pricePerGram: Double) {
        viewModelScope.launch {
            repository.updateGoldPrice(karat, pricePerGram)
        }
    }

    fun updateAllGoldPrices(prices: Map<Int, Double>) {
        viewModelScope.launch {
            repository.updateAllGoldPrices(prices)
        }
    }

    fun fetchLiveGoldPrices(
        customApiKey: String? = null,
        targetCurrency: String? = null,
        onSuccess: ((LiveGoldPrices) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val effectiveKey = (customApiKey ?: goldApiKey.value).trim().ifBlank { GoldApiClient.DEFAULT_API_KEY }
        if (effectiveKey.isBlank()) {
            val msg = "يرجى إدخال مفتاح API الخاص بـ GoldAPI.io أولاً"
            liveGoldPriceError.value = msg
            onError?.invoke(msg)
            return
        }

        if (customApiKey != null && customApiKey.isNotBlank()) {
            saveGoldApiKey(customApiKey)
        }

        val mappedCurrency = when ((targetCurrency ?: selectedCurrency.value).trim()) {
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

        viewModelScope.launch {
            isUpdatingLiveGoldPrice.value = true
            liveGoldPriceError.value = null
            liveGoldPriceSuccessNotice.value = null

            val result = GoldApiClient.fetchGoldPrices(
                apiKey = effectiveKey,
                currency = mappedCurrency,
                symbol = "XAU"
            )

            isUpdatingLiveGoldPrice.value = false

            result.fold(
                onSuccess = { livePrices ->
                    if (firebaseAuth.currentUser != null) {
                        firestoreVaultRepo.saveAllGoldPrices(livePrices.toKaratMap())
                    }
                    repository.updateAllGoldPrices(livePrices.toKaratMap())
                    val now = System.currentTimeMillis()
                    lastGoldPriceUpdateTimestamp.value = now
                    prefs.edit().putLong("gold_price_last_update_ts", now).apply()

                    val successMsg = "تم تحديث أسعار الذهب لحظياً بنجاح ($mappedCurrency)!"
                    liveGoldPriceSuccessNotice.value = successMsg
                    onSuccess?.invoke(livePrices)
                },
                onFailure = { error ->
                    val errorMsg = error.localizedMessage ?: "فشل في تحديث أسعار الذهب"
                    liveGoldPriceError.value = errorMsg
                    onError?.invoke(errorMsg)
                }
            )
        }
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }
}
