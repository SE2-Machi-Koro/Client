# Client WebSocket Protocol

This document describes the STOMP/WebSocket protocol used by the Android
client. The implementation source of truth is `WebSocketContract.kt` and
`OkHttpWebSocketClient.kt`.

## Connection

- Build-time property: `websocketUrl`
- Default URL (Railway production): `wss://machi-koro.up.railway.app/ws`
- Local emulator override: `ws://10.0.2.2:8080/ws`
- The client opens an OkHttp WebSocket and then sends a STOMP `CONNECT` frame.
- Auth is sent on the STOMP `CONNECT` frame as:
  - Header: `Authorization`
  - Value: `Bearer <sessionToken>`
- STOMP version: `1.2`
- Heartbeats: `0,0`

The client does not open a WebSocket when there is no active session token.
If the backend rejects the STOMP session with an auth failure, the client signs
out and does not auto-reconnect.

## Subscriptions

After a `CONNECTED` frame, the client subscribes before sending its session join
message so reconnect snapshots are not missed.

| Destination | Purpose |
| --- | --- |
| `/topic/public` | Public lobby/game events used by older server broadcasts. |
| `/user/queue/errors` | Per-user errors, including lobby join failures. |
| `/user/queue/game-sync` | Per-user reconnect snapshot messages. |
| `/queue/lobby-user{sessionId}` | Session-scoped lobby queue when the server provides a STOMP session id. |
| `/user/queue/lobby` | Fallback user-scoped lobby queue when no session id is present. |
| `/topic/game/{gameId}` | Game-specific broadcasts after a lobby/game id is known. |

When the known game id changes, the client unsubscribes from the previous game
topic before subscribing to the new one.

## Send Destinations

| Destination | Client method | Body |
| --- | --- | --- |
| `/app/chat.addUser` | Internal session join | `{"type":"JOIN","sender":"android-client"}` plus `gameId` when known. |
| `/app/lobby.create` | `sendCreateLobby` | Join-style lobby creation envelope. |
| `/app/lobby.join` | `sendJoinLobby` | Join envelope with `payload.lobbyCode`. |
| `/app/lobby.leave` | `sendLeaveLobby` | Leave envelope with `payload.gameId`. |
| `/app/game.start` | `sendGameStart` | `gameId`, `lobbyCode`, both, or `{}` depending on known state. |
| `/app/game.rollDice` | `rollDice` | Envelope with top-level `gameId` and `payload.gameId`/`payload.diceCount`. |
| `/app/game.rerollDice` | `rerollDice` | Same envelope as `rollDice`; Radio Tower reroll (#326). |
| `/app/game.purchase` | `sendPurchase` | `{"gameId":...,"purchaseType":"...","cardType":"..."}` or `landmarkType`. |
| `/app/game.advancePhase` | `advancePhase` | `{"gameId":...}` |
| `/app/game.resolveEffects` | `resolveEffects` | `{"gameId":...}` |
| `/app/game.endTurn` | `endTurn` | `{"gameId":...}` |

The game screen only sends turn-flow actions for the active player while the
game status is `IN_PROGRESS`. The visible game controls send
`/app/game.resolveEffects` during `RESOLVE_EFFECTS` and `/app/game.endTurn`
during `BUY_OR_BUILD`; `END_TURN` is treated as a server-side transition phase
and does not expose a client action. Server broadcasts remain authoritative for
the resulting phase and game state.

During `RESOLVE_EFFECTS`, the active player may additionally send
`/app/game.rerollDice` (#326) when they have built a `RADIO_TOWER` and a roll
already exists this turn. The client gates this to the active player with a
Radio Tower and limits it to once per turn (renewed on turn rotation); the
server stays authoritative for the once-per-turn rule and the result.

## Handled Message Types

| Type | Effect |
| --- | --- |
| `LOBBY_CREATED` | Stores lobby code/game id, subscribes to the game topic, and adds the host locally. |
| `LOBBY_JOINED` | Stores game id, subscribes to the game topic, updates roster, and emits lobby-entered navigation. |
| `LOBBY_LEFT` | Removes the leaving player from the lobby roster. |
| `LOBBY_ROSTER` | Replaces the local lobby roster with the server roster. |
| `GAME_STARTED` | Applies initial game state, player list, phase, active player, and shop data. |
| `GAME_ACTION` | Applies authoritative state snapshots and purchase success events. |
| `ROLL_DICE` | Applies authoritative state snapshots and the individual dice result. Covers both the initial roll (`payload.event == DICE_ROLLED`) and the Radio Tower reroll (`payload.event == DICE_REROLLED`, #326), which share `payload.result`. |
| `GAME_END` | Marks the game finished, stores winner/round data, and unsubscribes from the game topic. |
| `SYNC` | Restores a full reconnect snapshot from `/user/queue/game-sync`. |
| `ERROR` | Emits lobby or purchase errors; auth failures sign the user out. |

## Reconnect

Unexpected WebSocket close/failure schedules automatic reconnect with backoff.
Client-initiated disconnects and auth failures do not reconnect.

On successful reconnect, the client:

1. Re-subscribes to public, error, sync, lobby, and known game destinations.
2. Sends `/app/chat.addUser` again.
3. Receives `SYNC` on `/user/queue/game-sync`.
4. Rebuilds the visible game state from the snapshot.
