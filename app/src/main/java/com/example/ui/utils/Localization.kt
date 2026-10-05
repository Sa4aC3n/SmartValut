package com.example.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalAppLanguage = compositionLocalOf { "ar" }

class AppStrings(val language: String) {
    val isEn = language == "en"

    // App & Navigation
    val appTitle get() = AppText.textFor(language, com.example.R.string.label_app_title)
    val navHome get() = AppText.textFor(language, com.example.R.string.label_nav_home)
    val navSavings get() = AppText.textFor(language, com.example.R.string.label_nav_savings)
    val navTransactions get() = AppText.textFor(language, com.example.R.string.label_nav_transactions)
    val navCommitments get() = AppText.textFor(language, com.example.R.string.label_nav_commitments)
    val navOutings get() = AppText.textFor(language, com.example.R.string.label_nav_outings)
    val navReports get() = AppText.textFor(language, com.example.R.string.label_nav_reports)
    val navSettings get() = AppText.textFor(language, com.example.R.string.label_nav_settings)

    // Header Screen Titles
    val titleHome get() = AppText.textFor(language, com.example.R.string.label_title_home)
    val titleSavings get() = AppText.textFor(language, com.example.R.string.label_title_savings)
    val titleCommitments get() = AppText.textFor(language, com.example.R.string.label_title_commitments)
    val titleOutings get() = AppText.textFor(language, com.example.R.string.label_title_outings)
    val titleTransactions get() = AppText.textFor(language, com.example.R.string.label_title_transactions)
    val titleReports get() = AppText.textFor(language, com.example.R.string.label_title_reports)
    val titleSettings get() = AppText.textFor(language, com.example.R.string.label_title_settings)

    // Savings & Gold Module
    val totalSavings get() = AppText.textFor(language, com.example.R.string.label_total_savings)
    val totalCashSavings get() = AppText.textFor(language, com.example.R.string.label_total_cash_savings)
    val currentGoldValue get() = AppText.textFor(language, com.example.R.string.label_current_gold_value)
    val totalGoldWeight get() = AppText.textFor(language, com.example.R.string.label_total_gold_weight)
    val goldPiecesCount get() = AppText.textFor(language, com.example.R.string.label_gold_pieces_count)
    val goldProfits get() = AppText.textFor(language, com.example.R.string.label_gold_profits)
    val updateGoldPrices get() = AppText.textFor(language, com.example.R.string.label_update_gold_prices)
    val goldPricesToday get() = AppText.textFor(language, com.example.R.string.label_gold_prices_today)
    val tabGold get() = AppText.textFor(language, com.example.R.string.label_tab_gold)
    val tabCash get() = AppText.textFor(language, com.example.R.string.label_tab_cash)
    val tabAnalytics get() = AppText.textFor(language, com.example.R.string.label_tab_analytics)
    val tabSoldHistory get() = AppText.textFor(language, com.example.R.string.label_tab_sold_history)
    val addGoldPiece get() = AppText.textFor(language, com.example.R.string.label_add_gold_piece)
    val addCashSaving get() = AppText.textFor(language, com.example.R.string.label_add_cash_saving)
    val goldPieceName get() = AppText.textFor(language, com.example.R.string.label_gold_piece_name)
    val goldType get() = AppText.textFor(language, com.example.R.string.label_gold_type)
    val bullion get() = AppText.textFor(language, com.example.R.string.label_bullion)
    val jewelry get() = AppText.textFor(language, com.example.R.string.label_jewelry)
    val goldCoin get() = AppText.textFor(language, com.example.R.string.label_gold_coin)
    val goldPound get() = AppText.textFor(language, com.example.R.string.label_gold_pound)
    val otherGold get() = AppText.textFor(language, com.example.R.string.label_other_gold)
    val karat get() = AppText.textFor(language, com.example.R.string.label_karat)
    val karat24 get() = AppText.textFor(language, com.example.R.string.label_karat24)
    val karat22 get() = AppText.textFor(language, com.example.R.string.label_karat22)
    val karat21 get() = AppText.textFor(language, com.example.R.string.label_karat21)
    val karat18 get() = AppText.textFor(language, com.example.R.string.label_karat18)
    val karat14 get() = AppText.textFor(language, com.example.R.string.label_karat14)
    val karat12 get() = AppText.textFor(language, com.example.R.string.label_karat12)
    val weightInGrams get() = AppText.textFor(language, com.example.R.string.label_weight_in_grams)
    val purchasePrice get() = AppText.textFor(language, com.example.R.string.label_purchase_price)
    val purchaseDate get() = AppText.textFor(language, com.example.R.string.label_purchase_date)
    val purpose get() = AppText.textFor(language, com.example.R.string.label_purpose)
    val purposeSaving get() = AppText.textFor(language, com.example.R.string.label_purpose_saving)
    val purposeAdornment get() = AppText.textFor(language, com.example.R.string.label_purpose_adornment)
    val piecePhoto get() = AppText.textFor(language, com.example.R.string.label_piece_photo)
    val sellGold get() = AppText.textFor(language, com.example.R.string.label_sell_gold)
    val salePrice get() = AppText.textFor(language, com.example.R.string.label_sale_price)
    val saleDate get() = AppText.textFor(language, com.example.R.string.label_sale_date)
    val saleNotes get() = AppText.textFor(language, com.example.R.string.label_sale_notes)
    val realizedProfit get() = AppText.textFor(language, com.example.R.string.label_realized_profit)
    val profitPercentage get() = AppText.textFor(language, com.example.R.string.label_profit_percentage)
    val lossPercentage get() = AppText.textFor(language, com.example.R.string.label_loss_percentage)
    val inVault get() = AppText.textFor(language, com.example.R.string.label_in_vault)
    val sold get() = AppText.textFor(language, com.example.R.string.label_sold)
    val pieceDetails get() = AppText.textFor(language, com.example.R.string.label_piece_details)
    val currentGramPrice get() = AppText.textFor(language, com.example.R.string.label_current_gram_price)
    val noGoldYet get() = AppText.textFor(language, com.example.R.string.label_no_gold_yet)
    val noCashSavingsYet get() = AppText.textFor(language, com.example.R.string.label_no_cash_savings_yet)
    val noSoldGoldYet get() = AppText.textFor(language, com.example.R.string.label_no_sold_gold_yet)
    val assetAllocation get() = AppText.textFor(language, com.example.R.string.label_asset_allocation)
    val savingsAdvise get() = AppText.textFor(language, com.example.R.string.label_savings_advise)

    // Dashboard
    val welcomeUser get() = AppText.textFor(language, com.example.R.string.label_welcome_user)
    val currentBalance get() = AppText.textFor(language, com.example.R.string.label_current_balance)
    val monthlyIncome get() = AppText.textFor(language, com.example.R.string.label_monthly_income)
    val monthlyExpenses get() = AppText.textFor(language, com.example.R.string.label_monthly_expenses)
    val smartAnalytics get() = AppText.textFor(language, com.example.R.string.label_smart_analytics)
    val totalAvailableBalance get() = AppText.textFor(language, com.example.R.string.label_total_available_balance)
    val thisMonthIncome get() = AppText.textFor(language, com.example.R.string.label_this_month_income)
    val thisMonthExpense get() = AppText.textFor(language, com.example.R.string.label_this_month_expense)
    val addIncome get() = AppText.textFor(language, com.example.R.string.label_add_income)
    val addExpense get() = AppText.textFor(language, com.example.R.string.label_add_expense)
    val financialVaults get() = AppText.textFor(language, com.example.R.string.label_financial_vaults)
    val recentTransactions get() = AppText.textFor(language, com.example.R.string.label_recent_transactions)
    val viewAll get() = AppText.textFor(language, com.example.R.string.label_view_all)
    val noTransactionsYet get() = AppText.textFor(language, com.example.R.string.label_no_transactions_yet)
    val monthlyCategoryBudget get() = AppText.textFor(language, com.example.R.string.label_monthly_category_budget)
    val addBudgetLimit get() = AppText.textFor(language, com.example.R.string.label_add_budget_limit)
    val setBudget get() = AppText.textFor(language, com.example.R.string.label_set_budget)
    val noBudgetSet get() = AppText.textFor(language, com.example.R.string.label_no_budget_set)
    val upcomingCommitments get() = AppText.textFor(language, com.example.R.string.label_upcoming_commitments)
    val childLessons get() = AppText.textFor(language, com.example.R.string.label_child_lessons)
    val paid get() = AppText.textFor(language, com.example.R.string.label_paid)
    val unpaid get() = AppText.textFor(language, com.example.R.string.label_unpaid)
    val intelligentAdvisor get() = AppText.textFor(language, com.example.R.string.label_intelligent_advisor)
    val achievementsAndBadges get() = AppText.textFor(language, com.example.R.string.label_achievements_and_badges)

    // Transactions / Expenses Screen
    val filterAll get() = AppText.textFor(language, com.example.R.string.label_filter_all)
    val filterExpenses get() = AppText.textFor(language, com.example.R.string.label_filter_expenses)
    val filterIncome get() = AppText.textFor(language, com.example.R.string.label_filter_income)
    val searchPlaceholder get() = AppText.textFor(language, com.example.R.string.label_search_placeholder)
    val noMatchingTransactions get() = AppText.textFor(language, com.example.R.string.label_no_matching_transactions)
    val transactionDetails get() = AppText.textFor(language, com.example.R.string.label_transaction_details)
    val deleteTransaction get() = AppText.textFor(language, com.example.R.string.label_delete_transaction)
    val amount get() = AppText.textFor(language, com.example.R.string.label_amount)
    val date get() = AppText.textFor(language, com.example.R.string.label_date)
    val category get() = AppText.textFor(language, com.example.R.string.label_category)
    val vault get() = AppText.textFor(language, com.example.R.string.label_vault)
    val notes get() = AppText.textFor(language, com.example.R.string.label_notes)

    // Commitments & Lessons Screen
    val name get() = AppText.textFor(language, com.example.R.string.label_name)
    val dueDay get() = AppText.textFor(language, com.example.R.string.label_due_day)
    val repeatMonthly get() = AppText.textFor(language, com.example.R.string.label_repeat_monthly)
    val commitmentExample get() = AppText.textFor(language, com.example.R.string.label_commitment_example)
    val commitmentsTab get() = AppText.textFor(language, com.example.R.string.label_commitments_tab)
    val lessonsTab get() = AppText.textFor(language, com.example.R.string.label_lessons_tab)
    val monthlyCommitments get() = AppText.textFor(language, com.example.R.string.label_monthly_commitments)
    val addCommitment get() = AppText.textFor(language, com.example.R.string.label_add_commitment)
    val addLesson get() = AppText.textFor(language, com.example.R.string.label_add_lesson)
    val teacherName get() = AppText.textFor(language, com.example.R.string.label_teacher_name)
    val childName get() = AppText.textFor(language, com.example.R.string.label_child_name)
    val dueDate get() = AppText.textFor(language, com.example.R.string.label_due_date)
    val payFromVault get() = AppText.textFor(language, com.example.R.string.label_pay_from_vault)
    val noCommitments get() = AppText.textFor(language, com.example.R.string.label_no_commitments)
    val noLessons get() = AppText.textFor(language, com.example.R.string.label_no_lessons)

    // Reports & Analytics Screen
    val currentMonthSummary get() = AppText.textFor(language, com.example.R.string.label_current_month_summary)
    val remainingBalance get() = AppText.textFor(language, com.example.R.string.label_remaining_balance)
    val avgDailySpend get() = AppText.textFor(language, com.example.R.string.label_avg_daily_spend)
    val safeDailyLimit get() = AppText.textFor(language, com.example.R.string.label_safe_daily_limit)
    val expenseDistribution get() = AppText.textFor(language, com.example.R.string.label_expense_distribution)
    val exportPdf get() = AppText.textFor(language, com.example.R.string.label_export_pdf)
    val exportExcel get() = AppText.textFor(language, com.example.R.string.label_export_excel)
    val topExpenseCategory get() = AppText.textFor(language, com.example.R.string.label_top_expense_category)
    val incomeVsExpense get() = AppText.textFor(language, com.example.R.string.label_income_vs_expense)

    // Settings Screen
    val appPreferences get() = AppText.textFor(language, com.example.R.string.label_app_preferences)
    val appLanguage get() = AppText.textFor(language, com.example.R.string.label_app_language)
    val arabic get() = AppText.textFor(language, com.example.R.string.label_arabic)
    val english get() = AppText.textFor(language, com.example.R.string.label_english)
    val appCurrency get() = AppText.textFor(language, com.example.R.string.label_app_currency)
    val darkMode get() = AppText.textFor(language, com.example.R.string.label_dark_mode)
    val fingerprintPin get() = AppText.textFor(language, com.example.R.string.label_fingerprint_pin)
    val twoFactorAuth get() = AppText.textFor(language, com.example.R.string.label_two_factor_auth)
    val enabled2FA get() = AppText.textFor(language, com.example.R.string.label_enabled2_f_a)
    val disabled2FA get() = AppText.textFor(language, com.example.R.string.label_disabled2_f_a)
    val dailyReminder get() = AppText.textFor(language, com.example.R.string.label_daily_reminder)
    val reminderSub get() = AppText.textFor(language, com.example.R.string.label_reminder_sub)
    val backupRestore get() = AppText.textFor(language, com.example.R.string.label_backup_restore)
    val driveBackup get() = AppText.textFor(language, com.example.R.string.label_drive_backup)
    val restoreBackup get() = AppText.textFor(language, com.example.R.string.label_restore_backup)
    val editProfile get() = AppText.textFor(language, com.example.R.string.label_edit_profile)
    val logout get() = AppText.textFor(language, com.example.R.string.label_logout)
    val addAccountVault get() = AppText.textFor(language, com.example.R.string.label_add_account_vault)

    val attachReceipt get() = AppText.textFor(language, com.example.R.string.label_attach_receipt)
    val receiptImage get() = AppText.textFor(language, com.example.R.string.label_receipt_image)
    val optional get() = AppText.textFor(language, com.example.R.string.label_optional)

    // Common Buttons & Actions
    val save get() = AppText.textFor(language, com.example.R.string.label_save)
    val cancel get() = AppText.textFor(language, com.example.R.string.label_cancel)
    val delete get() = AppText.textFor(language, com.example.R.string.label_delete)
    val edit get() = AppText.textFor(language, com.example.R.string.label_edit)
    val confirm get() = AppText.textFor(language, com.example.R.string.label_confirm)
    val close get() = AppText.textFor(language, com.example.R.string.label_close)

    // Helper to translate default categories if needed
    fun translateCategory(cat: String): String {
        if (!isEn) return cat
        return when (cat) {
            "خضروات" -> "Vegetables"
            "فاكهة" -> "Fruits"
            "طلبات منزل" -> "Groceries"
            "سوبر ماركت" -> "Supermarket"
            "مطاعم" -> "Restaurants"
            "مواصلات" -> "Transport"
            "بنزين" -> "Fuel"
            "ملابس" -> "Clothing"
            "صيدلية" -> "Pharmacy"
            "أدوية" -> "Medicines"
            "تعليم" -> "Education"
            "دروس أطفال" -> "Children Lessons"
            "إيجار المنزل" -> "House Rent"
            "إيجار" -> "Rent"
            "فاتورة الكهرباء" -> "Electricity Bill"
            "كهرباء" -> "Electricity"
            "فاتورة المياه" -> "Water Bill"
            "فاتورة الغاز" -> "Gas Bill"
            "فاتورة الإنترنت" -> "Internet Bill"
            "فاتورة الهاتف" -> "Phone Bill"
            "ترفيه" -> "Entertainment"
            "هدايا" -> "Gifts"
            "صيانة" -> "Maintenance"
            "إصلاحات" -> "Repairs"
            "قسط سيارة" -> "Car Installment"
            "أخرى" -> "Other"
            "الراتب" -> "Salary"
            "مكافأة" -> "Bonus"
            "أرباح" -> "Profits"
            "تحويل مالي" -> "Transfer"
            "الالتزامات" -> "Commitments"
            "الخزنة الرئيسية" -> "Main Vault"
            else -> cat
        }
    }

    // GoldAPI.io Live Updates
    val liveGoldPriceUpdate get() = AppText.textFor(language, com.example.R.string.label_live_gold_price_update)
    val fetchLiveGoldPrices get() = AppText.textFor(language, com.example.R.string.label_fetch_live_gold_prices)
    val goldApiKeyLabel get() = AppText.textFor(language, com.example.R.string.label_gold_api_key_label)
    val goldApiKeyHint get() = AppText.textFor(language, com.example.R.string.label_gold_api_key_hint)
    val goldApiKeyExplanation get() = AppText.textFor(language, com.example.R.string.label_gold_api_key_explanation)
    val updatingPrices get() = AppText.textFor(language, com.example.R.string.label_updating_prices)
    val pricesUpdatedSuccess get() = AppText.textFor(language, com.example.R.string.label_prices_updated_success)
    val lastUpdated get() = AppText.textFor(language, com.example.R.string.label_last_updated)
}

val LocalStrings = compositionLocalOf { AppStrings("ar") }
