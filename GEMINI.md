# Project Context: MorseLab
**Purpose:** An Android application for experimenting with Morse code, specifically focusing on encoding/decoding and signal patterns.

## 1. Core Tech Stack
- **Language:** Kotlin 2.x (Primary).
- **UI Framework:** Jetpack Compose.
- **Asynchronous Logic:** Kotlin Coroutines and Flow.
- **Dependency Management:** Gradle (Kotlin DSL or Groovy).
- **Android Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles.

## 2. Real Directory Map (Android Standard)
- `/app/src/main/java/com/mauicoder/morselab/`: Core Kotlin logic, ViewModels, and UI.
- `/app/src/test/`: Unit tests for Morse logic.
- `/app/src/androidTest/`: UI and integration tests.
- `/gradle/`: Build configuration and dependency versions.

## 3. Coding Standards & Preferences
- **Patterns:** Prefer `StateFlow` for UI state and `SharedFlow` for events.
- **UI:** 100% Jetpack Compose. No XML layouts.
- **Testing:** Prioritize JUnit 5 and MockK for testing the Morse encoding/decoding engine.
- **Concurrency:** Always use `viewModelScope` for coroutines in ViewModels.

## 4. Current Objectives
- [x] Implement a Morse code encoder -> text to audio.
- [x] Implement a Morse code decoder -> audio to text
- [ ] Improve the morse code decoder.
- [ ] Refine the Morse code audio generation logic.
- [ ] Implement a real-time visualizer for the signal.
- [ ] Expand unit test coverage for complex Morse sequences.