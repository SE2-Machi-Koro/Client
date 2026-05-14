package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.shop.PurchaseType
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>

    val gamePhase: StateFlow<GamePhase>

    // Backend coin payload is still pending; expose the UI-ready state now for #37.
    val players: StateFlow<List<PlayerCoinState>>

    // Holds the latest created lobby code received from the server.
    // Null if no lobby has been created yet.
    val lobbyCode: StateFlow<String?>

    // Latest server game id, needed for game-scoped actions such as purchase.
    val gameId: StateFlow<Int?>
    // Fires when the server rejects the STOMP CONNECT for auth reasons (token
    // missing / invalid / server-side cleared). The UI layer is responsible for
    // calling SessionManager.signOut() and surfacing a "session expired"
    // message — kept out of the network client so transport and policy stay
    // separated.
    val authRejections: SharedFlow<Unit>

    fun connect()

    fun disconnect()

    fun sendCreateLobby()

    fun clearLobbyCode()

    fun sendGameStart()

    // Matches Server PR #216 PurchaseRequest: gameId + purchaseType + one target field.
    fun sendPurchase(
        gameId: Int,
        purchaseType: PurchaseType,
        cardType: String? = null,
        landmarkType: String? = null
    )
}
