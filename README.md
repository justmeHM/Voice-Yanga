# Voice Yanga

Your voice. Your platform.

Voice Yanga is an Android application designed to provide a platform for citizens to share their opinions and engage in meaningful conversations.

## Project Overview

This project follows a modern Android architecture (MVVM) and includes:
- **Splash Screen**: Initial landing screen with session checking.
- **Authentication**: Login and Home navigation flow managed by `SessionManager`.
- **Branding**: Custom app icons and themed splash layout using the `vote_icon`.
- **CI/CD**: Integrated GitHub Actions for automated linting, testing, and building.

## Tech Stack

- **Language**: Java
- **UI**: XML Layouts with ConstraintLayout
- **Architecture**: MVVM (ViewModel, LiveData)
- **Local Storage**: SharedPreferences (Session Management), Room (Planned)
- **Networking**: Retrofit, OkHttp (Planned)
- **Build System**: Gradle Kotlin DSL (.kts)
- **CI**: GitHub Actions

## Setup and Installation

1. **Prerequisites**:
   - Android Studio (Ladybug or newer)
   - JDK 17
   - Android SDK 24+

2. **Building the project**:
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Run the `./gradlew assembleDebug` command to build the APK.

3. **Running Tests**:
   - Unit Tests: `./gradlew test`
   - Linting: `./gradlew lint`

## Project Structure

- `app/src/main/java/com/voiceyanga/citizen/ui/`: Contains Activity classes (Splash, Login, Home).
- `app/src/main/java/com/voiceyanga/citizen/viewmodel/`: Contains ViewModels.
- `app/src/main/java/com/voiceyanga/citizen/data/`: Contains Repository and Local Data (SessionManager).
- `.github/workflows/`: Contains the CI pipeline configuration.

## Instructions for Development

- **Icons**: The official app icon is located at `app/src/main/res/mipmap-xxxhdpi/ic_voice_yanga.jpg`.
- **Dependencies**: Manage all dependencies in `gradle/libs.versions.toml`.
- **Manifest**: Ensure all new Activities are declared within the `<application>` tag in `AndroidManifest.xml`.

---
© 2026 Voice Yanga Project
