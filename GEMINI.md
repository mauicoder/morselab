# Project Context: MorseLab

**Purpose:** An Android application for experimenting with Morse code, specifically focusing on encoding/decoding, high-fidelity signal patterns, and real-time audio generation.

## 1. Core Tech Stack
- **Language:** Kotlin 2.x (Primary).
- **Build System:** AGP 9.0.1 (Built-in Kotlin enabled).
- **UI Framework:** Transitional from ViewBinding to 100% Jetpack Compose.
- **Data Persistence:** Jetpack DataStore using **Kotlinx Serialization (JSON)**.
- **Asynchronous Logic:** Kotlin Coroutines and Flow (`StateFlow` / `SharedFlow`).
- **Dependency Injection:** Hilt (Dagger) with KSP.
- **Architecture:** MVVM with Clean Architecture principles.

## 2. Real Directory Map (Android Standard)
- `/app/src/main/java/com/mauicoder/morselab/`: Core Kotlin logic, ViewModels, and UI.
- `/app/src/main/java/com/mauicoder/morselab/data/`: DataStore serializers and repositories.
- `/app/src/test/`: Unit tests for Morse logic.
- `/app/src/androidTest/`: UI and integration tests.
- `/gradle/`: Version catalog (`libs.versions.toml`).

## 3. Coding Standards & Preferences
- **State Management:** Use `StateFlow` with `SharingStarted.Eagerly` for persistent settings to ensure real-time updates across Fragments/Screens.
- **Data Modeling:** Prefer Kotlin `@Serializable` data classes over Protobuf for app-internal preferences.
- **Build Configuration:**
    - Leverage `android.newDsl=true` and `android.builtInKotlin=true`.
    - Use `configure<ApplicationExtension>` for module-level configuration.
- **Concurrency:** Always use `viewModelScope` for coroutines in ViewModels and `repeatOnLifecycle` for UI observation.
- **Testing:** Prioritize JUnit 5 and MockK; focus on the Morse engine's mathematical accuracy.

## 4. Current Objectives
- [x] Implement Morse code encoder (Text ➔ Audio).
- [x] Implement Morse code decoder (Audio ➔ Text).
- [x] Migrate persistence from Protobuf to Kotlin Serialization (JSON).
- [x] Implement real-time reactive preference updates across all app modules.
- [x] Modernize build system to AGP 9.0 Standards.
- [ ] Improve Morse code decoder algorithm for noise handling.
- [ ] Refine the Morse code audio generation (Waveform smoothing).
- [ ] Implement a real-time visualizer for the signal.
- [ ] Expand unit test coverage for complex/high-speed Morse sequences.
