package com.machikoro.client.network.websocket

import android.util.Log
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
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
    private val webSocketFactory: WebSocketFactory = OkHttpWebSocketFactory()
) : WebSocketClient {
    override val connectionStatus: StateFlow<ConnectionStatus>
        get() = mutableConnectionStatus.asStateFlow()

    override val gamePhase: StateFlow<GamePhase>
        get() = mutableGamePhase.asStateFlow()

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
    private val frameBuffer = StringBuilder()

    @Volatile
    private var webSocket: WebSocket? = null

    override fun connect() {
        synchronized(this) {
            if (webSocket != null) {
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

        currentSocket?.send(StompFrame(command = "DISCONNECT").serialize())
        currentSocket?.close(NORMAL_CLOSURE_STATUS, "Client disconnect")
        Log.d(TAG, "Disconnect requested by client")
        mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
        mutableGamePhase.value = GamePhase.NONE
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened: ${response.code} ${response.message}")
            webSocket.send(
                StompFrame(
                    command = "CONNECT",
                    headers = mapOf(
                        "accept-version" to WebSocketContract.stompVersion,
                        "host" to websocketHostHeader(),
                        "heart-beat" to "0,0"
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
            mutableGamePhase.value = GamePhase.NONE
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code / $reason")
            clearSocket()
            mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
            mutableGamePhase.value = GamePhase.NONE
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
            mutableGamePhase.value = GamePhase.NONE
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
    }
}
