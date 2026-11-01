# CalTrack

**A simple, modern Android calorie and nutrition tracker.**

CalTrack is a native Android application designed to make food logging fast, intuitive and accessible. Track your daily calorie intake, monitor macronutrients, manage your food library and explore nutritional information without unnecessary complexity.

Built with Kotlin, Jetpack Compose and Material 3 Expressive.

---

## Features

### Daily Food Tracking
- Log meals and food entries manually.
- Track daily calories and macronutrients.
- Navigate between different dates.
- Edit and delete food entries.
- Undo accidental deletions.
- Save frequently used foods for quick access.

### Food Library
- Manage your saved foods.
- Search locally stored foods.
- Discover frequently logged foods.
- Quickly add saved foods to your daily journal.
- Edit nutritional information.

### Barcode Scanner
- Scan product barcodes using Google Code Scanner.
- Retrieve product information through Open Food Facts.
- Preview nutritional information before saving.
- Display available calories, protein, carbohydrates and fat.
- Cache scanned products for offline reuse.
- Manually edit product information when needed.

### Statistics
- View statistics over 7, 30 and 90 days.
- Calculate average daily calorie consumption.
- Track protein, carbohydrate and fat averages.
- View daily calorie history.
- Compare the highest and lowest logged days.
- View macronutrient calorie distribution.

Daily averages use days with recorded food entries rather than treating unlogged days as zero-calorie days.

### User Interface
- Native Android experience.
- Google Material 3 Expressive design.
- Responsive Jetpack Compose interface.
- Light and dark theme design.
- Smooth animations and expressive components.

---

## Screenshots

Screenshots will be added as the interface evolves.

| Today | Statistics | Meals |
|---|---|---|
| Coming soon | Coming soon | Coming soon |

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Main programming language |
| Jetpack Compose | Declarative UI |
| Material 3 Expressive | Design system |
| Room | Local database |
| DataStore | Application settings |
| ViewModel | UI state management |
| Coroutines | Asynchronous operations |
| Google Code Scanner | Barcode scanning |
| Open Food Facts | Product information |
| Gradle | Build system |

---

## Architecture

CalTrack follows a lightweight MVVM-style architecture.

The project is organized into several main areas:

- `data/` – repositories and data management.
- `data/local/` – Room database, entities, DAOs and migrations.
- `ui/` – Jetpack Compose screens and reusable components.
- ViewModels – UI state and application logic.

The application uses local persistence and does not require an account to use its core food tracking features.

---

## Getting Started

### Requirements

- Android Studio.
- Android SDK compatible with the project configuration.
- JDK compatible with the configured Gradle version.
- An Android device or emulator.

### Installation

1. Clone the repository:

```bash
git clone YOUR_REPOSITORY_URL
```

2. Open the project in Android Studio.

3. Allow Gradle to synchronize dependencies.

4. Connect an Android device or start an emulator.

5. Run the application using Android Studio.

### Building the APK

On Windows:

```bash
gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

The generated APK can be found at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Data Sources

CalTrack uses Open Food Facts to retrieve available product information through barcode lookup.

Product information is community-maintained, so nutritional values may be incomplete or inaccurate.

Users can review and edit nutritional information before saving products.

---

## Privacy

CalTrack is designed around local-first food tracking.

- Food journal entries are stored locally using Room.
- Application preferences are stored using DataStore.
- Core food logging does not require an account.
- External product lookups require an internet connection.
- Third-party product data is retrieved from Open Food Facts.

Future AI functionality may introduce additional external data processing and will be documented separately.

---

## Roadmap

Planned improvements and future ideas include:

- [ ] English and Polish localization.
- [ ] Improved food grouping for repeated entries.
- [ ] More expressive UI and animations.
- [ ] AI Food Assistant with user-provided API keys.
- [ ] Additional nutritional data providers.
- [ ] Potential iOS version.

The roadmap is subject to change as development progresses.

---

## Development Philosophy

CalTrack aims to keep food tracking simple.

No unnecessary features, complicated meal planning or mandatory accounts.

The focus is on:
- Fast food logging.
- Clear nutritional information.
- Reliable calculations.
- A clean and expressive user interface.
- Giving users control over their own food data.

---

## Status

**Active development – Early release.**

CalTrack is a personal development project and is still evolving. Features, design and functionality may change between releases.

---

*Built with Kotlin and a lot of caffeine.* ☕
