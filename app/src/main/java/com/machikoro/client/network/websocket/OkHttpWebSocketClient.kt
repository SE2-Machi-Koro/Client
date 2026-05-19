package com.machikoro.client.network.websocket

import android.util.Log
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.ShopItemColor
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.ConnectionStatus
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

    override val lobbyJoinErrors: SharedFlow<String>
        get() = mutableLobbyJoinErrors.asSharedFlow()

    override val diceResult: StateFlow<List<Int>?>
        get() = mutableDiceResult.asStateFlow()

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

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())
    private val mutableLobbyCode = MutableStateFlow<String?>(null)
    private val mutableLobbyEntered = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableLobbyJoinErrors = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableDiceResult = MutableStateFlow<List<Int>?>(null)
    private val mutableActivePlayerId = MutableStateFlow<Int?>(null)
    private val mutableActiveGameId = MutableStateFlow<Int?>(null)
    private val mutableIsLobbyHost = MutableStateFlow(false)
    private val mutableGameStatus = MutableStateFlow<GameStatus?>(null)
    private val mutableRoundNumber = MutableStateFlow<Int?>(null)
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
    private val frameBuffer = StringBuilder()

    @Volatile
    private var webSocket: WebSocket? = null
    private var subscribedGameId: Int? = null

    // Auto-reconnect state. `intentionalDisconnect` is set whenever the client
    // tears the connection down itself (disconnect() or an auth rejection) so
    // the listener can tell a deliberate close apart from an unexpected drop.
    @Volatile
    private var intentionalDisconnect = false
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

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
            mutableIsLobbyHost.value = true
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
        val socket = synchronized(this) { webSocket }
        if (socket == null) {
            Log.w(TAG, "rollDice called but no active WebSocket connection")
            return
        }
        val gameId = mutableActiveGameId.value
        if (gameId == null) {
            Log.w(TAG, "rollDice called but no active game id")
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
                    "destination" to WebSocketContract.rollDiceDestination,
                    "content-type" to "application/json"
                ),
                body = body
            ).serialize()
        )
        Log.d(TAG, "Roll dice message sent (gameId=$gameId, diceCount=$diceCount)")
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
                // A live STOMP session: drop any pending retry and reset backoff.
                cancelReconnect()
                mutableConnectionStatus.value = ConnectionStatus.CONNECTED
                subscribeToPublicTopic()
                subscribeToErrorsQueue()

                // Subscribe before the JOIN send below: chat.addUser triggers the
                // server-side reconnect snapshot, and the SUBSCRIBE must be
                // registered first or the SYNC frame is delivered to nobody.
                subscribeToSyncQueue()

                // User-scoped lobby queue — Spring resolves /user/queue/lobby to the session-scoped
                // destination automatically, same as game-sync. No raw session ID needed.
                subscribeToLobbyQueue()

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
                handleLobbyJoined(json)
                handleLobbyLeft(json)
                handleLobbyRoster(json)
                handleLobbyError(json)
                handleGameStarted(json)
                handleSync(json)
                parseGameAction(json).let { (phase, activePlayerId) ->
                    phase?.let { mutableGamePhase.value = it }
                    activePlayerId?.let { mutableActivePlayerId.value = it }
                }
                parsePurchaseSuccess(json)?.let { mutablePurchaseEvents.tryEmit(it) }
                parseDiceResult(json)?.let { mutableDiceResult.value = it }
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
                    // Purchase validation failures arrive as regular STOMP ERROR frames.
                    // Surface them to the shop without marking the transport itself as failed.
                    mutablePurchaseEvents.tryEmit(
                        PurchaseEvent.Failure(frame.body.ifBlank { "Purchase failed" })
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
        if (mutableIsLobbyHost.value && code.isNotBlank()) {
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
        val newPlayer = PlayerCoinState(id = playerId, displayName = username, coins = coins)
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
     * Handles LOBBY_ROSTER sent only to the joining player after a successful join.
     * Replaces the full player list so the joiner sees everyone already in the lobby.
     */
    private fun handleLobbyRoster(json: JSONObject) {
        if (json.optString("type") != LOBBY_ROSTER_TYPE) return
        val players = json.optJSONArray("payload") ?: return
        mutablePlayers.value = (0 until players.length()).mapNotNull { index ->
            val entry = players.optJSONObject(index) ?: return@mapNotNull null
            val username = entry.optString("username").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val playerId = entry.optIntOrNull("playerId")?.toString() ?: return@mapNotNull null
            PlayerCoinState(
                id = playerId,
                displayName = username,
                coins = entry.optInt("coins", 3),
            )
        }
        Log.d(TAG, "Lobby roster received: ${mutablePlayers.value.size} players")
    }

    private fun handleLobbyError(json: JSONObject) {
        if (json.optString("type") != ERROR_TYPE) return

        val payload = json.optJSONObject("payload")
        val errorCode = payload?.optString("errorCode").orEmpty()
        val message = json.optString("content").ifBlank { "Failed to join lobby" }

        if (
            errorCode == "INVALID_LOBBY_CODE" ||
            errorCode == "GAME_NOT_FOUND" ||
            errorCode == "GAME_STARTED" ||
            errorCode == "GAME_FINISHED" ||
            errorCode == "LOBBY_FULL"
        ) {
            Log.w(TAG, "Lobby join error received [$errorCode]: $message")
            mutableLobbyJoinErrors.tryEmit(message)
        }
    }

    private fun handleGameStarted(json: JSONObject) {
        if (json.optString("type") != GAME_STARTED_TYPE) return

        val payload = json.optJSONObject("payload") ?: return
        val game = payload.optJSONObject("game") ?: return
        val gameId = json.optIntOrNull("gameId") ?: game.optIntOrNull("id") ?: return

        mutableActiveGameId.value = gameId
        subscribeToGameTopic(gameId)
        payload.optIntOrNull("activePlayerId")?.let { mutableActivePlayerId.value = it }

        game.optString("lobbyCode")
            .takeIf { it.isNotBlank() }
            ?.let { mutableLobbyCode.value = it }

        parseTurnPhase(game.optString("turnPhase"))?.let { mutableGamePhase.value = it }
        mutablePlayers.value = payload.optJSONArray("players").toPlayerCoinStates(payload, game)
        updateShopItemsFromState(payload)
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
        val game = state.optJSONObject("game") ?: return

        val gameId = json.optIntOrNull("gameId") ?: game.optIntOrNull("id")
        if (gameId != null) {
            mutableActiveGameId.value = gameId
            subscribeToGameTopic(gameId)
        }

        game.optString("lobbyCode")
            .takeIf { it.isNotBlank() }
            ?.let { mutableLobbyCode.value = it }

        parseGameStatus(game.optString("status"))?.let { mutableGameStatus.value = it }
        parseTurnPhase(game.optString("turnPhase"))?.let { mutableGamePhase.value = it }
        game.optIntOrNull("roundNumber")?.let { mutableRoundNumber.value = it }
        // The snapshot persists only the dice total (lastDiceRoll), not the
        // individual dice — surface it as a single-element list so the game
        // screen can show the last roll on reconnect.
        game.optIntOrNull("lastDiceRoll")?.let { mutableDiceResult.value = listOf(it) }

        mutablePlayers.value = state.optJSONArray("players").toPlayerCoinStates(state, game)
        mutableActivePlayerId.value = resolveActiveUserId(state, game)
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
            ShopItem(
                purchaseType = PurchaseType.ESTABLISHMENT,
                type = cardType.name,
                displayName = cardType.displayName(),
                cost = definition.optInt("cost"),
                color = definition.optString("color").toShopItemColor(),
                establishmentType = definition.optString("establishmentType"),
                imageKey = "card_${cardType.name.lowercase()}",
                isAvailable = (marketplace[cardType] ?: 0) > 0
            )
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
                imageKey = "landmark_${landmarkType.name.lowercase()}",
                isAvailable = true
            )
        }
    }

    private fun String.toShopItemColor(): ShopItemColor =
        runCatching { ShopItemColor.valueOf(this) }.getOrDefault(ShopItemColor.BLUE)

    private fun CardType.displayName(): String = name.toDisplayName()

    private fun LandmarkType.displayName(): String = name.toDisplayName()

    private fun String.toDisplayName(): String =
        lowercase()
            .split("_")
            .joinToString(" ") { part -> part.replaceFirstChar { it.titlecase() } }

    private fun parseGameStatus(name: String): GameStatus? =
        name.takeIf { it.isNotEmpty() }
            ?.let { runCatching { GameStatus.valueOf(it) }.getOrNull() }

    /**
     * Resolves the active player's **userId** (not playerId) from the snapshot:
     * turnOrder holds playerIds, currentTurnIndex points into it, and the
     * matching player row carries the userId that [activePlayerId] is compared
     * against (`myUserId == activePlayerId`).
     */
    private fun resolveActiveUserId(state: JSONObject, game: JSONObject): Int? {
        val currentTurnIndex = game.optIntOrNull("currentTurnIndex") ?: return null
        val turnOrder = state.optJSONArray("turnOrder") ?: return null
        if (currentTurnIndex !in 0 until turnOrder.length()) return null
        val activePlayerId = turnOrder.optInt(currentTurnIndex)
        val players = state.optJSONArray("players") ?: return null
        for (i in 0 until players.length()) {
            val player = players.optJSONObject(i) ?: continue
            if (player.optInt("id") == activePlayerId) return player.optIntOrNull("userId")
        }
        return null
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

    /**
     * Parses incoming ROLL_DICE results from the server.
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

    private fun subscribeToLobbyQueue() {
        webSocket?.send(
            StompFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "lobby-queue",
                    "destination" to WebSocketContract.lobbyQueue
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
            webSocket = null
            subscribedGameId = null
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

    private fun isAuthRejection(body: String): Boolean =
        body.trim().contains(AUTH_REJECTION_BODY)

    private fun resetGameState() {
        mutableGamePhase.value = GamePhase.NONE
        mutablePlayers.value = emptyList()
        mutableDiceResult.value = null
        mutableActivePlayerId.value = null
        mutableLobbyCode.value = null
        mutableGameStatus.value = null
        mutableRoundNumber.value = null
        mutablePlayerLandmarks.value = emptyMap()
        mutableMarketplace.value = emptyMap()
        mutableShopItems.value = emptyList()
    }

    private fun resetLobbyState() {
        mutableLobbyCode.value = null
        mutableActiveGameId.value = null
        mutableIsLobbyHost.value = false
        mutablePlayers.value = emptyList()
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
        val resolvedDisplayName =
            optString("username").takeIf { it.isNotBlank() }
                ?: optString("name").takeIf { it.isNotBlank() }
                ?: optString("displayName").takeIf { it.isNotBlank() }
                ?: "Player $playerId"
        return PlayerCoinState(
            id = playerId.toString(),
            displayName = resolvedDisplayName,
            coins = optInt("coins"),
            // The backend currently exposes one active turn player from currentTurnIndex.
            // Until the UI needs a separate local-player distinction, both flags
            // intentionally point to the same active player.
            isCurrentPlayer = playerId == currentPlayerId,
            isActivePlayer = playerId == currentPlayerId,
        )
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

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
        // Exponential backoff for auto-reconnect; the final value repeats once
        // the list is exhausted. Long enough to ride out a backend container
        // restart without hammering the server.
        private val RECONNECT_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L)
        private const val GAME_ACTION_TYPE = "GAME_ACTION"
        private const val GAME_STARTED_TYPE = "GAME_STARTED"
        private const val AUTH_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val LOBBY_CREATED_TYPE = "LOBBY_CREATED"
        private const val LOBBY_JOINED_TYPE = "LOBBY_JOINED"
        private const val LOBBY_LEFT_TYPE = "LOBBY_LEFT"
        private const val LOBBY_ROSTER_TYPE = "LOBBY_ROSTER"
        private const val ERROR_TYPE = "ERROR"
        private const val ROLL_DICE_TYPE = "ROLL_DICE"
        private const val SYNC_TYPE = "SYNC"
        // Frozen contract: matches GENERIC_AUTH_FAILURE on the server's
        // StompAuthChannelInterceptor. If the server message changes, this
        // client will silently fall through to the generic ERROR handling.
        private const val AUTH_REJECTION_BODY = "Authentication failed"
        private const val GAME_START_BODY =
            """{"type":"START","sender":"${WebSocketContract.defaultSender}"}"""

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
