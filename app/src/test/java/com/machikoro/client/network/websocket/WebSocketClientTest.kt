package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.shop.ShopItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertNotNull
import org.junit.Test

class DummyWebSocketClient : WebSocketClient {
    override val gameStatus: kotlinx.coroutines.flow.StateFlow<com.machikoro.client.domain.enums.GameStatus?> = kotlinx.coroutines.flow.MutableStateFlow(null)
    override val roundNumber: kotlinx.coroutines.flow.StateFlow<Int?> = kotlinx.coroutines.flow.MutableStateFlow(null)
    override val playerLandmarks: kotlinx.coroutines.flow.StateFlow<Map<Int, List<com.machikoro.client.domain.model.state.PlayerLandmarkState>>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
    override val marketplace: kotlinx.coroutines.flow.StateFlow<Map<com.machikoro.client.domain.enums.CardType, Int>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
    override val shopItems: StateFlow<List<ShopItem>> = MutableStateFlow(emptyList())
    override val purchaseEvents: SharedFlow<PurchaseEvent> = MutableSharedFlow(extraBufferCapacity = 1)
    override val connectionStatus: StateFlow<com.machikoro.client.domain.model.state.ConnectionStatus> = MutableStateFlow(com.machikoro.client.domain.model.state.ConnectionStatus.IDLE)
    override val gamePhase: StateFlow<com.machikoro.client.domain.enums.GamePhase> = MutableStateFlow(com.machikoro.client.domain.enums.GamePhase.NONE)
    override val players: StateFlow<List<com.machikoro.client.domain.model.state.PlayerCoinState>> = MutableStateFlow(emptyList())
    override val lobbyCode: StateFlow<String?> = MutableStateFlow(null)
    override val activeGameId: StateFlow<Int?> = MutableStateFlow(null)
    override val isLobbyHost: StateFlow<Boolean> = MutableStateFlow(false)
    override val diceResult: StateFlow<List<Int>?> = MutableStateFlow(null)
    override val activePlayerId: StateFlow<Int?> = MutableStateFlow(null)
    override val authRejections = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    override fun connect() {}
    override fun disconnect() {}
    override fun sendCreateLobby() {}
    override fun sendJoinLobby(lobbyCode: String) = Unit
    override fun clearLobbyCode() {}
    override fun sendGameStart() {}
    override fun rollDice(diceCount: Int) {}
    override fun sendPurchase(
        gameId: Int,
        purchaseType: PurchaseType,
        cardType: String?,
        landmarkType: String?
    ) {}
}

class WebSocketClientTest {
    @Test
    fun testDummyImplementation() {
        val client = DummyWebSocketClient()
        assertNotNull(client.connectionStatus)
        assertNotNull(client.gamePhase)
        assertNotNull(client.players)
        assertNotNull(client.lobbyCode)
        client.connect()
        client.disconnect()
        client.sendCreateLobby()
        client.clearLobbyCode()
        client.sendGameStart()
    }
}
