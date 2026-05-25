package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class DummyWebSocketClient : WebSocketClient {
    override val gameStatus: kotlinx.coroutines.flow.StateFlow<com.machikoro.client.domain.enums.GameStatus?> = kotlinx.coroutines.flow.MutableStateFlow(null)
    override val roundNumber: kotlinx.coroutines.flow.StateFlow<Int?> = kotlinx.coroutines.flow.MutableStateFlow(null)
    override val playerCards: kotlinx.coroutines.flow.StateFlow<Map<Int, List<PlayerCardState>>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
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
    override val winnerId: StateFlow<Int?> = MutableStateFlow(null)
    override val diceResult: StateFlow<List<Int>?> = MutableStateFlow(null)
    override val activePlayerId: StateFlow<Int?> = MutableStateFlow(null)
    override val authRejections = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    override val lobbyJoinErrors: SharedFlow<String> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val lobbyEntered: SharedFlow<Unit> = MutableSharedFlow()
    override fun connect() {}
    override fun disconnect() {}
    override fun sendCreateLobby() {}
    override fun sendJoinLobby(lobbyCode: String) = Unit
    override fun clearLobbyCode() {}
    override fun clearGameState() {}
    override fun sendGameStart() {}
    override fun rollDice(diceCount: Int) {}
    override fun sendLeaveLobby(gameId: Int) {}
    override fun sendPurchase(
        gameId: Int,
        purchaseType: PurchaseType,
        cardType: String?,
        landmarkType: String?
    ) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun gameActionSnapshotUpdatesAuthoritativeGameState() {
        val fixture = okHttpClientFixture()

        fixture.deliverMessage(gameActionMessage())

        assertEquals(77, fixture.client.activeGameId.value)
        assertEquals(GameStatus.IN_PROGRESS, fixture.client.gameStatus.value)
        assertEquals(GamePhase.BUY_OR_BUILD, fixture.client.gamePhase.value)
        assertEquals(2, fixture.client.roundNumber.value)
        assertEquals(202, fixture.client.activePlayerId.value)
        assertEquals(listOf(8), fixture.client.diceResult.value)
        assertEquals(
            listOf(
                "11" to 1,
                "22" to 4,
            ),
            fixture.client.players.value.map { it.id to it.coins }
        )
        assertFalse(fixture.client.players.value.first { it.id == "11" }.isActivePlayer)
        assertEquals(true, fixture.client.players.value.first { it.id == "22" }.isActivePlayer)
        assertEquals(
            mapOf(11 to listOf(PlayerCardState(CardType.WHEAT_FIELD, 1))),
            fixture.client.playerCards.value
        )
        assertEquals(
            mapOf(22 to listOf(PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true))),
            fixture.client.playerLandmarks.value
        )
        assertEquals(
            mapOf(CardType.WHEAT_FIELD to 0, CardType.BAKERY to 4),
            fixture.client.marketplace.value
        )
        assertEquals(false, fixture.client.shopItems.value.first { it.type == "WHEAT_FIELD" }.isAvailable)
        assertEquals(true, fixture.client.shopItems.value.first { it.type == "BAKERY" }.isAvailable)
    }

    @Test
    fun purchaseGameActionKeepsPurchaseSuccessFeedback() = runTest {
        val fixture = okHttpClientFixture()
        val purchaseEvents = mutableListOf<PurchaseEvent>()
        fixture.client.purchaseEvents.onEach { purchaseEvents += it }.launchIn(backgroundScope)
        runCurrent()

        fixture.deliverMessage(gameActionMessage(purchaseFields = true))
        runCurrent()

        assertEquals(
            listOf(PurchaseEvent.Success(PurchaseType.ESTABLISHMENT, "BAKERY")),
            purchaseEvents
        )
        assertEquals(4, fixture.client.players.value.first { it.id == "22" }.coins)
    }

    @Test
    fun rollDiceAppliesSnapshotAndKeepsIndividualDiceResult() {
        val fixture = okHttpClientFixture()

        fixture.deliverMessage(rollDiceMessage())

        assertEquals(listOf(2, 6), fixture.client.diceResult.value)
        assertEquals(GamePhase.RESOLVE_EFFECTS, fixture.client.gamePhase.value)
        assertEquals(202, fixture.client.activePlayerId.value)
        assertEquals(4, fixture.client.players.value.first { it.id == "22" }.coins)
    }

    @Test
    fun gameEndAppliesFinalSnapshot() {
        val fixture = okHttpClientFixture()

        fixture.deliverMessage(gameEndMessage())

        assertEquals(GameStatus.FINISHED, fixture.client.gameStatus.value)
        assertEquals(202, fixture.client.activePlayerId.value)
        assertEquals(GamePhase.END_TURN, fixture.client.gamePhase.value)
    }

    @Test
    fun syncStillAppliesSnapshotAfterRefactor() {
        val fixture = okHttpClientFixture()

        fixture.deliverMessage(syncMessage())

        assertEquals(GameStatus.IN_PROGRESS, fixture.client.gameStatus.value)
        assertEquals(GamePhase.BUY_OR_BUILD, fixture.client.gamePhase.value)
        assertEquals(202, fixture.client.activePlayerId.value)
        assertEquals(mapOf(CardType.WHEAT_FIELD to 0, CardType.BAKERY to 4), fixture.client.marketplace.value)
    }

    @Test
    fun syncUsesTurnOrderUserIdWhenPlayerDatabaseIdHasSameValue() {
        val fixture = okHttpClientFixture()

        fixture.deliverMessage(syncMessage(snapshot(includeActivePlayerId = false, firstPlayerId = 202)))

        assertEquals(202, fixture.client.activePlayerId.value)
        assertFalse(fixture.client.players.value.first { it.id == "202" }.isActivePlayer)
        assertEquals(true, fixture.client.players.value.first { it.id == "22" }.isActivePlayer)
    }

    private fun okHttpClientFixture(): OkHttpClientFixture {
        val factory = CapturingWebSocketFactory()
        val sessionHolder = object : SessionStateHolder {
            override val session: StateFlow<Session?> = MutableStateFlow(
                Session(sessionToken = "token", username = "alice", userId = 101)
            )
            override fun signIn(token: String, username: String, userId: Int) = Unit
            override fun signOut() = Unit
        }
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://localhost:8080/ws",
            sessionStateHolder = sessionHolder,
            webSocketFactory = factory,
            reconnectDelaysMs = listOf(1_000L),
        )
        client.connect()
        return OkHttpClientFixture(client, factory)
    }

    private class OkHttpClientFixture(
        val client: OkHttpWebSocketClient,
        private val factory: CapturingWebSocketFactory,
    ) {
        fun deliverMessage(body: String) {
            val listener = factory.listener ?: error("WebSocket listener was not captured")
            val socket = factory.socket
            listener.onMessage(socket, StompFrame(command = "MESSAGE", body = body).serialize())
        }
    }

    private class CapturingWebSocketFactory : WebSocketFactory {
        val socket = FakeWebSocket()
        var listener: WebSocketListener? = null

        override fun create(request: Request, listener: WebSocketListener): WebSocket {
            this.listener = listener
            return socket
        }
    }

    private class FakeWebSocket : WebSocket {
        val sent = mutableListOf<String>()
        override fun request(): Request = Request.Builder().url("ws://localhost:8080/ws").build()
        override fun queueSize(): Long = 0
        override fun send(text: String): Boolean {
            sent += text
            return true
        }
        override fun send(bytes: ByteString): Boolean = true
        override fun close(code: Int, reason: String?): Boolean = true
        override fun cancel() = Unit
    }

    private fun gameActionMessage(purchaseFields: Boolean = false): String {
        val extra = if (purchaseFields) {
            ",\"purchaseType\":\"ESTABLISHMENT\",\"cardType\":\"BAKERY\""
        } else {
            ""
        }
        return """
            {
              "type":"GAME_ACTION",
              "sender":"server",
              "gameId":77,
              "payload":{
                "event":"PURCHASE_COMPLETED",
                "turnPhase":"BUY_OR_BUILD",
                "activePlayerId":202,
                "state":${snapshot(status = "IN_PROGRESS", phase = "BUY_OR_BUILD")}
                $extra
              }
            }
        """.trimIndent()
    }

    private fun rollDiceMessage(): String =
        """
            {
              "type":"ROLL_DICE",
              "sender":"server",
              "gameId":77,
              "payload":{
                "event":"DICE_ROLLED",
                "turnPhase":"RESOLVE_EFFECTS",
                "activePlayerId":202,
                "result":[2,6],
                "total":8,
                "completed":true,
                "state":${snapshot(status = "IN_PROGRESS", phase = "RESOLVE_EFFECTS")}
              }
            }
        """.trimIndent()

    private fun gameEndMessage(): String =
        """
            {
              "type":"GAME_END",
              "sender":"server",
              "gameId":77,
              "payload":{
                "winnerId":22,
                "roundsPlayed":2,
                "state":${snapshot(status = "FINISHED", phase = "END_TURN")}
              }
            }
        """.trimIndent()

    private fun syncMessage(state: String = snapshot()): String =
        """
            {
              "type":"SYNC",
              "sender":"server",
              "gameId":77,
              "payload":{
                "targetUserId":101,
                "state":$state
              }
            }
        """.trimIndent()

    private fun snapshot(
        status: String = "IN_PROGRESS",
        phase: String = "BUY_OR_BUILD",
        includeActivePlayerId: Boolean = true,
        firstPlayerId: Int = 11,
    ): String {
        val activePlayerField = if (includeActivePlayerId) ",\n              \"activePlayerId\":202" else ""
        return """
            {
              "game":{
                "id":77,
                "status":"$status",
                "hostUserId":101,
                "lobbyCode":"ABC123",
                "maxPlayers":4,
                "currentTurnIndex":1,
                "turnPhase":"$phase",
                "lastDiceRoll":8,
                "roundNumber":2,
                "hasPurchasedThisTurn":false
              },
              "players":[
                {"id":$firstPlayerId,"gameId":77,"userId":101,"turnOrder":0,"coins":1,"lastSeenAt":null},
                {"id":22,"gameId":77,"userId":202,"turnOrder":1,"coins":4,"lastSeenAt":null}
              ],
              "playerCards":{
                "$firstPlayerId":[{"playerId":$firstPlayerId,"cardType":"WHEAT_FIELD","quantity":1}]
              },
              "playerLandmarks":{
                "22":[{"playerId":22,"landmarkType":"TRAIN_STATION","isBuilt":true}]
              },
              "marketplace":{
                "WHEAT_FIELD":0,
                "BAKERY":4
              },
              "cardDefinitions":[
                {"cardType":"WHEAT_FIELD","cost":1,"color":"BLUE","establishmentType":"PRIMARY_INDUSTRY"},
                {"cardType":"BAKERY","cost":1,"color":"GREEN","establishmentType":"RESTAURANT"}
              ],
              "landmarkDefinitions":[
                {"landmarkType":"TRAIN_STATION","cost":4}
              ],
              "turnOrder":[101,202]$activePlayerField,
              "playerUsernames":{
                "$firstPlayerId":"Alice",
                "22":"Bob"
              }
            }
        """.trimIndent()
    }
}
