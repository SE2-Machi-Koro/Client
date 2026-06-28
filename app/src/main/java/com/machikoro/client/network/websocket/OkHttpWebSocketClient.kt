package com.machikoro.client.network.websocket

import android.util.Log
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.ShopItemColor
import com.machikoro.client.domain.model.shop.CardDefinition
import com.machikoro.client.domain.model.shop.CardDefinitions
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.network.error.ClientError
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.AccusationResult
import com.machikoro.client.domain.model.state.ChatMessageState
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.session.SessionStateHolder
import java.net.URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val reconnectScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val reconnectDelaysMs: List<Long> = RECONNECT_DELAYS_MS,
) : WebSocketClient {
    override val connectionStatus: StateFlow<ConnectionStatus>
        get() = mutableConnectionStatus.asStateFlow()

    override val gamePhase: StateFlow<GamePhase>
        get() = mutableGamePhase.asStateFlow()

    override val players: StateFlow<List<PlayerCoinState>>
        get() = mutablePlayers.asStateFlow()

    override val lobbyCode: StateFlow<String?>
        get() = mutableLobbyCode.asStateFlow()

    override val lobbyEntered: SharedFlow<Unit>
        get() = mutableLobbyEntered.asSharedFlow()

    override val lobbyJoinErrors: SharedFlow<ClientError.WebSocket>
        get() = mutableLobbyJoinErrors.asSharedFlow()

    override val hostLeftLobby: SharedFlow<Unit>
        get() = mutableHostLeftLobby.asSharedFlow()

    override val winnerId: StateFlow<Int?>
        get() = mutableWinnerId.asStateFlow()

    override val diceResult: StateFlow<List<Int>?>
        get() = mutableDiceResult.asStateFlow()
    override val diceRollTick: StateFlow<Long>
        get() = mutableDiceRollTick.asStateFlow()

    override val activePlayerId: StateFlow<Int?>
        get() = mutableActivePlayerId.asStateFlow()

    override val activeGameId: StateFlow<Int?>
        get() = mutableActiveGameId.asStateFlow()

    override val isLobbyHost: StateFlow<Boolean>
        get() = mutableIsLobbyHost.asStateFlow()

    override val gameStatus: StateFlow<GameStatus?>
        get() = mutableGameStatus.asStateFlow()

    override val roundNumber: StateFlow<Int?>
        get() = mutableRoundNumber.asStateFlow()

    override val playerCards: StateFlow<Map<Int, List<PlayerCardState>>>
        get() = mutablePlayerCards.asStateFlow()

    override val playerLandmarks: StateFlow<Map<Int, List<PlayerLandmarkState>>>
        get() = mutablePlayerLandmarks.asStateFlow()

    override val marketplace: StateFlow<Map<CardType, Int>>
        get() = mutableMarketplace.asStateFlow()

    override val shopItems: StateFlow<List<ShopItem>>
        get() = mutableShopItems.asStateFlow()

    override val purchaseEvents: SharedFlow<PurchaseEvent>
        get() = mutablePurchaseEvents.asSharedFlow()

    override val authRejections: SharedFlow<Unit>
        get() = mutableAuthRejections.asSharedFlow()

    override val accusationResults: SharedFlow<AccusationResult>
        get() = mutableAccusationResults.asSharedFlow()

    override val coinDeltas: SharedFlow<Int>
        get() = mutableCoinDeltas.asSharedFlow()

    override val accusationErrors: SharedFlow<String>
        get() = mutableAccusationErrors.asSharedFlow()

    override val chatMessages: SharedFlow<ChatMessageState>
        get() = mutableChatMessages.asSharedFlow()

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())
    private val mutableLobbyCode = MutableStateFlow<String?>(null)
    private val mutableLobbyEntered = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableLobbyJoinErrors = MutableSharedFlow<ClientError.WebSocket>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val mutableHostLeftLobby = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableWinnerId = MutableStateFlow<Int?>(null)

    private val mutableDiceResult = MutableStateFlow<List<Int>?>(null)
    private val mutableDiceRollTick = MutableStateFlow(0L)
    private val mutableActivePlayerId = MutableStateFlow<Int?>(null)
    private val mutableActiveGameId = MutableStateFlow<Int?>(null)
    private val mutableIsLobbyHost = MutableStateFlow(false)
    private val mutableGameStatus = MutableStateFlow<GameStatus?>(null)
    private val mutableRoundNumber = MutableStateFlow<Int?>(null)
    private val mutablePlayerCards =
        MutableStateFlow<Map<Int, List<PlayerCardState>>>(emptyMap())
    private val mutablePlayerLandmarks =
        MutableStateFlow<Map<Int, List<PlayerLandmarkState>>>(emptyMap())
    private val mutableMarketplace = MutableStateFlow<Map<CardType, Int>>(emptyMap())
    private val mutableShopItems = MutableStateFlow<List<ShopItem>>(emptyList())
    private val mutablePurchaseEvents = MutableSharedFlow<PurchaseEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableAuthRejections = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableAccusationResults = MutableSharedFlow<AccusationResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableCoinDeltas = MutableSharedFlow<Int>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableAccusationErrors = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableChatMessages = MutableSharedFlow<ChatMessageState>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val frameBuffer = StringBuilder()

    @Volatile
    private var webSocket: WebSocket? = null
    private var subscribedGameId: Int? = null
    @Volatile
    private var pendingCreatedLobbyJoin = false
    // STOMP session ID assigned by the server on CONNECTED — used for lobby queue subscription
    private var stompSessionId: String? = null

    // Auto-reconnect state. `intentionalDisconnect` is set whenever the client
    // tears the connection down itself (disconnect() or an auth rejection) so
    // the listener can tell a deliberate close apart from an unexpected drop.
    @Volatile
    private var intentionalDisconnect = false
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    // Periodic STOMP heartbeat emitter, started on CONNECTED and cancelled when
    // the socket goes away. Keeps an idle connection alive so platform proxies
    // don't reap it mid-game (server #426).
    private var heartbeatJob: Job? = null

    init {
        require(reconnectDelaysMs.isNotEmpty()) { "reconnectDelaysMs must not be empty" }
    }

    override fun connect() {
        synchronized(this) {
            intentionalDisconnect = false
            if (webSocket != null) return
            if (sessionStateHolder.session.value == null) {
                Log.d(TAG, "Skipping WS connect — no session token")
                return
            }
            val request = try {
                Request.Builder().url(websocketUrl).build()
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
        intentionalDisconnect = true
        cancelReconnect()
        val currentSocket = synchronized(this) {
            heartbeatJob?.cancel()
            heartbeatJob = null
            val socket = webSocket
            webSocket = null
            socket
        }
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
            pendingCreatedLobbyJoin = true
            Log.d(TAG, "Lobby create message sent")
        } else {
            Log.w(TAG, "sendCreateLobby: failed to send create-lobby frame")
        }
    }

    /**
     * Sends a join-lobby request to the backend.
     *
     * The backend expects the lobby code inside the payload and resolves the
     * authenticated user from the STOMP session, so the sender field is not used
     * for identity.
     */
    override fun sendJoinLobby(lobbyCode: String) {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "sendJoinLobby called but no active WebSocket connection")
            return
        }

        val payload = JSONObject()
            .put("lobbyCode", lobbyCode)

        val enrichedBody = JSONObject()
            .put("type", "JOIN")
            .put("sender", WebSocketContract.defaultSender)
            .put("payload", payload)
            .toString()

        val frameStr = StompFrame(
            command = "SEND",
            headers = mapOf(
                "destination" to WebSocketContract.joinLobbyDestination,
                "content-type" to "application/json"
            ),
            body = enrichedBody
        ).serialize()

        if (socket.send(frameStr)) {
            Log.d(TAG, "Join lobby message sent with code: $lobbyCode")
        } else {
            Log.w(TAG, "sendJoinLobby: failed to send join-lobby frame")
        }
    }

    override fun sendReadyToggle(isReady: Boolean) {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "sendReadyToggle called but no active WebSocket connection")
            return
        }
        val gameId = mutableActiveGameId.value
        if (gameId == null) {
            Log.w(TAG, "sendReadyToggle: no active game, dropping toggle")
            return
        }
        val body = JSONObject()
            .put("type", "LOBBY_ROSTER")
            .put("sender", WebSocketContract.defaultSender)
            .put("payload", JSONObject()
                .put("gameId", gameId)
                .put("isReady", isReady)
            )
            .toString()
        val sent = socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.readyToggleDestination,
                    "content-type" to "application/json"
                ),
                body = body
            ).serialize()
        )
        if (sent) Log.d(TAG, "Ready toggle sent: isReady=$isReady, gameId=$gameId")
        else Log.w(TAG, "sendReadyToggle: failed to send frame")
    }

    override fun sendLeaveLobby(gameId: Int) {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "sendLeaveLobby called but no active WebSocket connection")
            return
        }
        val body = JSONObject()
            .put("type", "LEAVE")
            .put("sender", WebSocketContract.defaultSender)
            .put("payload", JSONObject().put("gameId", gameId))
            .toString()
        val sent = socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.leaveLobbyDestination,
                    "content-type" to "application/json"
                ),
                body = body
            ).serialize()
        )
        if (sent) Log.d(TAG, "Leave lobby message sent (gameId=$gameId)")
        else Log.w(TAG, "sendLeaveLobby: failed to send frame")
    }

    override fun clearLobbyCode() {
        resetLobbyState()
    }

    override fun clearGameState() {
        resetGameState()
        mutableActiveGameId.value = null
        mutableWinnerId.value = null
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

        val enrichedBody = when {
            gameId != null && lobbyCode != null -> "{\"gameId\":$gameId,\"lobbyCode\":\"$lobbyCode\"}"
            gameId != null -> "{\"gameId\":$gameId}"
            lobbyCode != null -> "{\"lobbyCode\":\"$lobbyCode\"}"
            else -> "{}"
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

    override fun rollDice(diceCount: Int) {
        sendDiceRoll(WebSocketContract.rollDiceDestination, diceCount, "Roll dice")
    }

    // Radio Tower reroll (#326). Identical envelope to rollDice but routed to the
    // rerollDice destination; the server reuses RollDiceRequest and enforces the
    // Radio Tower / once-per-turn gate.
    override fun rerollDice(diceCount: Int) {
        sendDiceRoll(WebSocketContract.rerollDiceDestination, diceCount, "Reroll dice")
    }

    private fun sendDiceRoll(destination: String, diceCount: Int, actionName: String) {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "$actionName called but no active WebSocket connection")
            return
        }
        val gameId = mutableActiveGameId.value
        if (gameId == null) {
            Log.w(TAG, "$actionName called but no active game id")
            return
        }
        val payload = JSONObject()
            .put("gameId", gameId)
            .put("diceCount", diceCount)
        val body = JSONObject()
            .put("type", ROLL_DICE_TYPE)
            .put("gameId", gameId)
            .put("payload", payload)
            .toString()
        socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to destination,
                    "content-type" to "application/json"
                ),
                body = body
            ).serialize()
        )
        Log.d(TAG, "$actionName message sent (gameId=$gameId, diceCount=$diceCount)")
    }

    override fun advancePhase(gameId: Int) {
        sendGameIdAction(WebSocketContract.advancePhaseDestination, gameId, "advancePhase")
    }

    override fun resolveEffects(gameId: Int) {
        sendGameIdAction(WebSocketContract.resolveEffectsDestination, gameId, "resolveEffects")
    }

    override fun endTurn(gameId: Int) {
        sendGameIdAction(WebSocketContract.endTurnDestination, gameId, "endTurn")
    }

    override fun reportCheat(gameId: Int) {
        sendGameIdAction(WebSocketContract.reportCheatDestination, gameId, "reportCheat")
    }

    override fun accuse(gameId: Int, accusedPlayerId: Int) {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "accuse called but no active WebSocket connection")
            return
        }
        val body = JSONObject()
            .put("gameId", gameId)
            .put("accusedPlayerId", accusedPlayerId)
            .toString()
        socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.accuseDestination,
                    "content-type" to "application/json"
                ),
                body = body
            ).serialize()
        )
        Log.d(TAG, "accuse sent for game $gameId against player $accusedPlayerId")
    }

    override fun sendPurchase(
        gameId: Int,
        purchaseType: PurchaseType,
        cardType: String?,
        landmarkType: String?
    ) {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "sendPurchase called but no active WebSocket connection")
            return
        }
        // Body is intentionally not wrapped in WebSocketMessage; Spring maps it to PurchaseRequest.
        socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.purchaseDestination,
                    "content-type" to "application/json"
                ),
                body = purchaseBody(
                    gameId = gameId,
                    purchaseType = purchaseType,
                    cardType = cardType,
                    landmarkType = landmarkType
                )
            ).serialize()
        )
        Log.d(TAG, "Purchase message sent for game id: $gameId")
    }

    override fun sendChatMessage(gameId: Int, message: String) {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "sendChatMessage called but no active WebSocket connection")
            return
        }

        val body = JSONObject()
            .put("message", message)
            .put("gameId", gameId)
            .toString()

        val sent = socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.gameChatSendDestination,
                    "content-type" to "application/json"
                ),
                body = body
            ).serialize()
        )

        if (!sent) {
            Log.w(TAG, "sendChatMessage: failed to send frame")
        } else {
            Log.d(TAG, "Chat message sent (gameId=$gameId)")
        }
    }

    private fun sendGameIdAction(destination: String, gameId: Int, actionName: String) {
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "$actionName called but no active WebSocket connection")
            return
        }
        val body = JSONObject()
            .put("gameId", gameId)
            .toString()
        socket.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to destination,
                    "content-type" to "application/json"
                ),
                body = body
            ).serialize()
        )
        Log.d(TAG, "$actionName message sent for game id: $gameId")
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened: ${response.code} ${response.message}")
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
                        "heart-beat" to "$CLIENT_HEARTBEAT_INTERVAL_MS,$CLIENT_HEARTBEAT_INTERVAL_MS",
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
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val responseDetails = response?.let { "HTTP ${it.code} ${it.message}" } ?: "No HTTP response"
            Log.e(TAG, "WebSocket failure for $websocketUrl. $responseDetails. Reason: ${t.message}", t)
            clearSocket()
            mutableConnectionStatus.value = ConnectionStatus.ERROR
            resetGameState()
            scheduleReconnect()
        }
    }

    private fun handleFrame(frame: StompFrame) {
        when (frame.command) {
            "CONNECTED" -> {
                Log.d(TAG, "STOMP connected")
                cancelReconnect()

                // All subscriptions must be queued on the socket before we signal CONNECTED.
                // HomeViewModel waits for CONNECTED before sending lobby.join — if the lobby
                // queue subscription hasn't been sent yet, the server's LOBBY_ROSTER reply
                // arrives on a queue the client hasn't registered, and the message is lost.
                subscribeToPublicTopic()
                subscribeToErrorsQueue()
                subscribeToSyncQueue()
                val sessionId = frame.headers["session"]
                stompSessionId = sessionId
                subscribeToLobbyQueue(sessionId)
                mutableActiveGameId.value?.let(::subscribeToGameTopic)

                // Signal ready only after all SUBSCRIBE frames are in the socket queue
                mutableConnectionStatus.value = ConnectionStatus.CONNECTED
                startHeartbeat(frame.headers["heart-beat"])
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
                handleLobbyJoined(json)
                handleLobbyLeft(json)
                handleHostLeft(json)
                handleLobbyRoster(json)
                handleLobbyError(json)
                handleGameStarted(json)
                handleSync(json)
                handleAuthoritativeSnapshot(json)
                handleCoinDeltas(json)
                handleAccusationResult(json)
                handleAccusationError(json)
                handleChatMessage(json)
                parseGameAction(json).let { (phase, activePlayerId) ->
                    phase?.let { mutableGamePhase.value = it }
                    activePlayerId?.let { mutableActivePlayerId.value = it }
                }
                parsePurchaseSuccess(json)?.let { mutablePurchaseEvents.tryEmit(it) }
                parsePurchaseFailure(json)?.let { mutablePurchaseEvents.tryEmit(it) }
                parseDiceResult(json)?.let {
                    // Set the result first, then bump the tick so any collector
                    // that reacts to the tick already sees the new dice (#346).
                    mutableDiceResult.value = it
                    mutableDiceRollTick.value += 1
                }
                handleGameEnded(json)
            }
            "ERROR" -> {
                Log.e(TAG, "STOMP error frame received: ${frame.body}")
                if (isAuthRejection(frame.body)) {
                    // An auth rejection is terminal — do not auto-reconnect.
                    intentionalDisconnect = true
                    mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
                    resetGameState()
                    sessionStateHolder.signOut()
                    mutableAuthRejections.tryEmit(Unit)
                } else {
                    // Map the STOMP ERROR body through WsErrorParser at this single boundary.
                    val userMessage = WsErrorParser.parseStompErrorBody(frame.body).userMessage
                        .takeIf { it != ClientError.UNKNOWN_USER_MESSAGE } ?: "Purchase failed"
                    mutablePurchaseEvents.tryEmit(
                        PurchaseEvent.Failure(userMessage)
                    )
                }
            }
        }
    }

    /**
     * Handles lobby creation responses from the backend.
     */
    private fun handleLobbyCreated(json: JSONObject) {
        if (json.optString("type") != LOBBY_CREATED_TYPE) return
        val payload = json.optJSONObject("payload") ?: return
        val code = payload.optString("lobbyCode")
        val gameId = json.optIntOrNull("gameId") ?: payload.optIntOrNull("gameId")

        updateLobbyHostOwnership(resolveHostUserId(json, payload, payload.optJSONObject("game")))

        if (code.isNotBlank()) {
            Log.d(TAG, "Lobby created with code: $code")
            mutableLobbyCode.value = code
        }
        if (gameId != null) {
            mutableActiveGameId.value = gameId
            subscribeToGameTopic(gameId)
        }
        // Add host immediately so they appear in the list before LOBBY_JOINED arrives
        sessionStateHolder.session.value?.username?.let { username ->
            if (mutablePlayers.value.none { it.displayName == username }) {
                val hostId = (payload.optIntOrNull("playerId") ?: payload.optIntOrNull("id"))
                    ?.toString() ?: "host-$username"
                mutablePlayers.value += PlayerCoinState(
                    id = hostId,
                    displayName = username,
                    coins = payload.optInt("coins", 3)
                )
            }
        }
        // Host must join their own lobby to become a player in the roster
        if (pendingCreatedLobbyJoin && code.isNotBlank()) {
            pendingCreatedLobbyJoin = false
            sendJoinLobby(code)
        }
    }

    /**
     * Handles successful lobby join responses from the backend.
     */
    private fun handleLobbyJoined(json: JSONObject) {
        if (json.optString("type") != LOBBY_JOINED_TYPE) return

        val payload = json.optJSONObject("payload") ?: return
        val gameId = json.optIntOrNull("gameId") ?: payload.optIntOrNull("gameId")

        updateLobbyHostOwnership(resolveHostUserId(json, payload, payload.optJSONObject("game")))

        if (gameId != null) {
            Log.d(TAG, "Joined lobby with gameId: $gameId")
            mutableActiveGameId.value = gameId
            subscribeToGameTopic(gameId)
            // Re-register session with gameId so the server can identify the host when startGame is called
            if (mutableIsLobbyHost.value) {
                sendJoinMessage()
            }
        }

        // Add player to lobby list; username is now included in the server response
        val username = payload.optString("username").takeIf { it.isNotBlank() } ?: return
        // Try both "playerId" and "id" since the server may use either field name
        val playerId = (payload.optIntOrNull("playerId") ?: payload.optIntOrNull("id"))?.toString() ?: return
        val coins = payload.optInt("coins", 3)
        val isReady = payload.optBoolean("isReady", false)
        val newPlayer = PlayerCoinState(id = playerId, displayName = username, coins = coins, isReady = isReady)
        // Replace any existing entry with same id or name (e.g., temp host entry) then add
        mutablePlayers.value = mutablePlayers.value
            .filter { it.id != playerId && it.displayName != username } + newPlayer

        mutableLobbyEntered.tryEmit(Unit)
    }

    private fun handleLobbyLeft(json: JSONObject) {
        if (json.optString("type") != LOBBY_LEFT_TYPE) return
        val payload = json.optJSONObject("payload") ?: return
        val playerId = payload.optIntOrNull("playerId")?.toString() ?: return
        mutablePlayers.value = mutablePlayers.value.filter { it.id != playerId }
        Log.d(TAG, "Player $playerId left lobby")
    }

    /**
     * Handles LOBBY_ROSTER sent only to the joining player on their user queue
     * after a successful join. This is the joiner's *primary* signal that they
     * are in the lobby — the LOBBY_JOINED broadcast on `/topic/game/{gameId}`
     * is fired by the server before the joiner can subscribe, so they would
     * otherwise miss it (and every subsequent broadcast on that topic).
     *
     * Replaces the full player list so the joiner sees everyone already in the
     * lobby, captures the gameId, subscribes to the game topic so future
     * broadcasts (GAME_STARTED, GAME_ACTION, …) arrive, and emits the lobby-
     * entered signal so the UI can navigate.
     */
    private fun handleLobbyRoster(json: JSONObject) {
        if (json.optString("type") != LOBBY_ROSTER_TYPE) return
        // Server payload is `LobbyRosterDto(players = [...])` — a JSON object, not a bare array.
        val payload = json.optJSONObject("payload") ?: return
        val players = payload.optJSONArray("players") ?: return
        val gameId = json.optIntOrNull("gameId") ?: payload.optIntOrNull("gameId")
        updateLobbyHostOwnership(
            resolveHostUserId(json, payload, payload.optJSONObject("game"))
                ?: resolveHostUserIdFromRoster(players)
        )
        if (gameId != null) {
            mutableActiveGameId.value = gameId
            subscribeToGameTopic(gameId)
        }
        mutablePlayers.value = (0 until players.length()).mapNotNull { index ->
            val entry = players.optJSONObject(index) ?: return@mapNotNull null
            val username = entry.optString("username").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val playerId = entry.optIntOrNull("playerId")?.toString() ?: return@mapNotNull null
            PlayerCoinState(
                id = playerId,
                displayName = username,
                coins = entry.optInt("coins", 3),
                isReady = entry.optBoolean("isReady", false),
            )
        }
        Log.d(TAG, "Lobby roster received: ${mutablePlayers.value.size} players, gameId=$gameId")
        mutableLobbyEntered.tryEmit(Unit)
    }

    private fun handleLobbyError(json: JSONObject) {
        if (json.optString("type") != ERROR_TYPE) return

        val payload = json.optJSONObject("payload")
        val errorCode = payload?.optString("errorCode").orEmpty()

        if (
            errorCode == "INVALID_LOBBY_CODE" ||
            errorCode == "GAME_NOT_FOUND" ||
            errorCode == "GAME_STARTED" ||
            errorCode == "GAME_FINISHED" ||
            errorCode == "LOBBY_FULL"
        ) {
            val error = WsErrorParser.parse(json).let { parsed ->
                if (parsed.userMessage == ClientError.UNKNOWN_USER_MESSAGE) {
                    parsed.copy(userMessage = "Failed to join lobby")
                } else {
                    parsed
                }
            }
            Log.w(TAG, "Lobby join error received [${error.serverCode}]: ${error.userMessage}")
            mutableLobbyJoinErrors.tryEmit(error)
        }
    }

    private fun handleGameStarted(json: JSONObject) {
        if (json.optString("type") != GAME_STARTED_TYPE) return

        val payload = json.optJSONObject("payload") ?: return
        if (payload.optJSONObject("game") != null) {
            applyGameStateSnapshot(payload, json.optIntOrNull("gameId"))
            return
        }

        val game = payload.optJSONObject("game") ?: return
        val gameId = json.optIntOrNull("gameId") ?: game.optIntOrNull("id") ?: return

        mutableActiveGameId.value = gameId
        subscribeToGameTopic(gameId)
        payload.optIntOrNull("activePlayerId")?.let { mutableActivePlayerId.value = it }

        game.optString("lobbyCode")
            .takeIf { it.isNotBlank() }
            ?.let { mutableLobbyCode.value = it }

        parseTurnPhase(game.optString("turnPhase"))?.let { mutableGamePhase.value = it }
        val playerUsernames = parsePlayerUsernames(payload.optJSONObject("playerUsernames"))
        mutablePlayers.value = payload.optJSONArray("players").toPlayerCoinStates(payload, game, playerUsernames)
        updateShopItemsFromState(payload)
    }
    private fun handleGameEnded(json: JSONObject) {
        if (json.optString("type") != GAME_ENDED_TYPE) return

        val payload = json.optJSONObject("payload") ?: return
        val winnerId = payload.optIntOrNull("winnerId") ?: return

        mutableWinnerId.value = winnerId
        payload.optIntOrNull("roundsPlayed")?.let { mutableRoundNumber.value = it }
        mutableGameStatus.value = GameStatus.FINISHED
        mutableGamePhase.value = GamePhase.NONE
        unsubscribeFromGameTopic(mutableActiveGameId.value)
    }

    private fun handleHostLeft(json: JSONObject) {
        if (json.optString("type") != HOST_LEFT_TYPE) return

        // Ignore if I am the host leaving intentionally
        if (mutableIsLobbyHost.value) return

        Log.d(TAG, "Host left lobby, lobby closed")

        resetLobbyState()

        mutableHostLeftLobby.tryEmit(Unit)
    }

    /**
     * Handles the reconnect snapshot pushed to `/user/queue/game-sync`.
     *
     * The SYNC message wraps a full `GameStateDto` under `payload.state`; this
     * restores every flow the game screen renders so a reconnecting client
     * reconstructs the board in a single round-trip — no follow-up queries.
     */
    private fun handleSync(json: JSONObject) {
        if (json.optString("type") != SYNC_TYPE) return
        val payload = json.optJSONObject("payload") ?: return
        val state = payload.optJSONObject("state") ?: return
        applyGameStateSnapshot(state, json.optIntOrNull("gameId"))
    }

    /**
     * Cheating-accusation result (#280). The embedded state snapshot is applied by
     * [handleAuthoritativeSnapshot] (so coin penalties show up); here we just emit a
     * one-shot result for the toast, resolving names from the freshly-applied roster.
     */
    /**
     * Parses the ACCUSATION_RESULT payload
     * `{accuserPlayerId, accusedPlayerId, caught, penalizedPlayerId,
     * penaltyCoins}` — the wire contract agreed in #280 / Server#361. Keep the
     * keys in lockstep with the server broadcast (see the #353 post-mortem); a
     * frame without `caught` is dropped loudly instead of being misread as a
     * wrong accusation.
     */
    private fun handleAccusationResult(json: JSONObject) {
        if (json.optString("type") != ACCUSATION_RESULT_TYPE) return
        val payload = json.optJSONObject("payload") ?: return
        if (!payload.has("caught")) {
            Log.w(TAG, "ACCUSATION_RESULT without 'caught' — server/client contract mismatch")
            return
        }
        val caught = payload.optBoolean("caught")
        val accuserId = payload.optIntOrNull("accuserPlayerId")
        val accusedId = payload.optIntOrNull("accusedPlayerId")
        val penalizedId = payload.optIntOrNull("penalizedPlayerId")
        val penaltyCoins = payload.optIntOrNull("penaltyCoins") ?: 0
        val roster = mutablePlayers.value
        fun nameOf(id: Int?): String =
            roster.firstOrNull { it.id == id?.toString() }?.displayName ?: "A player"
        mutableAccusationResults.tryEmit(
            AccusationResult(
                caught = caught,
                accuserId = accuserId,
                accuserName = nameOf(accuserId),
                accusedName = nameOf(accusedId),
                penalizedName = nameOf(penalizedId),
                penaltyCoins = penaltyCoins,
            )
        )
    }

    /**
     * Surfaces rejected accusations (#280). The server delivers them on the
     * private errors queue as a `WebSocketErrorDto {code, message, ...}` — a
     * payload with no `type` field, so no other handler matches it. Without
     * this, a rejection (e.g. "once per turn" after the client gate diverged
     * across a reconnect) would be silently dropped.
     */
    private fun handleAccusationError(json: JSONObject) {
        if (json.optString("code") != INVALID_ACCUSATION_CODE) return
        val message = json.optString("message").takeIf { it.isNotBlank() } ?: "Invalid accusation"
        mutableAccusationErrors.tryEmit(message)
    }

    private fun handleChatMessage(json: JSONObject) {
        if (json.optString("type") != "CHAT") return //ignore messages of other types
        // Flexible detection: the server might put message/sender in the top-level or under payload.
        val payload = json.optJSONObject("payload")
        val msg = payload?.optString("message") ?: json.optString("content")
        if (msg.isBlank()) return
        val sender = payload?.optString("sender") ?: json.optString("sender") ?: json.optString("username")
        if (sender.isBlank()) return

        Log.d(TAG, "Received chat message from $sender: $msg")

        mutableChatMessages.tryEmit(
            ChatMessageState(
                sender = sender,
                message = msg,
            )
        )
    }

    private fun handleAuthoritativeSnapshot(json: JSONObject) {
        val type = json.optString("type")
        if (type != GAME_ACTION_TYPE && type != ROLL_DICE_TYPE && type != GAME_END_TYPE && type != ACCUSATION_RESULT_TYPE) return
        val payload = json.optJSONObject("payload") ?: return
        val state = payload.optJSONObject("state") ?: return
        applyGameStateSnapshot(state, json.optIntOrNull("gameId") ?: payload.optIntOrNull("gameId"))
    }

    /**
     * Emits the local player's signed coin delta from an EFFECTS_RESOLVED
     * broadcast (#389) so the UI can play the coin / coin-drawer sound. The
     * server keys `coinDeltas` by player ID; we resolve our own player ID from
     * the freshly applied roster (handleAuthoritativeSnapshot runs first), not
     * from the user ID — the two ID spaces are independent.
     */
    private fun handleCoinDeltas(json: JSONObject) {
        if (json.optString("type") != GAME_ACTION_TYPE) return
        val payload = json.optJSONObject("payload") ?: return
        if (payload.optString("event") != EFFECTS_RESOLVED_EVENT) return
        val deltas = payload.optJSONObject("coinDeltas") ?: return
        val myPlayerId = mutablePlayers.value.firstOrNull { it.isCurrentPlayer }?.id ?: return
        if (!deltas.has(myPlayerId)) return
        val delta = deltas.optInt(myPlayerId)
        if (delta != 0) mutableCoinDeltas.tryEmit(delta)
    }

    private fun applyGameStateSnapshot(state: JSONObject, envelopeGameId: Int? = null) {
        val game = state.optJSONObject("game") ?: return

        val gameId = envelopeGameId ?: game.optIntOrNull("id")
        if (gameId != null) {
            mutableActiveGameId.value = gameId
            subscribeToGameTopic(gameId)
        }

        game.optString("lobbyCode")
            .takeIf { it.isNotBlank() }
            ?.let { mutableLobbyCode.value = it }

        updateLobbyHostOwnership(resolveHostUserId(state, game))

        parseGameStatus(game.optString("status"))?.let { mutableGameStatus.value = it }
        parseTurnPhase(game.optString("turnPhase"))?.let { mutableGamePhase.value = it }
        game.optIntOrNull("roundNumber")?.let { mutableRoundNumber.value = it }
        // The snapshot persists only the dice total (lastDiceRoll), not the
        // individual dice. Surface it as a single-element list so the last roll
        // still shows on reconnect — but do NOT clobber a richer per-die result we
        // already hold for the same roll (it sums to this total). Otherwise a
        // routine snapshot collapses [x, y] into a single generic die mid-turn,
        // which is the inconsistent "one vs two dice" display and also hides doubles.
        game.optIntOrNull("lastDiceRoll")?.let { total ->
            val current = mutableDiceResult.value
            if (current == null || current.sum() != total) {
                mutableDiceResult.value = listOf(total)
            }
        }

        val playerUsernames = parsePlayerUsernames(state.optJSONObject("playerUsernames"))
        mutablePlayers.value = state.optJSONArray("players").toPlayerCoinStates(state, game, playerUsernames)
        resolveActiveUserId(state, game)?.let { mutableActivePlayerId.value = it }
        mutablePlayerCards.value = parsePlayerCards(state.optJSONObject("playerCards"))
        mutablePlayerLandmarks.value = parsePlayerLandmarks(state.optJSONObject("playerLandmarks"))
        val marketplace = parseMarketplace(state.optJSONObject("marketplace"))
        mutableMarketplace.value = marketplace
        updateShopItemsFromState(state, marketplace)
    }

    private fun updateShopItemsFromState(
        state: JSONObject,
        marketplace: Map<CardType, Int> = parseMarketplace(state.optJSONObject("marketplace"))
    ) {
        val cardItems = parseCardDefinitions(state.optJSONArray("cardDefinitions"), marketplace)
        val landmarkItems = parseLandmarkDefinitions(state.optJSONArray("landmarkDefinitions"))
        if (cardItems.isNotEmpty() || landmarkItems.isNotEmpty()) {
            mutableShopItems.value = cardItems + landmarkItems
        }
    }

    private fun parseCardDefinitions(
        array: JSONArray?,
        marketplace: Map<CardType, Int>
    ): List<ShopItem> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val definition = array.optJSONObject(index) ?: return@mapNotNull null
            val cardType = runCatching { CardType.valueOf(definition.optString("cardType")) }
                .getOrNull() ?: return@mapNotNull null
            definition.toCardDefinition(cardType)
                .toShopItem(isAvailable = (marketplace[cardType] ?: 0) > 0)
        }
    }

    private fun parseLandmarkDefinitions(array: JSONArray?): List<ShopItem> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val definition = array.optJSONObject(index) ?: return@mapNotNull null
            val landmarkType = runCatching {
                LandmarkType.valueOf(definition.optString("landmarkType"))
            }.getOrNull() ?: return@mapNotNull null
            ShopItem(
                purchaseType = PurchaseType.LANDMARK,
                type = landmarkType.name,
                displayName = landmarkType.displayName(),
                cost = definition.optInt("cost"),
                color = ShopItemColor.LANDMARK,
                establishmentType = "LANDMARK",
                activationNumbers = emptyList(),
                effectText = definition.effectText(landmarkType),
                imageKey = "landmark_${landmarkType.name.lowercase()}",
                isAvailable = true
            )
        }
    }

    private fun JSONObject.toCardDefinition(cardType: CardType): CardDefinition {
        // All CardType values must have an entry — crash early if a new value is added without a definition.
        val fallback = checkNotNull(CardDefinitions.forType(cardType)) {
            "CardDefinitions missing entry for $cardType"
        }
        return CardDefinition(
            cardType = cardType,
            displayName = optString("displayName").ifBlank { fallback.displayName },
            cost = optIntOrNull("cost") ?: fallback.cost,
            color = optString("color").toShopItemColor(fallback.color),
            establishmentType = optString("establishmentType").ifBlank { fallback.establishmentType },
            activationNumbers = activationNumbers().ifEmpty { fallback.activationNumbers },
            effectText = effectText(cardType),
            imageKey = optString("imageKey").ifBlank { fallback.imageKey },
        )
    }

    private fun JSONObject.activationNumbers(): List<Int> =
        optJSONArray("activationNumbers")?.toActivationNumbers().orEmpty()

    private fun JSONObject.effectText(cardType: CardType): String =
        optString("effectText")
            .ifBlank { optString("effectDescription") }
            .ifBlank { optString("description") }
            .ifBlank { defaultShopItem(cardType.name)?.effectText.orEmpty() }

    private fun JSONObject.effectText(landmarkType: LandmarkType): String =
        optString("effectText")
            .ifBlank { optString("effectDescription") }
            .ifBlank { optString("description") }
            .ifBlank { defaultShopItem(landmarkType.name)?.effectText.orEmpty() }

    private fun JSONArray.toActivationNumbers(): List<Int> =
        (0 until length()).mapNotNull { index ->
            runCatching { getInt(index) }.getOrNull()
        }.distinct().sorted()

    private fun String.toShopItemColor(fallback: ShopItemColor? = null): ShopItemColor =
        runCatching { ShopItemColor.valueOf(this) }.getOrDefault(fallback ?: ShopItemColor.BLUE)

    private fun CardType.displayName(): String = name.toDisplayName()

    private fun LandmarkType.displayName(): String = name.toDisplayName()

    private fun defaultShopItem(type: String): ShopItem? =
        ShopCatalog.defaultItems.firstOrNull { it.type == type }

    private fun String.toDisplayName(): String =
        lowercase()
            .split("_")
            .joinToString(" ") { part -> part.replaceFirstChar { it.titlecase() } }

    private fun parseGameStatus(name: String): GameStatus? =
        name.takeIf { it.isNotEmpty() }
            ?.let { runCatching { GameStatus.valueOf(it) }.getOrNull() }

    /**
     * Resolves the active player's **userId** from the snapshot because the UI
     * enables turn actions by comparing `GameScreenState.myUserId` with
     * `activePlayerId`. Current server snapshots already send `activePlayerId`
     * and `turnOrder` in user-id space, so that value is authoritative.
     *
     * When `activePlayerId` is absent, `currentTurnIndex` selects the active
     * user ID directly from `turnOrder`, as defined by the server contract.
     * It must not be reinterpreted as a database player ID: those independent
     * ID spaces can contain the same numeric value.
     */
    private fun resolveActiveUserId(state: JSONObject, game: JSONObject): Int? {
        state.optIntOrNull("activePlayerId")?.let { return it }

        val currentTurnIndex = game.optIntOrNull("currentTurnIndex") ?: return null
        val turnOrder = state.optJSONArray("turnOrder") ?: return null
        if (currentTurnIndex !in 0 until turnOrder.length()) return null
        return turnOrder.optInt(currentTurnIndex)
    }

    private fun parsePlayerCards(obj: JSONObject?): Map<Int, List<PlayerCardState>> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<Int, List<PlayerCardState>>()
        for (key in obj.keys()) {
            val playerId = key.toIntOrNull() ?: continue
            val array = obj.optJSONArray(key) ?: continue
            result[playerId] = (0 until array.length()).mapNotNull { index ->
                val entry = array.optJSONObject(index) ?: return@mapNotNull null
                val type = runCatching { CardType.valueOf(entry.optString("cardType")) }
                    .getOrNull() ?: return@mapNotNull null
                PlayerCardState(cardType = type, quantity = entry.optInt("quantity"))
            }
        }
        return result
    }

    private fun parsePlayerLandmarks(obj: JSONObject?): Map<Int, List<PlayerLandmarkState>> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<Int, List<PlayerLandmarkState>>()
        for (key in obj.keys()) {
            val playerId = key.toIntOrNull() ?: continue
            val array = obj.optJSONArray(key) ?: continue
            result[playerId] = (0 until array.length()).mapNotNull { index ->
                val entry = array.optJSONObject(index) ?: return@mapNotNull null
                val type = runCatching { LandmarkType.valueOf(entry.optString("landmarkType")) }
                    .getOrNull() ?: return@mapNotNull null
                PlayerLandmarkState(landmarkType = type, isBuilt = entry.optBoolean("isBuilt"))
            }
        }
        return result
    }

    private fun parsePlayerUsernames(obj: JSONObject?): Map<Int, String> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<Int, String>()
        for (key in obj.keys()) {
            val playerId = key.toIntOrNull() ?: continue
            result[playerId] = obj.optString(key)
        }
        return result
    }

    private fun parseMarketplace(obj: JSONObject?): Map<CardType, Int> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<CardType, Int>()
        for (key in obj.keys()) {
            val cardType = runCatching { CardType.valueOf(key) }.getOrNull() ?: continue
            result[cardType] = obj.optInt(key)
        }
        return result
    }

    private fun parseGameAction(json: JSONObject): Pair<GamePhase?, Int?> {
        if (json.optString("type") != GAME_ACTION_TYPE) return Pair(null, null)
        val payload = json.optJSONObject("payload") ?: return Pair(null, null)
        val phaseName = payload.optString("turnPhase").takeIf { it.isNotEmpty() }
        val phase = phaseName?.let { parseTurnPhase(it) }
        val activePlayerId = if (payload.has("activePlayerId") && !payload.isNull("activePlayerId"))
            payload.optInt("activePlayerId") else null
        return Pair(phase, activePlayerId)
    }

    private fun parsePurchaseSuccess(json: JSONObject): PurchaseEvent.Success? {
        if (json.optString("type") != GAME_ACTION_TYPE) return null
        val payload = json.optJSONObject("payload") ?: return null
        // Server broadcasts the bought target in GAME_ACTION after PurchaseService accepts it.
        val purchaseType = runCatching {
            PurchaseType.valueOf(payload.optString("purchaseType"))
        }.getOrNull() ?: return null
        val itemType = when (purchaseType) {
            PurchaseType.ESTABLISHMENT -> payload.optString("cardType")
            PurchaseType.LANDMARK -> payload.optString("landmarkType")
        }.takeIf { it.isNotBlank() } ?: return null
        return PurchaseEvent.Success(purchaseType = purchaseType, itemType = itemType)
    }

    private fun parsePurchaseFailure(json: JSONObject): PurchaseEvent.Failure? {
        if (json.optString("type") != ERROR_TYPE) return null
        val payload = json.optJSONObject("payload") ?: return null
        if (payload.optString("event") != PURCHASE_FAILED_EVENT) return null
        val userMessage = WsErrorParser.parse(json).userMessage
            .takeIf { it != ClientError.UNKNOWN_USER_MESSAGE } ?: "Purchase failed"
        return PurchaseEvent.Failure(userMessage)
    }

    /**
     * Parses incoming ROLL_DICE results from the server. The server reuses the
     * ROLL_DICE type for both the initial roll (`payload.event == DICE_ROLLED`)
     * and the Radio Tower reroll (`payload.event == DICE_REROLLED`, #326), and
     * both carry the dice in `payload.result`, so this single parser handles both.
     */
    private fun parseDiceResult(json: JSONObject): List<Int>? {
        if (json.optString("type") != ROLL_DICE_TYPE) return null
        val payload = json.optJSONObject("payload") ?: return null
        val resultArray = payload.optJSONArray("result") ?: return null
        return (0 until resultArray.length()).mapNotNull { index ->
            runCatching { resultArray.getInt(index) }.getOrNull()
        }
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

    private fun subscribeToSyncQueue() {
        webSocket?.send(
            StompFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "user-game-sync",
                    "destination" to WebSocketContract.gameSyncQueue
                )
            ).serialize()
        )
    }

    private fun subscribeToErrorsQueue() {
        webSocket?.send(
            StompFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "user-errors",
                    "destination" to WebSocketContract.errorsQueue
                )
            ).serialize()
        )
    }

    private fun subscribeToLobbyQueue(sessionId: String?) {
        // Session-scoped destination when server provides session ID; user-scoped otherwise
        val destination = if (sessionId != null) {
            "${WebSocketContract.lobbyQueuePrefix}$sessionId"
        } else {
            WebSocketContract.lobbyQueue
        }
        webSocket?.send(
            StompFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "lobby-queue",
                    "destination" to destination
                )
            ).serialize()
        )
    }

    private fun subscribeToGameTopic(gameId: Int) {
        if (subscribedGameId == gameId) return

        val socket = webSocket ?: return

        subscribedGameId?.let { oldId ->
          unsubscribeFromGameTopic(oldId)
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
    private fun unsubscribeFromGameTopic(gameId: Int?) {
        if (subscribedGameId != gameId) return

        val socket = webSocket ?: return

        val unsubscribeFrame = StompFrame(
            command = "UNSUBSCRIBE",
            headers = mapOf(
                "id" to "game-topic-$gameId"
            )
        ).serialize()

        if (socket.send(unsubscribeFrame)) {
            subscribedGameId = null
        }
    }

    private fun sendJoinMessage() {
        val gameId = mutableActiveGameId.value
        val body = if (gameId != null) {
            """{"type":"JOIN","sender":"${WebSocketContract.defaultSender}","gameId":$gameId}"""
        } else {
            """{"type":"JOIN","sender":"${WebSocketContract.defaultSender}"}"""
        }
        webSocket?.send(
            StompFrame(
                command = "SEND",
                headers = mapOf(
                    "destination" to WebSocketContract.addUserDestination,
                    "content-type" to "application/json"
                ),
                body = body
            ).serialize()
        )
    }

    private fun clearSocket() {
        synchronized(this) {
            heartbeatJob?.cancel()
            heartbeatJob = null
            webSocket = null
            subscribedGameId = null
            stompSessionId = null
        }
    }

    /**
     * Schedules an automatic reconnect after an unexpected drop (network blip
     * or backend container restart). No-op when the client closed the
     * connection itself (disconnect() / auth rejection) or when there is no
     * session to reconnect with. Uses exponential backoff so a backend restart
     * is ridden out without hammering the server; a successful STOMP CONNECT
     * resets the backoff. The CONNECTED handler re-subscribes to the game-sync
     * queue and re-sends the JOIN, so reconnect-snapshot recovery is triggered
     * automatically once the connection is back.
     */
    private fun scheduleReconnect() {
        synchronized(this) {
            if (intentionalDisconnect) return
            if (sessionStateHolder.session.value == null) return
            val attempt = reconnectAttempt
            reconnectAttempt = attempt + 1
            val delayMs = reconnectDelaysMs.getOrElse(attempt) { reconnectDelaysMs.last() }
            reconnectJob?.cancel()
            reconnectJob = reconnectScope.launch {
                delay(delayMs)
                if (!intentionalDisconnect && sessionStateHolder.session.value != null) {
                    Log.d(TAG, "Auto-reconnect attempt ${attempt + 1}")
                    connect()
                }
            }
        }
    }

    /** Cancels any pending reconnect and resets the backoff counter. */
    private fun cancelReconnect() {
        synchronized(this) {
            reconnectJob?.cancel()
            reconnectJob = null
            reconnectAttempt = 0
        }
    }

    /**
     * Starts the periodic heartbeat emitter using the interval negotiated with
     * the server (see [negotiatedSendIntervalMs]). Cancels any previous emitter
     * first, so reconnects don't stack jobs. No-op when negotiation opts out.
     */
    private fun startHeartbeat(serverHeartBeatHeader: String?) {
        val sendIntervalMs = negotiatedSendIntervalMs(serverHeartBeatHeader)
        if (sendIntervalMs <= 0L) {
            Log.d(TAG, "STOMP heartbeats disabled by negotiation (server='$serverHeartBeatHeader')")
            stopHeartbeat()
            return
        }
        Log.d(TAG, "Starting STOMP heartbeats every ${sendIntervalMs}ms")
        synchronized(this) {
            heartbeatJob?.cancel()
            heartbeatJob = reconnectScope.launch {
                while (isActive) {
                    delay(sendIntervalMs)
                    val socket = synchronized(this@OkHttpWebSocketClient) { webSocket } ?: break
                    // A lone LF is a STOMP heartbeat. Emitting it on an otherwise idle
                    // socket keeps platform proxies (e.g. Railway's edge) from reaping
                    // the connection during a player's think-time — and satisfies the
                    // server, which now expects client heartbeats (server #426).
                    try {
                        if (!socket.send(HEARTBEAT_FRAME)) {
                            Log.w(TAG, "Heartbeat send failed; stopping heartbeats")
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Heartbeat send threw; stopping heartbeats", e)
                        break
                    }
                }
            }
        }
    }

    private fun stopHeartbeat() {
        synchronized(this) {
            heartbeatJob?.cancel()
            heartbeatJob = null
        }
    }

    /**
     * Resolves the client→server heartbeat interval from the negotiated values
     * per STOMP 1.2. We advertise [CLIENT_HEARTBEAT_INTERVAL_MS] in both
     * directions, so the effective send interval is the larger of our value and
     * the server's requested receive interval (the second field of its
     * `heart-beat` header). Returns 0 when either side opts out.
     */
    private fun negotiatedSendIntervalMs(serverHeartBeatHeader: String?): Long {
        val serverWantsToReceiveMs = serverHeartBeatHeader
            ?.split(",")
            ?.takeIf { it.size == 2 }
            ?.get(1)
            ?.trim()
            ?.toLongOrNull()
            ?: 0L
        if (CLIENT_HEARTBEAT_INTERVAL_MS == 0L || serverWantsToReceiveMs == 0L) return 0L
        return maxOf(CLIENT_HEARTBEAT_INTERVAL_MS, serverWantsToReceiveMs)
    }

    private fun isAuthRejection(body: String): Boolean =
        body.trim().contains(AUTH_REJECTION_BODY)

    private fun resetGameState() {
        mutableGamePhase.value = GamePhase.NONE
        mutablePlayers.value = emptyList()
        mutableDiceResult.value = null
        mutableDiceRollTick.value = 0L
        mutableActivePlayerId.value = null
        mutableLobbyCode.value = null
        mutableGameStatus.value = null
        mutableRoundNumber.value = null
        mutablePlayerCards.value = emptyMap()
        mutablePlayerLandmarks.value = emptyMap()
        mutableMarketplace.value = emptyMap()
        mutableShopItems.value = emptyList()
    }

    private fun resetLobbyState() {
        mutableLobbyCode.value = null
        mutableActiveGameId.value = null
        mutableIsLobbyHost.value = false
        mutablePlayers.value = emptyList()
        pendingCreatedLobbyJoin = false
    }

    private fun parseTurnPhase(phaseName: String): GamePhase? =
        phaseName.takeIf { it.isNotEmpty() }?.let { runCatching { GamePhase.valueOf(it) }.getOrNull() }

    private fun JSONArray?.toPlayerCoinStates(payload: JSONObject, game: JSONObject, playerUsernames: Map<Int, String> = emptyMap()): List<PlayerCoinState> {
        if (this == null) return emptyList()

        val localUserId = sessionStateHolder.session.value?.userId
        val activeUserId = resolveActiveUserId(payload, game)

        return List(length()) { index ->
            getJSONObject(index).toPlayerCoinState(localUserId, activeUserId, playerUsernames)
        }
    }

    private fun JSONObject.toPlayerCoinState(
        localUserId: Int?,
        activeUserId: Int?,
        playerUsernames: Map<Int, String> = emptyMap()
    ): PlayerCoinState {
        val playerId = optInt("id")
        val userId = optIntOrNull("userId")
        val resolvedDisplayName =
            optString("username").takeIf { it.isNotBlank() }
                ?: playerUsernames[playerId]
                ?: optString("name").takeIf { it.isNotBlank() }
                ?: optString("displayName").takeIf { it.isNotBlank() }
                ?: "Player $playerId"
        return PlayerCoinState(
            id = playerId.toString(),
            displayName = resolvedDisplayName,
            coins = optInt("coins"),
            isCurrentPlayer = userId != null && userId == localUserId,
            isActivePlayer = userId != null && userId == activeUserId,
        )
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private fun updateLobbyHostOwnership(hostUserId: Int?) {
        if (hostUserId == null) return
        mutableIsLobbyHost.value = sessionStateHolder.session.value?.userId == hostUserId
    }

    private fun resolveHostUserId(vararg sources: JSONObject?): Int? =
        sources.firstNotNullOfOrNull { source ->
            source?.hostUserId()
        }

    private fun JSONObject.hostUserId(): Int? =
        HOST_USER_ID_KEYS.firstNotNullOfOrNull { key -> optIntOrNull(key) }

    private fun resolveHostUserIdFromRoster(players: JSONArray): Int? =
        (0 until players.length()).firstNotNullOfOrNull { index ->
            val entry = players.optJSONObject(index) ?: return@firstNotNullOfOrNull null
            if (!entry.optBoolean("isHost", false) && !entry.optBoolean("host", false)) {
                return@firstNotNullOfOrNull null
            }
            entry.optIntOrNull("userId")
                ?: entry.optIntOrNull("user_id")
                ?: entry.optIntOrNull("id")
        }

    private fun websocketHostHeader(): String {
        val uri = URI(websocketUrl)
        val port = uri.port
        return if (port == -1 || port == defaultPort(uri.scheme.orEmpty())) uri.host
        else "${uri.host}:$port"
    }

    private fun defaultPort(scheme: String): Int = when (scheme) {
        "wss", "https" -> 443
        else -> 80
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
        private const val TAG = "OkHttpWebSocketClient"
        // Heartbeat interval (ms) advertised in the STOMP CONNECT frame, in both
        // directions. Kept in sync with the server's WebSocketConfig
        // (HEARTBEAT_INTERVAL_MS, server #426) and well under the ~60s idle window
        // after which platform proxies reap the connection.
        private const val CLIENT_HEARTBEAT_INTERVAL_MS = 10_000L
        // A bare line-feed is the STOMP heartbeat frame (no NUL terminator).
        private const val HEARTBEAT_FRAME = "\n"
        // Exponential backoff for auto-reconnect; the final value repeats once
        // the list is exhausted. Long enough to ride out a backend container
        // restart without hammering the server.
        private val RECONNECT_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L)
        private const val GAME_ACTION_TYPE = "GAME_ACTION"
        private const val ACCUSATION_RESULT_TYPE = "ACCUSATION_RESULT"
        private const val INVALID_ACCUSATION_CODE = "INVALID_ACCUSATION"
        private const val GAME_END_TYPE = "GAME_END"
        private const val GAME_STARTED_TYPE = "GAME_STARTED"
        private const val GAME_ENDED_TYPE = "GAME_END"
        private const val AUTH_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val LOBBY_CREATED_TYPE = "LOBBY_CREATED"
        private const val LOBBY_JOINED_TYPE = "LOBBY_JOINED"
        private const val LOBBY_LEFT_TYPE = "LOBBY_LEFT"
        private const val HOST_LEFT_TYPE = "HOST_LEFT"

        private const val LOBBY_ROSTER_TYPE = "LOBBY_ROSTER"
        private const val ERROR_TYPE = "ERROR"
        private const val PURCHASE_FAILED_EVENT = "PURCHASE_FAILED"
        private const val EFFECTS_RESOLVED_EVENT = "EFFECTS_RESOLVED"
        private const val ROLL_DICE_TYPE = "ROLL_DICE"
        private const val SYNC_TYPE = "SYNC"
        private val HOST_USER_ID_KEYS = listOf(
            "hostId",
            "host_id",
            "hostUserId",
            "host_user_id",
            "hostUserID",
        )
        // Frozen contract: matches GENERIC_AUTH_FAILURE on the server's
        // StompAuthChannelInterceptor. If the server message changes, this
        // client will silently fall through to the generic ERROR handling.
        private const val AUTH_REJECTION_BODY = "Authentication failed"

        private fun purchaseBody(
            gameId: Int,
            purchaseType: PurchaseType,
            cardType: String?,
            landmarkType: String?
        ): String {
            val targetField = when (purchaseType) {
                PurchaseType.ESTABLISHMENT -> ",\"cardType\":\"$cardType\""
                PurchaseType.LANDMARK -> ",\"landmarkType\":\"$landmarkType\""
            }
            // Keep field names aligned with Server PurchaseRequest.kt.
            return """{"gameId":$gameId,"purchaseType":"${purchaseType.name}"$targetField}"""
        }
    }
}
