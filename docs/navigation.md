# Navigation Architecture

This document describes the client navigation setup implemented for parent issue
[#29](https://github.com/SE2-Machi-Koro/Client/issues/29).

## Scope

The navigation work covers the top-level game flow:

```text
Main/Start -> Home -> Lobby -> Game -> Winner
```

The route priority is:

```text
Winner > Game > Lobby > Home > Main
```

This means a finished game always shows the winner screen, an active game phase
shows the game screen, a requested lobby shows the lobby screen, a logged-in
user shows the home screen, and unauthenticated users stay on the main/start
screen.

## Main Files

| File | Responsibility |
| :--- | :--- |
| `ui/AppRoot.kt` | Hosts the single top-level `NavHost`, declares destinations, observes navigation events, and renders the current screen. |
| `ui/navigation/AppRoute.kt` | Defines all top-level routes and builds concrete destinations with optional arguments. |
| `ui/navigation/AppNavigator.kt` | Wraps `NavHostController.navigate(...)` and owns navigation options and duplicate-route guards. |
| `ui/navigation/NavigationViewModel.kt` | Owns navigation state, chooses target routes from app state, and emits navigation events. |
| `MainActivity.kt` | Creates the screen ViewModels, forwards user actions, and calls navigation state actions such as `showLobby()` and `leaveLobby()`. |

## Routes

`AppRoute` is the single source for route strings.

| Route | Purpose | Arguments |
| :--- | :--- | :--- |
| `Main` | Start screen with login/register entry points. | None |
| `Home` | Authenticated home screen with lobby actions. | None |
| `Lobby` | Lobby screen before a game starts. | Optional `lobbyCode` |
| `Game` | Active game screen. | Optional `gameId` |
| `Winner` | Finished-game winner screen. | None |
| `Leaderboard` | Global leaderboard, opened as an overlay from Home or Winner. | None |

Argumented destinations are built through `AppRoute.destination(arguments)`.
Callers should not assemble route strings manually.

### Overlay routes

`Leaderboard` is an **overlay route**: it is opened by an explicit user action
(tapping "View Leaderboard") and layers on top of the state-driven flow rather
than being derived from app state. While an overlay route is the current
destination, `updateNavigationBasedOnState` will not navigate away from it, so a
competing state-driven `NavigateTo` (for example a server snapshot computing
`Home`) cannot bounce the user off it (issue
[#373](https://github.com/SE2-Machi-Koro/Client/issues/373)). A forced logout is
the only exception, because unauthenticated users must always return to `Main`.

The current destination is tracked through
`NavigationViewModel.onDestinationChanged(route)`, which `AppRoot` wires to the
`NavController`'s `OnDestinationChangedListener`. The current route is also a key
of the `updateNavigationBasedOnState` `LaunchedEffect`, so leaving an overlay
(e.g. pressing Back from Leaderboard) re-evaluates the state-driven target. This
matters because the Winner → Leaderboard flow clears the finished-game state: on
Back the re-evaluation routes the user to `Home` rather than stranding them on a
Winner screen with cleared data.

## Navigation Flow

`NavigationViewModel.updateNavigationBasedOnState(...)` derives the target route
from the current app state:

```text
gameStatus == FINISHED      -> Winner
gamePhase != NONE           -> Game
uiState.showLobbyScreen     -> Lobby
startScreenState.loggedInAs -> Home
else                        -> Main
```

The ViewModel then emits `NavigationEvent.NavigateTo(route, arguments)`.
`AppRoot` collects that event and delegates the actual `NavHostController`
operation to `AppNavigator`.

## State And Events

Navigation keeps state and commands separate:

- `NavigationUiState` stores durable navigation UI state, currently
  `showLobbyScreen`.
- `NavigationEvent` represents one-time navigation commands.
- `NavigationViewModel.showLobby()` and `leaveLobby()` update navigation state.
- `NavigationViewModel.navigateTo(...)` emits navigation events.

This keeps navigation state stable across recompositions and configuration
changes because `MainActivity` no longer owns lobby navigation as local
`remember` state.

## AppRoot Responsibilities

`AppRoot` is intentionally thin:

- it creates and owns the Compose `NavHost`;
- it declares top-level destinations;
- it observes `NavigationViewModel.uiState` so state changes trigger route
  recalculation;
- it collects `NavigationEvent` and applies navigation through `AppNavigator`.

`AppRoot` should not add independent route priority logic. New route decisions
belong in `NavigationViewModel` unless there is a Compose-only reason to keep
them local.

## MainActivity Responsibilities

`MainActivity` wires user actions to ViewModels:

- `onGoToLobbyClick` calls `navigationViewModel.showLobby()`;
- `onLeaveLobby` calls `navigationViewModel.leaveLobby()`, then clears lobby
  data through the lobby and home ViewModels;
- when a joined game is detected for a non-host, it calls
  `navigationViewModel.showLobby()`.

Transient Home-screen form state, such as whether the join-lobby input is
visible, remains local UI state and is separate from navigation state.

## Tests

Navigation behavior is covered by:

- `NavigationViewModelTest` for route priority, route arguments, duplicate
  event suppression, and `NavigationUiState`;
- `AppRouteTest` for route argument construction;
- `AppRootTest` for screen-level routing behavior in Compose.

Run the relevant checks with:

```bash
./gradlew compileDebugKotlin
./gradlew compileDebugAndroidTestKotlin
./gradlew testDebugUnitTest
./gradlew lint
```

