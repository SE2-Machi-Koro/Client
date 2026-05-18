package com.machikoro.client.network.debug

import org.junit.Assert.assertNotNull
import org.junit.Test

class DebugApiTest {
    @Test
    fun interfaceCanBeReferenced() {
        val clazz = DebugApi::class
        assertNotNull(clazz)
    }

    @Test
    fun fillLobbyMethodExistsOnInterface() {
        val method = DebugApi::class.java.methods.find { it.name == "fillLobby" }
        assertNotNull(method)
    }

    @Test
    fun resetLobbyMethodExistsOnInterface() {
        val method = DebugApi::class.java.methods.find { it.name == "resetLobby" }
        assertNotNull(method)
    }
}