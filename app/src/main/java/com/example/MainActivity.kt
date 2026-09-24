package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.screens.CloudVaultScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SmartVaultViewModel
import com.example.data.entity.VaultEntity
import com.example.ui.dialogs.AddChildLessonDialog
import com.example.ui.dialogs.AddCommitmentDialog
import com.example.ui.dialogs.AddExpenseDialog
import com.example.ui.dialogs.AddIncomeDialog
import com.example.ui.dialogs.AddVaultDialog
import com.example.ui.dialogs.EditProfileDialog
import com.example.ui.dialogs.EditVaultDialog
import com.example.ui.dialogs.SetBudgetDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommitmentsAndLessonsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesAndSearchScreen
import com.example.ui.screens.OutingsScreen
import com.example.ui.screens.ReportsAndChartsScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.SettingsAndVaultsScreen
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SmartVaultTheme

import com.example.ui.utils.AppStrings
import com.example.ui.utils.LocalAppLanguage
import com.example.ui.utils.LocalStrings

class MainActivity : ComponentActivity() {
    private val viewModel: SmartVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

            val layoutDirection = if (selectedLanguage == "en") LayoutDirection.Ltr else LayoutDirection.Rtl
            val appStrings = remember(selectedLanguage) { AppStrings(selectedLanguage) }

            SmartVaultTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalAppLanguage provides selectedLanguage,
                    LocalStrings provides appStrings
                ) {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: SmartVaultViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val pending2FA by viewModel.pending2FA.collectAsStateWithLifecycle()

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val savingsSummaryState by viewModel.savingsSummaryState.collectAsStateWithLifecycle()
    val allGoldAssets by viewModel.allGoldAssets.collectAsStateWithLifecycle()
    val allCashSavings by viewModel.allCashSavings.collectAsStateWithLifecycle()
    val goldPriceMap by viewModel.goldPriceMap.collectAsStateWithLifecycle()

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allCommitments by viewModel.allCommitments.collectAsStateWithLifecycle()
    val allChildLessons by viewModel.allChildLessons.collectAsStateWithLifecycle()
    val allVaults by viewModel.allVaults.collectAsStateWithLifecycle()
    val allBudgetLimits by viewModel.allBudgetLimits.collectAsStateWithLifecycle()
    val allOutingExpenses by viewModel.allOutingExpenses.collectAsStateWithLifecycle()
    val outingParticipantsCount by viewModel.outingParticipantsCount.collectAsStateWithLifecycle()
    val selectedOutingId by viewModel.selectedOutingId.collectAsStateWithLifecycle()

    val goldApiKey by viewModel.goldApiKey.collectAsStateWithLifecycle()
    val isUpdatingLiveGoldPrice by viewModel.isUpdatingLiveGoldPrice.collectAsStateWithLifecycle()
    val lastGoldPriceUpdateTimestamp by viewModel.lastGoldPriceUpdateTimestamp.collectAsStateWithLifecycle()
    val liveGoldPriceError by viewModel.liveGoldPriceError.collectAsStateWithLifecycle()
    val liveGoldPriceSuccess by viewModel.liveGoldPriceSuccess.collectAsStateWithLifecycle()
    val isDailyGoldPriceUpdateEnabled by viewModel.isDailyGoldPriceUpdateEnabled.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilterCategory by viewModel.selectedFilterCategory.collectAsStateWithLifecycle()
    val selectedFilterType by viewModel.selectedFilterType.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val isDailyReminderEnabled by viewModel.isDailyReminderEnabled.collectAsStateWithLifecycle()
    val appStrings = LocalStrings.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.toggleDailyReminder(true)
            }
        }
    )

    // Dialog state controllers
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddCommitmentDialog by remember { mutableStateOf(false) }
    var showAddLessonDialog by remember { mutableStateOf(false) }
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var vaultToEdit by remember { mutableStateOf<VaultEntity?>(null) }

    var budgetTargetCategory by remember { mutableStateOf("سوبر ماركت") }
    var budgetTargetLimit by remember { mutableStateOf(3000.0) }
    var showCloudVaultScreen by remember { mutableStateOf(false) }

    val vaultNames = remember(allVaults) { allVaults.map { it.name } }

    if (!userProfile.isLoggedIn || pending2FA != null) {
        AuthScreen(
            pending2FA = pending2FA,
            onRegisterFirebase = { email, password, name, onSent, onError ->
                viewModel.registerWithFirebase(email, password, name, onSent, onError)
            },
            onLoginFirebase = { email, password, onSuccess, onUnverified, onError ->
                viewModel.loginWithFirebase(email, password, onSuccess, onUnverified, onError)
            },
            onResendVerification = { email, password, onSuccess, onError ->
                viewModel.resendVerificationEmail(email, password, onSuccess, onError)
            },
            onLoginPhone = { phone, name -> viewModel.loginWithPhone(phone, name) },
            onLoginGoogle = { email, name -> viewModel.loginWithGoogle(email, name) },
            onLoginApple = { email, name -> viewModel.loginWithApple(email, name) },
            onLoginMicrosoft = { email, name -> viewModel.loginWithMicrosoft(email, name) },
            onForgotPassword = { email, onSuccess, onError ->
                viewModel.sendPasswordReset(email, onSuccess, onError)
            },
            onVerify2FA = { code -> viewModel.verify2FACode(code) },
            onCancel2FA = { viewModel.cancel2FA() }
        )
        return
    }

    if (showCloudVaultScreen) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الخزنة السحابية المشفرة", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🛡️☁️", fontSize = 14.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showCloudVaultScreen = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CloudVaultScreen(
                    viewModel = viewModel,
                    onBack = { showCloudVaultScreen = false }
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            if (selectedTab != 0 && selectedTab != 1 && selectedTab != 3 && selectedTab != 4) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Right Side (RTL Start): App Shield Icon & Section Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (selectedTab) {
                                    0 -> appStrings.titleHome
                                    1 -> appStrings.titleTransactions
                                    2 -> appStrings.titleCommitments
                                    3 -> appStrings.titleReports
                                    4 -> appStrings.titleSavings
                                    5 -> appStrings.titleOutings
                                    6 -> appStrings.titleSettings
                                    else -> appStrings.titleHome
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Left Side (RTL End): Cloud Vault, Language & Settings Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showCloudVaultScreen = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "الخزنة السحابية",
                                    tint = EmeraldGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.setLanguage(if (selectedLanguage == "ar") "en" else "ar") },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = EmeraldGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { selectedTab = 6 },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedTab == 6) EmeraldGreenPrimary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = appStrings.titleSettings,
                                    tint = if (selectedTab == 6) EmeraldGreenPrimary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple(appStrings.navHome, Icons.Default.Home, 0),
                    Triple(appStrings.navTransactions, Icons.Default.ReceiptLong, 1),
                    Triple(appStrings.navCommitments, Icons.Default.EventNote, 2),
                    Triple(appStrings.navReports, Icons.Default.PieChart, 3),
                    Triple(appStrings.navSavings, Icons.Default.AccountBalanceWallet, 4),
                    Triple(appStrings.navOutings, Icons.Default.Attractions, 5)
                )

                navItems.forEach { (label, icon, index) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) EmeraldGreenPrimary else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EmeraldGreenPrimary else Color.Gray,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = EmeraldGreenPrimary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300)))
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300)))
                }.using(
                    SizeTransform(clip = false)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            label = "ScreenTransition"
        ) { tabIndex ->
            when (tabIndex) {
                0 -> DashboardScreen(
                    state = dashboardState,
                    vaults = allVaults,
                    recentTransactions = allTransactions,
                    userProfile = userProfile,
                    commitments = allCommitments,
                    lessons = allChildLessons,
                    onPayCommitment = { item -> viewModel.payCommitment(item) },
                    onPayLesson = { lesson -> viewModel.payChildLesson(lesson) },
                    onSelectVault = { name -> viewModel.selectedVaultName.value = name },
                    onOpenAddIncome = { showAddIncomeDialog = true },
                    onOpenAddExpense = { showAddExpenseDialog = true },
                    onOpenAddVault = { showAddVaultDialog = true },
                    onOpenSetBudget = { cat, limit ->
                        budgetTargetCategory = cat
                        budgetTargetLimit = limit
                        showSetBudgetDialog = true
                    },
                    onOpenSettings = { selectedTab = 6 },
                    onToggleLanguage = { viewModel.setLanguage(if (selectedLanguage == "ar") "en" else "ar") },
                    onNavigateTab = { index -> selectedTab = index }
                )
                1 -> ExpensesAndSearchScreen(
                    transactions = allTransactions,
                    searchQuery = searchQuery,
                    selectedCategory = selectedFilterCategory,
                    selectedType = selectedFilterType,
                    currency = dashboardState.currency,
                    onSearchQueryChange = { q -> viewModel.searchQuery.value = q },
                    onSelectCategory = { cat -> viewModel.selectedFilterCategory.value = cat },
                    onSelectType = { type -> viewModel.selectedFilterType.value = type },
                    onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx.id, tx) },
                    onOpenAddExpense = { showAddExpenseDialog = true },
                    onOpenAddIncome = { showAddIncomeDialog = true },
                    onOpenSettings = { selectedTab = 6 },
                    onToggleLanguage = { viewModel.setLanguage(if (selectedLanguage == "ar") "en" else "ar") }
                )
                2 -> CommitmentsAndLessonsScreen(
                    commitments = allCommitments,
                    lessons = allChildLessons,
                    currency = dashboardState.currency,
                    onPayCommitment = { item -> viewModel.payCommitment(item) },
                    onPayLesson = { lesson -> viewModel.payChildLesson(lesson) },
                    onDeleteCommitment = { id -> viewModel.deleteCommitment(id) },
                    onDeleteLesson = { id -> viewModel.deleteChildLesson(id) },
                    onOpenAddCommitment = { showAddCommitmentDialog = true },
                    onOpenAddLesson = { showAddLessonDialog = true }
                )
                3 -> ReportsAndChartsScreen(
                    transactions = allTransactions,
                    currency = dashboardState.currency,
                    userProfile = userProfile,
                    vaults = allVaults,
                    commitments = allCommitments,
                    lessons = allChildLessons,
                    goldAssets = allGoldAssets,
                    cashSavings = allCashSavings,
                    goldPriceMap = goldPriceMap,
                    onOpenSettings = { selectedTab = 6 },
                    onToggleLanguage = { viewModel.setLanguage(if (selectedLanguage == "ar") "en" else "ar") }
                )
                4 -> SavingsScreen(
                    savingsState = savingsSummaryState,
                    goldAssets = allGoldAssets,
                    cashSavings = allCashSavings,
                    goldPriceMap = goldPriceMap,
                    goldApiKey = goldApiKey,
                    isUpdatingLiveGoldPrice = isUpdatingLiveGoldPrice,
                    lastGoldPriceUpdateTimestamp = lastGoldPriceUpdateTimestamp,
                    liveGoldPriceError = liveGoldPriceError,
                    liveGoldPriceSuccess = liveGoldPriceSuccess,
                    isDailyGoldPriceUpdateEnabled = isDailyGoldPriceUpdateEnabled,
                    onToggleDailyGoldPriceUpdate = { viewModel.toggleDailyGoldPriceUpdate(it) },
                    onFetchLiveGoldPrices = { key, curr, onS, onE ->
                        viewModel.fetchLiveGoldPrices(key, curr, onS, onE)
                    },
                    onSaveGoldApiKey = { key ->
                        viewModel.saveGoldApiKey(key)
                    },
                    onClearGoldPriceMessages = {
                        viewModel.clearLiveGoldPriceMessages()
                    },
                    onAddGoldAsset = { name, type, karat, weight, price, date, purpose, notes, img ->
                        viewModel.addGoldAsset(name, type, karat, weight, price, date, purpose, notes, img)
                    },
                    onUpdateGoldAsset = { asset -> viewModel.updateGoldAsset(asset) },
                    onDeleteGoldAsset = { id -> viewModel.deleteGoldAsset(id) },
                    onSellGoldAsset = { id, salePrice, saleDate, saleNotes ->
                        viewModel.sellGoldAsset(id, salePrice, saleDate, saleNotes)
                    },
                    onAddCashSaving = { amount, currency, notes, date ->
                        viewModel.addCashSaving(amount, currency, notes, date)
                    },
                    onUpdateCashSaving = { saving -> viewModel.updateCashSaving(saving) },
                    onDeleteCashSaving = { id -> viewModel.deleteCashSaving(id) },
                    onUpdateGoldPrices = { prices -> viewModel.updateAllGoldPrices(prices) }
                )
                5 -> OutingsScreen(
                    outingExpenses = allOutingExpenses,
                    participantsCount = outingParticipantsCount,
                    currency = dashboardState.currency,
                    onAddOutingExpense = { title, amount, payer, receiptPath ->
                        viewModel.addOutingExpense(title, amount, payer, receiptPath)
                    },
                    onDeleteOutingExpense = { id -> viewModel.deleteOutingExpense(id) },
                    onClearAllOutingExpenses = { viewModel.clearAllOutingExpenses() },
                    onParticipantsCountChange = { count ->
                        viewModel.setOutingParticipantsCount(count)
                    }
                )
                6 -> SettingsAndVaultsScreen(
                    userProfile = userProfile,
                    vaults = allVaults,
                    budgetLimits = allBudgetLimits,
                    transactions = allTransactions,
                    commitments = allCommitments,
                    lessons = allChildLessons,
                    currency = dashboardState.currency,
                    selectedLanguage = selectedLanguage,
                    isDarkMode = dashboardState.isDarkMode,
                    isPinEnabled = isPinEnabled,
                    isDailyReminderEnabled = isDailyReminderEnabled,
                    onSelectCurrency = { cur -> viewModel.selectedCurrency.value = cur },
                    onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                    onToggleDarkMode = { dark -> viewModel.isDarkMode.value = dark },
                    onTogglePin = { pin -> viewModel.isPinEnabled.value = pin },
                    onToggleDailyReminder = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.toggleDailyReminder(enabled)
                    },
                    onSendTestNotification = { viewModel.sendTestReminder() },
                    onOpenAddVault = { showAddVaultDialog = true },
                    onEditVault = { vault -> vaultToEdit = vault },
                    onDeleteVault = { id -> viewModel.deleteVault(id) },
                    onOpenSetBudget = { cat, currentLimit ->
                        budgetTargetCategory = cat
                        budgetTargetLimit = currentLimit
                        showSetBudgetDialog = true
                    },
                    onDeleteBudget = { cat -> viewModel.deleteBudgetLimit(cat) },
                    onOpenEditProfile = { showEditProfileDialog = true },
                    onToggleTwoFactor = { enabled -> viewModel.toggleTwoFactor(enabled) },
                    onLogout = { viewModel.logout() },
                    onOpenCloudVault = { showCloudVaultScreen = true }
                )
            }
        }
    }

    // DIALOGS
    if (showEditProfileDialog) {
        EditProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, email, phone, avatarId ->
                viewModel.updateProfile(name, email, phone, avatarId)
            }
        )
    }
    if (showAddIncomeDialog) {
        AddIncomeDialog(
            vaults = vaultNames,
            onDismiss = { showAddIncomeDialog = false },
            onConfirm = { amount, category, description, vaultName, dateMillis ->
                viewModel.addIncome(amount, category, description, vaultName, dateMillis)
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            vaults = vaultNames,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { amount, category, description, vaultName, receiptPath, dateMillis ->
                viewModel.addExpense(amount, category, description, vaultName, receiptPath, dateMillis)
            }
        )
    }

    if (showAddCommitmentDialog) {
        AddCommitmentDialog(
            onDismiss = { showAddCommitmentDialog = false },
            onConfirm = { title, amount, isRecurring, notes, receiptPath ->
                viewModel.addCommitment(
                    title, amount, System.currentTimeMillis(), isRecurring, notes, receiptPath,
                    onSuccess = { showAddCommitmentDialog = false }
                )
            }
        )
    }

    if (showAddLessonDialog) {
        AddChildLessonDialog(
            onDismiss = { showAddLessonDialog = false },
            onConfirm = { childName, subject, teacherName, amount, receiptPath ->
                viewModel.addChildLesson(
                    childName, subject, teacherName, amount, System.currentTimeMillis(), receiptPath,
                    onSuccess = { showAddLessonDialog = false }
                )
            }
        )
    }

    if (showAddVaultDialog) {
        AddVaultDialog(
            onDismiss = { showAddVaultDialog = false },
            onConfirm = { name, initialBalance ->
                viewModel.addVault(name, initialBalance)
            }
        )
    }

    if (showSetBudgetDialog) {
        SetBudgetDialog(
            category = budgetTargetCategory,
            currentLimit = budgetTargetLimit,
            onDismiss = { showSetBudgetDialog = false },
            onConfirm = { cat, limit ->
                viewModel.setBudgetLimit(cat, limit)
            },
            onDelete = { cat ->
                viewModel.deleteBudgetLimit(cat)
            }
        )
    }

    if (vaultToEdit != null) {
        EditVaultDialog(
            vault = vaultToEdit!!,
            onDismiss = { vaultToEdit = null },
            onConfirm = { id, name, balance ->
                viewModel.updateVault(id, name, balance)
            },
            onDelete = { id ->
                viewModel.deleteVault(id)
            }
        )
    }
}
