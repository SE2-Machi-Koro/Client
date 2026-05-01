package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_ACTION","sender":"server","payload":{"turnPhase":"ROLL_DICE"}}""")
        )

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
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_ACTION","payload":{"other":"value"}}""")
        )

        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

    @Test
    fun disconnectResetsGamePhaseToNone() {
        val factory = FakeWebSocketFactory()
        val client = newClient(factory)

        client.connect()
        factory.simulateOpen()
        factory.simulateText("CONNECTED\nversion:1.2\n\n\u0000")
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"BUY_OR_BUILD"}}""")
        )
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
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"RESOLVE_EFFECTS"}}""")
        )
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
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"END_TURN"}}""")
        )
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
        factory.simulateText(
            gameActionFrame("""{"type":"GAME_ACTION","payload":{"turnPhase":"ROLL_DICE"}}""")
        )
        assertEquals(GamePhase.ROLL_DICE, client.gamePhase.value)

        factory.simulateFailure(IOException("boom"))

        assertEquals(GamePhase.NONE, client.gamePhase.value)
    }

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

        fun simulateOpen() {
            listener.onOpen(socket, createResponse(socket.request))
        }

        fun simulateText(text: String) {
            listener.onMessage(socket, text)
        }

        fun simulateClosing() {
            listener.onClosing(socket, 1000, "closing")
        }

        fun simulateClosed() {
            listener.onClosed(socket, 1000, "closed")
        }

        fun simulateFailure(throwable: Throwable) {
            listener.onFailure(socket, throwable, createResponse(socket.request))
        }

        private fun createResponse(request: Request): Response {
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(101)
                .message("Switching Protocols")
                .build()
        }
    }

    private class FakeWebSocket : WebSocket {
        lateinit var request: Request
        var closed = false
        val sentMessages = mutableListOf<String>()

        override fun request(): Request = request

        override fun queueSize(): Long = 0L

        override fun send(text: String): Boolean {
            sentMessages += text
            return true
        }

        override fun send(bytes: ByteString): Boolean = false

        override fun close(code: Int, reason: String?): Boolean {
            closed = true
            return true
        }

        override fun cancel() {
            closed = true
        }
    }

    private class FakeSessionStateHolder(initial: Session? = null) : SessionStateHolder {
        private val mutableSession = MutableStateFlow(initial)
        override val session: StateFlow<Session?> = mutableSession.asStateFlow()
        override fun signIn(token: String, username: String) {
            mutableSession.value = Session(token, username)
        }
        override fun signOut() {
            mutableSession.value = null
        }
    }

    private companion object {
        const val DEFAULT_TOKEN = "test-token"
        const val DEFAULT_USERNAME = "test-user"
        val DEFAULT_SESSION = Session(DEFAULT_TOKEN, DEFAULT_USERNAME)
    }

    /**
     * Construct a client with a stub session by default so existing test
     * assertions that don't care about auth keep working.
     */
    private fun newClient(
        factory: FakeWebSocketFactory,
        sessionStateHolder: SessionStateHolder = FakeSessionStateHolder(DEFAULT_SESSION),
    ) = OkHttpWebSocketClient(
        websocketUrl = "ws://10.0.2.2:8080/ws",
        sessionStateHolder = sessionStateHolder,
        webSocketFactory = factory,
    )

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
        val sessionHolder = FakeSessionStateHolder(initial = Session("stale-token", "alice"))
        val client = newClient(factory, sessionStateHolder = sessionHolder)

        client.connect()
        sessionHolder.signIn(token = "fresh-token", username = "alice")
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
}
