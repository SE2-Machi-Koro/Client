package com.machikoro.client.network.websocket

import okhttp3.Request
import okhttp3.WebSocketListener
import org.junit.Assert.assertNotNull
import org.junit.Test

class WebSocketFactoryInterfaceTest {
    @Test
    fun testInterfaceReference() {
        val clazz = WebSocketFactory::class
        assertNotNull(clazz)
    }
}
