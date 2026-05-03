package com.machikoro.client.network.websocket

import android.util.Log
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.session.SessionStateHolder
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject

class OkHttpWebSocketClient(
    private val websocketUrl: String,
    private val sessionStateHolder: SessionStateHolder,
    private val webSocketFactory: WebSocketFactory = OkHttpWebSocketFactory(),
) : WebSocketClient {
    override val connectionStatus: StateFlow<ConnectionStatus>
        get() = mutableConnectionStatus.asStateFlow()

    override val gamePhase: StateFlow<GamePhase>
        get() = mutableGamePhase.asStateFlow()

    override val players: StateFlow<List<PlayerCoinState>>
        get() = mutablePlayers.asStateFlow()

    override val lobbyCode: StateFlow<String?> // Internal state for the latest lobby code returned by the backend
        get() = mutableLobbyCode.asStateFlow()

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())
    private val mutableLobbyCode = MutableStateFlow<String?>(null) // Exposes lobby code as read-only StateFlow to UI/ViewModels
    private val frameBuffer = StringBuilder()

    @Volatile
    private var webSocket: WebSocket? = null

    override fun connect() {
        synchronized(this) {
            if (webSocket != null) {
                return
            }
            // Strict-reject server (#159): no session means we have nothing to
            // present at STOMP CONNECT. Don't open a socket only to be kicked.
            // Status stays at whatever it was — IDLE on first launch, DISCONNECTED
            // after a logout — so the user doesn't see a misleading "connection
            // error" when they simply aren't logged in yet.
            if (sessionStateHolder.session.value == null) {
                Log.d(TAG, "Skipping WS connect — no session token")
                return
            }

            val request = try {
                Request.Builder()
                    .url(websocketUrl)
                    .build()
            } catch (_: IllegalArgumentException) {
                Log.e(TAG, "Invalid WebSocket URL: $websocketUrl")
                mutableConnectionStatus.value = ConnectionStatus.ERROR
                return
            }

            mutableConnectionStatus.value = ConnectionStatus.CONNECTING
            frameBuffer.setLength(0)
            Log.d(TAG, "Opening WebSocket connection to $websocketUrl")
            webSocket = webSocketFactory.create(request, listener)
        }
    }

    override fun disconnect() {
        val currentSocket = synchronized(this) {
            val socket = webSocket
            webSocket = null
            socket
        }
        // True no-op when there was nothing to disconnect. Without this, every
        // session-driven LaunchedEffect emission of `null` (including the initial
        // one on cold start) would flip the status from IDLE to DISCONNECTED,
        // which the start screen renders as "Connection status: disconnected"
        // before the user has tried to connect.
        if (currentSocket == null) return

        currentSocket.send(StompFrame(command = "DISCONNECT").serialize())
        currentSocket.close(NORMAL_CLOSURE_STATUS, "Client disconnect")
        Log.d(TAG, "Disconnect requested by client")
        mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
        resetGameState()
    }

    override fun sendCreateLobby() {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "sendCreateLobby called but no active WebSocket connection")
            return
        }

        socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.createLobbyDestination,
                    "content-type" to "application/json"
                ),
                body = """{"type":"JOIN","sender":"${WebSocketContract.defaultSender}"}"""
            ).serialize()
        )

        Log.d(TAG, "Lobby create message sent")
    }
    override fun sendGameStart() {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "sendGameStart called but no active WebSocket connection")
            return
        }
        socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.gameStartDestination,
                    "content-type" to "application/json"
                ),
                body = GAME_START_BODY
            ).serialize()
        )
        Log.d(TAG, "Game start message sent")
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened: ${response.code} ${response.message}")
            // Read the token at handshake time, not at connect-time. Closes the
            // race where the user logged out between connect() and the WS being
            // ready — without this we'd send a CONNECT frame with a stale token
            // that the server would reject anyway.
            val token = sessionStateHolder.session.value?.sessionToken
            if (token == null) {
                Log.w(TAG, "WS opened but session vanished — closing without sending CONNECT")
                webSocket.close(NORMAL_CLOSURE_STATUS, "No session at CONNECT time")
                return
            }
            webSocket.send(
                StompFrame(
                    command = "CONNECT",
                    headers = mapOf(
                        "accept-version" to WebSocketContract.stompVersion,
                        "host" to websocketHostHeader(),
                        "heart-beat" to "0,0",
                        AUTH_HEADER to "$BEARER_PREFIX$token",
                    )
                ).serialize()
            )
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            synchronized(frameBuffer) {
                frameBuffer.append(text)
                parseFrames(frameBuffer).forEach(::handleFrame)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code / $reason")
            webSocket.close(code, reason)
            clearSocket()
            mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
            resetGameState()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code / $reason")
            clearSocket()
            mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
            resetGameState()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val responseDetails = response?.let { "HTTP ${it.code} ${it.message}" } ?: "No HTTP response"
            Log.e(
                TAG,
                "WebSocket failure for $websocketUrl. $responseDetails. Reason: ${t.message}",
                t
            )
            clearSocket()
            mutableConnectionStatus.value = ConnectionStatus.ERROR
            resetGameState()
        }
    }

    private fun handleFrame(frame: StompFrame) {
        when (frame.command) {
            "CONNECTED" -> {
                Log.d(TAG, "STOMP connected")
                mutableConnectionStatus.value = ConnectionStatus.CONNECTED
                subscribeToPublicTopic()
                sendJoinMessage()
            }

            "MESSAGE" -> {
                Log.d(TAG, "STOMP message received: ${frame.body}")
                parseGameActionPhase(frame.body)?.let { mutableGamePhase.value = it }
            }

            "ERROR" -> {
                Log.e(TAG, "STOMP error frame received: ${frame.body}")
                mutableConnectionStatus.value = ConnectionStatus.ERROR
            }
        }
    }

    private fun parseGameActionPhase(body: String): GamePhase? {
        if (body.isBlank()) return null
        val json = try {
            JSONObject(body)
        } catch (e: JSONException) {
            Log.w(TAG, "Failed to parse MESSAGE frame as JSON: ${e.message}")
            return null
        }
        if (json.optString("type") != GAME_ACTION_TYPE) return null
        val payload = json.optJSONObject("payload") ?: return null
        val phaseName = payload.optString("turnPhase").takeIf { it.isNotEmpty() } ?: return null
        return runCatching { GamePhase.valueOf(phaseName) }.getOrNull()
    }

    private fun subscribeToPublicTopic() {
        webSocket?.send(
            StompFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "public-topic",
                    "destination" to WebSocketContract.publicTopic
                )
            ).serialize()
        )
    }

    private fun sendJoinMessage() {
        webSocket?.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.addUserDestination,
                    "content-type" to "application/json"
                ),
                body = """{"type":"JOIN","sender":"${WebSocketContract.defaultSender}"}"""
            ).serialize()
        )
    }

    private fun clearSocket() {
        synchronized(this) {
            webSocket = null
        }
    }

    private fun resetGameState() {
        mutableGamePhase.value = GamePhase.NONE
        // Keep #37 coin display clean after game end/disconnect until #45 reset flow owns this state.
        mutablePlayers.value = emptyList()
    }

    private fun websocketHostHeader(): String {
        val uri = URI(websocketUrl)
        val port = uri.port
        return if (port == -1 || port == defaultPort(uri.scheme.orEmpty())) {
            uri.host
        } else {
            "${uri.host}:$port"
        }
    }

    private fun defaultPort(scheme: String): Int = when (scheme) {
        "wss", "https" -> 443
        else -> 80
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
        private const val TAG = "OkHttpWebSocketClient"
        private const val GAME_ACTION_TYPE = "GAME_ACTION"
        // Match Server #159's StompAuthChannelInterceptor expectation:
        // accessor.getFirstNativeHeader("Authorization") + BEARER_PREFIX = "Bearer ".
        private const val AUTH_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val GAME_START_BODY =
            """{"type":"START","sender":"${WebSocketContract.defaultSender}"}"""
    }
}
