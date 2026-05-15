package com.machikoro.client.network.websocket

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock

class WebSocketFactoryTest {
    @Test
    fun testOkHttpWebSocketFactoryCreate() {
        val factory = OkHttpWebSocketFactory(OkHttpClient())
        val request = Request.Builder().url("ws://localhost").build()
        val listener = mock(WebSocketListener::class.java)
        val ws = factory.create(request, listener)
        assertNotNull(ws)
    }
}
