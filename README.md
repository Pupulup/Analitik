# Dota Analyzer — Android

Native Android application built with Kotlin and Jetpack Compose.

## Current scope
- Match lookup by numeric match ID through the public OpenDota API.
- Match result, score, duration and game mode.
- Player-by-player K/D/A, KDA, GPM, XPM, net worth, hero damage, last hits and level.
- Dark compact interface designed for phone screens.
- Network loading and basic error states.

## Build
Open the project root in Android Studio (JDK 17), allow Gradle sync, then run the `app` configuration on an emulator or Android device (Android 8.0 / API 26 or newer).

## Data source
The first implementation uses OpenDota's public match endpoint. API availability and match parsing depend on OpenDota processing status. No API key or user secret is embedded in the application.

## Planned next milestones
1. Hero names and portraits from a versioned Dota 2 hero catalog.
2. Role-aware player contribution model with explainable component scores.
3. Match timeline, economy and item build views.
4. Match history and saved matches.
5. Exportable PDF match report.
6. Optional STRATZ integration with a user-provided token stored securely on-device.
