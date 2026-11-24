# Dota Analyzer — Android

Native Android application built with Kotlin and Jetpack Compose.

## Current scope
- Match lookup by numeric match ID through OpenDota.
- Match result, score, duration, game mode and match start time.
- Player statistics: K/D/A, KDA, GPM, XPM, net worth, hero/building damage, healing, last hits and denies.
- Extended player metrics when returned by OpenDota: teamfight participation, stuns, lane efficiency, wards, camps stacked, rune pickups, tower/Roshan/courier kills, buybacks and per-player gold/XP/last-hit timelines.
- Explainable match-relative player contribution score with separate components for combat, economy, experience, hero damage, objectives, last hits and survivability.
- Team comparison, kill timeline and player strengths/weaknesses.
- Export and share a multi-page PDF match report.
- Multi-match player profile from the last 30 OpenDota matches: win rate, KDA, GPM/XPM, hero distribution, recent-match history and period comparison.
- Hero guide library with item popularity by game phase and difficult matchups from OpenDota.
- Dark compact interface designed for phone screens.

## Build
Open the project root in Android Studio (JDK 17), allow Gradle sync, then run the `app` configuration on an Android 8.0 / API 26 or newer device.

## Data source and scoring notes
The Android branch currently requests match details and hero names from the public OpenDota API. Optional fields are displayed only when the API supplies them; missing values are not treated as negative performance. The contribution index is a heuristic comparison among players in one match, not an official Valve rating or a validated skill metric. Lane role is shown as lane metadata and is not treated as a guaranteed player position.

No API key or user secret is embedded in the application. Ability progression and talent recommendations from the previous STRATZ-backed guide implementation require a protected backend endpoint; the Android app does not embed a STRATZ key.
