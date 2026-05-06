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
    override val diceResult: StateFlow<DiceRollResult?>
        get() = mutableDiceResult
    override val players: StateFlow<List<PlayerCoinState>>
        get() = mutablePlayers
    override val lobbyCode: StateFlow<String?>
        get() = mutableLobbyCode

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val mutableDiceResult = MutableStateFlow<DiceRollResult?>(null)
    private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())
    private val mutableLobbyCode = MutableStateFlow<String?>(null)

    var gameStartSent = false
        private set
    var createLobbySent = false
        private set
    var lastRollDicePlayerId: String? = null
        private set
    var lastRollDiceDiceCount: Int? = null
        private set

    override fun connect() = Unit
    override fun disconnect() = Unit

    override fun rollDice(playerId: String, diceCount: Int) {
        lastRollDicePlayerId = playerId
        lastRollDiceDiceCount = diceCount
    }

    override fun sendGameStart() {
        gameStartSent = true
    }

    override fun sendCreateLobby() {
        createLobbySent = true
    }

    fun emitConnectionStatus(status: ConnectionStatus) {
        mutableConnectionStatus.value = status
    }

    fun emitGamePhase(phase: GamePhase) {
        mutableGamePhase.value = phase
    }

    fun emitDiceResult(result: DiceRollResult?) {
        mutableDiceResult.value = result
    }

    fun emitPlayers(players: List<PlayerCoinState>) {
        mutablePlayers.value = players
    }
}