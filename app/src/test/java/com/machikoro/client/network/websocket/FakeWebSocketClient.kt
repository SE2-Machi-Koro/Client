package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.shop.PurchaseType
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeWebSocketClient : WebSocketClient {
    override val connectionStatus: StateFlow<ConnectionStatus>
        get() = mutableConnectionStatus

    override val gamePhase: StateFlow<GamePhase>
        get() = mutableGamePhase

    override val players: StateFlow<List<PlayerCoinState>>
        get() = mutablePlayers

    override val lobbyCode: StateFlow<String?>
        get() = mutableLobbyCode

    override val gameId: StateFlow<Int?>
        get() = mutableGameId

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())
    private val mutableLobbyCode = MutableStateFlow<String?>(null)
    private val mutableGameId = MutableStateFlow<Int?>(null)

    var gameStartSent = false
        private set

    override fun connect() = Unit

    override fun disconnect() = Unit

    override fun sendGameStart() {
        gameStartSent = true
    }

    var createLobbySent = false
        private set

    var lastPurchase: PurchaseCall? = null
        private set

    override fun sendCreateLobby() {
        createLobbySent = true
    }

    override fun sendPurchase(
        gameId: Int,
        purchaseType: PurchaseType,
        cardType: String?,
        landmarkType: String?
    ) {
        lastPurchase = PurchaseCall(
            gameId = gameId,
            purchaseType = purchaseType,
            cardType = cardType,
            landmarkType = landmarkType
        )
    }

    fun emitConnectionStatus(status: ConnectionStatus) {
        mutableConnectionStatus.value = status
    }

    fun emitGamePhase(phase: GamePhase) {
        mutableGamePhase.value = phase
    }

    fun emitPlayers(players: List<PlayerCoinState>) {
        mutablePlayers.value = players
    }

    fun emitGameId(gameId: Int?) {
        mutableGameId.value = gameId
    }

    data class PurchaseCall(
        val gameId: Int,
        val purchaseType: PurchaseType,
        val cardType: String?,
        val landmarkType: String?
    )
}
