# Machi Koro Android Client

Android client connected to the Machi Koro backend through native WebSocket transport and STOMP.

## Getting Started

### Open the Project

Open this project in Android Studio.

### Run the App

Run the `app` configuration on an Android emulator or device.

The app opens the start screen and attempts to connect to the backend WebSocket endpoint automatically.

### Local Development Defaults

The client is configured for Android emulator development by default:

- Backend base URL: `http://10.0.2.2:8080`
- WebSocket URL: `ws://10.0.2.2:8080/ws`

Both values are configurable through Gradle properties:

```bash
./gradlew assembleDebug -PbackendBaseUrl=http://10.0.2.2:8080 -PwebsocketUrl=ws://10.0.2.2:8080/ws
```

---

## Current Status

### Implemented

- Configurable backend and WebSocket URLs via `BuildConfig`
- Native WebSocket transport using OkHttp
- STOMP connect flow over the native WebSocket endpoint
- Live connection status rendered on the start screen
- Graceful disconnect/error handling without app crash

### Not Implemented Yet

- Dedicated Android chat UI
- Sending chat messages from the Android UI
- Lobby/game flow beyond placeholder UI

---

## Architecture

### Project Structure

```text
app/src/main/java/com/machikoro/client/
├── config/                   # App-level backend/WebSocket configuration
├── model/state/              # UI and connection state models
├── network/websocket/        # Native WebSocket + STOMP client setup
├── ui/start/                 # Start screen and state holder
├── ui/theme/                 # Compose theme setup
└── MainActivity.kt           # Application entry point
```

---

## Testing

- Unit tests for config and protocol contract
- Mocked unit tests for WebSocket connect/disconnect lifecycle
- Mocked unit tests for STOMP handshake flow
- Unit tests for start screen connection-state mapping
- Compose UI test for rendered connection status

---

## Next Steps

Current follow-up work can focus on:

- adding send-message support from the Android client
- replacing placeholder lobby/start UI with real game/lobby state
