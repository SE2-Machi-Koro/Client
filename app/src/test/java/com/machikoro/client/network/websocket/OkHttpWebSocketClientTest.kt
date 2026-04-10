package com.machikoro.client.network.websocket

import com.machikoro.client.model.state.ConnectionStatus
import java.io.IOException
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OkHttpWebSocketClientTest {
    @Test
    fun connectMovesStatusToConnecting() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()

        assertEquals(ConnectionStatus.CONNECTING, client.connectionStatus.value)
    }

    @Test
    fun successfulOpenMovesStatusToConnected() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()
        factory.simulateOpen()

        assertEquals(ConnectionStatus.CONNECTED, client.connectionStatus.value)
    }

    @Test
    fun disconnectClosesSocketAndMovesStatusToDisconnected() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()
        client.disconnect()

        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
        assertTrue(factory.socket.closed)
    }

    @Test
    fun failureMovesStatusToError() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()
        factory.simulateFailure(IOException("boom"))

        assertEquals(ConnectionStatus.ERROR, client.connectionStatus.value)
    }

    @Test
    fun invalidUrlMovesStatusToError() {
        val client = OkHttpWebSocketClient(websocketUrl = "not-a-url")

        client.connect()

        assertEquals(ConnectionStatus.ERROR, client.connectionStatus.value)
    }

    private class FakeWebSocketFactory : WebSocketFactory {
        lateinit var listener: WebSocketListener
        val socket = FakeWebSocket()

        override fun create(request: Request, listener: WebSocketListener): WebSocket {
            this.listener = listener
            socket.request = request
            return socket
        }

        fun simulateOpen() {
            listener.onOpen(socket, createResponse(socket.request))
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

        override fun request(): Request = request

        override fun queueSize(): Long = 0L

        override fun send(text: String): Boolean = true

        override fun send(bytes: ByteString): Boolean = false

        override fun close(code: Int, reason: String?): Boolean {
            closed = true
            return true
        }

        override fun cancel() {
            closed = true
        }
    }
}
