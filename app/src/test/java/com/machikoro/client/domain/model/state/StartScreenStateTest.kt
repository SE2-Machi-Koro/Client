package com.machikoro.client.domain.model.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartScreenStateTest {
    @Test
    fun placeholderProvidesDefaultStartScreenState() {
        val state = StartScreenState.placeholder()

        assertEquals("Machi Koro Client", state.title)
        assertEquals(ConnectionStatus.IDLE, state.connectionStatus)
        assertNull(state.loggedInAs)
    }
}
