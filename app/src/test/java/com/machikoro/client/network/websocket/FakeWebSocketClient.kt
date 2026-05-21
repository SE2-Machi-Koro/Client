package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.shop.ShopItem
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

    override val lobbyJoinErrors: SharedFlow<String>
        get() = mutableLobbyJoinErrors

    override val diceResult: StateFlow<List<Int>?>
        get() = mutableDiceResult

    override val activePlayerId: StateFlow<Int?>
        get() = mutableActivePlayerId

    override val activeGameId: StateFlow<Int?>
        get() = mutableActiveGameId

    override val isLobbyHost: StateFlow<Boolean>
        get() = mutableIsLobbyHost

    override val gameStatus: StateFlow<GameStatus?>
        get() = mutableGameStatus

    override val roundNumber: StateFlow<Int?>
        get() = mutableRoundNumber

    override val playerLandmarks: StateFlow<Map<Int, List<PlayerLandmarkState>>>
        get() = mutablePlayerLandmarks

    override val marketplace: StateFlow<Map<CardType, Int>>
        get() = mutableMarketplace

    override val shopItems: StateFlow<List<ShopItem>>
        get() = mutableShopItems

    override val purchaseEvents: SharedFlow<PurchaseEvent>
        get() = mutablePurchaseEvents

    override val authRejections: SharedFlow<Unit>
        get() = mutableAuthRejections

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())
    private val mutableLobbyCode = MutableStateFlow<String?>(null)
    private val mutableLobbyJoinErrors = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableDiceResult = MutableStateFlow<List<Int>?>(null)
    private val mutableActivePlayerId = MutableStateFlow<Int?>(null)
    private val mutableActiveGameId = MutableStateFlow<Int?>(null)
    private val mutableIsLobbyHost = MutableStateFlow(false)
    private val mutableGameStatus = MutableStateFlow<GameStatus?>(null)
    private val mutableRoundNumber = MutableStateFlow<Int?>(null)
    private val mutablePlayerLandmarks =
        MutableStateFlow<Map<Int, List<PlayerLandmarkState>>>(emptyMap())
    private val mutableMarketplace = MutableStateFlow<Map<CardType, Int>>(emptyMap())
    private val mutableShopItems = MutableStateFlow<List<ShopItem>>(emptyList())
    private val mutablePurchaseEvents = MutableSharedFlow<PurchaseEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableAuthRejections = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    var gameStartSent = false
        private set

    var lastRolledDiceCount: Int? = null
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

    var lastJoinLobbyCode: String? = null
        private set

    override fun sendJoinLobby(lobbyCode: String) {
        lastJoinLobbyCode = lobbyCode
    }

    override val lobbyEntered: SharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)

    var leaveLobbyGameId: Int? = null
        private set

    override fun sendLeaveLobby(gameId: Int) {
        leaveLobbyGameId = gameId
    }

    override fun clearGameState() {
        mutableLobbyCode.value = null
        mutableActiveGameId.value = null
        mutableGamePhase.value = GamePhase.NONE
        mutablePlayers.value = emptyList()
    }

    override fun clearLobbyCode() {
        mutableLobbyCode.value = null
        mutableActiveGameId.value = null
        mutableIsLobbyHost.value = false
    }

    override fun rollDice(diceCount: Int) {
        lastRolledDiceCount = diceCount
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
    
    fun emitDiceResult(dice: List<Int>) {
        mutableDiceResult.value = dice
    }

    fun emitActivePlayerId(id: Int?) {
        mutableActivePlayerId.value = id
    }

    fun emitActiveGameId(gameId: Int?) {
        mutableActiveGameId.value = gameId
    }

    fun emitIsLobbyHost(isHost: Boolean) {
        mutableIsLobbyHost.value = isHost
    }

    fun emitGameStatus(status: GameStatus?) {
        mutableGameStatus.value = status
    }

    fun emitRoundNumber(round: Int?) {
        mutableRoundNumber.value = round
    }

    fun emitPlayerLandmarks(landmarks: Map<Int, List<PlayerLandmarkState>>) {
        mutablePlayerLandmarks.value = landmarks
    }

    fun emitMarketplace(marketplace: Map<CardType, Int>) {
        mutableMarketplace.value = marketplace
    }

    fun emitShopItems(shopItems: List<ShopItem>) {
        mutableShopItems.value = shopItems
    }

    fun emitPurchaseEvent(event: PurchaseEvent) {
        mutablePurchaseEvents.tryEmit(event)
    }

    fun emitAuthRejection() {
        mutableAuthRejections.tryEmit(Unit)
    }

    fun emitLobbyCode(code: String?) {
        mutableLobbyCode.value = code
    }
    data class PurchaseCall(
        val gameId: Int,
        val purchaseType: PurchaseType,
        val cardType: String?,
        val landmarkType: String?
    )
}
