package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.shop.ShopItem

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
    // Reconnect snapshot fields (from /app/game.sync).
    val gameStatus: GameStatus? = null,
    val roundNumber: Int? = null,
    val playerLandmarks: Map<Int, List<PlayerLandmarkState>> = emptyMap(),
    val marketplace: Map<CardType, Int> = emptyMap(),
    val shopItems: List<ShopItem> = emptyList(),
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
            isRolling = false,
            gameStatus = null,
            roundNumber = null,
            playerLandmarks = emptyMap(),
            marketplace = emptyMap(),
            shopItems = emptyList(),
        )
    }
}
