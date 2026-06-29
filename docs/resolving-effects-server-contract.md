# Resolving Effects Server Contract

Tracks Client issue #305: verify whether the server sends everything needed for
the resolving-effects UI and the coin animations from issue #304.

## Current Client Inputs

The client can currently rebuild a best-effort resolving-effects preview from
authoritative snapshots:

| Data | Server source currently parsed by client | Status |
| --- | --- | --- |
| Game phase | `GAME_STARTED`, `GAME_ACTION`, `ROLL_DICE`, `GAME_END`, `SYNC` snapshot `game.turnPhase` | Confirmed |
| Dice result | `ROLL_DICE.payload.result`, or snapshot `game.lastDiceRoll` after reconnect | Confirmed |
| Active player | Snapshot `activePlayerId`, fallback `currentTurnIndex` + `turnOrder` | Confirmed |
| Player balances | Snapshot `players[].coins` | Confirmed |
| Owned establishments | Snapshot `playerCards` | Confirmed |
| Built landmarks | Snapshot `playerLandmarks` | Confirmed |
| Marketplace/card metadata | Snapshot `marketplace`, `cardDefinitions`, `landmarkDefinitions` | Confirmed |
| Local-player coin delta | `GAME_ACTION.payload.event == "EFFECTS_RESOLVED"` with `payload.coinDeltas` keyed by player DB id | Confirmed for non-zero changed players |

## What Is Client-Derived Today

The server does not currently provide a parsed client model for triggered
establishments, affected players, or complete transfers. The client derives
those from the snapshot:

- Triggered establishments are inferred from dice total, player ownership,
  card activation numbers, card color, and active player.
- Affected players are inferred locally from the visible card effects.
- Coin preview deltas are computed locally for regular bank income, red-card
  payments, and Stadium.
- TV Station and Business Center are displayed as triggered card art only,
  because the selected target/player/card is not exposed by the server.
- Final visible balances stay server-authoritative: coin badges remain bound to
  `players[].coins` from the latest server snapshot. The local preview is used
  only to plan animations before sending `/app/game.resolveEffects`.

This means the client can animate common cases without changing server state.
The final balances are authoritative, but the per-card/per-transfer animation is
not fully authoritative until the server exposes the exact resolution outcome.

## Confirmed Server Behavior

Server-side checks for #305 confirmed:

- `EFFECTS_RESOLVED.payload.coinDeltas` includes only players whose final coin
  balance changed with a non-zero net delta. Unchanged players are omitted.
- `coinDeltas` is a net delta map, not a per-transfer or per-effect log.
- `coinDeltas` keys are player database IDs (`PlayerModel.id`), not user IDs.
  This differs from snapshot `activePlayerId`, which is a user ID.
- In `EFFECTS_RESOLVED`, `payload.state.players[].coins` is already the final
  post-resolution balance. The server resolves earnings, updates DB coins, then
  builds the snapshot.
- Before `/app/game.resolveEffects`, the server broadcasts `DICE_ROLLED` /
  `DICE_REROLLED` with dice result, total, phase, active player, round, and
  state. It does not explicitly include triggered establishments, affected
  players, or transfers.
- TV Station does not expose a selected target player or paid amount. The
  current resolve request only contains `gameId`; current purple logic treats
  non-`ALL_PLAYERS` purple cards as active-player income.
- Business Center does not expose swapped cards or involved players. There is no
  swap target/card payload in `resolveEffects`, and Business Center currently has
  income `0`, so it is effectively not implemented as a swap effect.
- Red-card and Stadium payments are capped by available payer coins before
  emitting deltas. Red cards are capped by the active player's remaining coins;
  Stadium is capped per opponent's remaining coins.
- Future transfer payloads should model the bank explicitly as `"BANK"`, not
  `null`, matching the server domain's `PaymentSource.BANK`.

## Missing Server Data

To make resolving-effects animation fully server-authoritative and safe from
desync, the client needs one of these:

1. A pre-resolution preview event before the phase advances out of
   `RESOLVE_EFFECTS`.
2. Or an expanded `EFFECTS_RESOLVED` event that is available before/while the UI
   animates and contains the full resolved outcome.

Recommended payload shape:

```json
{
  "type": "GAME_ACTION",
  "payload": {
    "event": "EFFECTS_RESOLVED",
    "resolutionId": "game-7-round-3-roll-1",
    "triggeredEstablishments": [
      {
        "playerId": 11,
        "cardType": "CAFE",
        "quantity": 2,
        "activationRoll": 3,
        "amountPerCard": 1,
        "totalAmount": 2
      }
    ],
    "coinDeltas": {
      "11": 2,
      "22": -2
    },
    "coinTransfers": [
      {
        "from": { "type": "PLAYER", "playerId": 22 },
        "to": { "type": "PLAYER", "playerId": 11 },
        "amount": 2,
        "sourceCardType": "CAFE"
      },
      {
        "from": { "type": "BANK" },
        "to": { "type": "PLAYER", "playerId": 11 },
        "amount": 3,
        "sourceCardType": "WHEAT_FIELD"
      }
    ],
    "affectedPlayers": [11, 22],
    "state": {}
  }
}
```

Field expectations:

| Field | Needed for |
| --- | --- |
| `triggeredEstablishments[].playerId` | Show which player owns each triggered card. |
| `triggeredEstablishments[].cardType` | Show exact card art and text. |
| `triggeredEstablishments[].quantity` | Stack repeated owned cards and calculate repeated effects. |
| `triggeredEstablishments[].amountPerCard` / `totalAmount` | Avoid duplicating server income rules on the client. |
| `coinDeltas` keyed by player id | Verify final net balance changes for every affected player, not only the local player. |
| `coinTransfers[].from` and `to` | Animate player-to-player and bank transfers using explicit `PLAYER` / `BANK` participants. |
| `coinTransfers[].amount` | Animate exact amount moved. |
| `coinTransfers[].sourceCardType` | Tie a transfer back to the card that caused it. |
| `affectedPlayers` | Quickly highlight players touched by the resolution. |
| Stable `resolutionId` | Prevent duplicate animation on reconnect or repeated broadcasts. |

## Current Closure Assessment

Issue #305 can be closed with the current branch as a client-side verification
document, with this conclusion:

- The client has enough snapshot data for a best-effort local preview.
- The server already provides authoritative final balances through state
  snapshots, including the `EFFECTS_RESOLVED` snapshot.
- The server does not yet expose enough detailed pre-resolution data for fully
  authoritative animations of all effect types.
- The missing contract is documented above.
