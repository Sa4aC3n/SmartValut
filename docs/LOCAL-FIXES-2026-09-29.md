# Local review fixes — 2026-09-29

## Behavior

- Deleting a transaction reverses its balance impact in the same Room transaction. Repeated deletion is a no-op. Missing vaults cause a reported error and preserve the transaction.
- Settings and the vault backup action now open the same password-protected file export/restore dialog. Restoring requires an explicit replacement confirmation and is scoped to the account that opened the dialog.
- New backups use the V3 envelope. Secret contents are decrypted in memory, protected by the backup password, and encrypted with the destination device's Keystore key on restore. V1/V2 financial backups remain readable. Old device-bound secret backups require their original key; otherwise restoration rolls back.
- Export reads a consistent database snapshot. Restore remaps outing IDs and their expense references, and gives restored secret entries fresh IDs. Other users' records are retained.
- Guest replacement includes legacy records with an empty user ID.
- SAFE_MERGE is deliberately rejected before writes. Previous backups lack stable identities across every entity, and the old merge silently duplicated assets and produced inconsistent balances. Only explicit full replacement is exposed in the UI.
- Media attachments are not bundled. The dialog states this before export and restore.
- Debug builds use Android's generated local signing key instead of requiring a repository-root debug.keystore.

## Verification

`BackupAndLedgerRegressionTest` covers balance reversal, repeat deletion, ownership, missing-vault rollback, recovery with a new encryption key, cross-account relationship remapping, repeated guest replacement, rejected merging, and legacy-key failure rollback. Existing backup tests now expect V3 and verify rejection of unsafe merging.

The GitHub Actions workflow runs `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, publishes reports, and uploads a debug APK on success. A workflow definition is not evidence of a passing run; use its actual result.

## Local build requirements

AGP 9.1.1 requires JDK 17 and Gradle 9.3.1. Install Android SDK platform 36.1 and build tools 36.0.0. Reference: https://developer.android.com/build/releases/agp-9-1-0-release-notes

The missing Gradle Wrapper has now been restored from the official `gradle/gradle` v9.3.1 source. It is configured to download Gradle 9.3.1. Run `bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` (Windows: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`). Use JDK 17 rather than Java 8. Keep your local `app/google-services.json`; it is excluded from Git and this change set.

Wrapper source: https://github.com/gradle/gradle/tree/v9.3.1/gradle/wrapper. Expected Git blob SHA for the downloaded wrapper JAR: `61285a659d17295f1de7c53e24fdf13ad755c379`.

The continuation on 2026-09-30 also removes repeated identical imports in AvatarView, Dialogs, and SettingsAndVaultsScreen.

Before deploying, manually test Android's document picker, canceled saves, wrong passwords, and a backup transfer between two installations. Changes do not repair historical balance drift caused by transactions that were already deleted.
