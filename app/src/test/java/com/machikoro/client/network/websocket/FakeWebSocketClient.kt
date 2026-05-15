package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
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

    override val diceResult: StateFlow<List<Int>?>
        get() = mutableDiceResult

    override val activePlayerId: StateFlow<Int?>
        get() = mutableActivePlayerId

    override val activeGameId: StateFlow<Int?>
        get() = mutableActiveGameId

    override val isLobbyHost: StateFlow<Boolean>
        get() = mutableIsLobbyHost

    override val authRejections: SharedFlow<Unit>
        get() = mutableAuthRejections

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())
    private val mutableLobbyCode = MutableStateFlow<String?>(null)
    private val mutableDiceResult = MutableStateFlow<List<Int>?>(null)
    private val mutableActivePlayerId = MutableStateFlow<Int?>(null)
    private val mutableActiveGameId = MutableStateFlow<Int?>(null)
    private val mutableIsLobbyHost = MutableStateFlow(false)
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

    override fun sendCreateLobby() {
        createLobbySent = true
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

    fun emitAuthRejection() {
        mutableAuthRejections.tryEmit(Unit)
    }
}