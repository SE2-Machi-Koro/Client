package com.machikoro.client.domain.enums

/**
 * Mirrors the server's `GameStatus` — the lifecycle state of a game.
 * Carried in the `/app/game.sync` reconnect snapshot and used by AppRoot to
 * pick the screen: WAITING → lobby, IN_PROGRESS → game, FINISHED → winner.
 */
enum class GameStatus {
    WAITING,
    IN_PROGRESS,
    FINISHED,
}
