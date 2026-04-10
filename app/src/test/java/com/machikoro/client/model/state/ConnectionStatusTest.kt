package com.machikoro.client.model.state

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionStatusTest {
    @Test
    fun displayTextMatchesExpectedLabels() {
        assertEquals("idle", ConnectionStatus.IDLE.toDisplayText())
        assertEquals("connecting", ConnectionStatus.CONNECTING.toDisplayText())
        assertEquals("connected", ConnectionStatus.CONNECTED.toDisplayText())
        assertEquals("disconnected", ConnectionStatus.DISCONNECTED.toDisplayText())
        assertEquals("connection error", ConnectionStatus.ERROR.toDisplayText())
    }
}
