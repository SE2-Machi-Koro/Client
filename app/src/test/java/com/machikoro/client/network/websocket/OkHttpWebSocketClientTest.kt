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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun sendCreateLobbyDoesNotSetHostFlagBeforeServerHostIdArrives() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        client.sendCreateLobby()

        assertFalse(client.isLobbyHost.value)
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

        client.lobbyJoinErrors.onEach { errors += it.userMessage }.launchIn(backgroundScope)
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

        client.lobbyJoinErrors.onEach { errors += it.userMessage }.launchIn(backgroundScope)
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

        client.lobbyJoinErrors.onEach { errors += it.userMessage }.launchIn(backgroundScope)
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
                    "activePlayerId":1,
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
        assertEquals(1, client.activePlayerId.value)
    }

    @Test
    fun rollDiceSendsStompFrameToRollDiceDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(gameStartedFrame(gameId = 7))
        client.rollDice(diceCount = 1)
        assertTrue(factory.socket.sentMessages.any {
            it.startsWith("SEND\n") &&
                it.contains("destination:/app/game.rollDice") &&
                it.contains("\"gameId\":7") &&
                it.contains("\"diceCount\":1")
        })
    }

    @Test
    fun rollDiceIncludesGameIdInTopLevelAndPayload() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(gameStartedFrame(gameId = 7))

        client.rollDice(diceCount = 2)

        val body = JSONObject(factory.socket.rollDiceFrames().last().body)
        assertEquals("ROLL_DICE", body.getString("type"))
        assertEquals(7, body.getInt("gameId"))
        assertEquals(7, body.getJSONObject("payload").getInt("gameId"))
        assertEquals(2, body.getJSONObject("payload").getInt("diceCount"))
    }

    @Test
    fun rollDiceWithoutActiveGameIdIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        client.rollDice(diceCount = 1)

        assertTrue(factory.socket.rollDiceFrames().isEmpty())
    }

    @Test
    fun rollDiceWithoutConnectionIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.rollDice(diceCount = 1)

        assertTrue(factory.socket.sentMessages.isEmpty())
    }

    @Test
    fun advancePhaseSendsGameIdPayloadToAdvancePhaseDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        client.advancePhase(gameId = 7)

        val frame = factory.socket.sentFrames().last {
            it.headers["destination"] == WebSocketContract.advancePhaseDestination
        }
        assertEquals("""{"gameId":7}""", frame.body)
    }

    @Test
    fun resolveEffectsSendsGameIdPayloadToResolveEffectsDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        client.resolveEffects(gameId = 7)

        val frame = factory.socket.sentFrames().last {
            it.headers["destination"] == WebSocketContract.resolveEffectsDestination
        }
        assertEquals("""{"gameId":7}""", frame.body)
    }

    @Test
    fun endTurnSendsGameIdPayloadToEndTurnDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        client.endTurn(gameId = 7)

        val frame = factory.socket.sentFrames().last {
            it.headers["destination"] == WebSocketContract.endTurnDestination
        }
        assertEquals("""{"gameId":7}""", frame.body)
    }

    @Test
    fun turnFlowActionsWithoutConnectionAreIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.advancePhase(gameId = 7)
        client.resolveEffects(gameId = 7)
        client.endTurn(gameId = 7)

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

        client.lobbyJoinErrors.onEach { errors += it.userMessage }.launchIn(backgroundScope)
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

        client.lobbyJoinErrors.onEach { errors += it.userMessage }.launchIn(backgroundScope)
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
    fun gameTopicPurchaseFailureMessageEmitsPurchaseFailureEvent() = runTest {
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
                """{"type":"ERROR","sender":"server","payload":{"event":"PURCHASE_FAILED","code":"DUPLICATE_PURPLE_ESTABLISHMENT","message":"Player already owns purple establishment STADIUM","purchaseType":"ESTABLISHMENT","cardType":"STADIUM"},"gameId":42}"""
            )
        )
        runCurrent()

        assertEquals(
            listOf(PurchaseEvent.Failure("Player already owns purple establishment STADIUM")),
            purchaseEvents
        )
    }

    @Test
    fun errorMessageWithoutPurchaseFailureEventDoesNotEmitPurchaseFailure() = runTest {
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
                """{"type":"ERROR","sender":"server","payload":{"event":"SOME_OTHER_FAILURE","message":"Not a purchase rejection"},"gameId":42}"""
            )
        )
        runCurrent()

        assertTrue(purchaseEvents.isEmpty())
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
        factory.simulateText(gameStartedFrame(gameId = 7))
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
    fun stompErrorFrameWithBlankBodyEmitsDefaultPurchaseFailureMessage() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val purchaseEvents = mutableListOf<PurchaseEvent>()
        client.purchaseEvents.onEach { purchaseEvents += it }.launchIn(backgroundScope)
        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText("ERROR\n\n\u0000")
        runCurrent()

        assertEquals(listOf(PurchaseEvent.Failure("Purchase failed")), purchaseEvents)
        assertEquals(ConnectionStatus.CONNECTED, client.connectionStatus.value)
    }

    @Test
    fun purchaseFailureWithBlankMessageFallsBackToDefaultMessage() = runTest {
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
                """{"type":"ERROR","sender":"server","payload":{"event":"PURCHASE_FAILED","message":"","purchaseType":"ESTABLISHMENT","cardType":"STADIUM"},"gameId":42}"""
            )
        )
        runCurrent()

        assertEquals(listOf(PurchaseEvent.Failure("Purchase failed")), purchaseEvents)
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

    // ── handleLobbyCreated — host auto-add ───────────────────────────────────

    @Test
    fun lobbyCreatedAddsHostToPlayerListFromSessionUsername() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123"}}"""
            )
        )
        assertEquals(1, client.players.value.size)
        assertEquals(DEFAULT_USERNAME, client.players.value.first().displayName)
    }

    @Test
    fun lobbyCreatedUsesServerPlayerIdWhenPresentInPayload() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123","playerId":42}}"""
            )
        )
        assertEquals("42", client.players.value.first().id)
    }

    @Test
    fun lobbyCreatedUsesIdFieldWhenPlayerIdAbsent() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123","id":99}}"""
            )
        )
        assertEquals("99", client.players.value.first().id)
    }

    @Test
    fun lobbyCreatedUsesFallbackHostIdWhenNoIdInPayload() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123"}}"""
            )
        )
        assertEquals("host-$DEFAULT_USERNAME", client.players.value.first().id)
    }

    @Test
    fun lobbyCreatedDoesNotDuplicateHostWhenReceivedTwice() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        val frame = gameActionFrame(
            """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123"}}"""
        )
        factory.simulateText(frame)
        factory.simulateText(frame)
        assertEquals(1, client.players.value.size)
    }

    @Test
    fun lobbyCreatedSetsHostFlagWhenHostIdMatchesCurrentUser() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123","hostId":$DEFAULT_USER_ID}}"""
            )
        )

        assertTrue(client.isLobbyHost.value)
    }

    @Test
    fun lobbyCreatedKeepsHostFlagFalseWhenHostIdDoesNotMatchCurrentUser() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123","host_id":999}}"""
            )
        )

        assertFalse(client.isLobbyHost.value)
    }

    @Test
    fun lobbyCreatedTriggersAutoJoinAfterCreateRequest() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        client.sendCreateLobby()
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123"}}"""
            )
        )
        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SEND\n") &&
                        it.contains("destination:${WebSocketContract.joinLobbyDestination}") &&
                        it.contains("ABC123")
            }
        )
    }

    @Test
    fun lobbyCreatedDoesNotTriggerJoinWhenNotHost() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        val messagesBefore = factory.socket.sentMessages.size
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123"}}"""
            )
        )
        assertFalse(
            factory.socket.sentMessages.drop(messagesBefore).any {
                it.contains("destination:${WebSocketContract.joinLobbyDestination}")
            }
        )
    }

    // ── handleLobbyJoined — id fallback + name deduplication ─────────────────

    @Test
    fun lobbyJoinedAddsPlayerWithPlayerId() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","sender":"SERVER","payload":{"username":"alice","playerId":42,"coins":5}}"""
            )
        )
        assertEquals(1, client.players.value.size)
        assertEquals("alice", client.players.value.first().displayName)
        assertEquals("42", client.players.value.first().id)
        assertEquals(5, client.players.value.first().coins)
    }

    @Test
    fun lobbyJoinedAddsPlayerUsingIdFieldWhenPlayerIdAbsent() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","sender":"SERVER","payload":{"username":"alice","id":99}}"""
            )
        )
        assertEquals(1, client.players.value.size)
        assertEquals("alice", client.players.value.first().displayName)
        assertEquals("99", client.players.value.first().id)
    }

    @Test
    fun lobbyJoinedSkipsPlayerWhenUsernameBlank() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","sender":"SERVER","payload":{"username":"","playerId":42}}"""
            )
        )
        assertTrue(client.players.value.isEmpty())
    }

    @Test
    fun lobbyJoinedSkipsPlayerWhenNoIdFields() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","sender":"SERVER","payload":{"username":"alice"}}"""
            )
        )
        assertTrue(client.players.value.isEmpty())
    }

    @Test
    fun lobbyJoinedDefaultsCoinsToThreeWhenAbsent() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","sender":"SERVER","payload":{"username":"alice","playerId":1}}"""
            )
        )
        assertEquals(3, client.players.value.first().coins)
    }

    @Test
    fun lobbyJoinedReplacesEntryWithSameDisplayName() {
        // LOBBY_CREATED adds host with temp id; LOBBY_JOINED must replace — not duplicate
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_CREATED","sender":"SERVER","payload":{"lobbyCode":"ABC123"}}"""
            )
        )
        assertEquals("host-$DEFAULT_USERNAME", client.players.value.first().id)
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","sender":"SERVER","payload":{"username":"$DEFAULT_USERNAME","playerId":7,"coins":3}}"""
            )
        )
        assertEquals(1, client.players.value.size)
        assertEquals("7", client.players.value.first().id)
        assertEquals(DEFAULT_USERNAME, client.players.value.first().displayName)
    }

    @Test
    fun lobbyJoinedAddsSecondPlayerWithoutAffectingFirst() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","sender":"SERVER","payload":{"username":"alice","playerId":1}}"""
            )
        )
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","sender":"SERVER","payload":{"username":"bob","playerId":2}}"""
            )
        )
        assertEquals(2, client.players.value.size)
        assertTrue(client.players.value.any { it.displayName == "alice" })
        assertTrue(client.players.value.any { it.displayName == "bob" })
    }

    @Test
    fun lobbyJoinedDoesNotDuplicatePlayerWhenSentTwice() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        val frame = gameActionFrame(
            """{"type":"LOBBY_JOINED","sender":"SERVER","payload":{"username":"alice","playerId":1}}"""
        )
        factory.simulateText(frame)
        factory.simulateText(frame)
        assertEquals(1, client.players.value.size)
    }

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
        // Backend contract: turnOrder[currentTurnIndex=0] is userId 1.
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
    fun gameEndMessageMarksGameFinishedAndKeepsWinnerId() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateText(
            gameActionFrame("""{"type":"GAME_END","sender":"server","payload":{"winnerId":11,"roundsPlayed":4}}""")
        )

        assertEquals(GameStatus.FINISHED, client.gameStatus.value)
        assertEquals(GamePhase.NONE, client.gamePhase.value)
        assertEquals(11, client.winnerId.value)
        assertEquals(4, client.roundNumber.value)
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

    @Test
    fun clearGameStateResetsFinishedGameData() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_END","sender":"server","payload":{"winnerId":11,"roundsPlayed":4}}""")
        )

        client.clearGameState()

        assertNull(client.gameStatus.value)
        assertNull(client.winnerId.value)
        assertNull(client.activeGameId.value)
        assertNull(client.lobbyCode.value)
    }

    // ── auto-reconnect (#166) ────────────────────────────────────────────────

    @Test
    fun unexpectedFailureTriggersAutomaticReconnect() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory, reconnectScope = backgroundScope)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        assertEquals(1, factory.createCount)

        factory.simulateFailure(IOException("backend container restarted"))
        runCurrent()

        assertEquals(2, factory.createCount)
    }

    @Test
    fun unexpectedCloseTriggersAutomaticReconnect() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory, reconnectScope = backgroundScope)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateClosed()
        runCurrent()

        assertEquals(2, factory.createCount)
    }

    @Test
    fun clientInitiatedDisconnectDoesNotReconnect() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory, reconnectScope = backgroundScope)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        client.disconnect()
        factory.simulateClosed()
        runCurrent()

        assertEquals(1, factory.createCount)
    }

    @Test
    fun authRejectionDoesNotReconnect() = runTest {
        val factory = FakeWebSocketFactory()
        val sessionHolder = FakeSessionStateHolder(DEFAULT_SESSION)
        val client = newClient(
            factory,
            sessionStateHolder = sessionHolder,
            reconnectScope = backgroundScope,
        )
        client.connect()
        factory.simulateOpen()
        factory.simulateText(
            StompFrame(command = "ERROR", body = "Authentication failed").serialize()
        )
        factory.simulateClosed()
        runCurrent()

        assertEquals(1, factory.createCount)
        assertNull(sessionHolder.session.value)
    }

    @Test
    fun reconnectKeepsRetryingWhileBackendStaysDown() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory, reconnectScope = backgroundScope)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateFailure(IOException("down"))
        runCurrent()
        assertEquals(2, factory.createCount)

        factory.simulateFailure(IOException("still down"))
        runCurrent()
        assertEquals(3, factory.createCount)
    }

    @Test
    fun reconnectReSubscribesToGameSyncQueueAndReTriggersSnapshot() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory, reconnectScope = backgroundScope)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        factory.simulateFailure(IOException("backend container restarted"))
        runCurrent()
        factory.simulateOpen()
        factory.socket.sentMessages.clear()
        factory.simulateText(connectedFrame())

        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SUBSCRIBE") && it.contains("destination:/user/queue/game-sync")
            }
        )
        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SEND") && it.contains("destination:/app/chat.addUser")
            }
        )
    }

    @Test
    fun connectedFrameWithSessionHeaderSubscribesToLobbyQueue() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\nsession:sess-abc\n\n\u0000")
        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SUBSCRIBE") && it.contains("destination:/queue/lobby-usersess-abc")
            }
        )
    }

    @Test
    fun connectedFrameWithoutSessionHeaderSkipsLobbyQueueSubscription() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        assertFalse(
            factory.socket.sentMessages.any { it.contains("destination:/queue/lobby-user") }
        )
    }

    @Test
    fun lobbyRosterMessagePopulatesPlayerList() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        val rosterJson = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"players":[{"playerId":5,"userId":20,"username":"Alice","coins":3},{"playerId":6,"userId":21,"username":"Bob","coins":5}]}}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$rosterJson\u0000")
        val players = client.players.value
        assertEquals(2, players.size)
        assertEquals("5", players[0].id)
        assertEquals("Alice", players[0].displayName)
        assertEquals(3, players[0].coins)
        assertEquals("6", players[1].id)
        assertEquals("Bob", players[1].displayName)
        assertEquals(5, players[1].coins)
    }

    @Test
    fun lobbyRosterReplacesExistingPlayerList() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        // Seed one player via LOBBY_JOINED
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_JOINED","gameId":1,"payload":{"playerId":5,"userId":20,"username":"Alice","coins":3,"gameId":1}}""")
        )
        assertEquals(1, client.players.value.size)
        // LOBBY_ROSTER with two players replaces the list entirely
        val rosterJson = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"players":[{"playerId":5,"userId":20,"username":"Alice","coins":3},{"playerId":7,"userId":22,"username":"Carol","coins":3}]}}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$rosterJson\u0000")
        val players = client.players.value
        assertEquals(2, players.size)
        assertTrue(players.any { it.displayName == "Carol" })
    }

    @Test
    fun lobbyRosterSetsHostFlagWhenHostIdMatchesCurrentUser() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")

        val rosterJson = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"hostId":$DEFAULT_USER_ID,"players":[{"playerId":5,"userId":$DEFAULT_USER_ID,"username":"Alice","coins":3}]}}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$rosterJson\u0000")

        assertTrue(client.isLobbyHost.value)
    }

    @Test
    fun lobbyRosterUpdatesHostFlagWhenHostChanges() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")

        val hostRoster = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"host_id":$DEFAULT_USER_ID,"players":[{"playerId":5,"userId":$DEFAULT_USER_ID,"username":"Alice","coins":3},{"playerId":6,"userId":2,"username":"Bob","coins":3}]}}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$hostRoster\u0000")
        assertTrue(client.isLobbyHost.value)

        val guestRoster = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"host_id":2,"players":[{"playerId":5,"userId":$DEFAULT_USER_ID,"username":"Alice","coins":3},{"playerId":6,"userId":2,"username":"Bob","coins":3}]}}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$guestRoster\u0000")

        assertFalse(client.isLobbyHost.value)
    }

    @Test
    fun lobbyRosterCanResolveHostFromHostPlayerEntryWhenNoHostIdFieldExists() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")

        val rosterJson = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"players":[{"playerId":5,"userId":$DEFAULT_USER_ID,"username":"Alice","coins":3,"isHost":true},{"playerId":6,"userId":2,"username":"Bob","coins":3}]}}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$rosterJson\u0000")

        assertTrue(client.isLobbyHost.value)
    }

    @Test
    fun lobbyRosterWithEmptyArrayClearsPlayerList() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_JOINED","gameId":1,"payload":{"playerId":5,"userId":20,"username":"Alice","coins":3,"gameId":1}}""")
        )
        assertEquals(1, client.players.value.size)
        val emptyRoster = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"players":[]}}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$emptyRoster\u0000")
        assertEquals(0, client.players.value.size)
    }

    @Test
    fun lobbyRosterSkipsEntriesWithMissingUsername() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        // Second entry has no username field
        val rosterJson = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"players":[{"playerId":5,"userId":20,"username":"Alice","coins":3},{"playerId":6,"userId":21,"coins":3}]}}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$rosterJson\u0000")
        val players = client.players.value
        assertEquals(1, players.size)
        assertEquals("Alice", players[0].displayName)
    }

    @Test
    fun lobbyRosterSkipsEntriesWithMissingPlayerId() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        // First entry missing playerId, second is valid
        val rosterJson = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"players":[{"userId":20,"username":"Alice","coins":3},{"playerId":6,"userId":21,"username":"Bob","coins":3}]}}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$rosterJson\u0000")
        val players = client.players.value
        assertEquals(1, players.size)
        assertEquals("Bob", players[0].displayName)
    }

    @Test
    fun lobbyRosterMessageWithNoPayloadFieldIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        // LOBBY_ROSTER with no payload key — handler must silently return
        val noPayload = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1}"""
        factory.simulateText("MESSAGE\ndestination:/queue/lobby-user1\ncontent-type:application/json\n\n$noPayload ")
        assertEquals(emptyList<PlayerCoinState>(), client.players.value)
    }

    // ── isReady field in LOBBY_ROSTER ───────────────────────────────────────────

    @Test
    fun lobbyRosterMessagePopulatesIsReadyField() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        // Player entry with isReady:true
        val rosterJson = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"players":[{"playerId":5,"userId":20,"username":"Alice","coins":3,"isReady":true}]}}"""
        factory.simulateText(gameActionFrame(rosterJson))
        assertTrue(client.players.value[0].isReady)
    }

    @Test
    fun lobbyRosterMessageDefaultsIsReadyToFalseWhenAbsent() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        // Player entry without isReady field — should default to false
        val rosterJson = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":1,"payload":{"players":[{"playerId":5,"userId":20,"username":"Alice","coins":3}]}}"""
        factory.simulateText(gameActionFrame(rosterJson))
        assertFalse(client.players.value[0].isReady)
    }

    // ── sendReadyToggle ──────────────────────────────────────────────────────────

    @Test
    fun sendReadyToggleSendsStompFrameToReadyToggleDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        // Lobby roster sets activeGameId so the toggle has a game to target
        val rosterJson = """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":7,"payload":{"players":[{"playerId":1,"userId":1,"username":"Alice","coins":3}]}}"""
        factory.simulateText(gameActionFrame(rosterJson))
        factory.socket.sentMessages.clear()

        client.sendReadyToggle(true)

        val readyFrame = factory.socket.sentMessages.firstOrNull {
            it.contains("destination:${WebSocketContract.readyToggleDestination}")
        }
        assertNotNull("expected a SEND frame to ${WebSocketContract.readyToggleDestination}", readyFrame)
        assertTrue(readyFrame!!.contains("\"isReady\":true"))
        assertTrue(readyFrame.contains("\"gameId\":7"))
    }

    @Test
    fun sendReadyToggleWithoutActiveGameIdIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        // No roster → activeGameId stays null
        factory.socket.sentMessages.clear()

        client.sendReadyToggle(true)

        assertFalse(
            factory.socket.sentMessages.any {
                it.contains("destination:${WebSocketContract.readyToggleDestination}")
            }
        )
    }

    @Test
    fun sendReadyToggleWithoutConnectionIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        // Never connected — webSocket is null

        client.sendReadyToggle(true)

        assertTrue(factory.socket.sentMessages.isEmpty())
    }

    @Test
    fun lobbyRosterSessionIdClearedOnDisconnectAndResubscribedOnReconnect() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory, reconnectScope = backgroundScope)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(StompFrame(command = "CONNECTED", headers = mapOf("version" to "1.2", "session" to "sess-1")).serialize())
        assertTrue(factory.socket.sentMessages.any { it.contains("destination:/queue/lobby-usersess-1") })

        // Disconnect clears session ID; reconnect with new session gets new subscription
        factory.simulateFailure(IOException("drop"))
        runCurrent()
        factory.simulateOpen()
        factory.socket.sentMessages.clear()
        factory.simulateText(StompFrame(command = "CONNECTED", headers = mapOf("version" to "1.2", "session" to "sess-2")).serialize())
        assertTrue(factory.socket.sentMessages.any { it.contains("destination:/queue/lobby-usersess-2") })
    }

    @Test
    fun clearGameStateResetsActiveGameIdLobbyCodeAndGamePhase() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"GAME_STARTED","gameId":42,"payload":{"activePlayerId":1,"game":{"id":42,"lobbyCode":"ABC123","turnPhase":"ROLL_DICE"},"players":[]}}"""
            )
        )
        assertEquals(42, client.activeGameId.value)
        assertEquals(GamePhase.ROLL_DICE, client.gamePhase.value)
        assertEquals("ABC123", client.lobbyCode.value)

        client.clearGameState()

        assertNull(client.activeGameId.value)
        assertEquals(GamePhase.NONE, client.gamePhase.value)
        assertNull(client.lobbyCode.value)
    }

    @Test
    fun gameStartedMessageUsesPlayerUsernamesMapForDisplayNames() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"GAME_STARTED","gameId":42,"payload":{"activePlayerId":1,"game":{"id":42,"lobbyCode":"ABC123","turnPhase":"ROLL_DICE"},"players":[{"id":1,"coins":3},{"id":2,"coins":5}],"playerUsernames":{"1":"alice","2":"bob"}}}"""
            )
        )

        val players = client.players.value
        assertEquals(2, players.size)
        assertEquals("alice", players.first { it.id == "1" }.displayName)
        assertEquals("bob", players.first { it.id == "2" }.displayName)
    }

    @Test
    fun syncMessageUsesPlayerUsernamesMapForDisplayNames() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            syncFrame(
                """{"type":"SYNC","sender":"server","gameId":7,"payload":{"targetUserId":1,"state":{"game":{"id":7,"status":"IN_PROGRESS","turnPhase":"BUY_OR_BUILD","currentTurnIndex":0},"players":[{"id":11,"userId":1,"coins":10},{"id":22,"userId":2,"coins":7}],"playerUsernames":{"11":"alice","22":"bob"},"playerLandmarks":{},"marketplace":{},"turnOrder":[1,2]}}}"""
            )
        )

        val players = client.players.value
        assertEquals(2, players.size)
        assertEquals("alice", players.first { it.id == "11" }.displayName)
        assertEquals("bob", players.first { it.id == "22" }.displayName)
    }

    // ── sendLeaveLobby ────────────────────────────────────────────────────────

    @Test
    fun sendLeaveLobbyWithConnectionSendsStompFrameToLeaveLobbyDestination() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        client.sendLeaveLobby(gameId = 5)
        assertTrue(
            factory.socket.sentMessages.any {
                it.startsWith("SEND\n") &&
                    it.contains("destination:${WebSocketContract.leaveLobbyDestination}") &&
                    it.contains("\"type\":\"LEAVE\"") &&
                    it.contains("\"gameId\":5")
            }
        )
    }

    @Test
    fun sendLeaveLobbyWithoutConnectionIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.sendLeaveLobby(gameId = 5)
        assertTrue(factory.socket.sentMessages.isEmpty())
    }

    // ── handleLobbyLeft ───────────────────────────────────────────────────────

    @Test
    fun lobbyLeftMessageRemovesPlayerFromList() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_JOINED","payload":{"username":"alice","playerId":7,"coins":3}}""")
        )
        assertEquals(1, client.players.value.size)
        factory.simulateText(gameActionFrame("""{"type":"LOBBY_LEFT","payload":{"playerId":7}}"""))
        assertTrue(client.players.value.isEmpty())
    }

    @Test
    fun lobbyLeftWithNoPayloadIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_JOINED","payload":{"username":"alice","playerId":7,"coins":3}}""")
        )
        factory.simulateText(gameActionFrame("""{"type":"LOBBY_LEFT"}"""))
        assertEquals(1, client.players.value.size)
    }

    @Test
    fun lobbyLeftWithMissingPlayerIdIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_JOINED","payload":{"username":"alice","playerId":7,"coins":3}}""")
        )
        factory.simulateText(gameActionFrame("""{"type":"LOBBY_LEFT","payload":{"reason":"left"}}"""))
        assertEquals(1, client.players.value.size)
    }

    // ── handleHostLeft ───────────────────────────────────────────────────────

    @Test
    fun hostLeftMessageResetsLobbyStateForNonHost() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        // Join as regular player (NOT host)
        factory.simulateText(
            gameActionFrame(
                """{
                "type":"LOBBY_JOINED",
                "payload":{
                    "username":"alice",
                    "playerId":1,
                    "coins":3,
                    "gameId":42
                }
            }"""
            )
        )

        // Seed lobby state
        factory.simulateText(
            gameActionFrame(
                """{
                "type":"LOBBY_CREATED",
                "payload":{
                    "lobbyCode":"ABC123",
                    "gameId":42
                }
            }"""
            )
        )

        assertEquals("ABC123", client.lobbyCode.value)

        factory.simulateText(
            gameActionFrame(
                """{
                "type":"HOST_LEFT",
                "payload":{
                    "userId":123
                }
            }"""
            )
        )

        assertNull(client.lobbyCode.value)
        assertNull(client.activeGameId.value)
        assertTrue(client.players.value.isEmpty())
    }

    @Test
    fun hostLeftMessageEmitsEventForNonHost() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val events = mutableListOf<Unit>()

        client.hostLeftLobby
            .onEach { events += it }
            .launchIn(backgroundScope)

        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        // Regular player, not host
        factory.simulateText(
            gameActionFrame(
                """{
                "type":"LOBBY_JOINED",
                "payload":{
                    "username":"alice",
                    "playerId":1,
                    "coins":3
                }
            }"""
            )
        )

        assertFalse(client.isLobbyHost.value)

        factory.simulateText(
            gameActionFrame(
                """{
                "type":"HOST_LEFT",
                "payload":{
                    "userId":123
                }
            }"""
            )
        )

        runCurrent()

        assertEquals(1, events.size)
    }

    @Test
    fun hostLeftMessageIgnoredForHost() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val events = mutableListOf<Unit>()

        client.hostLeftLobby
            .onEach { events += it }
            .launchIn(backgroundScope)

        runCurrent()

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        // Host creates lobby
        client.sendCreateLobby()

        factory.simulateText(
            gameActionFrame(
                """{
                "type":"LOBBY_CREATED",
                "payload":{
                    "lobbyCode":"ABC123",
                    "hostId":$DEFAULT_USER_ID,
                    "gameId":42
                }
            }"""
            )
        )

        assertTrue(client.isLobbyHost.value)

        factory.simulateText(
            gameActionFrame(
                """{
                "type":"HOST_LEFT",
                "payload":{
                    "userId":123
                }
            }"""
            )
        )

        runCurrent()

        assertTrue(events.isEmpty())
    }

    // ── handleLobbyJoined host branch + lobbyEntered ──────────────────────────

    @Test
    fun lobbyJoinedWithHostFlagAndGameIdResendsJoinMessageWithGameId() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.socket.sentMessages.clear()
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","gameId":10,"payload":{"username":"alice","playerId":1,"coins":3,"gameId":10,"hostId":$DEFAULT_USER_ID}}"""
            )
        )
        assertTrue(
            factory.socket.sentMessages.any {
                it.contains("destination:${WebSocketContract.addUserDestination}") &&
                    it.contains("\"gameId\":10")
            }
        )
    }

    @Test
    fun lobbyJoinedEmitsLobbyEnteredEvent() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        val events = mutableListOf<Unit>()
        client.lobbyEntered.onEach { events += it }.launchIn(backgroundScope)
        runCurrent()
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_JOINED","payload":{"username":"alice","playerId":1,"coins":3}}""")
        )
        runCurrent()
        assertEquals(1, events.size)
    }

    // ── sendGameStart branches ────────────────────────────────────────────────

    @Test
    fun sendGameStartWithOnlyLobbyCodeSendsLobbyCodeOnly() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        // No gameId in payload: sets lobbyCode only
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_CREATED","payload":{"lobbyCode":"XYZ"}}""")
        )
        assertNull(client.activeGameId.value)
        assertEquals("XYZ", client.lobbyCode.value)
        client.sendGameStart()
        val startFrame = factory.socket.sentMessages.last {
            it.startsWith("SEND\n") && it.contains("destination:${WebSocketContract.gameStartDestination}")
        }
        assertTrue(startFrame.contains("\"lobbyCode\":\"XYZ\""))
        assertFalse(startFrame.contains("gameId"))
    }

    @Test
    fun sendGameStartWithBothGameIdAndLobbyCodeSendsBoth() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(gameStartedFrame(gameId = 42))
        assertEquals(42, client.activeGameId.value)
        assertTrue(client.lobbyCode.value != null)
        client.sendGameStart()
        val startFrame = factory.socket.sentMessages.last {
            it.startsWith("SEND\n") && it.contains("destination:${WebSocketContract.gameStartDestination}")
        }
        assertTrue(startFrame.contains("\"gameId\":42"))
        assertTrue(startFrame.contains("\"lobbyCode\":\"ABC1234\""))
    }

    @Test
    fun sendGameStartWithGameIdOnlySendsGameIdOnly() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        // LOBBY_JOINED sets activeGameId but not lobbyCode
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","gameId":10,"payload":{"username":"alice","playerId":1,"coins":3,"gameId":10}}"""
            )
        )
        assertEquals(10, client.activeGameId.value)
        assertNull(client.lobbyCode.value)
        client.sendGameStart()
        val startFrame = factory.socket.sentMessages.last {
            it.startsWith("SEND\n") && it.contains("destination:${WebSocketContract.gameStartDestination}")
        }
        assertTrue(startFrame.contains("\"gameId\":10"))
        assertFalse(startFrame.contains("lobbyCode"))
    }

    // ── MESSAGE blank body ────────────────────────────────────────────────────

    @Test
    fun messageFrameWithBlankBodyIsIgnoredGracefully() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText("MESSAGE\ndestination:/topic/public\n\n \u0000")
        assertEquals(GamePhase.NONE, client.gamePhase.value)
        assertTrue(client.players.value.isEmpty())
    }

    // ── subscribeToGameTopic dedup + UNSUBSCRIBE ──────────────────────────────

    @Test
    fun subscribingToSameGameTopicTwiceDoesNotSendDuplicateSubscribe() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_CREATED","gameId":1,"payload":{"lobbyCode":"ABC","gameId":1}}""")
        )
        val countAfterFirst = factory.socket.sentMessages.count {
            it.startsWith("SUBSCRIBE\n") && it.contains("/topic/game/1")
        }
        // LOBBY_JOINED with same gameId should not add another SUBSCRIBE
        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_JOINED","gameId":1,"payload":{"username":"alice","playerId":5,"coins":3,"gameId":1}}"""
            )
        )
        val countAfterSecond = factory.socket.sentMessages.count {
            it.startsWith("SUBSCRIBE\n") && it.contains("/topic/game/1")
        }
        assertEquals(countAfterFirst, countAfterSecond)
    }

    @Test
    fun subscribingToNewGameTopicUnsubscribesOldOne() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_CREATED","gameId":1,"payload":{"lobbyCode":"AAA","gameId":1}}""")
        )
        factory.simulateText(
            gameActionFrame("""{"type":"LOBBY_CREATED","gameId":2,"payload":{"lobbyCode":"BBB","gameId":2}}""")
        )
        assertTrue(factory.socket.sentMessages.any { it.startsWith("UNSUBSCRIBE\n") && it.contains("id:game-topic-1") })
        assertTrue(factory.socket.sentMessages.any { it.startsWith("SUBSCRIBE\n") && it.contains("/topic/game/2") })
    }

    // ── websocketHostHeader standard ports ────────────────────────────────────

    @Test
    fun connectFrameHostHeaderOmitsPortForDefaultWssPort() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "wss://example.com:443/ws",
            sessionStateHolder = FakeSessionStateHolder(DEFAULT_SESSION),
            webSocketFactory = factory,
        )
        client.connect()
        factory.simulateOpen()
        val connectFrame = factory.socket.sentMessages.first { it.startsWith("CONNECT\n") }
        assertTrue(connectFrame.contains("host:example.com\n"))
        assertFalse(connectFrame.contains(":443"))
    }

    @Test
    fun connectFrameHostHeaderOmitsPortForDefaultWsPort() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://example.com:80/ws",
            sessionStateHolder = FakeSessionStateHolder(DEFAULT_SESSION),
            webSocketFactory = factory,
        )
        client.connect()
        factory.simulateOpen()
        val connectFrame = factory.socket.sentMessages.first { it.startsWith("CONNECT\n") }
        assertTrue(connectFrame.contains("host:example.com\n"))
        assertFalse(connectFrame.contains(":80"))
    }

    @Test
    fun connectFrameHostHeaderOmitsPortWhenNoPortInUrl() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://example.com/ws",
            sessionStateHolder = FakeSessionStateHolder(DEFAULT_SESSION),
            webSocketFactory = factory,
        )
        client.connect()
        factory.simulateOpen()
        val connectFrame = factory.socket.sentMessages.first { it.startsWith("CONNECT\n") }
        assertTrue(connectFrame.contains("host:example.com\n"))
    }

    // ── parseCardDefinitions / parseLandmarkDefinitions unknown types ──────────

    @Test
    fun parseCardDefinitionsSkipsEntriesWithUnknownCardType() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            syncFrame(
                """{"type":"SYNC","sender":"server","gameId":1,"payload":{"targetUserId":1,"state":{""" +
                    """"game":{"id":1,"status":"IN_PROGRESS","turnPhase":"ROLL_DICE","currentTurnIndex":0},""" +
                    """"players":[],"playerLandmarks":{},"marketplace":{},""" +
                    """"cardDefinitions":[{"cardType":"INVALID_CARD_TYPE","cost":1}],""" +
                    """"landmarkDefinitions":[],"turnOrder":[]}}}"""
            )
        )
        assertTrue(client.shopItems.value.isEmpty())
    }

    @Test
    fun parseCardDefinitionsStoresNumericActivationNumbersAndDerivesText() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            syncFrame(
                """{"type":"SYNC","sender":"server","gameId":1,"payload":{"targetUserId":1,"state":{""" +
                    """"game":{"id":1,"status":"IN_PROGRESS","turnPhase":"ROLL_DICE","currentTurnIndex":0},""" +
                    """"players":[],"playerLandmarks":{},"marketplace":{"BAKERY":4},""" +
                    """"cardDefinitions":[{"cardType":"BAKERY","cost":1,"color":"GREEN",""" +
                    """"establishmentType":"BREAD","activationNumbers":[2,3],""" +
                    """"effectText":"Get 1 coin from the bank on your turn."}],""" +
                    """"landmarkDefinitions":[],"turnOrder":[]}}}"""
            )
        )

        val bakery = client.shopItems.value.single { it.type == CardType.BAKERY.name }
        assertEquals(listOf(2, 3), bakery.activationNumbers)
        assertEquals("2-3", bakery.activationText)
        assertTrue(bakery.isAvailable)
    }

    @Test
    fun parseCardDefinitionsDoesNotTreatDisplayTextAsActivationNumbers() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            syncFrame(
                """{"type":"SYNC","sender":"server","gameId":1,"payload":{"targetUserId":1,"state":{""" +
                    """"game":{"id":1,"status":"IN_PROGRESS","turnPhase":"ROLL_DICE","currentTurnIndex":0},""" +
                    """"players":[],"playerLandmarks":{},"marketplace":{"BAKERY":4},""" +
                    """"cardDefinitions":[{"cardType":"BAKERY","cost":1,"color":"GREEN",""" +
                    """"establishmentType":"BREAD","activationText":"9-10","activationRange":"11-12",""" +
                    """"effectText":"Get 1 coin from the bank on your turn."}],""" +
                    """"landmarkDefinitions":[],"turnOrder":[]}}}"""
            )
        )

        val bakery = client.shopItems.value.single { it.type == CardType.BAKERY.name }
        assertEquals(listOf(2, 3), bakery.activationNumbers)
        assertEquals("2-3", bakery.activationText)
    }

    @Test
    fun parseLandmarkDefinitionsSkipsEntriesWithUnknownLandmarkType() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            syncFrame(
                """{"type":"SYNC","sender":"server","gameId":1,"payload":{"targetUserId":1,"state":{""" +
                    """"game":{"id":1,"status":"IN_PROGRESS","turnPhase":"ROLL_DICE","currentTurnIndex":0},""" +
                    """"players":[],"playerLandmarks":{},"marketplace":{},""" +
                    """"cardDefinitions":[],"landmarkDefinitions":[{"landmarkType":"INVALID_LANDMARK_TYPE","cost":4}],""" +
                    """"turnOrder":[]}}}"""
            )
        )
        assertTrue(client.shopItems.value.isEmpty())
    }

    // ── handleGameStarted missing game object ─────────────────────────────────

    @Test
    fun gameStartedMessageWithMissingGameObjectIsIgnored() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            gameActionFrame(
                """{"type":"GAME_STARTED","gameId":42,"payload":{"activePlayerId":1,"players":[]}}"""
            )
        )
        assertNull(client.activeGameId.value)
        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    // ── parseMarketplace unknown card type key ────────────────────────────────

    @Test
    fun parseMarketplaceSkipsUnknownCardTypeKeys() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(
            syncFrame(
                """{"type":"SYNC","sender":"server","gameId":1,"payload":{"targetUserId":1,"state":{""" +
                    """"game":{"id":1,"status":"IN_PROGRESS","turnPhase":"ROLL_DICE","currentTurnIndex":0},""" +
                    """"players":[],"playerLandmarks":{},"marketplace":{"UNKNOWN_CARD":3,"BAKERY":5},""" +
                    """"cardDefinitions":[],"landmarkDefinitions":[],"turnOrder":[]}}}"""
            )
        )
        assertEquals(1, client.marketplace.value.size)
        assertEquals(5, client.marketplace.value[CardType.BAKERY])
    }

    // ── two-client synchronization (#180) ────────────────────────────────────

    @Test
    fun twoClientsConvergeFromGameStartedBroadcast() = runTest {
        val harness = twoConnectedClients(backgroundScope)
        harness.assertBothSubscribedToGame(gameId = 7)

        harness.broadcastToBoth(gameStartedTwoPlayerBody())

        listOf(harness.playerA, harness.playerB).forEach { client ->
            assertEquals(7, client.activeGameId.value)
            assertEquals(GameStatus.IN_PROGRESS, client.gameStatus.value)
            assertEquals(GamePhase.ROLL_DICE, client.gamePhase.value)
            assertEquals(1, client.activePlayerId.value)
            assertEquals(listOf("alice", "bob"), client.players.value.map { it.displayName })
            assertEquals(5, client.marketplace.value[CardType.BAKERY])
            assertTrue(client.shopItems.value.any { it.type == "BAKERY" && it.isAvailable })
        }
    }

    @Test
    fun twoClientsApplyGameActionBroadcastToNonSender() = runTest {
        val harness = twoConnectedClients(backgroundScope)
        harness.assertBothSubscribedToGame(gameId = 7)
        harness.broadcastToBoth(gameStartedTwoPlayerBody())

        harness.playerA.resolveEffects(gameId = 7)
        assertEquals(GamePhase.ROLL_DICE, harness.playerB.gamePhase.value)
        assertFalse(
            harness.playerBFactory.socket.sentFrames().any {
                it.command == "SEND" &&
                    it.headers["destination"] == WebSocketContract.resolveEffectsDestination
            }
        )

        harness.broadcastToBoth(
            gameActionSnapshotBody(
                turnPhase = "BUY_OR_BUILD",
                activeUserId = 1,
                player11Coins = 5,
                player22Coins = 2,
            )
        )

        assertTrue(
            harness.playerAFactory.socket.sentFrames().any {
                it.command == "SEND" &&
                    it.headers["destination"] == WebSocketContract.resolveEffectsDestination &&
                    JSONObject(it.body).getInt("gameId") == 7
            }
        )
        assertEquals(GamePhase.BUY_OR_BUILD, harness.playerB.gamePhase.value)
        assertEquals(1, harness.playerB.activePlayerId.value)
        assertEquals(5, harness.playerB.players.value.first { it.id == "11" }.coins)
        assertEquals(2, harness.playerB.players.value.first { it.id == "22" }.coins)
    }

    @Test
    fun twoClientsApplyRollDiceBroadcast() = runTest {
        val harness = twoConnectedClients(backgroundScope)
        harness.assertBothSubscribedToGame(gameId = 7)
        harness.broadcastToBoth(gameStartedTwoPlayerBody())

        harness.playerA.rollDice(diceCount = 2)
        harness.broadcastToBoth(
            rollDiceSnapshotBody(
                result = "[3,5]",
                lastDiceRoll = 8,
            )
        )

        assertTrue(
            harness.playerAFactory.socket.rollDiceFrames().any {
                JSONObject(it.body).getJSONObject("payload").getInt("diceCount") == 2
            }
        )
        assertEquals(listOf(3, 5), harness.playerA.diceResult.value)
        assertEquals(listOf(3, 5), harness.playerB.diceResult.value)
        assertEquals(GamePhase.RESOLVE_EFFECTS, harness.playerB.gamePhase.value)
    }

    @Test
    fun twoClientsApplyPurchaseBroadcastToNonSender() = runTest {
        val harness = twoConnectedClients(backgroundScope)
        val playerBPurchaseEvents = mutableListOf<PurchaseEvent>()
        harness.playerB.purchaseEvents.onEach { playerBPurchaseEvents += it }.launchIn(backgroundScope)
        runCurrent()
        harness.assertBothSubscribedToGame(gameId = 7)
        harness.broadcastToBoth(gameStartedTwoPlayerBody())

        harness.playerA.sendPurchase(
            gameId = 7,
            purchaseType = PurchaseType.LANDMARK,
            landmarkType = "TRAIN_STATION",
        )
        harness.broadcastToBoth(
            gameActionSnapshotBody(
                turnPhase = "BUY_OR_BUILD",
                activeUserId = 1,
                bakerySupply = 4,
                trainStationBuiltForPlayer11 = true,
                purchaseType = "LANDMARK",
                landmarkType = "TRAIN_STATION",
            )
        )
        runCurrent()

        assertEquals(
            listOf(PurchaseEvent.Success(PurchaseType.LANDMARK, "TRAIN_STATION")),
            playerBPurchaseEvents
        )
        assertEquals(4, harness.playerB.marketplace.value[CardType.BAKERY])
        assertTrue(
            harness.playerB.playerLandmarks.value
                .getValue(11)
                .first { it.landmarkType == LandmarkType.TRAIN_STATION }
                .isBuilt
        )
    }

    @Test
    fun reconnectingClientRestoresStateFromSyncToMatchConnectedClient() = runTest {
        val harness = twoConnectedClients(backgroundScope)
        harness.assertBothSubscribedToGame(gameId = 7)
        val latestState = twoPlayerState(
            turnPhase = "BUY_OR_BUILD",
            activeUserId = 2,
            player11Coins = 8,
            player22Coins = 6,
            bakerySupply = 3,
            trainStationBuiltForPlayer11 = true,
            lastDiceRoll = 9,
        )
        harness.broadcastToBoth(gameActionSnapshotBody(state = latestState))

        harness.playerBFactory.simulateFailure(IOException("backend restarted"))
        runCurrent()
        assertEquals(GamePhase.NONE, harness.playerB.gamePhase.value)

        harness.playerBFactory.simulateOpen()
        harness.playerBFactory.simulateText(connectedFrame())
        harness.playerBFactory.simulateText(syncFrame(syncBody(latestState, targetUserId = 2)))

        assertEquals(harness.playerA.activeGameId.value, harness.playerB.activeGameId.value)
        assertEquals(harness.playerA.gamePhase.value, harness.playerB.gamePhase.value)
        assertEquals(harness.playerA.activePlayerId.value, harness.playerB.activePlayerId.value)
        assertEquals(
            harness.playerA.players.value.map { player ->
                listOf(player.id, player.displayName, player.coins, player.isActivePlayer)
            },
            harness.playerB.players.value.map { player ->
                listOf(player.id, player.displayName, player.coins, player.isActivePlayer)
            }
        )
        assertEquals(true, harness.playerA.players.value.first { it.id == "11" }.isCurrentPlayer)
        assertFalse(harness.playerA.players.value.first { it.id == "22" }.isCurrentPlayer)
        assertFalse(harness.playerB.players.value.first { it.id == "11" }.isCurrentPlayer)
        assertEquals(true, harness.playerB.players.value.first { it.id == "22" }.isCurrentPlayer)
        assertEquals(harness.playerA.marketplace.value, harness.playerB.marketplace.value)
        assertEquals(harness.playerA.playerLandmarks.value, harness.playerB.playerLandmarks.value)
        assertEquals(listOf(9), harness.playerB.diceResult.value)
        assertFalse(harness.playerB.players.value.first { it.id == "11" }.isCurrentPlayer)
        assertFalse(harness.playerB.players.value.first { it.id == "11" }.isActivePlayer)
        assertTrue(harness.playerB.players.value.first { it.id == "22" }.isCurrentPlayer)
        assertTrue(harness.playerB.players.value.first { it.id == "22" }.isActivePlayer)
        assertTrue(
            harness.playerBFactory.socket.sentFrames().any {
                it.command == "SUBSCRIBE" &&
                    it.headers["destination"] == WebSocketContract.gameSyncQueue
            }
        )
    }

    @Test
    fun syncKeepsLocalPlayerCurrentWhenDifferentPlayerIsActive() {
        val factory = FakeWebSocketFactory()
        val client = newClient(
            factory,
            sessionStateHolder = FakeSessionStateHolder(Session("token-a", "alice", 1)),
        )
        val latestState = twoPlayerState(
            turnPhase = "BUY_OR_BUILD",
            activeUserId = 2,
        )

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        factory.simulateText(syncFrame(syncBody(latestState, targetUserId = 1)))

        val alice = client.players.value.first { it.id == "11" }
        val bob = client.players.value.first { it.id == "22" }
        assertTrue(alice.isCurrentPlayer)
        assertFalse(alice.isActivePlayer)
        assertFalse(bob.isCurrentPlayer)
        assertTrue(bob.isActivePlayer)
    }

    // ── resetGameState ────────────────────────────────────────────────
    @Test
    fun transientDisconnectDoesNotClearWinnerOrActiveGameId() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())

        // Seed active game
        factory.simulateText(
            gameActionFrame(
                """{
                "type":"GAME_STARTED",
                "gameId":42,
                "payload":{
                    "activePlayerId":1,
                    "game":{
                        "id":42,
                        "lobbyCode":"ABC123",
                        "turnPhase":"ROLL_DICE"
                    },
                    "players":[]
                }
            }"""
            )
        )

        // Seed winner state
        factory.simulateText(
            gameActionFrame(
                """{
                "type":"GAME_END",
                "sender":"server",
                "payload":{
                    "winnerId":11,
                    "roundsPlayed":4
                }
            }"""
            )
        )

        // Simulate transient disconnect (reconnect path)
        factory.simulateClosed()

        // Identity should survive reconnect-safe resetGameState()
        assertEquals(42, client.activeGameId.value)
        assertEquals(11, client.winnerId.value)
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
        StompFrame(
            command = "MESSAGE",
            headers = mapOf(
                "destination" to WebSocketContract.publicTopic,
                "content-type" to "application/json",
            ),
            body = body,
        ).serialize()

    private fun lobbyQueueFrame(body: String): String =
        StompFrame(
            command = "MESSAGE",
            headers = mapOf(
                "destination" to WebSocketContract.lobbyQueue,
                "content-type" to "application/json",
            ),
            body = body,
        ).serialize()

    private fun gameTopicFrame(gameId: Int, body: String): String =
        StompFrame(
            command = "MESSAGE",
            headers = mapOf(
                "destination" to "${WebSocketContract.gameTopicPrefix}/$gameId",
                "content-type" to "application/json",
            ),
            body = body,
        ).serialize()

    private fun gameStartedFrame(gameId: Int): String =
        gameActionFrame(
            """{"type":"GAME_STARTED","gameId":$gameId,"payload":{"activePlayerId":1,"game":{"id":$gameId,"lobbyCode":"ABC1234","turnPhase":"ROLL_DICE"},"players":[]}}"""
        )

    private fun twoConnectedClients(reconnectScope: CoroutineScope): TwoClientHarness {
        val playerAFactory = FakeWebSocketFactory()
        val playerBFactory = FakeWebSocketFactory()
        val playerA = newClient(
            playerAFactory,
            sessionStateHolder = FakeSessionStateHolder(Session("token-a", "alice", 1)),
            reconnectScope = reconnectScope,
        )
        val playerB = newClient(
            playerBFactory,
            sessionStateHolder = FakeSessionStateHolder(Session("token-b", "bob", 2)),
            reconnectScope = reconnectScope,
        )

        playerA.connect()
        playerB.connect()
        playerAFactory.simulateOpen()
        playerBFactory.simulateOpen()
        playerAFactory.simulateText(connectedFrame())
        playerBFactory.simulateText(connectedFrame())

        val harness = TwoClientHarness(
            playerA = playerA,
            playerB = playerB,
            playerAFactory = playerAFactory,
            playerBFactory = playerBFactory,
        )
        harness.establishSharedLobby(gameId = 7)

        return harness
    }

    private fun FakeWebSocket.rollDiceFrames(): List<StompFrame> =
        sentFrames()
            .filter { it.headers["destination"] == WebSocketContract.rollDiceDestination }

    private fun FakeWebSocket.sentFrames(): List<StompFrame> =
        sentMessages.flatMap { parseFrames(StringBuilder(it)) }

    private fun FakeWebSocket.isSubscribedToGame(gameId: Int): Boolean =
        sentFrames().any {
            it.command == "SUBSCRIBE" &&
                it.headers["destination"] == "${WebSocketContract.gameTopicPrefix}/$gameId"
        }

    private fun TwoClientHarness.establishSharedLobby(gameId: Int) {
        playerAFactory.simulateText(
            lobbyQueueFrame(
                """{"type":"LOBBY_CREATED","sender":"server","gameId":$gameId,"payload":{"lobbyCode":"ABC123","gameId":$gameId,"playerId":11,"username":"alice","coins":3}}"""
            )
        )
        assertTrue(playerAFactory.socket.isSubscribedToGame(gameId))
        assertFalse(playerBFactory.socket.isSubscribedToGame(gameId))

        playerAFactory.simulateText(
            gameTopicFrame(
                gameId,
                """{"type":"LOBBY_JOINED","sender":"server","gameId":$gameId,"payload":{"gameId":$gameId,"playerId":22,"userId":2,"username":"bob","coins":3}}"""
            )
        )
        playerBFactory.simulateText(
            lobbyQueueFrame(
                """{"type":"LOBBY_ROSTER","sender":"server","gameId":$gameId,"payload":{"gameId":$gameId,"players":[{"playerId":11,"userId":1,"username":"alice","coins":3},{"playerId":22,"userId":2,"username":"bob","coins":3}]}}"""
            )
        )
        assertBothSubscribedToGame(gameId)
    }

    private fun TwoClientHarness.broadcastToBoth(body: String, gameId: Int = 7) {
        val frame = gameTopicFrame(gameId, body)
        listOf(playerAFactory, playerBFactory).forEach { factory ->
            if (factory.socket.isSubscribedToGame(gameId)) {
                factory.simulateText(frame)
            }
        }
    }

    private fun TwoClientHarness.assertBothSubscribedToGame(gameId: Int) {
        listOf(playerAFactory, playerBFactory).forEach { factory ->
            assertTrue(
                "expected ${factory.socket.request.url} to subscribe to ${WebSocketContract.gameTopicPrefix}/$gameId",
                factory.socket.isSubscribedToGame(gameId),
            )
        }
    }

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

    private data class TwoClientHarness(
        val playerA: OkHttpWebSocketClient,
        val playerB: OkHttpWebSocketClient,
        val playerAFactory: FakeWebSocketFactory,
        val playerBFactory: FakeWebSocketFactory,
    )

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

        fun gameStartedTwoPlayerBody(): String =
            """{"type":"GAME_STARTED","sender":"server","gameId":7,"payload":${twoPlayerState("ROLL_DICE", activeUserId = 1)}}"""

        fun gameActionSnapshotBody(
            state: String? = null,
            turnPhase: String? = null,
            activeUserId: Int = 2,
            player11Coins: Int = 3,
            player22Coins: Int = 4,
            bakerySupply: Int = 5,
            trainStationBuiltForPlayer11: Boolean = false,
            purchaseType: String? = null,
            landmarkType: String? = null,
        ): String {
            val resolvedState = state ?: run {
                twoPlayerState(
                    turnPhase = turnPhase ?: "RESOLVE_EFFECTS",
                    activeUserId = activeUserId,
                    player11Coins = player11Coins,
                    player22Coins = player22Coins,
                    bakerySupply = bakerySupply,
                    trainStationBuiltForPlayer11 = trainStationBuiltForPlayer11,
                )
            }
            val purchaseFields = buildString {
                purchaseType?.let { append(",\"purchaseType\":\"").append(it).append("\"") }
                landmarkType?.let { append(",\"landmarkType\":\"").append(it).append("\"") }
            }
            return """{"type":"GAME_ACTION","sender":"server","gameId":7,"payload":{"state":$resolvedState$purchaseFields}}"""
        }

        fun rollDiceSnapshotBody(result: String, lastDiceRoll: Int): String =
            """{"type":"ROLL_DICE","sender":"server","gameId":7,"payload":{"result":$result,"state":${twoPlayerState("RESOLVE_EFFECTS", activeUserId = 1, lastDiceRoll = lastDiceRoll)}}}"""

        fun syncBody(state: String, targetUserId: Int): String =
            """{"type":"SYNC","sender":"server","gameId":7,"payload":{"targetUserId":$targetUserId,"state":$state}}"""

        fun twoPlayerState(
            turnPhase: String,
            activeUserId: Int,
            player11Coins: Int = 3,
            player22Coins: Int = 4,
            bakerySupply: Int = 5,
            trainStationBuiltForPlayer11: Boolean = false,
            lastDiceRoll: Int? = null,
        ): String {
            val currentTurnIndex = if (activeUserId == 1) 0 else 1
            val lastDiceRollField = lastDiceRoll?.let { ""","lastDiceRoll":$it""" } ?: ""
            return """{"game":{"id":7,"lobbyCode":"ABC1234","status":"IN_PROGRESS","turnPhase":"$turnPhase","roundNumber":2,"currentTurnIndex":$currentTurnIndex$lastDiceRollField},"activePlayerId":$activeUserId,"players":[{"id":11,"userId":1,"coins":$player11Coins},{"id":22,"userId":2,"coins":$player22Coins}],"playerUsernames":{"11":"alice","22":"bob"},"playerCards":{"11":[{"playerId":11,"cardType":"BAKERY","quantity":1}],"22":[{"playerId":22,"cardType":"CAFE","quantity":1}]},"playerLandmarks":{"11":[{"playerId":11,"landmarkType":"TRAIN_STATION","isBuilt":$trainStationBuiltForPlayer11}],"22":[{"playerId":22,"landmarkType":"TRAIN_STATION","isBuilt":false}]},"marketplace":{"BAKERY":$bakerySupply,"CAFE":6},"cardDefinitions":[{"cardType":"BAKERY","cost":1,"income":1,"color":"GREEN","establishmentType":"BREAD","paymentSource":"BANK","activationNumbers":[2,3]},{"cardType":"CAFE","cost":2,"income":1,"color":"RED","establishmentType":"RESTAURANT","paymentSource":"PLAYER","activationNumbers":[3]}],"landmarkDefinitions":[{"landmarkType":"TRAIN_STATION","cost":4}],"turnOrder":[1,2]}"""
        }

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
                """"turnOrder":[1,2]}}}"""
    }

    /**
     * Regression test for the 2-player join bug: the joiner-only `LOBBY_ROSTER`
     * message must (a) parse the `payload.players` array — the server wraps it
     * in `LobbyRosterDto`, not as a bare JSON array — (b) capture the gameId
     * and subscribe to `/topic/game/{gameId}` so subsequent broadcasts arrive,
     * and (c) emit `lobbyEntered` so the UI navigates. Without all three, the
     * joiner gets stuck on Home even though the server has accepted them.
     */
    @Test
    fun lobbyRosterPopulatesPlayersSubscribesToGameTopicAndEmitsLobbyEntered() = runTest {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)
        var enteredCount = 0
        client.lobbyEntered.onEach { enteredCount++ }.launchIn(backgroundScope)

        client.connect()
        factory.simulateOpen()
        factory.simulateText(connectedFrame())
        runCurrent()

        factory.simulateText(
            gameActionFrame(
                """{"type":"LOBBY_ROSTER","sender":"SERVER","gameId":7,"payload":{"players":[""" +
                """{"playerId":1,"userId":1,"username":"host","gameId":7,"turnOrder":0,"coins":3},""" +
                """{"playerId":2,"userId":2,"username":"alice","gameId":7,"turnOrder":1,"coins":3}]}}"""
            )
        )
        runCurrent()

        assertEquals(7, client.activeGameId.value)
        assertEquals(2, client.players.value.size)
        assertEquals("host", client.players.value[0].displayName)
        assertEquals("alice", client.players.value[1].displayName)
        assertTrue(
            "expected a SUBSCRIBE frame for /topic/game/7",
            factory.socket.sentMessages.any {
                it.startsWith("SUBSCRIBE\n") && it.contains("destination:/topic/game/7")
            }
        )
        assertEquals(1, enteredCount)
    }

    private fun newClient(
        factory: FakeWebSocketFactory,
        sessionStateHolder: SessionStateHolder = FakeSessionStateHolder(DEFAULT_SESSION),
        // Inert by default: a StandardTestDispatcher whose scheduler is never
        // advanced, so an auto-reconnect scheduled by a close/failure stays
        // queued and does not race assertions in tests that don't drive it.
        // Reconnect tests pass backgroundScope explicitly and drive it.
        reconnectScope: CoroutineScope = CoroutineScope(StandardTestDispatcher()),
        reconnectDelaysMs: List<Long> = listOf(0L),
    ) = OkHttpWebSocketClient(
        websocketUrl = "ws://10.0.2.2:8080/ws",
        sessionStateHolder = sessionStateHolder,
        webSocketFactory = factory,
        reconnectScope = reconnectScope,
        reconnectDelaysMs = reconnectDelaysMs,
    )
}
