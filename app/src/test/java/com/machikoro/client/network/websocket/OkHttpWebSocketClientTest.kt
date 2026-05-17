package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OkHttpWebSocketClientTest {
    @Test
    fun connectMovesStatusToConnecting() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        assertEquals(ConnectionStatus.CONNECTING, client.connectionStatus.value)
    }

    @Test
    fun openSendsStompConnectFrame() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        assertTrue(factory.socket.sentMessages.first().startsWith("CONNECT\n"))
        assertTrue(factory.socket.sentMessages.first().contains("accept-version:1.2"))
        assertTrue(factory.socket.sentMessages.first().contains("host:10.0.2.2:8080"))
    }

    @Test
    fun connectedFrameMovesStatusToConnectedAndTriggersSubscribeAndJoin() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        assertEquals(ConnectionStatus.CONNECTED, client.connectionStatus.value)
        assertTrue(factory.socket.sentMessages.any { it.startsWith("SUBSCRIBE\n") && it.contains("destination:/topic/public") })
        assertTrue(factory.socket.sentMessages.any { it.startsWith("SEND\n") && it.contains("destination:/app/chat.addUser") })
        assertTrue(factory.socket.sentMessages.any { it.contains("\"type\":\"JOIN\"") })
    }

    @Test
    fun disconnectClosesSocketAndMovesStatusToDisconnected() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        client.disconnect()
        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
        assertTrue(factory.socket.closed)
        assertTrue(factory.socket.sentMessages.any { it.startsWith("DISCONNECT\n") })
    }

    @Test
    fun failureMovesStatusToError() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateFailure(IOException("boom"))
        assertEquals(ConnectionStatus.ERROR, client.connectionStatus.value)
    }

    @Test
    fun closingMovesStatusToDisconnected() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateClosing()
        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
        assertTrue(factory.socket.closed)
    }

    @Test
    fun closedMovesStatusToDisconnected() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateClosed()
        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
    }

    @Test
    fun secondConnectDoesNotCreateAnotherSocket() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        client.connect()
        assertEquals(1, factory.createCount)
    }

    @Test
    fun invalidUrlMovesStatusToError() {
        val client = OkHttpWebSocketClient(
            websocketUrl = "not-a-url",
            sessionStateHolder = FakeSessionStateHolder(DEFAULT_SESSION),
        )
        client.connect()
        assertEquals(ConnectionStatus.ERROR, client.connectionStatus.value)
    }

    @Test
    fun gamePhaseStartsAsNone() {
        val client = newClient(FakeWebSocketFactory())
        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun playersStartEmpty() {
        val client = newClient(FakeWebSocketFactory())
        assertEquals(emptyList<PlayerCoinState>(), client.players.value)
    }

    @Test
    fun gameActionMessageUpdatesGamePhase() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"GAME_ACTION","sender":"server","payload":{"turnPhase":"ROLL_DICE"}}"""))
        assertEquals(GamePhase.ROLL_DICE, client.gamePhase.value)
    }

    @Test
    fun gameActionMessagesAdvanceThroughAllPhases() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")

        listOf(
            GamePhase.ROLL_DICE,
            GamePhase.RESOLVE_EFFECTS,
            GamePhase.BUY_OR_BUILD,
            GamePhase.END_TURN
        ).forEach { phase ->
            factory.simulateText(
                gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"${phase.name}"}}""")
            )
            assertEquals(phase, client.gamePhase.value)
        }
    }

    @Test
    fun nonGameActionMessageDoesNotChangeGamePhase() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(
            gameActionFrame("""{"type":"CHAT","sender":"someone","content":"hello"}""")
        )

        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun messageWithoutCoinPayloadLeavesPlayersUnchanged() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"ROLL_DICE"}}""")
        )

        assertEquals(emptyList<PlayerCoinState>(), client.players.value)
    }

    @Test
    fun malformedJsonMessageDoesNotCrashAndLeavesGamePhaseUnchanged() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("not even json"))
        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun gameActionWithoutPayloadLeavesGamePhaseUnchanged() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_ACTION","sender":"server"}""")
        )

        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun gameActionWithUnknownTurnPhaseLeavesGamePhaseUnchanged() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"NOT_A_PHASE"}}""")
        )

        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun gameActionWithMissingTurnPhaseLeavesGamePhaseUnchanged() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"GAME_ACTION","payload":{"other":"value"}}"""))
        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun disconnectResetsGamePhaseToNone() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"BUY_OR_BUILD"}}"""))
        assertEquals(GamePhase.BUY_OR_BUILD, client.gamePhase.value)
        client.disconnect()
        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun closingResetsGamePhaseToNone() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"RESOLVE_EFFECTS"}}"""))
        assertEquals(GamePhase.RESOLVE_EFFECTS, client.gamePhase.value)
        factory.simulateClosing()
        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun closedResetsGamePhaseToNone() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"END_TURN"}}"""))
        assertEquals(GamePhase.END_TURN, client.gamePhase.value)
        factory.simulateClosed()
        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun failureResetsGamePhaseToNone() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"ROLL_DICE"}}"""))
        assertEquals(GamePhase.ROLL_DICE, client.gamePhase.value)
        factory.simulateFailure(IOException("boom"))
        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun sendGameStartSendsStompFrameToGameStartDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        client.sendGameStart()
        assertTrue(factory.socket.sentMessages.any { it.startsWith("SEND\n") && it.contains("destination:/app/game.start") })
    }

    @Test
    fun sendGameStartWithoutConnectionIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        // No connect() call — should not throw
        client.sendGameStart()

        assertTrue(factory.socket.sentMessages.isEmpty())
    }

    @Test
    fun connectWithNoSessionDoesNotOpenSocketAndDoesNotTransitionStatus() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory, sessionStateHolder = FakeSessionStateHolder(initial = null))
        client.connect()
        assertEquals(ConnectionStatus.IDLE, client.connectionStatus.value)
        assertEquals(0, factory.createCount)
    }

    @Test
    fun disconnectWhenNeverConnectedIsNoOpAndDoesNotTransitionStatus() {
        // Important for the LaunchedEffect in MainActivity: on cold start with no
        // session, the initial collect emission is null and triggers disconnect().
        // If disconnect() flipped status from IDLE to DISCONNECTED, the start
        // screen would render "Connection status: disconnected" before the user
        // has tried to connect.
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.disconnect()

        assertEquals(ConnectionStatus.IDLE, client.connectionStatus.value)
        assertEquals(0, factory.createCount)
    }

    @Test
    fun connectFrameIncludesAuthorizationBearerHeaderWhenSessionPresent() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        val connectFrame = factory.socket.sentMessages.first { it.startsWith("CONNECT\n") }
        assertTrue(connectFrame.contains("Authorization:Bearer $DEFAULT_TOKEN"))
    }

    @Test
    fun connectFrameUsesCurrentSessionTokenAtHandshakeTime() {
        // Locks down "read token at onOpen, not at connect()" — if we ever capture
        // the token at connect() time, mid-flight session changes would send the
        // wrong header.
        val factory = FakeWebSocketFactory()
        val sessionHolder = FakeSessionStateHolder(initial = Session("stale-token", "alice", DEFAULT_USER_ID))
        val client = newClient(factory, sessionStateHolder = sessionHolder)

        client.connect()
        sessionHolder.signIn(token = "fresh-token", username = "alice", userId = DEFAULT_USER_ID)
        factory.simulateOpen()

        val connectFrame = factory.socket.sentMessages.first { it.startsWith("CONNECT\n") }
        assertTrue(connectFrame.contains("Authorization:Bearer fresh-token"))
        assertFalse(connectFrame.contains("stale-token"))
    }

    @Test
    fun handshakeClosesCleanlyIfSessionVanishedBetweenConnectAndOnOpen() {
        val factory = FakeWebSocketFactory()
        val sessionHolder = FakeSessionStateHolder(initial = DEFAULT_SESSION)
        val client = newClient(factory, sessionStateHolder = sessionHolder)

        client.connect()
        sessionHolder.signOut()  // user logs out before WS handshake completes
        factory.simulateOpen()

        assertTrue(factory.socket.closed)
        assertFalse(factory.socket.sentMessages.any { it.startsWith("CONNECT\n") })
        // Belt-and-braces — there should be no Authorization header in any frame.
        assertFalse(factory.socket.sentMessages.any { it.contains("Authorization") })
    }

    @Test
    fun lobbyCodeStartsAsNull() {
        val client = newClient(FakeWebSocketFactory())
        assertEquals(null, client.lobbyCode.value)
    }

    @Test
    fun sendCreateLobbySendsStompFrameToCreateLobbyDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")

        client.sendCreateLobby()

        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SEND\n") &&
                        it.contains("destination:/app/lobby.create") &&
                        it.contains("\"type\":\"JOIN\"")
            }
        )
    }

    @Test
    fun sendCreateLobbyWithoutConnectionIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.sendCreateLobby()
        assertTrue(factory.socket.sentMessages.isEmpty())
    }

    @Test
    fun lobbyCreatedMessageUpdatesLobbyCode() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")

        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"AJ25Z39"}}"""
            )
        )

        assertEquals("AJ25Z39", client.lobbyCode.value)
    }

    @Test
    fun lobbyJoinErrorWithoutContentUsesFallbackMessage() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val errors = mutableListOf<String>()

        client.lobbyJoinErrors.onEach { errors += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            StompFrame(
                command = "MESSAGE",
                headers = mapOf(
                    "destination" to WebSocketContract.errorsQueue,
                    "content-type" to "application/json"
                ),
                body = """{"type":"ERROR","sender":"SERVER","payload":{"errorCode":"LOBBY_FULL"}}"""
            ).serialize()
        )

        runCurrent()

        assertEquals(listOf("Failed to join lobby"), errors)
    }

    @Test
    fun allLobbyJoinErrorCodesEmitLobbyJoinError() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val errors = mutableListOf<String>()

        client.lobbyJoinErrors.onEach { errors += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        listOf(
            "INVALID_LOBBY_CODE",
            "GAME_NOT_FOUND",
            "GAME_STARTED",
            "GAME_FINISHED",
            "LOBBY_FULL",
        ).forEach { errorCode ->
            factory.simulateText(
                StompFrame(
                    command = "MESSAGE",
                    headers = mapOf(
                        "destination" to WebSocketContract.errorsQueue,
                        "content-type" to "application/json"
                    ),
                    body = """{"type":"ERROR","sender":"SERVER","content":"Could not join lobby","payload":{"errorCode":"$errorCode"}}"""
                ).serialize()
            )
            runCurrent()
        }

        assertEquals(5, errors.size)
    }

    @Test
    fun nonLobbyJoinErrorCodeDoesNotEmitLobbyJoinError() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val errors = mutableListOf<String>()

        client.lobbyJoinErrors.onEach { errors += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            StompFrame(
                command = "MESSAGE",
                headers = mapOf(
                    "destination" to WebSocketContract.errorsQueue,
                    "content-type" to "application/json"
                ),
                body = """{"type":"ERROR","sender":"SERVER","content":"Other error","payload":{"errorCode":"SOMETHING_ELSE"}}"""
            ).serialize()
        )

        runCurrent()

        assertTrue(errors.isEmpty())
    }

    @Test
    fun lobbyCreatedMessageUpdatesActiveGameId() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            gameActionFrame(
                """{
                "type":"LOBBY_CREATED",
                "gameId":42,
                "payload":{
                    "lobbyCode":"ABC1234",
                    "gameId":42
                }
            }"""
            )
        )

        assertEquals(42, client.activeGameId.value)

        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SUBSCRIBE\n") &&
                        it.contains("/topic/game/42")
            }
        )
    }

    @Test
    fun malformedLobbyCreatedMessageDoesNotCrashAndLeavesLobbyCodeNull() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("not json"))
        assertEquals(null, client.lobbyCode.value)
    }

    @Test
    fun lobbyCreatedWithoutPayloadLeavesLobbyCodeNull() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"LOBBY_CREATED","sender":"SERVER"}"""))
        assertEquals(null, client.lobbyCode.value)
    }

    @Test
    fun disconnectWithoutActiveSocketAndNoSessionResetsLobbyState() {
        val factory = FakeWebSocketFactory()
        val sessionHolder = FakeSessionStateHolder(initial = null)
        val client = newClient(factory, sessionStateHolder = sessionHolder)

        client.disconnect()

        assertNull(client.lobbyCode.value)
        assertEquals(0, factory.createCount)
    }

    @Test
    fun clearLobbyCodeResetsLobbyState() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.clearLobbyCode()

        assertNull(client.lobbyCode.value)
    }

    @Test
    fun gameStartedMessageUpdatesGameState() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            gameActionFrame(
                """{
                "type":"GAME_STARTED",
                "gameId":42,
                "payload":{
                    "game":{
                        "id":42,
                        "lobbyCode":"ABC1234",
                        "turnPhase":"ROLL_DICE"
                    },
                    "players":[]
                }
            }"""
            )
        )

        assertEquals(42, client.activeGameId.value)
        assertEquals("ABC1234", client.lobbyCode.value)
    }

    @Test
    fun rollDiceSendsStompFrameToRollDiceDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        client.rollDice(diceCount = 1)
        assertTrue(factory.socket.sentMessages.any {
            it.startsWith("SEND\n") && it.contains("destination:/app/game.rollDice") && it.contains("\"diceCount\":1")
        })
    }

    @Test
    fun rollDiceWithoutConnectionIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.rollDice(diceCount = 1)

        assertTrue(factory.socket.sentMessages.isEmpty())
    }


    @Test
    fun sendPurchaseEstablishmentSendsServerAlignedPayload() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        client.sendPurchase(
            gameId = 7,
            purchaseType = PurchaseType.ESTABLISHMENT,
            cardType = "BAKERY",
            landmarkType = null
        )

        val purchaseFrame = factory.socket.sentMessages.last { it.startsWith("SEND\n") }
        assertTrue(purchaseFrame.contains("destination:/app/game.purchase"))
        assertTrue(purchaseFrame.contains("\"gameId\":7"))
        assertTrue(purchaseFrame.contains("\"purchaseType\":\"ESTABLISHMENT\""))
        assertTrue(purchaseFrame.contains("\"cardType\":\"BAKERY\""))
        assertFalse(purchaseFrame.contains("landmarkType"))
    }

    @Test
    fun sendPurchaseLandmarkSendsServerAlignedPayload() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        client.sendPurchase(
            gameId = 7,
            purchaseType = PurchaseType.LANDMARK,
            cardType = null,
            landmarkType = "TRAIN_STATION"
        )

        val purchaseFrame = factory.socket.sentMessages.last { it.startsWith("SEND\n") }
        assertTrue(purchaseFrame.contains("destination:/app/game.purchase"))
        assertTrue(purchaseFrame.contains("\"gameId\":7"))
        assertTrue(purchaseFrame.contains("\"purchaseType\":\"LANDMARK\""))
        assertTrue(purchaseFrame.contains("\"landmarkType\":\"TRAIN_STATION\""))
        assertFalse(purchaseFrame.contains("cardType"))
    }

    @Test
    fun sendPurchaseWithoutConnectionIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.sendPurchase(
            gameId = 7,
            purchaseType = PurchaseType.ESTABLISHMENT,
            cardType = "BAKERY",
            landmarkType = null
        )

        assertTrue(factory.socket.sentMessages.isEmpty())
    }

    @Test
    fun lobbyJoinedMessageUpdatesActiveGameIdAndClearsHostFlag() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","sender":"SERVER","gameId":42,"payload":{"gameId":42}}"""
            )
        )

        assertEquals(42, client.activeGameId.value)
        assertFalse(client.isLobbyHost.value)
    }

    @Test
    fun invalidLobbyCodeErrorEmitsLobbyJoinError() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val errors = mutableListOf<String>()

        client.lobbyJoinErrors.onEach { errors += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            gameActionFrame(
                """{"type":"ERROR","sender":"SERVER","content":"Lobby code is invalid","payload":{"errorCode":"INVALID_LOBBY_CODE"}}"""
            )
        )

        runCurrent()

        assertEquals(listOf("Lobby code is invalid"), errors)
    }

    @Test
    fun sendJoinLobbySendsStompFrameToJoinLobbyDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        client.sendJoinLobby("ABC1234")

        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SEND\n") &&
                        it.contains("destination:${WebSocketContract.joinLobbyDestination}") &&
                        it.contains("\"type\":\"JOIN\"") &&
                        it.contains("\"lobbyCode\":\"ABC1234\"")
            }
        )
    }

    @Test
    fun connectedFrameSubscribesToErrorsQueue() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SUBSCRIBE\n") &&
                        it.contains("destination:${WebSocketContract.errorsQueue}")
            }
        )
    }

    @Test
    fun gameStartedLobbyErrorEmitsLobbyJoinError() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val errors = mutableListOf<String>()

        client.lobbyJoinErrors.onEach { errors += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            StompFrame(
                command = "MESSAGE",
                headers = mapOf(
                    "destination" to WebSocketContract.errorsQueue,
                    "content-type" to "application/json"
                ),
                body = """{"type":"ERROR","sender":"SERVER","content":"Could not join lobby","payload":{"errorCode":"GAME_STARTED"}}"""
            ).serialize()
        )

        runCurrent()

        assertEquals(listOf("Could not join lobby"), errors)
    }

    /*
    @Test
    fun lobbyJoinRelatedErrorCodesEmitLobbyJoinError() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val errors = mutableListOf<String>()

        client.lobbyJoinErrors.onEach { errors += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        listOf("GAME_NOT_FOUND", "GAME_STARTED", "GAME_FINISHED", "LOBBY_FULL").forEach { errorCode ->
            factory.simulateText(
                StompFrame(
                    command = "MESSAGE",
                    headers = mapOf(
                        "destination" to WebSocketContract.errorsQueue,
                        "content-type" to "application/json"
                    ),
                    body = """{"type":"ERROR","sender":"SERVER","content":"Could not join lobby","payload":{"errorCode":"$errorCode"}}"""
                ).serialize()
            )
        }

        runCurrent()

        assertEquals(
            listOf(
                "Could not join lobby",
                "Could not join lobby",
                "Could not join lobby",
                "Could not join lobby"
            ),
            errors
        )
    }*/

        
    @Test
    fun malformedPurchasePayloadDoesNotEmitPurchaseSuccessEvent() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val purchaseEvents = mutableListOf<PurchaseEvent>()
        client.purchaseEvents.onEach { purchaseEvents += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"GAME_ACTION","payload":{"turnPhase":"BUY_OR_BUILD","purchaseType":"ESTABLISHMENT"}}"""
            )
        )
        runCurrent()

        assertTrue(purchaseEvents.isEmpty())
        assertEquals(GamePhase.BUY_OR_BUILD, client.gamePhase.value)
     }

    @Test
    fun gameActionWithPurchasePayloadEmitsPurchaseSuccessEvent() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val purchaseEvents = mutableListOf<PurchaseEvent>()

        client.purchaseEvents.onEach { purchaseEvents += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"GAME_ACTION","payload":{"turnPhase":"BUY_OR_BUILD","purchaseType":"ESTABLISHMENT","cardType":"BAKERY"}}"""
            )
        )
        runCurrent()

        assertEquals(
            listOf(PurchaseEvent.Success(PurchaseType.ESTABLISHMENT, "BAKERY")),
            purchaseEvents
        )
        assertEquals(GamePhase.BUY_OR_BUILD, client.gamePhase.value)
    }

    @Test
    fun sendJoinLobbyWithoutConnectionIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.sendJoinLobby("ABC1234")

        assertTrue(factory.socket.sentMessages.isEmpty())
    }

    @Test
    fun rollDiceWithTwoDiceSendsDiceCountTwo() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        client.rollDice(diceCount = 2)
        assertTrue(factory.socket.sentMessages.any { it.contains("\"diceCount\":2") })
    }

    @Test
    fun rollDiceMessageFromServerUpdatesDiceResult() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"ROLL_DICE","payload":{"playerId":"p1","result":[3,5],"timestamp":123}}"""))
        assertEquals(listOf(3, 5), client.diceResult.value)
    }

    @Test
    fun disconnectResetsDiceResultToNull() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"ROLL_DICE","payload":{"playerId":"p1","result":[6],"timestamp":1}}"""))
        assertEquals(listOf(6), client.diceResult.value)
        client.disconnect()
        assertNull(client.diceResult.value)
    }

    @Test
    fun stompErrorFrameWithAuthFailureBodyEmitsAuthRejectionAndDisconnects() = runTest {
        val factory = FakeWebSocketFactory()
        val sessionHolder = FakeSessionStateHolder(initial = DEFAULT_SESSION)
        val client = newClient(factory, sessionStateHolder = sessionHolder)
        val rejections = mutableListOf<Unit>()
        client.authRejections.onEach { rejections += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText("ERROR\nmessage:Authentication failed\n\nAuthentication failed\u0000")
        runCurrent()

        assertEquals(1, rejections.size)
        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
        // Sign-out is performed by the WS client itself so the policy survives
        // activity destruction (rotation / process death) — it must not depend
        // on a Compose collector being attached.
        assertEquals(null, sessionHolder.session.value)
    }

    @Test
    fun stompErrorFrameWithNonAuthBodyEmitsPurchaseErrorWithoutConnectionError() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val rejections = mutableListOf<Unit>()
        val purchaseEvents = mutableListOf<PurchaseEvent>()
        client.authRejections.onEach { rejections += it }.launchIn(backgroundScope)
        client.purchaseEvents.onEach { purchaseEvents += it }.launchIn(backgroundScope)
        runCurrent()
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText("ERROR\n\nSome other error\u0000")
        runCurrent()
        assertTrue(rejections.isEmpty())
        assertEquals(listOf(PurchaseEvent.Failure("Some other error")), purchaseEvents)
        assertEquals(ConnectionStatus.CONNECTED, client.connectionStatus.value)
    }

    @Test
    fun disconnectClearsLobbyCode() {
        // Regression: a stale lobby code must not persist across a sign-out/
        // sign-in cycle within the same app session. Same contract applies to
        // the auth-rejection path which also funnels through resetGameState().
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"AJ25Z39"}}"""
            )
        )
        assertEquals("AJ25Z39", client.lobbyCode.value)

        client.disconnect()

        assertEquals(null, client.lobbyCode.value)
    }

    @Test
    fun authRejectionClearsLobbyCode() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.authRejections.onEach { }.launchIn(backgroundScope)
        runCurrent()
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameActionFrame("""{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"AJ25Z39"}}"""))
        assertEquals("AJ25Z39", client.lobbyCode.value)
        factory.simulateText("ERROR\nmessage:Authentication failed\n\nAuthentication failed\u0000")
        runCurrent()
        assertEquals(null, client.lobbyCode.value)
    }
    private fun authRejectionErrorFrame(): String =
        "ERROR\nmessage:Authentication failed\n\nAuthentication failed\u0000"
  
    // ── reconnect snapshot (/app/game.sync -> /user/queue/game-sync) ─────────

    @Test
    fun connectedFrameSubscribesToGameSyncQueue() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SUBSCRIBE") && it.contains("destination:/user/queue/game-sync")
            }
        )
    }

    @Test
    fun syncMessageRestoresGameStatusPhaseAndRound() {
        val client = clientAfterSync()
        assertEquals(GameStatus.IN_PROGRESS, client.gameStatus.value)
        assertEquals(GamePhase.BUY_OR_BUILD, client.gamePhase.value)
        assertEquals(3, client.roundNumber.value)
    }

    @Test
    fun syncMessageRestoresPlayers() {
        val client = clientAfterSync()
        assertEquals(2, client.players.value.size)
        assertEquals(10, client.players.value.first { it.id == "11" }.coins)
        assertEquals(7, client.players.value.first { it.id == "22" }.coins)
    }

    @Test
    fun syncMessageResolvesActivePlayerUserIdFromTurnOrder() {
        // turnOrder[currentTurnIndex=0] = playerId 11, whose userId is 1.
        val client = clientAfterSync()
        assertEquals(1, client.activePlayerId.value)
    }

    @Test
    fun syncMessageSurfacesLastDiceRollAsDiceResult() {
        val client = clientAfterSync()
        assertEquals(listOf(8), client.diceResult.value)
    }

    @Test
    fun syncMessageRestoresMarketplaceSupply() {
        val client = clientAfterSync()
        assertEquals(
            mapOf(CardType.WHEAT_FIELD to 6, CardType.BAKERY to 5),
            client.marketplace.value
        )
    }

    @Test
    fun syncMessageBuildsShopItemsFromServerDefinitions() {
        val client = clientAfterSync()
        val bakery = client.shopItems.value.first { it.type == "BAKERY" }
        val trainStation = client.shopItems.value.first { it.type == "TRAIN_STATION" }

        assertEquals(PurchaseType.ESTABLISHMENT, bakery.purchaseType)
        assertEquals("Bakery", bakery.displayName)
        assertEquals(1, bakery.cost)
        assertTrue(bakery.isAvailable)
        assertEquals(PurchaseType.LANDMARK, trainStation.purchaseType)
        assertEquals("Train Station", trainStation.displayName)
        assertEquals(4, trainStation.cost)
    }

    @Test
    fun syncMessageRestoresPlayerLandmarkBuildState() {
        val client = clientAfterSync()
        val playerOneLandmarks = client.playerLandmarks.value[11].orEmpty()
        assertEquals(
            PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
            playerOneLandmarks.first { it.landmarkType == LandmarkType.TRAIN_STATION }
        )
        assertFalse(
            playerOneLandmarks.first { it.landmarkType == LandmarkType.SHOPPING_MALL }.isBuilt
        )
        assertFalse(client.playerLandmarks.value[22].orEmpty().single().isBuilt)
    }

    @Test
    fun malformedSyncMessageDoesNotCrashAndLeavesSnapshotEmpty() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(syncFrame("""{"type":"SYNC","payload":{"state":"not an object"}}"""))
        assertNull(client.gameStatus.value)
        assertNull(client.roundNumber.value)
        assertTrue(client.marketplace.value.isEmpty())
        assertTrue(client.playerLandmarks.value.isEmpty())
    }

    @Test
    fun disconnectResetsSnapshotState() {
        val client = clientAfterSync()
        client.disconnect()
        assertNull(client.gameStatus.value)
        assertNull(client.roundNumber.value)
        assertTrue(client.marketplace.value.isEmpty())
        assertTrue(client.playerLandmarks.value.isEmpty())
    }

    /** Connects a client and feeds it one realistic SYNC snapshot frame. */
    private fun clientAfterSync(): OkHttpWebSocketClient {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(syncFrame(SYNC_SNAPSHOT_BODY))
        return client
    }

    /** A bare STOMP CONNECTED frame, correctly NUL-terminated by serialize(). */
    private fun connectedFrame(): String =
        StompFrame(command = "CONNECTED", headers = mapOf("version" to "1.2")).serialize()

    /** A MESSAGE frame on the per-user game-sync queue carrying [body]. */
    private fun syncFrame(body: String): String =
        StompFrame(
            command = "MESSAGE",
            headers = mapOf(
                "destination" to WebSocketContract.gameSyncQueue,
                "content-type" to "application/json",
            ),
            body = body,
        ).serialize()

    private fun gameActionFrame(body: String): String =
        "MESSAGE\ndestination:/topic/public\ncontent-type:application/json\n\n$body\u0000"

    private class FakeWebSocketFactory : WebSocketFactory {
        lateinit var listener: WebSocketListener
        val socket = FakeWebSocket()
        var createCount = 0

        override fun create(request: Request, listener: WebSocketListener): WebSocket {
            this.listener = listener
            socket.request = request
            createCount += 1
            return socket
        }

        fun simulateOpen() { listener.onOpen(socket, createResponse(socket.request)) }
        fun simulateText(text: String) { listener.onMessage(socket, text) }
        fun simulateClosing() { listener.onClosing(socket, 1000, "closing") }
        fun simulateClosed() { listener.onClosed(socket, 1000, "closed") }
        fun simulateFailure(throwable: Throwable) { listener.onFailure(socket, throwable, createResponse(socket.request)) }

        private fun createResponse(request: Request): Response =
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(101).message("Switching Protocols").build()
    }

    private class FakeWebSocket : WebSocket {
        lateinit var request: Request
        var closed = false
        val sentMessages = mutableListOf<String>()
        override fun request(): Request = request
        override fun queueSize(): Long = 0L
        override fun send(text: String): Boolean { sentMessages += text; return true }
        override fun send(bytes: ByteString): Boolean = false
        override fun close(code: Int, reason: String?): Boolean { closed = true; return true }
        override fun cancel() { closed = true }
    }

    private class FakeSessionStateHolder(initial: Session? = null) : SessionStateHolder {
        private val mutableSession = MutableStateFlow(initial)
        override val session: StateFlow<Session?> = mutableSession.asStateFlow()
        override fun signIn(token: String, username: String, userId: Int) {
            mutableSession.value = Session(token, username, userId)
        }
        override fun signOut() { mutableSession.value = null }
    }

    private companion object {
        const val DEFAULT_TOKEN = "test-token"
        const val DEFAULT_USERNAME = "test-user"
        const val DEFAULT_USER_ID = 1
        val DEFAULT_SESSION = Session(DEFAULT_TOKEN, DEFAULT_USERNAME, DEFAULT_USER_ID)

        // A full /app/game.sync snapshot: game IN_PROGRESS / BUY_OR_BUILD,
        // round 3, last roll 8; player 11 (userId 1) is active with one
        // landmark built; marketplace has WHEAT_FIELD x6 and BAKERY x5.
        const val SYNC_SNAPSHOT_BODY =
            """{"type":"SYNC","sender":"server","gameId":7,"payload":{"targetUserId":1,""" +
                """"state":{"game":{"id":7,"status":"IN_PROGRESS","turnPhase":"BUY_OR_BUILD",""" +
                """"lastDiceRoll":8,"roundNumber":3,"currentTurnIndex":0},""" +
                """"players":[{"id":11,"userId":1,"coins":10},{"id":22,"userId":2,"coins":7}],""" +
                """"playerLandmarks":{"11":[{"playerId":11,"landmarkType":"TRAIN_STATION","isBuilt":true},""" +
                """{"playerId":11,"landmarkType":"SHOPPING_MALL","isBuilt":false}],""" +
                """"22":[{"playerId":22,"landmarkType":"TRAIN_STATION","isBuilt":false}]},""" +
                """"marketplace":{"WHEAT_FIELD":6,"BAKERY":5},""" +
                """"cardDefinitions":[{"cardType":"BAKERY","cost":1,"income":1,"color":"GREEN",""" +
                """"establishmentType":"BREAD","paymentSource":"BANK","activationNumbers":[2,3]}],""" +
                """"landmarkDefinitions":[{"landmarkType":"TRAIN_STATION","cost":4}],""" +
                """"turnOrder":[11,22]}}}"""
    }

    private fun newClient(
        factory: FakeWebSocketFactory,
        sessionStateHolder: SessionStateHolder = FakeSessionStateHolder(DEFAULT_SESSION),
    ) = OkHttpWebSocketClient(
        websocketUrl = "ws://10.0.2.2:8080/ws",
        sessionStateHolder = sessionStateHolder,
        webSocketFactory = factory,
    )
}
