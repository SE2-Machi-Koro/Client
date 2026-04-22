package com.machikoro.client.network.websocket

import com.machikoro.client.domain.model.state.ConnectionStatus
import java.io.IOException
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
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()

        assertEquals(ConnectionStatus.CONNECTING, client.connectionStatus.value)
    }

    @Test
    fun openSendsStompConnectFrame() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()
        factory.simulateOpen()

        assertTrue(factory.socket.sentMessages.first().startsWith("CONNECT\n"))
        assertTrue(factory.socket.sentMessages.first().contains("accept-version:1.2"))
        assertTrue(factory.socket.sentMessages.first().contains("host:10.0.2.2:8080"))
    }

    @Test
    fun connectedFrameMovesStatusToConnectedAndTriggersSubscribeAndJoin() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

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
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

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
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()
        factory.simulateFailure(IOException("boom"))

        assertEquals(ConnectionStatus.ERROR, client.connectionStatus.value)
    }

    @Test
    fun closingMovesStatusToDisconnected() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()
        factory.simulateClosing()

        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
        assertTrue(factory.socket.closed)
    }

    @Test
    fun closedMovesStatusToDisconnected() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()
        factory.simulateClosed()

        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
    }

    @Test
    fun secondConnectDoesNotCreateAnotherSocket() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.connect()
        client.connect()

        assertEquals(1, factory.createCount)
    }

    @Test
    fun disconnectWithoutActiveSocketStillUpdatesStatus() {
        val factory = FakeWebSocketFactory()
        val client = OkHttpWebSocketClient(
            websocketUrl = "ws://10.0.2.2:8080/ws",
            webSocketFactory = factory
        )

        client.disconnect()

        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
        assertFalse(factory.socket.closed)
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
}
