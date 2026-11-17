# Contributing to Openfy 🎵

Thank you for your interest in contributing to **Openfy**! Openfy is an open-source, native Android music streaming application designed to combine Spotify's refined mobile UI with the unrestricted, ad-free streaming capabilities of RVX (ReVanced Extended) YouTube Music.

We welcome all contributions—from bug fixes and performance improvements to UI refinements and translations.

---

## 📋 Table of Contents

- [Code of Conduct](#-code-of-conduct)
- [How Can I Contribute?](#-how-can-i-contribute)
- [Development Setup](#-development-setup)
- [Architectural Principles](#-architectural-principles)
- [Pull Request Process](#-pull-request-process)
- [Coding Standards](#-coding-standards)
- [Reporting Bugs](#-reporting-bugs)
- [Feature Requests](#-feature-requests)

---

## 🤝 Code of Conduct

By participating in this project, you agree to uphold a welcoming, respectful, and harassment-free environment for everyone regardless of level of experience, gender, identity, or background. Be constructive, considerate, and helpful in all interactions.

---

## 💡 How Can I Contribute?

- **Bug Fixes**: Resolve open issues on GitHub or fix edge-case bugs encountered during testing.
- **UI / UX Polish**: Enhance Jetpack Compose animations, transitions, and 1:1 Spotify fidelity.
- **Audio & Extraction**: Improve stream resolver resilience, audio buffer efficiency, and lyrics synchronization.
- **Documentation**: Improve guides, inline documentation, architecture diagrams, and translation strings.

---

## 🛠️ Development Setup

### Prerequisites

1. **JDK**: Java 17 or Java 21 (recommended: Eclipse Temurin or Azul Zulu).
2. **Android Studio**: Android Studio Koala, Ladybug, or newer.
3. **Android SDK**: API level 35 (Android 15) with platform tools.
4. **Git**: Installed and configured.

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Zcross091/Openfy.git
   cd Openfy
   ```

2. **Open in Android Studio**:
   - Open Android Studio, select **Open**, and navigate to the root `Openfy/` directory.
   - Let Gradle sync project dependencies.

3. **Compile Debug APK via CLI**:
   ```bash
   # Linux / macOS
   ./gradlew assembleDebug

   # Windows PowerShell
   .\gradlew.bat assembleDebug
   ```
   The compiled debug APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on a Connected Device**:
   ```bash
   ./gradlew installDebug
   ```

---

## 🏗️ Architectural Principles

When writing code for Openfy, please adhere to these core architectural guidelines:

1. **Keep the APK Lean (~13.5MB)**:
   - Do **NOT** bundle heavy native C/C++ libraries (`.so` files) or bulky standalone JS engines.
   - We utilize Android System WebView's native V8 engine headless for lightweight cipher solving, keeping binary sizes tiny and startup instantaneous.

2. **Pure Jetpack Compose UI**:
   - All UI must be written in declarative **Jetpack Compose** using Material 3.
   - Maintain the Spotify dark aesthetic:
     - Background: `#121212`
     - Card / Surface: `#181818` / `#282828`
     - Brand Accent: Spotify Green `#1DB954`
     - Text Primary: `#FFFFFF`
     - Text Subdued: `#B3B3B3`

3. **Background Audio Resilience**:
   - Audio playback is driven by `AndroidX Media3 (ExoPlayer)` inside a foreground `MediaSessionService` ([MusicService.kt](app/src/main/java/com/openfy/music/service/MusicService.kt)).
   - Always preserve `C.WAKE_MODE_NETWORK` and generous load buffering so playback never stalls when screens lock.

4. **Single-Active-Account Paradigm**:
   - Users authenticate with either **Spotify** or **YouTube Music** (or Guest mode).
   - Tapping the Heart icon or `+` Add to Playlist button must dynamically route to the user's active account provider via `AccountManager`.

---

## 🚀 Pull Request Process

1. **Fork the Repository**:
   - Create your own fork on GitHub.

2. **Create a Feature Branch**:
   ```bash
   git checkout -b feature/your-feature-name
   # or for bug fixes:
   git checkout -b fix/issue-description
   ```

3. **Follow Conventional Commits**:
   - `feat: add sleep timer dialog`
   - `fix: resolve playback pause on Bluetooth disconnect`
   - `docs: update build instructions in README`
   - `refactor: optimize search track parser`

4. **Test Before Pushing**:
   - Ensure the project compiles cleanly without errors:
     ```bash
     ./gradlew assembleRelease
     ```
   - Verify changes on a physical device or emulator.

5. **Submit the PR**:
   - Open a PR against the `main` branch.
   - Provide a clear summary of what was changed and why. Include screenshots or screen recordings for visual UI modifications.

---

## 📝 Coding Standards

- **Language**: Kotlin 2.0+ idiomatic code.
- **Coroutines & Reactive State**:
  - Use `StateFlow` and `SharedFlow` for reactive view models and singletons.
  - Dispatch I/O tasks (network, disk, scraping) explicitly on `Dispatchers.IO`.
  - Dispatch UI interactions on `Dispatchers.Main`.
- **Formatting**:
  - 4 spaces indentation.
  - PascalCase for Composable functions (`@Composable fun TrackItem(...)`).
  - camelCase for functions and variables.
  - Descriptive, self-documenting naming conventions.

---

## 🐛 Reporting Bugs

If you find a bug:
1. Search [Existing GitHub Issues](https://github.com/Zcross091/Openfy/issues) to ensure it hasn't been reported yet.
2. If new, open an issue including:
   - **Device model** and **Android OS version** (e.g. Pixel 8, Android 14).
   - **Openfy Version / Commit hash**.
   - **Steps to reproduce** the issue.
   - **Expected vs Actual behavior**.
   - Relevant `adb logcat` output if applicable.

---

## 💡 Feature Requests

Have an idea to make Openfy even better?
- Open a feature request in [GitHub Discussions / Issues](https://github.com/Zcross091/Openfy/issues).
- Describe the feature, why it would be beneficial, and any mockups or reference apps (e.g. official Spotify UI or RVX player).

---

Thank you for helping make Openfy better for music lovers worldwide! 🎧
