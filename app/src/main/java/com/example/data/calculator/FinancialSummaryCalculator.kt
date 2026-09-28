package com.example.data.calculator

import com.example.data.entity.CashSavingEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.DebtEntity
import com.example.data.entity.GoldAssetEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity

data class CategoryExpenseShare(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val count: Int
)

data class PeriodComparisonResult(
    val currentValue: Double,
    val previousValue: Double,
    val percentageChange: Double?, // null if previousValue == 0
    val hasIncreased: Boolean,
    val difference: Double
)

data class NetWorthBreakdown(
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netWorth: Double = 0.0,
    val vaultsTotal: Double = 0.0,
    val cashSavingsTotal: Double = 0.0,
    val goldEstimatedValue: Double = 0.0,
    val moneyOwedToMe: Double = 0.0,
    val moneyIOwe: Double = 0.0,
    val assetsPercentage: Double = 100.0,
    val liabilitiesPercentage: Double = 0.0,
    val unpaidCommitments: Double = 0.0,
    val unpaidLessons: Double = 0.0
)

data class FinancialSummaryResult(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val savingsRate: Double? = null,
    val netWorthBreakdown: NetWorthBreakdown = NetWorthBreakdown(),
    val categoryShares: List<CategoryExpenseShare> = emptyList()
)

object FinancialSummaryCalculator {

    fun calculate(
        vaults: List<VaultEntity>,
        transactions: List<TransactionEntity>,
        cashSavings: List<CashSavingEntity>,
        goldAssets: List<GoldAssetEntity>,
        debts: List<DebtEntity>,
        goldPriceMap: Map<Int, Double>,
        commitments: List<CommitmentEntity> = emptyList(),
        lessons: List<ChildLessonEntity> = emptyList()
    ): FinancialSummaryResult {
        val income = calculateIncome(transactions)
        val expenses = calculateExpenses(transactions)
        val netCashFlow = calculateNetCashFlow(income, expenses)
        val savingsRate = calculateSavingsRate(income, expenses)
        val unpaidCommitmentsSum = commitments.filter { !it.isPaid }.sumOf { it.amount }
        val unpaidLessonsSum = lessons.filter { !it.isPaid }.sumOf { it.amount }

        val netWorth = getFullNetWorthBreakdown(
            vaults = vaults,
            cashSavings = cashSavings,
            goldAssets = goldAssets,
            goldPriceMap = goldPriceMap,
            debts = debts,
            unpaidCommitments = unpaidCommitmentsSum,
            unpaidLessons = unpaidLessonsSum
        )

        val categoryShares = calculateCategoryBreakdown(transactions)

        return FinancialSummaryResult(
            totalIncome = income,
            totalExpenses = expenses,
            netCashFlow = netCashFlow,
            savingsRate = savingsRate,
            netWorthBreakdown = netWorth,
            categoryShares = categoryShares
        )
    }

    fun calculateIncome(
        transactions: List<TransactionEntity>,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE
    ): Double {
        return transactions
            .filter { it.type == "INCOME" && it.dateMillis in startMillis..endMillis }
            .sumOf { it.amount }
    }

    fun calculateExpenses(
        transactions: List<TransactionEntity>,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE
    ): Double {
        return transactions
            .filter { it.type == "EXPENSE" && it.dateMillis in startMillis..endMillis }
            .sumOf { it.amount }
    }

    fun calculateNetCashFlow(income: Double, expenses: Double): Double {
        return income - expenses
    }

    /**
     * Formula: (Income - Expenses) / Income * 100
     * If Income <= 0, returns null to avoid dividing by zero.
     */
    fun calculateSavingsRate(income: Double, expenses: Double): Double? {
        if (income <= 0.0) return null
        val rate = ((income - expenses) / income) * 100.0
        return rate.coerceIn(-100.0, 100.0)
    }

    fun calculateAssets(
        vaults: List<VaultEntity>,
        cashSavings: List<CashSavingEntity>,
        goldAssets: List<GoldAssetEntity>,
        goldPriceMap: Map<Int, Double>,
        debtsOwedToMe: List<DebtEntity>
    ): Double {
        val vaultsSum = vaults.sumOf { it.balance.coerceAtLeast(0.0) }
        val cashSavingsSum = cashSavings.sumOf { it.amount.coerceAtLeast(0.0) }

        val activeGold = goldAssets.filter { it.status != "SOLD" }
        val goldSum = activeGold.sumOf { asset ->
            val livePrice = goldPriceMap[asset.karat] ?: 0.0
            if (livePrice > 0.0) {
                asset.weight * livePrice
            } else {
                asset.purchasePrice
            }
        }

        val owedToMeSum = debtsOwedToMe
            .filter { it.type == "OWED_TO_ME" && it.status != "PAID" }
            .sumOf { it.remainingAmount.coerceAtLeast(0.0) }

        return vaultsSum + cashSavingsSum + goldSum + owedToMeSum
    }

    fun calculateLiabilities(debtsIOwe: List<DebtEntity>): Double {
        return debtsIOwe
            .filter { it.type == "I_OWE" && it.status != "PAID" }
            .sumOf { it.remainingAmount.coerceAtLeast(0.0) }
    }

    fun calculateNetWorth(assets: Double, liabilities: Double): Double {
        return assets - liabilities
    }

    fun getFullNetWorthBreakdown(
        vaults: List<VaultEntity>,
        cashSavings: List<CashSavingEntity>,
        goldAssets: List<GoldAssetEntity>,
        goldPriceMap: Map<Int, Double>,
        debts: List<DebtEntity>,
        unpaidCommitments: Double = 0.0,
        unpaidLessons: Double = 0.0
    ): NetWorthBreakdown {
        val vaultsTotal = vaults.sumOf { it.balance.coerceAtLeast(0.0) }
        val cashSavingsTotal = cashSavings.sumOf { it.amount.coerceAtLeast(0.0) }

        val activeGold = goldAssets.filter { it.status != "SOLD" }
        val goldValue = activeGold.sumOf { asset ->
            val livePrice = goldPriceMap[asset.karat] ?: 0.0
            if (livePrice > 0.0) {
                asset.weight * livePrice
            } else {
                asset.purchasePrice
            }
        }

        val moneyOwedToMe = debts
            .filter { it.type == "OWED_TO_ME" && it.status != "PAID" }
            .sumOf { it.remainingAmount.coerceAtLeast(0.0) }

        val moneyIOwe = debts
            .filter { it.type == "I_OWE" && it.status != "PAID" }
            .sumOf { it.remainingAmount.coerceAtLeast(0.0) }

        val totalAssets = vaultsTotal + cashSavingsTotal + goldValue + moneyOwedToMe
        val totalLiabilities = moneyIOwe + unpaidCommitments + unpaidLessons
        val netWorth = totalAssets - totalLiabilities

        val totalVolume = totalAssets + totalLiabilities
        val (assetsPct, liabilitiesPct) = if (totalVolume > 0.0) {
            val aPct = (totalAssets / totalVolume) * 100.0
            val lPct = (totalLiabilities / totalVolume) * 100.0
            Pair(aPct, lPct)
        } else {
            Pair(100.0, 0.0)
        }

        return NetWorthBreakdown(
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = netWorth,
            vaultsTotal = vaultsTotal,
            cashSavingsTotal = cashSavingsTotal,
            goldEstimatedValue = goldValue,
            moneyOwedToMe = moneyOwedToMe,
            moneyIOwe = moneyIOwe,
            assetsPercentage = assetsPct,
            liabilitiesPercentage = liabilitiesPct,
            unpaidCommitments = unpaidCommitments,
            unpaidLessons = unpaidLessons
        )
    }

    fun calculateDailyAverage(expenses: Double, daysCount: Int): Double {
        if (daysCount <= 0) return 0.0
        return expenses / daysCount
    }

    fun calculateCategoryBreakdown(
        transactions: List<TransactionEntity>,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE
    ): List<CategoryExpenseShare> {
        val periodExpenses = transactions.filter {
            it.type == "EXPENSE" && it.dateMillis in startMillis..endMillis
        }
        val totalExpense = periodExpenses.sumOf { it.amount }
        if (totalExpense <= 0.0) return emptyList()

        return periodExpenses
            .groupBy { it.category }
            .map { (category, txList) ->
                val amount = txList.sumOf { it.amount }
                val pct = (amount / totalExpense) * 100.0
                CategoryExpenseShare(
                    category = category,
                    amount = amount,
                    percentage = pct,
                    count = txList.size
                )
            }
            .sortedByDescending { it.amount }
    }

    fun calculatePeriodComparison(currentVal: Double, previousVal: Double): PeriodComparisonResult {
        val diff = currentVal - previousVal
        val pct = if (previousVal > 0.0) {
            (diff / previousVal) * 100.0
        } else {
            null
        }
        return PeriodComparisonResult(
            currentValue = currentVal,
            previousValue = previousVal,
            percentageChange = pct,
            hasIncreased = diff > 0.0,
            difference = kotlin.math.abs(diff)
        )
    }
}
