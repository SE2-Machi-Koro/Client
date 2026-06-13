package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.AccusationResult
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.network.error.ClientError
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>

    val gamePhase: StateFlow<GamePhase>
    val players: StateFlow<List<PlayerCoinState>>
    val lobbyCode: StateFlow<String?>

    // Fires once when the user successfully enters a lobby (create or join), NOT on reconnect.
    val lobbyEntered: SharedFlow<Unit>
    // Fires when joining a lobby fails, e.g. because the lobby code is invalid.
    val lobbyJoinErrors: SharedFlow<ClientError.WebSocket>
    val hostLeftLobby: SharedFlow<Unit>
    val activeGameId: StateFlow<Int?>
    val isLobbyHost: StateFlow<Boolean>

    // Holds the latest dice result received from the server.
    // Null if no dice have been rolled yet in the current turn.
    val diceResult: StateFlow<List<Int>?>
    val activePlayerId: StateFlow<Int?>
    val winnerId: StateFlow<Int?>

    // Reconnect snapshot fields, populated from the /app/game.sync response.
    // gameStatus is null until the first snapshot arrives.
    val gameStatus: StateFlow<GameStatus?>
    val roundNumber: StateFlow<Int?>
    // playerId -> that player's owned establishment cards.
    val playerCards: StateFlow<Map<Int, List<PlayerCardState>>>
    // playerId -> that player's landmarks (built / unbuilt).
    val playerLandmarks: StateFlow<Map<Int, List<PlayerLandmarkState>>>
    // Remaining marketplace supply per card type.
    val marketplace: StateFlow<Map<CardType, Int>>
    // DB-backed shop definitions received from GAME_STARTED or /app/game.sync.
    val shopItems: StateFlow<List<ShopItem>>
    // One-shot purchase success/error updates used to move the shop out of optimistic UI state.
    val purchaseEvents: SharedFlow<PurchaseEvent>

    // One-shot result of a cheating accusation (#280), for a toast.
    val accusationResults: SharedFlow<AccusationResult>

    // One-shot server rejection of an accusation (INVALID_ACCUSATION on the
    // private errors queue), e.g. "once per turn" when the local gate diverged
    // across a reconnect. Carries the server's message, for a toast.
    val accusationErrors: SharedFlow<String>

    // Fires when the server rejects the STOMP CONNECT for auth reasons (token
    // missing / invalid / server-side cleared). The UI layer is responsible for
    // calling SessionManager.signOut() and surfacing a "session expired"
    // message — kept out of the network client so transport and policy stay
    // separated.
    val authRejections: SharedFlow<Unit>

    fun connect()
    fun disconnect()
    fun sendCreateLobby()
    fun sendJoinLobby(lobbyCode: String)     // Sends a request to join an existing lobby by lobby code.
    fun sendLeaveLobby(gameId: Int)          // Notifies the server that this player is leaving the lobby.
    fun sendReadyToggle(isReady: Boolean)    // Notifies the server of this player's ready state.
    fun clearLobbyCode()
    fun clearGameState()
    fun sendGameStart()

    // Matches Server PR #216 PurchaseRequest: gameId + purchaseType + one target field.
    fun sendPurchase(
        gameId: Int,
        purchaseType: PurchaseType,
        cardType: String? = null,
        landmarkType: String? = null
    )

    fun rollDice(diceCount: Int = 1)
    fun advancePhase(gameId: Int)
    fun resolveEffects(gameId: Int)
    fun endTurn(gameId: Int)

    // Cheating accusations (#280). reportCheat silently tells the server the
    // local active player used the Insider Trading cheat; accuse bets that
    // [accusedPlayerId] cheated.
    fun reportCheat(gameId: Int)
    fun accuse(gameId: Int, accusedPlayerId: Int)
}
