package com.example.data.insights

import com.example.data.calculator.FinancialSummaryCalculator
import com.example.data.calculator.FinancialSummaryResult
import com.example.data.entity.BudgetLimitEntity
import com.example.data.entity.ChildLessonEntity
import com.example.data.entity.CommitmentEntity
import com.example.data.entity.DebtEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.VaultEntity
import java.util.Calendar

enum class InsightPriority {
    CRITICAL,   // 1. Budget exceeded, urgent overdue
    WARNING,    // 2. Approaching budget limit, debt due soon
    POSITIVE,   // 3. Good savings rate, reduced spending
    INFO        // 4. Daily average, spending distribution
}

data class FinancialInsight(
    val id: String,
    val priority: InsightPriority,
    val title: String,
    val message: String,
    val iconType: String = "info" // "warning", "success", "alert", "trend_down", "trend_up", "info"
)

object FinancialInsightEngine {

    fun generateInsights(
        summary: FinancialSummaryResult,
        budgets: List<BudgetLimitEntity>,
        commitments: List<CommitmentEntity>,
        lessons: List<ChildLessonEntity>,
        currency: String = "ج.م"
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val now = System.currentTimeMillis()
        val threeDaysAhead = now + (3 * 86400000L)

        // 1. Budget Warnings
        for (budget in budgets) {
            val spent = summary.categoryShares.find { it.category == budget.category }?.amount ?: 0.0
            if (budget.monthlyLimit > 0) {
                val ratio = spent / budget.monthlyLimit
                if (ratio >= 1.0) {
                    val overPct = ((ratio - 1.0) * 100).toInt()
                    insights.add(
                        FinancialInsight(
                            id = "budget_over_${budget.category}",
                            priority = InsightPriority.CRITICAL,
                            title = "تجاوز الميزانية",
                            message = "تجاوزت ميزانية «${budget.category}» بنسبة $overPct% (أنفقت ${spent.toInt()} من أصل ${budget.monthlyLimit.toInt()} $currency)",
                            iconType = "alert"
                        )
                    )
                } else if (ratio >= 0.85) {
                    val remaining = (budget.monthlyLimit - spent).toInt()
                    insights.add(
                        FinancialInsight(
                            id = "budget_near_${budget.category}",
                            priority = InsightPriority.WARNING,
                            title = "اقتراب من الحد الشهري",
                            message = "أنت قريب من سقف ميزانية «${budget.category}»، المتبقي لك $remaining $currency فقط.",
                            iconType = "warning"
                        )
                    )
                }
            }
        }

        // 2. Urgent Commitments & Lessons
        val urgentCommitments = commitments.filter { !it.isPaid && it.dueDateMillis in now..threeDaysAhead }
        for (c in urgentCommitments) {
            insights.add(
                FinancialInsight(
                    id = "commitment_due_${c.id}",
                    priority = InsightPriority.WARNING,
                    title = "التزام مستحق قريبًا",
                    message = "لديك التزام «${c.title}» بقيمة ${c.amount.toInt()} $currency خلال أيام قليلة.",
                    iconType = "warning"
                )
            )
        }

        val urgentLessons = lessons.filter { !it.isPaid && it.dueDateMillis in now..threeDaysAhead }
        for (l in urgentLessons) {
            insights.add(
                FinancialInsight(
                    id = "lesson_due_${l.id}",
                    priority = InsightPriority.WARNING,
                    title = "مصروف درس مستحق",
                    message = "درس «${l.subject}» للطفل ${l.childName} بقيمة ${l.amount.toInt()} $currency مستحق قريبًا.",
                    iconType = "warning"
                )
            )
        }

        // 3. Positive Savings Progress
        val savingsRate = summary.savingsRate
        if (savingsRate != null && savingsRate > 0) {
            val rateInt = savingsRate.toInt()
            insights.add(
                FinancialInsight(
                    id = "savings_rate_positive",
                    priority = InsightPriority.POSITIVE,
                    title = "نسبة ادخار ممتازة",
                    message = "وفّرت $rateInt% من دخلك هذا الشهر! أحسنت الاستمرار في إدارة مصاريفك.",
                    iconType = "success"
                )
            )
        }

        // 4. Highest spending category
        val topCategory = summary.categoryShares.maxByOrNull { it.amount }
        if (topCategory != null && topCategory.amount > 0) {
            insights.add(
                FinancialInsight(
                    id = "top_expense_category",
                    priority = InsightPriority.INFO,
                    title = "أعلى بند إنفاق",
                    message = "أعلى بند إنفاق هو «${topCategory.category}» بنسبة ${topCategory.percentage.toInt()}% من إجمالي مصروفاتك.",
                    iconType = "info"
                )
            )
        }

        return insights.sortedBy { it.priority.ordinal }
    }

    fun generateInsights(
        transactions: List<TransactionEntity>,
        vaults: List<VaultEntity>,
        budgetLimits: List<BudgetLimitEntity>,
        commitments: List<CommitmentEntity>,
        lessons: List<ChildLessonEntity>,
        debts: List<DebtEntity>,
        currency: String = "ج.م"
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val now = System.currentTimeMillis()

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        // Previous month range
        cal.add(Calendar.MONTH, -1)
        val prevMonthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val prevMonthEnd = cal.timeInMillis

        // Current month transactions
        val thisMonthTxs = transactions.filter { it.dateMillis >= monthStart }
        val thisMonthIncome = FinancialSummaryCalculator.calculateIncome(thisMonthTxs)
        val thisMonthExpense = FinancialSummaryCalculator.calculateExpenses(thisMonthTxs)

        // Previous month transactions
        val prevMonthTxs = transactions.filter { it.dateMillis in prevMonthStart..prevMonthEnd }
        val prevMonthExpense = FinancialSummaryCalculator.calculateExpenses(prevMonthTxs)

        // 1. Budget Warnings (Priority 1 & 2)
        val expensesByCategory = thisMonthTxs.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        for (budget in budgetLimits) {
            val spent = expensesByCategory[budget.category] ?: 0.0
            if (budget.monthlyLimit > 0) {
                val ratio = spent / budget.monthlyLimit
                if (ratio >= 1.0) {
                    val overPct = ((ratio - 1.0) * 100).toInt()
                    insights.add(
                        FinancialInsight(
                            id = "budget_over_${budget.category}",
                            priority = InsightPriority.CRITICAL,
                            title = "تجاوز الميزانية",
                            message = "تجاوزت ميزانية «${budget.category}» بنسبة $overPct% (أنفقت ${spent.toInt()} من أصل ${budget.monthlyLimit.toInt()} $currency)",
                            iconType = "alert"
                        )
                    )
                } else if (ratio >= 0.85) {
                    val remaining = (budget.monthlyLimit - spent).toInt()
                    insights.add(
                        FinancialInsight(
                            id = "budget_near_${budget.category}",
                            priority = InsightPriority.WARNING,
                            title = "اقتراب من الحد الشهري",
                            message = "أنت قريب من سقف ميزانية «${budget.category}»، المتبقي لك $remaining $currency فقط.",
                            iconType = "warning"
                        )
                    )
                }
            }
        }

        // 2. Upcoming Obligations (Commitments, Lessons, Debts) within next 3 days
        val threeDaysAhead = now + (3 * 86400000L)
        val urgentCommitments = commitments.filter { !it.isPaid && it.dueDateMillis in now..threeDaysAhead }
        for (c in urgentCommitments) {
            insights.add(
                FinancialInsight(
                    id = "commitment_due_${c.id}",
                    priority = InsightPriority.WARNING,
                    title = "التزام مستحق قريبًا",
                    message = "لديك التزام «${c.title}» بقيمة ${c.amount.toInt()} $currency خلال أيام قليلة.",
                    iconType = "warning"
                )
            )
        }

        val urgentLessons = lessons.filter { !it.isPaid && it.dueDateMillis in now..threeDaysAhead }
        for (l in urgentLessons) {
            insights.add(
                FinancialInsight(
                    id = "lesson_due_${l.id}",
                    priority = InsightPriority.WARNING,
                    title = "مصروف درس مستحق",
                    message = "درس «${l.subject}» للطفل ${l.childName} بقيمة ${l.amount.toInt()} $currency مستحق قريبًا.",
                    iconType = "warning"
                )
            )
        }

        val urgentDebts = debts.filter { it.type == "I_OWE" && it.status != "PAID" && it.dueDateMillis != null && it.dueDateMillis in now..threeDaysAhead }
        for (d in urgentDebts) {
            insights.add(
                FinancialInsight(
                    id = "debt_due_${d.id}",
                    priority = InsightPriority.CRITICAL,
                    title = "سداد دين مستحق",
                    message = "يجب سداد مبلغ ${d.remainingAmount.toInt()} $currency للمستحق ${d.personName} خلال 3 أيام.",
                    iconType = "alert"
                )
            )
        }

        // 3. Positive Savings Progress
        val savingsRate = FinancialSummaryCalculator.calculateSavingsRate(thisMonthIncome, thisMonthExpense)
        if (savingsRate != null && savingsRate > 0) {
            val rateInt = savingsRate.toInt()
            insights.add(
                FinancialInsight(
                    id = "savings_rate_positive",
                    priority = InsightPriority.POSITIVE,
                    title = "نسبة ادخار ممتازة",
                    message = "وفّرت $rateInt% من دخلك هذا الشهر! أحسنت الاستمرار في إدارة مصاريفك.",
                    iconType = "success"
                )
            )
        }

        // 4. Spending Trend vs Previous Month
        if (prevMonthExpense > 0 && thisMonthExpense > 0) {
            val comparison = FinancialSummaryCalculator.calculatePeriodComparison(thisMonthExpense, prevMonthExpense)
            if (comparison.percentageChange != null) {
                val pct = kotlin.math.abs(comparison.percentageChange.toInt())
                if (!comparison.hasIncreased && pct >= 5) {
                    insights.add(
                        FinancialInsight(
                            id = "spending_trend_down",
                            priority = InsightPriority.POSITIVE,
                            title = "تحسن في معدل الإنفاق",
                            message = "إنفاقك هذا الشهر أقل من الشهر الماضي بنسبة $pct% حتى الآن.",
                            iconType = "trend_down"
                        )
                    )
                } else if (comparison.hasIncreased && pct >= 15) {
                    insights.add(
                        FinancialInsight(
                            id = "spending_trend_up",
                            priority = InsightPriority.WARNING,
                            title = "ارتفاع وتيرة الإنفاق",
                            message = "أنفقت حتى الآن $pct% أكثر مقارنة بالشهر الماضي.",
                            iconType = "trend_up"
                        )
                    )
                }
            }
        }

        // 5. Highest Spending Category
        val topCategory = expensesByCategory.maxByOrNull { it.value }
        if (topCategory != null && topCategory.value > 0) {
            val pct = ((topCategory.value / thisMonthExpense.coerceAtLeast(1.0)) * 100).toInt()
            insights.add(
                FinancialInsight(
                    id = "top_expense_category",
                    priority = InsightPriority.INFO,
                    title = "أعلى بند إنفاق",
                    message = "أعلى بند إنفاق هذا الشهر هو «${topCategory.key}» ويمثل $pct% من إجمالي مصروفاتك.",
                    iconType = "info"
                )
            )
        }

        // 6. Daily Average Spending
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        if (thisMonthExpense > 0 && dayOfMonth > 0) {
            val dailyAvg = (thisMonthExpense / dayOfMonth).toInt()
            insights.add(
                FinancialInsight(
                    id = "daily_average_expense",
                    priority = InsightPriority.INFO,
                    title = "متوسط الإنفاق اليومي",
                    message = "متوسط إنفاقك اليومي خلال هذا الشهر هو $dailyAvg $currency.",
                    iconType = "info"
                )
            )
        }

        // 7. Check if no transactions recorded today
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayTxs = transactions.filter { it.dateMillis >= startOfToday }
        if (todayTxs.none { it.type == "EXPENSE" }) {
            insights.add(
                FinancialInsight(
                    id = "no_expense_today",
                    priority = InsightPriority.INFO,
                    title = "يوم بلا مصروفات",
                    message = "لم تسجل أي مصروفات اليوم، يوم ممتاز للادخار!",
                    iconType = "success"
                )
            )
        }

        // Sort by priority order: CRITICAL -> WARNING -> POSITIVE -> INFO
        return insights.sortedBy { it.priority.ordinal }
    }
}
