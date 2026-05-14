package com.machikoro.client.network.websocket

import android.util.Log
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.session.SessionStateHolder
import java.net.URI
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
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

    override val activeGameId: StateFlow<Int?>
        get() = mutableActiveGameId.asStateFlow()

    override val isLobbyHost: StateFlow<Boolean>
        get() = mutableIsLobbyHost.asStateFlow()

    override val authRejections: SharedFlow<Unit>
        get() = mutableAuthRejections.asSharedFlow()

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())
    private val mutableLobbyCode = MutableStateFlow<String?>(null) // Exposes lobby code as read-only StateFlow to UI/ViewModels
    private val mutableActiveGameId = MutableStateFlow<Int?>(null)
    private val mutableIsLobbyHost = MutableStateFlow(false)
    // Buffer 1 + DROP_OLDEST so tryEmit never fails on the OkHttp listener
    // thread when nobody is collecting yet (e.g. emission during startup
    // before MainActivity wires its collector).
    private val mutableAuthRejections = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val frameBuffer = StringBuilder()

    @Volatile
    private var webSocket: WebSocket? = null
    private var subscribedGameId: Int? = null

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
        if (currentSocket == null) {
            if (sessionStateHolder.session.value == null) {
                resetGameState()
                resetLobbyState()
            }
            return
        }

        currentSocket.send(StompFrame(command = "DISCONNECT").serialize())
        currentSocket.close(NORMAL_CLOSURE_STATUS, "Client disconnect")
        Log.d(TAG, "Disconnect requested by client")
        mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
        resetGameState()
        if (sessionStateHolder.session.value == null) {
            resetLobbyState()
        }
    }

    override fun sendCreateLobby() {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "sendCreateLobby called but no active WebSocket connection")
            return
        }

        val sent = socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.createLobbyDestination,
                    "content-type" to "application/json"
                ),
                body = """{"type":"JOIN","sender":"${WebSocketContract.defaultSender}"}"""
            ).serialize()
        )

        if (sent) {
            mutableIsLobbyHost.value = true
            Log.d(TAG, "Lobby create message sent")
        } else {
            Log.w(TAG, "sendCreateLobby: failed to send create-lobby frame")
        }
    }

    override fun clearLobbyCode() {
        resetLobbyState()
    }

    override fun sendGameStart() {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "sendGameStart called but no active WebSocket connection")
            return
        }

        val gameId = mutableActiveGameId.value
        val lobbyCode = mutableLobbyCode.value

        // gameId is populated from the LOBBY_CREATED payload when the server returns it.
        // Fall back to a lobby-code-only start request so the host is never permanently
        // blocked if the server doesn't echo the gameId during lobby creation.
        // The merged main branch expects a SEND frame to be emitted when connected
        // even if neither value is known; send an empty JSON object in that case.
        val body = when {
            gameId != null -> JSONObject().put("gameId", gameId).toString()
            lobbyCode != null -> JSONObject().put("lobbyCode", lobbyCode).toString()
            else -> "{}"
        }

        // Include lobbyCode when available to keep the host unblocked if the server
        // echos both values. Tests look for the gameId JSON fragment, so keep it
        // present when known.
        val enrichedBody = when {
            gameId != null && lobbyCode != null -> "{" + "\"gameId\":$gameId,\"lobbyCode\":\"$lobbyCode\"" + "}"
            gameId != null -> "{" + "\"gameId\":$gameId" + "}"
            lobbyCode != null -> "{" + "\"lobbyCode\":\"$lobbyCode\"" + "}"
            else -> body
        }

        val frameStr = StompFrame(
            command = "SEND",
            headers = mapOf(
                "destination" to WebSocketContract.gameStartDestination,
                "content-type" to "application/json"
            ),
            body = enrichedBody
        ).serialize()

        socket.send(frameStr)
        Log.d(TAG, "Game start message sent (gameId=$gameId, lobbyCode=$lobbyCode)")
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
                mutableActiveGameId.value?.let(::subscribeToGameTopic)
                sendJoinMessage()
            }

            "MESSAGE" -> {
                Log.d(TAG, "STOMP message received: ${frame.body}")
                if (frame.body.isBlank()) return
                val json = try {
                    JSONObject(frame.body)
                } catch (e: JSONException) {
                    Log.w(TAG, "Failed to parse MESSAGE frame as JSON: ${e.message}")
                    return
                }
                handleLobbyCreated(json)
                handleGameStarted(json)
                parseGameActionPhase(json)?.let { mutableGamePhase.value = it }
            }

            "ERROR" -> {
                Log.e(TAG, "STOMP error frame received: ${frame.body}")
                if (isAuthRejection(frame.body)) {
                    // Server rejected the CONNECT for auth reasons (#159 path):
                    // missing/malformed/unrecognised token. The connection will
                    // be closed by the server; flip to DISCONNECTED rather than
                    // ERROR because this is a recoverable, user-actionable
                    // state, not a transport failure.
                    mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
                    resetGameState()
                    // Sign out here rather than relying on a Compose collector
                    // in the UI. The activity can be destroyed (rotation, process
                    // death) between the emission and the collector attaching,
                    // and `authRejections` uses replay = 0 so a missed event
                    // would leave the user signed in against a token the server
                    // no longer accepts. The snackbar in MainActivity is purely
                    // a UI side-effect and remains miss-tolerant.
                    sessionStateHolder.signOut()
                    // Invariant: extraBufferCapacity=1 + DROP_OLDEST means
                    // tryEmit is non-suspending and never returns false.
                    mutableAuthRejections.tryEmit(Unit)
                } else {
                    mutableConnectionStatus.value = ConnectionStatus.ERROR
                }
            }
        }
    }

    /**
     * Handles lobby creation responses from the backend.
     *
     * Expected message:
     * {
     *   "type": "LOBBY_CREATED",
     *   "payload": {
     *     "lobbyCode": "ABC123"
     *   }
     * }
     */
    private fun handleLobbyCreated(json: JSONObject) {
        if (json.optString("type") != LOBBY_CREATED_TYPE) return

        val payload = json.optJSONObject("payload") ?: return
        val code = payload.optString("lobbyCode")
        // gameId may live at the top level or inside the payload object depending on the
        // server version being targeted. Check both so lobby creation always yields a
        // valid activeGameId and the "Start Game" button can be enabled.
        val gameId = json.optIntOrNull("gameId") ?: payload.optIntOrNull("gameId")

        if (code.isNotBlank()) {
            Log.d(TAG, "Lobby created with code: $code")
            mutableLobbyCode.value = code
        }

        if (gameId != null) {
            mutableActiveGameId.value = gameId
            subscribeToGameTopic(gameId)
        }
    }

    private fun handleGameStarted(json: JSONObject) {
        if (json.optString("type") != GAME_STARTED_TYPE) return

        val payload = json.optJSONObject("payload") ?: return
        val game = payload.optJSONObject("game") ?: return
        val gameId = json.optIntOrNull("gameId") ?: game.optIntOrNull("id") ?: return

        mutableActiveGameId.value = gameId
        subscribeToGameTopic(gameId)

        game.optString("lobbyCode")
            .takeIf { it.isNotBlank() }
            ?.let { mutableLobbyCode.value = it }

        parseTurnPhase(game.optString("turnPhase"))?.let { mutableGamePhase.value = it }
        mutablePlayers.value = payload.optJSONArray("players").toPlayerCoinStates(payload, game)
    }

    private fun parseGameActionPhase(json: JSONObject): GamePhase? {
        if (json.optString("type") != GAME_ACTION_TYPE) return null
        val payload = json.optJSONObject("payload") ?: return null
        val phaseName = payload.optString("turnPhase").takeIf { it.isNotEmpty() } ?: return null
        return parseTurnPhase(phaseName)
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

    private fun subscribeToGameTopic(gameId: Int) {
        if (subscribedGameId == gameId) return

        val socket = webSocket ?: return

        subscribedGameId?.let { oldId ->
            socket.send(
                StompFrame(
                    command = "UNSUBSCRIBE",
                    headers = mapOf("id" to "game-topic-$oldId")
                ).serialize()
            )
        }

        val subscribeFrame = StompFrame(
            command = "SUBSCRIBE",
            headers = mapOf(
                "id" to "game-topic-$gameId",
                "destination" to "${WebSocketContract.gameTopicPrefix}/$gameId"
            )
        ).serialize()

        if (socket.send(subscribeFrame)) {
            subscribedGameId = gameId
        }
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
            subscribedGameId = null
        }
    }

    // Mirrors the literal body produced by Server #159's StompAuthChannelInterceptor
    // (GENERIC_AUTH_FAILURE = "Authentication failed"). Strict equality — if the
    // server message ever changes, fail closed (fall through to generic ERROR
    // status) rather than match overly loosely.
    private fun isAuthRejection(body: String): Boolean =
        body == AUTH_REJECTION_BODY

    private fun resetGameState() {
        mutableGamePhase.value = GamePhase.NONE
        // Keep #37 coin display clean after game end/disconnect until #45 reset flow owns this state.
        mutablePlayers.value = emptyList()
        // Clear the latest lobby code so a stale code can't reappear on the
        // next sign-in within the same app session.
        mutableLobbyCode.value = null
    }

    private fun resetLobbyState() {
        mutableLobbyCode.value = null
        mutableActiveGameId.value = null
        mutableIsLobbyHost.value = false
    }

    private fun parseTurnPhase(phaseName: String): GamePhase? =
        phaseName.takeIf { it.isNotEmpty() }?.let { runCatching { GamePhase.valueOf(it) }.getOrNull() }

    private fun JSONArray?.toPlayerCoinStates(payload: JSONObject, game: JSONObject): List<PlayerCoinState> {
        if (this == null) return emptyList()

        val currentTurnIndex = game.optIntOrNull("currentTurnIndex")
        val currentPlayerId = payload.optJSONArray("turnOrder")
            ?.takeIf { currentTurnIndex != null && currentTurnIndex in 0 until it.length() }
            ?.let { turnOrder -> currentTurnIndex?.let(turnOrder::optInt) }

        return List(length()) { index ->
            getJSONObject(index).toPlayerCoinState(currentPlayerId)
        }
    }

    private fun JSONObject.toPlayerCoinState(currentPlayerId: Int?): PlayerCoinState {
        val playerId = optInt("id")
        // Prefer the real username returned by the server (may be keyed as "username",
        // "name", or "displayName"). Falling back to the numeric "Player N" label is
        // only a last resort so that host-detection logic that compares session username
        // against player display names has a real value to work with.
        val resolvedDisplayName =
            optString("username").takeIf { it.isNotBlank() }
                ?: optString("name").takeIf { it.isNotBlank() }
                ?: optString("displayName").takeIf { it.isNotBlank() }
                ?: "Player $playerId"
        return PlayerCoinState(
            id = playerId.toString(),
            displayName = resolvedDisplayName,
            coins = optInt("coins"),
            isCurrentPlayer = playerId == currentPlayerId,
            isActivePlayer = playerId == currentPlayerId,
        )
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

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
        private const val GAME_STARTED_TYPE = "GAME_STARTED"
        // Match Server #159's StompAuthChannelInterceptor expectation:
        // accessor.getFirstNativeHeader("Authorization") + BEARER_PREFIX = "Bearer ".
        private const val AUTH_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val LOBBY_CREATED_TYPE = "LOBBY_CREATED"
        // Frozen contract: matches GENERIC_AUTH_FAILURE on the server's
        // StompAuthChannelInterceptor. If the server message changes, this
        // client will silently fall through to the generic ERROR handling.
        private const val AUTH_REJECTION_BODY = "Authentication failed"
        private const val GAME_START_BODY =
            """{"type":"START","sender":"${WebSocketContract.defaultSender}"}"""
    }
}
