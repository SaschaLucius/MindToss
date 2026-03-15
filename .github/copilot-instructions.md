# MindToss – Copilot Instructions

## Architecture

- Single-module Android app
- MVVM pattern with Jetpack ViewModels
- Jetpack Compose for UI (Material Design 3)
- DataStore Preferences for local storage
- Ktor for HTTP requests (Resend API, title fetching)
- WorkManager for offline queue

## Package Structure

```
lukulent.mindtoss.app/
├── MindTossApp.kt              Application class
├── MainActivity.kt             Entry point, navigation, share target
├── ui/
│   ├── theme/                  Material3 theme (Theme.kt, Color.kt, Type.kt)
│   ├── main/                   Main screen (MainScreen.kt, MainViewModel.kt)
│   └── settings/               Settings screen (SettingsScreen.kt, SettingsViewModel.kt)
├── data/
│   ├── model/                  Data classes (HistoryEntry, MessageType)
│   ├── SettingsRepository.kt   Preferences DataStore wrapper
│   └── HistoryRepository.kt    History entries (JSON in DataStore)
├── network/
│   ├── ResendApi.kt            Resend REST API client
│   └── TitleFetcher.kt         HTML title extraction
└── worker/
    └── SendMailWorker.kt       WorkManager worker for offline send
```

## Conventions

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0 Oreo)
- **UI:** Jetpack Compose only (no XML layouts)
- **Async:** Kotlin Coroutines + Flow
- **Serialization:** kotlinx.serialization
- **UI strings:** hardcoded in German (de)
- **Trailing commas:** yes (Kotlin style)
- **No DI framework:** manual dependency creation in ViewModels via AndroidViewModel

## Key Behaviors

1. **Send flow:** Type text → Send → App closes automatically (`finishAffinity()`)
2. **Error handling:** Show Snackbar, app stays open
3. **Offline:** Queue with WorkManager (NetworkType.CONNECTED constraint) → auto-send when online
4. **Share target:** Receives `text/plain` via ACTION_SEND, optionally fetches page titles for URLs
5. **Draft auto-save:** Text persists between sessions via DataStore
6. **Mail format:** Subject = first line, Body = remaining lines, plain text only
7. **Task button:** Hidden when no task recipient is configured

## Build

- Debug: `./gradlew assembleDebug`
- Release: `./gradlew assembleRelease` (requires signing config)
- GitHub Actions: triggered by git tag `v*`
