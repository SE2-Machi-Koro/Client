# Machi Koro Android Client

[![CI](https://github.com/valentinaschiavon/SE2-Client/actions/workflows/ci.yml/badge.svg)](https://github.com/valentinaschiavon/SE2-Client/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=valentinaschiavon_SE2-Client&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=valentinaschiavon_SE2-Client)

A modern, reactive Android frontend for the Machi Koro board game. This application serves as the primary user interface, connecting to a dedicated backend via a robust WebSocket-based communication layer using the STOMP protocol.

## 🚀 Key Frontend Features

- **Modern Declarative UI**: Built entirely with **Jetpack Compose** for a fluid and responsive user experience.
- **Real-time Synchronization**: Native WebSocket integration with STOMP protocol for live game state updates.
- **Material 3 Design**: Adheres to the latest Android design standards for a clean, modern aesthetic.
- **Reactive State Management**: Utilizes `ViewModel` and `StateFlow` to ensure a single source of truth and predictable UI transitions.
- **Configurable Environment**: Flexible build-time configuration for different backend environments.

## 🛠 Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **State Management** | Android ViewModel + StateFlow |
| **Networking** | OkHttp + Custom STOMP Client |
| **JSON Parsing** | org.json |
| **Build System** | Gradle (Kotlin DSL) |
| **Architecture** | Clean Architecture / MVVM |

## 📂 Project Structure

The project follows a modularized directory structure within `app/src/main/java/com/machikoro/client/` to ensure scalability and maintainability:

```text
├── config/           # Application-level configurations (AppConfig)
├── domain/           # Business logic and domain models
│   ├── model/state/  # Immutable UI and Game state models
│   └── enums/        # Game constants (GamePhase, etc.)
├── network/          # Communication layer
│   └── websocket/    # STOMP protocol implementation and WebSocket client
├── ui/               # Presentation layer
│   ├── game/         # Game board and active gameplay UI
│   ├── start/        # Lobby, connection, and entry screens
│   └── theme/        # Material 3 colors, typography, and theme definitions
└── MainActivity.kt   # Single activity entry point
```

## 🏁 Getting Started

### Prerequisites

#### Local Development (Client)
- **Android Studio**: Ladybug (2024.2.1) or newer recommended.
- **JDK**: version 11 or higher.
- **Android SDK**: API Level 29 (Android 10) minimum, API 36 (target).

#### Backend Infrastructure
To enable full game functionality, a running instance of the Machi Koro Backend is required. 
- **Docker & Docker Compose**: The backend services are fully containerized. Ensure Docker is installed on your server or local development machine to orchestrate the API and WebSocket services.

### Installation & Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/valentinaschiavon/SE2-Client.git
   cd Client
   ```

2. **Sync Project**: Open the project in Android Studio and allow Gradle to sync dependencies.

3. **Run the application**:
   - Using Android Studio: Press `Shift + F10` or the **Run** button.
   - Using CLI:
     ```bash
     ./gradlew installDebug
     ```

## ⚙️ Environment Variables

The application uses Gradle properties to configure the backend connection. These can be defined in your `local.properties` or passed via command line.

| Property Name | Default Value (Emulator) | Description |
| :--- | :--- | :--- |
| `backendBaseUrl` | `http://10.0.2.2:8080` | The REST API base URL |
| `websocketUrl` | `ws://10.0.2.2:8080/ws` | The STOMP WebSocket endpoint |

**Example CLI build with custom URLs:**
```bash
./gradlew assembleDebug -PbackendBaseUrl=http://api.myapp.com -PwebsocketUrl=ws://api.myapp.com/ws
```

## 📦 Build & Deployment

- **Debug Build**: `./gradlew assembleDebug`
- **Release Build**: `./gradlew assembleRelease`
- **Linting**: `./gradlew lint`
- **Full Quality Check**: `./gradlew check` (Runs tests, linting, and coverage verification)

## 🧪 Testing

The project maintains high code quality through a comprehensive testing strategy:

### Unit Tests
Located in `app/src/test`. Focuses on ViewModels, Domain models, and Network protocol logic.
```bash
./gradlew testDebugUnitTest
```

### UI & Instrumentation Tests
Located in `app/src/androidTest`. Uses Espresso and Compose UI Testing.
```bash
./gradlew connectedDebugAndroidTest
```

### Code Coverage
We use **Jacoco** for coverage reporting. The build will fail if line coverage falls below **80%**.
- **Generate Report**: `./gradlew jacocoTestReport`
- Reports are available at: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

---

## ⚡️ Note for Developers

> **Important:** To run the full Machi Koro system locally, you must have the backend server running. If you haven't already, clone and start the backend project:
>
> ```bash
> git clone https://github.com/valentinaschiavon/SE2-Backend.git
> cd SE2-Backend
> docker compose up --build
> ```
>
> The client will not function without the backend API and WebSocket services available. For more details, see the [SE2-Backend repository](https://github.com/valentinaschiavon/SE2-Backend).

---

*Developed as part of the SE2 Project.*
