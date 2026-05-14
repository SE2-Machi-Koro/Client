package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase

data class GameScreenState(
    // Required by Server PurchaseRequest; populated from lobby/game WebSocket messages.
    val gameId: Int?,
    val connectionStatus: ConnectionStatus,
    val gamePhase: GamePhase,
    val players: List<PlayerCoinState>,
    val diceResult: List<Int>? = null,
    val activePlayerId: Int? = null,
    val myUserId: Int? = null,
    val purchaseState: PurchaseState,
    val isRolling: Boolean = false,
) {
    val isActivePlayer: Boolean
        get() = myUserId != null && myUserId == activePlayerId
  
    // Keeps UI visibility tied to the existing phase stream from the server.
    val isBuyingPhase: Boolean
        get() = gamePhase == GamePhase.BUY_OR_BUILD
  
    companion object {
        fun initial() = GameScreenState(
            gameId = null,
            connectionStatus = ConnectionStatus.IDLE,
            gamePhase = GamePhase.NONE,
            players = emptyList(),
            diceResult = null,
            activePlayerId = null,
            myUserId = null,
            purchaseState = PurchaseState.IDLE,
            isRolling = false
        )
    }
}