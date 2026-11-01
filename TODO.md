# CalTrack Roadmap

Keep this roadmap current as features are implemented. Do not mark work complete until it has actually been delivered.

## Completed features

- [x] Native Android application using Kotlin and Jetpack Compose.
- [x] Material 3 Expressive design foundation.
- [x] Dark theme.
- [x] Light theme.
- [x] System theme selection.
- [x] Dynamic wallpaper color support.
- [x] Local Room database.
- [x] Daily food journal.
- [x] Manual food entry.
- [x] Food editing and deletion with undo.
- [x] Remembering previously used foods.
- [x] Floating pill bottom navigation.
- [x] Initial animations.
- [x] Statistics dashboard with 7-, 30-, and 90-day periods.
- [x] Daily calorie averages and history.
- [x] Macronutrient statistics.
- [x] Statistics calculation tests.
- [x] Menu crash fix.
- [x] Frequent Foods section in the Add Food flow.
- [x] Search shortcut in the Add Food flow.
- [x] Local food search with case- and diacritic-insensitive matching.
- [x] Barcode scanning, Open Food Facts lookup, and reviewed product saving through Meals and Today.
- [x] Food Library workflows from Meals: local search, manual saved-food creation, product review, and quick-add.
- [x] One shared Add Food editor for Today and Meals with per-100 g/ml reference values and automatic quantity-based totals.
- [x] AI Food Assistant with Gemini BYOK, Keystore-encrypted key storage, first-use privacy consent, grounded source cards, and confirmed journal entry creation.
- [x] AI Food Assistant as a floating chat window opened from a dedicated button above + on Azi, with per-answer food cards, retry, and an explicit no-search fallback labelled as estimates.

## Priority 1 — Core functionality

### Duplicate food entries

- [x] Show a multiplication indicator (for example, ×2 or ×3) when the same food is logged multiple times on one day.
- [x] Group visually identical food entries where appropriate.
- [x] Preserve correct quantities and nutritional totals.
- [x] Ensure editing and deleting entries do not produce incorrect counts.
- [x] Preserve access to each individual entry when items are grouped.

### Localization

- [ ] Complete English localization.
- [ ] Complete Polish localization.
- [ ] Preserve Romanian as the primary language.
- [ ] Use Android string resources consistently.
- [ ] Support language changes without redesigning the application.

## Priority 2 — UI and UX improvements

### More animations

- [ ] Improve screen transitions.
- [ ] Add smooth food selection animations.
- [ ] Improve add and delete interactions.
- [x] Add subtle animations for nutritional value updates.
- [ ] Keep animations purposeful and performant.

### More expressive UI

- [x] Refine the Statistics screen.
- [x] Improve card hierarchy and spacing.
- [x] Improve chart presentation.
- [x] Refine Today and Add Food with coordinated Light and Dark Material 3 Expressive palettes.
- [ ] Improve visual consistency across screens.
- [ ] Continue following Google Material 3 Expressive.

UI refinements must preserve existing functionality and CalTrack's visual identity.

## Priority 3 — Optional future features

The AI Food Assistant implementation and its privacy/security limitations are documented in [AI_FOOD_ASSISTANT.md](AI_FOOD_ASSISTANT.md). Additional providers and direct USDA API integration remain future work.

## Priority 4 — Long-term expansion

### iOS — Kotlin Multiplatform migration in progress

- [x] Add a Kotlin Multiplatform shared module with Android and iOS framework targets.
- [x] Move nutritional reference calculations and quantity units into shared Kotlin code used by Android.
- [ ] Migrate persistence, application state, and the Compose UI into shared source sets.
- [ ] Add the iOS host app and platform barcode-scanning implementation.
- [ ] Verify feature parity on Android and iOS.

Keep Android working throughout the migration. iOS builds require macOS and Xcode.

## Roadmap maintenance

- Move work to the completed section only after it has been implemented.
- Keep priorities clear and avoid duplicate tasks.
- Add newly requested features when appropriate.
- Do not automatically start the next roadmap item; wait for the user's next development request.
