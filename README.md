# Machi Koro Android Client

Android client foundation prepared for upcoming WebSocket integration.

## Getting Started

### Open the Project

Open this project in Android Studio.

### Run the App

Run the `app` configuration on an Android emulator or device.

The current app launches into a minimal placeholder start screen.

---

## Current Foundation

### Implemented

- Minimal start screen
- Placeholder connection status UI
- Placeholder lobby/start UI
- Basic package structure prepared for upcoming WebSocket work

### Not Implemented Yet

- Backend/WebSocket URL configuration
- Actual WebSocket client connection
- Live connection status updates
- Disconnect handling

---

## Architecture

### Project Structure

```text
app/src/main/java/com/machikoro/client/
├── model/state/              # Placeholder UI and connection state models
├── network/websocket/        # WebSocket client contract for upcoming integration
├── ui/start/                 # Minimal start screen
├── ui/theme/                 # Compose theme setup
└── MainActivity.kt           # Application entry point
```

---

## Testing

- Unit test for placeholder start screen state
- Instrumentation/UI test for start screen rendering

---

## Next Steps

WebSocket connection work continues in the next issue, including:

- backend/WebSocket URL configuration
- actual client connectivity
- visible live connection status
- disconnect handling
