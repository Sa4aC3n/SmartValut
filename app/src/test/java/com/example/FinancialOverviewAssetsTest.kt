package com.example

import com.example.data.calculator.FinancialSummaryCalculator
import com.example.data.entity.DebtEntity
import com.example.data.entity.VaultEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for the product decision that the financial-overview
 * headline represents what the user currently owns, while liabilities remain
 * a separate figure.
 *
 * The accounting net position may be negative internally, but that negative
 * value must never replace the user's total-owned headline.
 */
class FinancialOverviewAssetsTest {

    @Test
    fun liabilitiesLargerThanAssets_keepOwnedAssetsAsPositiveHeadline() {
        val breakdown = FinancialSummaryCalculator.getFullNetWorthBreakdown(
            vaults = listOf(
                VaultEntity(
                    name = "الخزنة الرئيسية",
                    balance = 20_000.0,
                    userId = "user"
                )
            ),
            cashSavings = emptyList(),
            goldAssets = emptyList(),
            goldPriceMap = emptyMap(),
            debts = listOf(
                DebtEntity(
                    personName = "التزام مالي",
                    type = "I_OWE",
                    originalAmount = 90_000.0,
                    remainingAmount = 90_000.0,
                    userId = "user",
                    status = "ACTIVE"
                )
            )
        )

        // This is the figure shown as "إجمالي أموالي وممتلكاتي".
        assertEquals(20_000.0, breakdown.totalAssets, 0.001)
        assertTrue(breakdown.totalAssets >= 0.0)

        // Liabilities remain explicit and separate.
        assertEquals(90_000.0, breakdown.totalLiabilities, 0.001)

        // The accounting position is preserved internally for the detail
        // explanation, but it is not used as the headline balance.
        assertEquals(-70_000.0, breakdown.netWorth, 0.001)
    }

    @Test
    fun headlineAssets_includeBalancesSavingsGoldAndReceivables() {
        val breakdown = FinancialSummaryCalculator.getFullNetWorthBreakdown(
            vaults = listOf(
                VaultEntity(name = "بنك", balance = 10_000.0, userId = "user"),
                VaultEntity(name = "محفظة", balance = 5_000.0, userId = "user")
            ),
            cashSavings = listOf(
                com.example.data.entity.CashSavingEntity(
                    amount = 2_000.0,
                    currency = "EGP",
                    userId = "user"
                )
            ),
            goldAssets = listOf(
                com.example.data.entity.GoldAssetEntity(
                    name = "ذهب",
                    karat = 21,
                    weight = 1.0,
                    purchasePrice = 3_000.0,
                    userId = "user",
                    status = "ACTIVE"
                )
            ),
            goldPriceMap = mapOf(21 to 3_500.0),
            debts = listOf(
                DebtEntity(
                    personName = "أموال لي",
                    type = "OWED_TO_ME",
                    originalAmount = 4_000.0,
                    remainingAmount = 4_000.0,
                    userId = "user",
                    status = "ACTIVE"
                )
            )
        )

        // 15,000 balances + 2,000 savings + 3,500 gold + 4,000 receivable.
        assertEquals(24_500.0, breakdown.totalAssets, 0.001)
        assertEquals(15_000.0, breakdown.vaultsTotal, 0.001)
        assertEquals(2_000.0, breakdown.cashSavingsTotal, 0.001)
        assertEquals(3_500.0, breakdown.goldEstimatedValue, 0.001)
        assertEquals(4_000.0, breakdown.moneyOwedToMe, 0.001)
    }
}
