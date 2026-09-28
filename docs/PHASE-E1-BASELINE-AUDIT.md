# PHASE E.1 — BASELINE AUDIT REPORT
## Financial Integrity & Regression Audit — Smart Vault (الخزنة الذكية)

**Audit Date:** 2026-09-28  
**Auditor Role:** Senior Android Engineer, Financial Software Architect, Security Engineer, QA Lead  
**Scope:** Comprehensive stabilization audit of Phase E additions and baseline financial components.

---

### DEFECT 1: Backup & Restore — Debt Payment Foreign Key Disconnect (Critical)
- **Current Behavior:** Restoring a backup creates new debts with auto-generated auto-increment IDs (`id = 0` in Room entity). Restored `DebtPaymentEntity` items keep the original `debtId` exported from the backup JSON.
- **Root Cause:** In `LocalBackupManager.applyRestore`, `DebtEntity` instances are inserted in bulk, generating fresh IDs. `DebtPaymentEntity` instances are then inserted using the legacy `debtId` from the JSON without mapping old IDs to the new database IDs.
- **Affected Files:** `com.example.data.backup.LocalBackupManager.kt`
- **Data-Loss / Corruption Risk:** Severe. Debt payments become orphaned or attached to unrelated debts belonging to other records, corrupting user debt balances and payment histories.
- **Proposed Correction:** Maintain a mapping table `Map<Int, Int>` (`oldDebtId -> newDebtId`) during debt insertion. Remap `debtId` on every `DebtPaymentEntity` before insertion. Explicitly drop or reject orphaned payments.
- **Regression Test:** `testRestore_preservesDebtPaymentRelationships_withReplacedIds()`

---

### DEFECT 2: Backup & Restore — Data Duplication on Repeated Restore (Critical)
- **Current Behavior:** Calling `applyRestore` repeatedly appends transactions, vaults, debts, savings, and gold assets again, duplicating every single financial record.
- **Root Cause:** `LocalBackupManager.applyRestore` directly performs `insertAll` without clearing or replacing the target user's existing records.
- **Affected Files:** `com.example.data.backup.LocalBackupManager.kt`
- **Data-Loss / Corruption Risk:** High financial data distortion (2x, 3x balances and net worth).
- **Proposed Correction:** Implement an explicit, transactional "REPLACE" policy: inside a single atomic database transaction, clear the target user's existing local financial data and then populate the validated restored records.
- **Regression Test:** `testRepeatedRestore_doesNotDuplicateRecords()`

---

### DEFECT 3: Backup & Restore — Non-Atomic Partial Restoration (Critical)
- **Current Behavior:** If parsing or insertion fails midway through `applyRestore` (e.g., at debts or snapshots), previously inserted transactions and vaults remain committed in the database.
- **Root Cause:** `applyRestore` does not wrap the restoration process inside `db.withTransaction { ... }`.
- **Affected Files:** `com.example.data.backup.LocalBackupManager.kt`
- **Data-Loss / Corruption Risk:** Database left in a corrupt, half-restored inconsistent state.
- **Proposed Correction:** Wrap all deletion and insertion operations inside `db.withTransaction { ... }` so any failure triggers an immediate rollback.
- **Regression Test:** `testRestore_rollsBackCompletelyOnCorruptData()`

---

### DEFECT 4: Backup & Restore — Hardcoded Universal Backup Secret Fallback (Security)
- **Current Behavior:** `LocalBackupManager.createEncryptedBackup` defaults to a hardcoded constant password `"SmartVault_Secure_Local_Key_2026"`.
- **Root Cause:** Optional parameter with universal fallback secret in `createEncryptedBackup` and `validateBackup`.
- **Affected Files:** `com.example.data.backup.LocalBackupManager.kt`
- **Data-Loss / Corruption Risk:** Security vulnerability; any backup generated with default secret could be decrypted by anyone with access to the source code.
- **Proposed Correction:** Require a non-blank user password for all newly created backups. Preserve the default password strictly as a legacy fallback for existing V1 backup validation.
- **Regression Test:** `testBackupPassword_requiresExplicitUserPassword_andValidatesCorrectly()`

---

### DEFECT 5: Multi-User Data Isolation — Unscoped Record Deletion & Updates (Security)
- **Current Behavior:** Several DAO deletion and update operations (`deleteTransactionById`, `deleteVaultById`, `deleteDebtById`, `deleteTransferById`, `deleteOuting`, `deleteGoldAssetById`, `deleteCashSavingById`, `deleteCommitmentById`, `deleteChildLessonById`) only take a record `id` without verifying the owner's `userId`.
- **Root Cause:** DAOs have queries like `DELETE FROM transactions WHERE id = :id` without `AND userId = :userId`.
- **Affected Files:** `com.example.data.dao.AppDaos.kt`, `com.example.data.SmartVaultRepository.kt`, `com.example.ui.SmartVaultViewModel.kt`
- **Data-Loss / Corruption Risk:** Critical privacy and security violation. If User B knows or guesses an ID, they can delete or modify User A's financial transactions or vaults.
- **Proposed Correction:** Add `userId: String` to all mutation queries in DAOs (`WHERE id = :id AND userId = :userId`) and propagate the authenticated/active `userId` from ViewModel and Repository.
- **Regression Test:** `testCrossUserAccess_cannotDeleteOrModifyOtherUserData()`

---

### DEFECT 6: Atomic Financial Operations — Non-Atomic Income & Expense Insertion (Integrity)
- **Current Behavior:** In `SmartVaultRepository.addIncome` and `addExpense`, the transaction record is inserted, followed by a separate vault balance update query.
- **Root Cause:** Operations are executed sequentially outside of `db.withTransaction`. If the app process dies or a crash occurs between the two statements, the transaction is logged but the vault balance is never updated.
- **Affected Files:** `com.example.data.SmartVaultRepository.kt`
- **Data-Loss / Corruption Risk:** Ledger and account balance discrepancy.
- **Proposed Correction:** Wrap both transaction creation and vault balance modification inside `db.withTransaction`. Validate that `amount` is finite, positive, and non-zero.
- **Regression Test:** `testAddTransaction_atomicVaultBalanceUpdate()`

---

### DEFECT 7: Financial Accounting — Silent Negative Balance Coercion (Accounting)
- **Current Behavior:** In `addExpense`, the updated vault balance is computed as `(vault.balance - amount).coerceAtLeast(0.0)`.
- **Root Cause:** Arbitrary truncation to `0.0` disguises overdrafts and corrupts true financial balances.
- **Affected Files:** `com.example.data.SmartVaultRepository.kt`
- **Data-Loss / Corruption Risk:** Destroys balance accuracy; phantom money appears if expenses exceed vault balance.
- **Proposed Correction:** Preserve the true mathematical balance (`vault.balance - amount`) or enforce explicit overdraft validation rules before allowing the expense.
- **Regression Test:** `testExpense_preservesTrueMathematicalBalance()`

---

### DEFECT 8: Transfer Reversal — Missing Balance Reversal on Transfer Deletion (Integrity)
- **Current Behavior:** `SmartVaultRepository.deleteTransfer` only deletes the row from `transfers` table without reversing the vault balance movements.
- **Root Cause:** No balance adjustment logic inside `deleteTransfer`.
- **Affected Files:** `com.example.data.SmartVaultRepository.kt`, `com.example.ui.SmartVaultViewModel.kt`
- **Data-Loss / Corruption Risk:** Deleting a transfer leaves permanent, untraceable balance changes in both vaults.
- **Proposed Correction:** Implement an atomic `reverseTransfer` method that returns funds to the source vault and deducts from the destination vault inside a `@Transaction`, then removes or marks the transfer record.
- **Regression Test:** `testTransferReversal_restoresBothVaultBalancesAtomically()`

---

### DEFECT 9: Debt Accounting — Silently Clamping Overpayments and Non-Atomic Updates (Integrity)
- **Current Behavior:** `recordDebtPayment` uses `paymentAmount.coerceAtMost(debt.remainingAmount)` without notifying the caller if payment exceeds remaining debt.
- **Root Cause:** Permissive clamping instead of strict validation.
- **Affected Files:** `com.example.data.SmartVaultRepository.kt`
- **Data-Loss / Corruption Risk:** Overpayment amounts are silently absorbed or discarded.
- **Proposed Correction:** Validate that `paymentAmount <= debt.remainingAmount`. If payment exceeds remaining, return an explicit `Result.failure(IllegalArgumentException(...))`.
- **Regression Test:** `testDebtPayment_rejectsExceedingAmount()`

---

### DEFECT 10: Net Worth Accounting — Unconditional Treatment of Future Obligations as Debt (Accounting)
- **Current Behavior:** `FinancialSummaryCalculator.calculate` and `getFullNetWorthBreakdown` calculate `totalLiabilities = moneyIOwe + unpaidCommitments + unpaidLessons`.
- **Root Cause:** Conflating scheduled future monthly living expenses (unpaid commitments like internet bill or child lesson due next week) with confirmed financial liabilities (debts/loans owed).
- **Affected Files:** `com.example.data.calculator.FinancialSummaryCalculator.kt`
- **Data-Loss / Corruption Risk:** Distorts Net Worth calculation; treating scheduled upcoming bills as existing debt artificially reduces confirmed net worth.
- **Proposed Correction:** Set `totalLiabilities = moneyIOwe`. Keep `unpaidCommitments` and `unpaidLessons` as separate informational metrics inside `NetWorthBreakdown` without adding them to confirmed debt.
- **Regression Test:** `testNetWorth_doesNotTreatFutureBillsAsExistingDebt()`

---

### DEFECT 11: Financial Calculations — Artificial Clamping of Savings Rate to -100% (Correctness)
- **Current Behavior:** In `FinancialSummaryCalculator.calculateSavingsRate`, `rate.coerceIn(-100.0, 100.0)` is returned.
- **Root Cause:** Artificial constraint prevents reporting actual negative savings rate when spending is 3x or 5x income.
- **Affected Files:** `com.example.data.calculator.FinancialSummaryCalculator.kt`
- **Data-Loss / Corruption Risk:** Incorrect financial metric presented to user.
- **Proposed Correction:** Allow true negative savings rate when `income > 0` (e.g. `(income - expenses) / income * 100`), capping only the upper bound to `100.0` (or `null` when `income <= 0`).
- **Regression Test:** `testSavingsRate_correctlyReflectsDeepDeficitWithoutClamping()`

---

### DEFECT 12: Reactive Financial Summary — Stale Snapshot Values & Missing Flow Triggers (Reactivity)
- **Current Behavior:** In `SmartVaultViewModel.financialSummary`, `goldPrices` is read via one-off `firstOrNull()` inside the `combine` block, and `commitments`/`lessons` are read via `.value`.
- **Root Cause:** Neither `allGoldPrices`, `allCommitments`, nor `allChildLessons` are reactive triggers of the `combine` flow.
- **Affected Files:** `com.example.ui.SmartVaultViewModel.kt`
- **Data-Loss / Corruption Risk:** Live gold price updates or commitment changes do not refresh Net Worth or financial summary in the UI until an unrelated vault or transaction changes.
- **Proposed Correction:** Include `allGoldPrices`, `allCommitments`, and `allChildLessons` in the reactive combine stream.
- **Regression Test:** `testReactiveFinancialSummary_updatesImmediatelyOnGoldPriceChange()`

---

### DEFECT 13: Reports & Charts — Month Selector Does Not Filter Transactions (Correctness)
- **Current Behavior:** In `ReportsAndChartsScreen.kt`, selecting a different month from the dropdown updates `selectedMonthName`, but `currentMonthTxs` only filters transactions against `currentCal` (the system's current month).
- **Root Cause:** `currentMonthTxs` is memoized against `transactions` only and uses `currentCal` instead of parsing `selectedMonthName`. Also, unpaid commitments are hardcoded into category expense shares.
- **Affected Files:** `com.example.ui.screens/ReportsAndChartsScreen.kt`
- **Data-Loss / Corruption Risk:** Misleading reports; browsing historical months displays current-month transactions.
- **Proposed Correction:** Parse the year and month of `selectedMonthName` and filter transactions accordingly. Separate recorded expenses from unpaid future commitments.
- **Regression Test:** `testReportsScreen_monthSelectionFiltersCorrectTransactions()`

---

### DEFECT 14: Net Worth Snapshots — Empty dateKey Overwrites All Historical Days (Integrity)
- **Current Behavior:** `SmartVaultViewModel.takeNetWorthSnapshot()` instantiates `NetWorthSnapshotEntity` without providing `dateKey`.
- **Root Cause:** `dateKey` defaults to `""`. The repository query `getSnapshotByDateKey(userId, "")` matches every snapshot, causing each new snapshot to overwrite the previous one, eliminating historical trend data.
- **Affected Files:** `com.example.ui.SmartVaultViewModel.kt`
- **Data-Loss / Corruption Risk:** High. Complete loss of net worth history; trend charts (7d, 30d, 90d, 1y) only ever contain one row.
- **Proposed Correction:** Populate `dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))` on every snapshot.
- **Regression Test:** `testNetWorthSnapshots_dailyDateKeyPreservesHistoryAcrossDays()`

---

### DEFECT 15: Module Preferences — Active User Switch Does Not Update Preferences (Isolation)
- **Current Behavior:** `SmartVaultViewModel.moduleConfig` is initialized once from `modulePreferencesManager.configurationFlow` without observing `activeUserId`.
- **Root Cause:** When switching from User A to User B, `moduleConfig` continues displaying User A's enabled/disabled modules.
- **Affected Files:** `com.example.ui.SmartVaultViewModel.kt`, `com.example.data.preferences.ModulePreferencesManager.kt`
- **Data-Loss / Corruption Risk:** Configuration leak between users on the same device.
- **Proposed Correction:** Make `moduleConfig` react to `activeUserId.flatMapLatest { uid -> modulePreferencesManager.getConfigurationFlowForUser(uid) }`.
- **Regression Test:** `testModulePreferences_switchesCleanlyWithActiveUser()`

---
