package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
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

    // NEU
    override val diceResult: StateFlow<List<Int>?>
        get() = mutableDiceResult

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())
    private val mutableLobbyCode = MutableStateFlow<String?>(null)
    private val mutableDiceResult = MutableStateFlow<List<Int>?>(null) // NEU

    var gameStartSent = false
        private set

    var lastRolledDiceCount: Int? = null // NEU
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
    }

    // NEU
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

    // NEU
    fun emitDiceResult(dice: List<Int>) {
        mutableDiceResult.value = dice
    }
}