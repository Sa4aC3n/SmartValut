# Arabic and English localization

## Files

- `app/src/main/res/values/strings.xml`: complete Arabic/default Android string resources.
- `app/src/main/res/values-en/strings.xml`: complete English Android string resources.
- `docs/localization/catalog.json`: bilingual review catalog, including stable resource identifiers.

The `{0}`, `{1}`, etc. tokens are positional arguments. Preserve each token exactly when editing a translation. `AppText` substitutes arguments once and never translates user input.

The saved in-app language controls Compose text, dialogs, validation messages, date labels, reports and worker notifications. Built-in gold types, categories and currency codes are translated for display only; stored values and financial calculations are unchanged. User-authored names, notes and descriptions remain as entered.

## Google Play Console

App strings and store listing text are different:

1. These Android XML files are compiled into the next APK/AAB. Updating the app through the existing AI Studio publishing workflow delivers the translations to testers. Uploading a standalone XML file does not update an installed app.
2. If a Play translation workflow requests Android strings, supply the appropriate `strings.xml`. Both language files are included here for review and translation tools.
3. Store listing translations use the app title, short description and full description. The current listing text has not been supplied, so this change does not invent or publish a store description. Add English listing copy separately under your listing's translation controls.

Official guidance: https://support.google.com/googleplay/android-developer/answer/9844778

## Verification

Run with JDK 21:

```text
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

`LocalizationTest` checks resource coverage, placeholder expansion, live screen recomposition, saved language restoration, changing enum labels, date locale, and preservation of user input/canonical values.

Before release, manually switch Arabic → English → Arabic on every tab and dialog, inspect long text and RTL/LTR layouts, and export a sample report in each language. A native-name Arabic language selector and user-entered Arabic data are intentional exceptions to an English-only interface.

No Play release, signing changes or merge to main is performed by this PR. If version code 9 has already been uploaded, choose a higher unused version code before publishing the next internal test build.
