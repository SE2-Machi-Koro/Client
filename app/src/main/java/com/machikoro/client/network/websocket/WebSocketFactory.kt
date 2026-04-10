package com.machikoro.client.network.websocket

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

fun interface WebSocketFactory {
    fun create(request: Request, listener: WebSocketListener): WebSocket
}

class OkHttpWebSocketFactory(
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : WebSocketFactory {
    override fun create(request: Request, listener: WebSocketListener): WebSocket {
        return okHttpClient.newWebSocket(request, listener)
    }
}
