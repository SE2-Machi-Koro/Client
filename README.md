# Machi Koro - Android Client

Mobile client application for Machi Koro game built with Android and Jetpack Compose.

## 📚 Documentation

- [CI/CD Fix Summary](./CI_FIX_SUMMARY.md) - Summary of fixes to the CI pipeline
- [Keystore Setup Guide](./KEYSTORE_SETUP.md) - How to generate and configure the keystore for the release APK
- [Environment Variables](./.env.example) - Required environment variables

## 🚀 Quick Start

### Prerequisites
- Android Studio Jellyfish or newer
- JDK 21
- Gradle 9.3.1+

### Build Locally

```bash
# Build debug APK
./gradlew assembleDebug

# Build and run tests
./gradlew build

# Run linter
./gradlew lint
```

### CI/CD Pipeline

The project uses GitHub Actions for CI/CD:

- **build-and-test**: Runs on pushes to `main` and `develop`, and on PRs
  - Builds the application
  - Runs unit tests
  - Performs lint analysis
  - Analyzes with SonarCloud
  
- **deploy**: Runs only on push to `main` branch
  - Builds release APK
  - Signs with keystore (if configured)
  - Uploads APK as artifact

**See [CI_FIX_SUMMARY.md](./CI_FIX_SUMMARY.md) for configuration details.**

## 🔑 Release APK Signing

To enable release APK signing in GitHub Actions, configure these repository secrets:

1. `SONAR_TOKEN` - SonarCloud authentication token
2. `KEYSTORE_FILE` - Base64-encoded keystore file
3. `KEYSTORE_PASSWORD` - Keystore password
4. `KEY_ALIAS` - Key alias in keystore
5. `KEY_PASSWORD` - Key password

See [KEYSTORE_SETUP.md](./KEYSTORE_SETUP.md) for detailed instructions.

## 🏗️ Project Structure

```
Client/
├── app/                    # Android application module
│   ├── src/
│   │   ├── main/          # Source code and resources
│   │   ├── test/          # Unit tests
│   │   └── androidTest/   # Instrumentation tests
│   └── build.gradle.kts   # App-level Gradle configuration
├── .github/
│   └── workflows/
│       └── ci.yml         # GitHub Actions CI/CD workflow
├── gradle/
│   └── libs.versions.toml # Dependency versions
└── build.gradle.kts       # Project-level Gradle configuration
```

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Build Tool**: Gradle with Kotlin DSL
- **Android SDK**: API 36 (target), API 29 (minimum)
- **Testing**: JUnit 4
- **Code Quality**: SonarCloud

## 📋 Dependencies

- AndroidX Core KTX
- Jetpack Compose (Material 3)
- AndroidX Lifecycle Runtime KTX
- JUnit & Espresso for testing

See [libs.versions.toml](./gradle/libs.versions.toml) for complete version management.

## 🔍 Code Quality

The project includes:
- **Lint Analysis**: Automatic Android lint checks
- **Unit Tests**: testDebugUnitTest task
- **SonarCloud**: Continuous code quality monitoring

## 📝 License

No license has been specified yet. Add a `LICENSE` file and update this section once a license is chosen.
