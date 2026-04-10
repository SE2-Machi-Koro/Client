package com.machikoro.client.network.websocket

import com.machikoro.client.model.state.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class OkHttpWebSocketClient(
    private val websocketUrl: String,
    private val webSocketFactory: WebSocketFactory = OkHttpWebSocketFactory()
) : WebSocketClient {
    override val connectionStatus: StateFlow<ConnectionStatus>
        get() = mutableConnectionStatus.asStateFlow()

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)

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
                mutableConnectionStatus.value = ConnectionStatus.ERROR
                return
            }

            mutableConnectionStatus.value = ConnectionStatus.CONNECTING
            webSocket = webSocketFactory.create(request, listener)
        }
    }

    override fun disconnect() {
        val currentSocket = synchronized(this) {
            val socket = webSocket
            webSocket = null
            socket
        }

        currentSocket?.close(NORMAL_CLOSURE_STATUS, "Client disconnect")
        mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            mutableConnectionStatus.value = ConnectionStatus.CONNECTED
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
            clearSocket()
            mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            clearSocket()
            mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            clearSocket()
            mutableConnectionStatus.value = ConnectionStatus.ERROR
        }
    }

    private fun clearSocket() {
        synchronized(this) {
            webSocket = null
        }
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}
