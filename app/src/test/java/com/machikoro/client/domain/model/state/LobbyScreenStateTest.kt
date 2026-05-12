package com.machikoro.client.domain.model.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LobbyScreenStateTest {
    @Test
    fun placeholderProvidesDefaultLobbyScreenState() {
        val state = LobbyScreenState.placeholder()

        assertEquals(ConnectionStatus.IDLE, state.connectionStatus)
        assertEquals(LobbyStatus.WAITING_FOR_PLAYERS, state.lobbyStatus)
        assertEquals(emptyList<String>(), state.playerList)
        assertEquals(4, state.maxPlayers)
        assertFalse(state.isHost)
        assertNull(state.loggedInAs)
    }
}