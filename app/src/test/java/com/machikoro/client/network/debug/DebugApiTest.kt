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
        // Verify fillLobby is declared on the interface
        val method = DebugApi::class.java.methods.find { it.name == "fillLobby" }
        assertNotNull(method)
    }
}