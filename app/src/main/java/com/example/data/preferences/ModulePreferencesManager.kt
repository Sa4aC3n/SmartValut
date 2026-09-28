package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ModuleConfiguration(
    val incomeExpense: Boolean = true, // Always true
    val netWorth: Boolean = true,
    val debts: Boolean = true,
    val calendar: Boolean = true,
    val savings: Boolean = true,
    val gold: Boolean = true,
    val commitments: Boolean = true,
    val lessons: Boolean = true,
    val outings: Boolean = true,
    val budgets: Boolean = true
)

class ModulePreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_vault_modules_prefs", Context.MODE_PRIVATE)

    private val _configurationFlow = MutableStateFlow(loadConfiguration(""))
    val configurationFlow: StateFlow<ModuleConfiguration> = _configurationFlow.asStateFlow()

    private val userConfigFlows = mutableMapOf<String, MutableStateFlow<ModuleConfiguration>>()

    fun getConfigurationFlowForUser(userId: String = ""): StateFlow<ModuleConfiguration> {
        val key = userId.ifBlank { "" }
        val flow = userConfigFlows.getOrPut(key) {
            MutableStateFlow(loadConfiguration(key))
        }
        flow.value = loadConfiguration(key)
        return flow.asStateFlow()
    }

    private fun userKey(userId: String, key: String): String =
        if (userId.isBlank()) "guest_$key" else "${userId}_$key"

    private fun loadConfiguration(userId: String = ""): ModuleConfiguration {
        return ModuleConfiguration(
            incomeExpense = true,
            netWorth = prefs.getBoolean(userKey(userId, "mod_net_worth"), true),
            debts = prefs.getBoolean(userKey(userId, "mod_debts"), true),
            calendar = prefs.getBoolean(userKey(userId, "mod_calendar"), true),
            savings = prefs.getBoolean(userKey(userId, "mod_savings"), true),
            gold = prefs.getBoolean(userKey(userId, "mod_gold"), true),
            commitments = prefs.getBoolean(userKey(userId, "mod_commitments"), true),
            lessons = prefs.getBoolean(userKey(userId, "mod_lessons"), true),
            outings = prefs.getBoolean(userKey(userId, "mod_outings"), true),
            budgets = prefs.getBoolean(userKey(userId, "mod_budgets"), true)
        )
    }

    fun getConfiguration(userId: String = ""): ModuleConfiguration {
        val config = loadConfiguration(userId)
        _configurationFlow.value = config
        userConfigFlows[userId.ifBlank { "" }]?.value = config
        return config
    }

    fun updateModule(userId: String = "", moduleKey: String, enabled: Boolean) {
        prefs.edit().putBoolean(userKey(userId, "mod_$moduleKey"), enabled).apply()
        val config = getConfiguration(userId)
        userConfigFlows[userId.ifBlank { "" }]?.value = config
    }

    fun saveFullConfiguration(userId: String = "", config: ModuleConfiguration) {
        prefs.edit()
            .putBoolean(userKey(userId, "mod_net_worth"), config.netWorth)
            .putBoolean(userKey(userId, "mod_debts"), config.debts)
            .putBoolean(userKey(userId, "mod_calendar"), config.calendar)
            .putBoolean(userKey(userId, "mod_savings"), config.savings)
            .putBoolean(userKey(userId, "mod_gold"), config.gold)
            .putBoolean(userKey(userId, "mod_commitments"), config.commitments)
            .putBoolean(userKey(userId, "mod_lessons"), config.lessons)
            .putBoolean(userKey(userId, "mod_outings"), config.outings)
            .putBoolean(userKey(userId, "mod_budgets"), config.budgets)
            .apply()
        _configurationFlow.value = config
        userConfigFlows[userId.ifBlank { "" }]?.value = config
    }

    fun saveConfiguration(config: ModuleConfiguration, userId: String = "") {
        saveFullConfiguration(userId, config)
    }
}
