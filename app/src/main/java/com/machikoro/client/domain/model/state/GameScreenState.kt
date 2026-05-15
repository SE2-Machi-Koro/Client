package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus

data class GameScreenState(
    val connectionStatus: ConnectionStatus,
    val gamePhase: GamePhase,
    val players: List<PlayerCoinState>,
    val diceResult: List<Int>? = null,
    val activePlayerId: Int? = null,
    val myUserId: Int? = null,
    val isRolling: Boolean = false,
    // Reconnect snapshot fields (from /app/game.sync).
    val gameStatus: GameStatus? = null,
    val roundNumber: Int? = null,
    val playerLandmarks: Map<Int, List<PlayerLandmarkState>> = emptyMap(),
    val marketplace: Map<CardType, Int> = emptyMap(),
) {
    val isActivePlayer: Boolean
        get() = myUserId != null && myUserId == activePlayerId

    companion object {
        fun initial() = GameScreenState(
            connectionStatus = ConnectionStatus.IDLE,
            gamePhase = GamePhase.NONE,
            players = emptyList(),
            diceResult = null,
            activePlayerId = null,
            myUserId = null,
            isRolling = false,
            gameStatus = null,
            roundNumber = null,
            playerLandmarks = emptyMap(),
            marketplace = emptyMap(),
        )
    }
}
