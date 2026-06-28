package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.ChatMessageState
import com.machikoro.client.network.error.ClientError
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    override val diceRollTick: StateFlow<Long> = MutableStateFlow(0L)
    override val activePlayerId: StateFlow<Int?> = MutableStateFlow(null)
    override val authRejections = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    override val lobbyJoinErrors: SharedFlow<ClientError.WebSocket> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val hostLeftLobby: SharedFlow<Unit> = MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val lobbyEntered: SharedFlow<Unit> = MutableSharedFlow()
    override val accusationResults: SharedFlow<com.machikoro.client.domain.model.state.AccusationResult> = MutableSharedFlow(extraBufferCapacity = 1)
    override val coinDeltas: SharedFlow<Int> = MutableSharedFlow(extraBufferCapacity = 1)
    override val accusationErrors: SharedFlow<String> = MutableSharedFlow(extraBufferCapacity = 1)
    override val domainErrors: SharedFlow<ClientError.WebSocket> = MutableSharedFlow(extraBufferCapacity = 1)
    override val chatMessages: SharedFlow<ChatMessageState> = MutableSharedFlow(extraBufferCapacity = 1)

    override fun connect() {}
    override fun disconnect() {}
    override fun sendCreateLobby() {}
    override fun sendJoinLobby(lobbyCode: String) = Unit
    override fun clearLobbyCode() {}
    override fun clearGameState() {}
    override fun sendGameStart() {}
    override fun rollDice(diceCount: Int) {}
    override fun rerollDice(diceCount: Int) {}
    override fun advancePhase(gameId: Int) {}
    override fun resolveEffects(gameId: Int) {}
    override fun endTurn(gameId: Int) {}
    override fun reportCheat(gameId: Int) {}
    override fun accuse(gameId: Int, accusedPlayerId: Int) {}
    override fun sendLeaveLobby(gameId: Int) {}
    override fun sendReadyToggle(isReady: Boolean) {}
    override fun sendChatMessage(gameId: Int, message: String) {}
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
        assertEquals(true, fixture.client.players.value.first { it.id == "11" }.isCurrentPlayer)
        assertEquals(true, fixture.client.players.value.first { it.id == "22" }.isActivePlayer)
        assertFalse(fixture.client.players.value.first { it.id == "22" }.isCurrentPlayer)
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
        val bakery = fixture.client.shopItems.value.first { it.type == "BAKERY" }
        assertEquals(true, bakery.isAvailable)
        assertEquals("2-3", bakery.activationText)
        assertEquals("Get 1 coin from the bank on your turn.", bakery.effectText)
        val trainStation = fixture.client.shopItems.value.first { it.type == "TRAIN_STATION" }
        assertEquals("Permanent", trainStation.activationText)
        assertEquals("You may roll one or two dice.", trainStation.effectText)
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
    fun effectsResolvedEmitsLocalPlayerCoinGain() = runTest {
        // Local userId 101 maps to playerId 11 in the snapshot roster.
        val fixture = okHttpClientFixture()
        val deltas = mutableListOf<Int>()
        fixture.client.coinDeltas.onEach { deltas += it }.launchIn(backgroundScope)
        runCurrent()

        fixture.deliverMessage(effectsResolvedMessage("""{"11":3,"22":-3}"""))
        runCurrent()

        assertEquals(listOf(3), deltas)
    }

    @Test
    fun effectsResolvedEmitsLocalPlayerCoinLoss() = runTest {
        val fixture = okHttpClientFixture()
        val deltas = mutableListOf<Int>()
        fixture.client.coinDeltas.onEach { deltas += it }.launchIn(backgroundScope)
        runCurrent()

        fixture.deliverMessage(effectsResolvedMessage("""{"11":-2}"""))
        runCurrent()

        assertEquals(listOf(-2), deltas)
    }

    @Test
    fun effectsResolvedWithoutLocalDeltaEmitsNothing() = runTest {
        val fixture = okHttpClientFixture()
        val deltas = mutableListOf<Int>()
        fixture.client.coinDeltas.onEach { deltas += it }.launchIn(backgroundScope)
        runCurrent()

        // Only an opponent's balance changed — the local player hears nothing.
        fixture.deliverMessage(effectsResolvedMessage("""{"22":5}"""))
        runCurrent()

        assertEquals(emptyList<Int>(), deltas)
    }

    @Test
    fun accusationResultEmitsOutcomeWithAccuserPlayerId() = runTest {
        val fixture = okHttpClientFixture()
        val results = mutableListOf<com.machikoro.client.domain.model.state.AccusationResult>()
        fixture.client.accusationResults.onEach { results += it }.launchIn(backgroundScope)
        runCurrent()

        fixture.deliverMessage(accusationResultMessage())
        runCurrent()

        assertEquals(1, results.size)
        val result = results.first()
        assertEquals(true, result.caught)
        assertEquals(11, result.accuserId)
        assertEquals(2, result.penaltyCoins)
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
    fun laterSnapshotDoesNotCollapsePerDiceResultIntoTotal() {
        val fixture = okHttpClientFixture()

        // Live roll gives the individual dice (2 + 6 = 8).
        fixture.deliverMessage(rollDiceMessage())
        assertEquals(listOf(2, 6), fixture.client.diceResult.value)

        // A routine snapshot only carries the total (lastDiceRoll:8). It must not
        // overwrite the richer per-die result for the same roll, or the dice display
        // would flip from two dice to one generic die and hide doubles.
        fixture.deliverMessage(gameActionMessage())

        assertEquals(listOf(2, 6), fixture.client.diceResult.value)
    }

    @Test
    fun rollDiceSnapshotClearsDiceResultSoRollButtonUnblocks() {
        val fixture = okHttpClientFixture()

        // Simulate a prior roll being in state (e.g. from the previous turn's snapshot).
        fixture.deliverMessage(rollDiceMessage())
        assertEquals(listOf(2, 6), fixture.client.diceResult.value)

        // A GAME_STARTED (or any snapshot) with turnPhase=ROLL_DICE means a new turn has
        // started and no dice have been rolled yet. lastDiceRoll is a stale previous-turn
        // total — restoring it would hide the Roll button and freeze the game.
        fixture.deliverMessage(gameStartedMessage())

        assertNull(fixture.client.diceResult.value)
        assertEquals(GamePhase.ROLL_DICE, fixture.client.gamePhase.value)
    }

    @Test
    fun gameEndAppliesFinalSnapshot() {
        val fixture = okHttpClientFixture()

        fixture.deliverMessage(gameEndMessage())

        assertEquals(GameStatus.FINISHED, fixture.client.gameStatus.value)
        assertEquals(202, fixture.client.activePlayerId.value)
        assertEquals(GamePhase.NONE, fixture.client.gamePhase.value)
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
        assertEquals(true, fixture.client.players.value.first { it.id == "202" }.isCurrentPlayer)
        assertEquals(true, fixture.client.players.value.first { it.id == "22" }.isActivePlayer)
        assertFalse(fixture.client.players.value.first { it.id == "22" }.isCurrentPlayer)
    }

    @Test
    fun shopDefinitionsPreferServerProvidedActivationAndEffectText() {
        val fixture = okHttpClientFixture()

        fixture.deliverMessage(syncMessage(snapshotWithDetailedShopDefinitions()))

        val bakery = fixture.client.shopItems.value.first { it.type == "BAKERY" }
        assertEquals("2-3", bakery.activationText)
        assertEquals("Server bakery effect", bakery.effectText)
        val trainStation = fixture.client.shopItems.value.first { it.type == "TRAIN_STATION" }
        assertEquals("Permanent", trainStation.activationText)
        assertEquals("Server train station effect", trainStation.effectText)
    }

    @Test
    fun twoClientsApplySameGameStartedBroadcast() {
        val playerA = okHttpClientFixture(userId = 101)
        val playerB = okHttpClientFixture(userId = 202)
        val started = gameStartedMessage()

        playerA.deliverMessage(started)
        playerB.deliverMessage(started)

        assertEquals(77, playerA.client.activeGameId.value)
        assertEquals(77, playerB.client.activeGameId.value)
        assertEquals(GameStatus.IN_PROGRESS, playerA.client.gameStatus.value)
        assertEquals(GameStatus.IN_PROGRESS, playerB.client.gameStatus.value)
        assertEquals(GamePhase.ROLL_DICE, playerA.client.gamePhase.value)
        assertEquals(GamePhase.ROLL_DICE, playerB.client.gamePhase.value)
        assertEquals(2, playerA.client.roundNumber.value)
        assertEquals(2, playerB.client.roundNumber.value)
        assertEquals(202, playerA.client.activePlayerId.value)
        assertEquals(202, playerB.client.activePlayerId.value)
        assertEquals(
            listOf(
                Triple("11", "Alice", 1),
                Triple("22", "Bob", 4),
            ),
            playerA.client.players.value.map { Triple(it.id, it.displayName, it.coins) }
        )
        assertEquals(
            playerA.client.players.value.map { Triple(it.id, it.displayName, it.coins) },
            playerB.client.players.value.map { Triple(it.id, it.displayName, it.coins) }
        )
        assertFalse(playerA.client.players.value.first { it.id == "11" }.isActivePlayer)
        assertEquals(true, playerA.client.players.value.first { it.id == "22" }.isActivePlayer)
        assertEquals(
            mapOf(11 to listOf(PlayerCardState(CardType.WHEAT_FIELD, 1))),
            playerA.client.playerCards.value
        )
        assertEquals(
            mapOf(22 to listOf(PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true))),
            playerA.client.playerLandmarks.value
        )
        assertEquals(
            mapOf(CardType.WHEAT_FIELD to 0, CardType.BAKERY to 4),
            playerA.client.marketplace.value
        )
        assertEquals(false, playerA.client.shopItems.value.first { it.type == "WHEAT_FIELD" }.isAvailable)
        val bakery = playerA.client.shopItems.value.first { it.type == "BAKERY" }
        assertEquals(PurchaseType.ESTABLISHMENT, bakery.purchaseType)
        assertEquals("Bakery", bakery.displayName)
        assertEquals(1, bakery.cost)
        assertEquals(true, bakery.isAvailable)
        val trainStation = playerA.client.shopItems.value.first { it.type == "TRAIN_STATION" }
        assertEquals(PurchaseType.LANDMARK, trainStation.purchaseType)
        assertEquals("Train Station", trainStation.displayName)
        assertEquals(4, trainStation.cost)
    }

    @Test
    fun actionSentByOneClientUpdatesOtherClientFromBroadcast() {
        val playerA = okHttpClientFixture(userId = 101)
        val playerB = okHttpClientFixture(userId = 202)

        playerA.client.advancePhase(gameId = 77)
        playerB.deliverMessage(gameActionMessage())

        assertEquals(
            WebSocketContract.advancePhaseDestination,
            playerA.sentFrames().last().headers["destination"]
        )
        assertEquals(GamePhase.BUY_OR_BUILD, playerB.client.gamePhase.value)
        assertEquals(202, playerB.client.activePlayerId.value)
        assertEquals(4, playerB.client.players.value.first { it.id == "22" }.coins)
    }

    @Test
    fun rollDiceBroadcastUpdatesNonSenderClient() {
        val playerB = okHttpClientFixture(userId = 202)

        playerB.deliverMessage(rollDiceMessage())

        assertEquals(listOf(2, 6), playerB.client.diceResult.value)
        assertEquals(GamePhase.RESOLVE_EFFECTS, playerB.client.gamePhase.value)
        assertEquals(202, playerB.client.activePlayerId.value)
    }

    @Test
    fun purchaseBroadcastUpdatesNonSenderSnapshotAndPurchaseEvent() = runTest {
        val playerB = okHttpClientFixture(userId = 202)
        val purchaseEvents = mutableListOf<PurchaseEvent>()
        playerB.client.purchaseEvents.onEach { purchaseEvents += it }.launchIn(backgroundScope)
        runCurrent()

        playerB.deliverMessage(gameActionMessage(purchaseFields = true))
        runCurrent()

        assertEquals(
            listOf(PurchaseEvent.Success(PurchaseType.ESTABLISHMENT, "BAKERY")),
            purchaseEvents
        )
        assertEquals(mapOf(CardType.WHEAT_FIELD to 0, CardType.BAKERY to 4), playerB.client.marketplace.value)
        assertEquals(4, playerB.client.players.value.first { it.id == "22" }.coins)
    }

    @Test
    fun reconnectSyncRestoresNonSenderClient() {
        val playerB = okHttpClientFixture(userId = 202)

        playerB.deliverMessage(syncMessage())

        assertEquals(GameStatus.IN_PROGRESS, playerB.client.gameStatus.value)
        assertEquals(GamePhase.BUY_OR_BUILD, playerB.client.gamePhase.value)
        assertEquals(mapOf(CardType.WHEAT_FIELD to 0, CardType.BAKERY to 4), playerB.client.marketplace.value)
        assertEquals(202, playerB.client.activePlayerId.value)
    }
    @Test
    fun fakeClientSendChatRecordsLastSentAndEchoesIntoFlow() = runTest {
        val fake = FakeWebSocketClient()
        val received = mutableListOf<ChatMessageState>()
        fake.chatMessages.onEach { received += it }.launchIn(backgroundScope)
        runCurrent()

        fake.sendChatMessage(gameId = 42, message = "test-msg")

        assertEquals(Pair(42, "test-msg"), fake.lastSentChat)
        // Fake implementation echoes a ChatMessageState(sender="test", message=message)
        runCurrent()
        assertTrue(received.any { it.message == "test-msg" })
    }

    private fun okHttpClientFixture(userId: Int = 101): OkHttpClientFixture {
        val factory = CapturingWebSocketFactory()
        val sessionHolder = object : SessionStateHolder {
            override val session: StateFlow<Session?> = MutableStateFlow(
                Session(sessionToken = "token", username = "user-$userId", userId = userId)
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

        fun sentFrames(): List<StompFrame> =
            factory.socket.sent.flatMap { parseFrames(StringBuilder(it)) }
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

    private fun effectsResolvedMessage(coinDeltas: String): String =
        """
            {
              "type":"GAME_ACTION",
              "sender":"server",
              "gameId":77,
              "payload":{
                "event":"EFFECTS_RESOLVED",
                "turnPhase":"BUY_OR_BUILD",
                "activePlayerId":202,
                "coinDeltas":$coinDeltas,
                "state":${snapshot(status = "IN_PROGRESS", phase = "BUY_OR_BUILD")}
              }
            }
        """.trimIndent()

    private fun accusationResultMessage(): String =
        """
            {
              "type":"ACCUSATION_RESULT",
              "sender":"server",
              "gameId":77,
              "payload":{
                "accuserPlayerId":11,
                "accusedPlayerId":22,
                "caught":true,
                "penalizedPlayerId":22,
                "penaltyCoins":2,
                "state":${snapshot(status = "IN_PROGRESS", phase = "BUY_OR_BUILD")}
              }
            }
        """.trimIndent()

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

    private fun gameStartedMessage(): String =
        """
            {
              "type":"GAME_STARTED",
              "sender":"server",
              "gameId":77,
              "payload":${snapshot(status = "IN_PROGRESS", phase = "ROLL_DICE")}
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

    private fun snapshotWithDetailedShopDefinitions(): String =
        """
            {
              "game":{
                "id":77,
                "status":"IN_PROGRESS",
                "hostUserId":101,
                "lobbyCode":"ABC123",
                "maxPlayers":4,
                "currentTurnIndex":1,
                "turnPhase":"BUY_OR_BUILD",
                "lastDiceRoll":8,
                "roundNumber":2,
                "hasPurchasedThisTurn":false
              },
              "players":[
                {"id":11,"gameId":77,"userId":101,"turnOrder":0,"coins":1,"lastSeenAt":null},
                {"id":22,"gameId":77,"userId":202,"turnOrder":1,"coins":4,"lastSeenAt":null}
              ],
              "marketplace":{
                "BAKERY":4
              },
              "cardDefinitions":[
                {
                  "cardType":"BAKERY",
                  "cost":1,
                  "color":"GREEN",
                  "establishmentType":"RESTAURANT",
                  "activationNumbers":[2,3],
                  "effectDescription":"Server bakery effect"
                }
              ],
              "landmarkDefinitions":[
                {
                  "landmarkType":"TRAIN_STATION",
                  "cost":4,
                  "description":"Server train station effect"
                }
              ],
              "turnOrder":[101,202],
              "activePlayerId":202
            }
        """.trimIndent()
}
