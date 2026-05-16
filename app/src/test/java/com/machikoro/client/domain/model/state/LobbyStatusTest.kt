package com.machikoro.client.domain.model.state

import org.junit.Assert.assertEquals
import org.junit.Test

class LobbyStatusTest {
    @Test
    fun testEnumValues() {
        val values = LobbyStatus.values()
        assertEquals(3, values.size)
        assertEquals(LobbyStatus.PLACEHOLDER, values[0])
        assertEquals(LobbyStatus.WAITING_FOR_PLAYERS, values[1])
        assertEquals(LobbyStatus.READY, values[2])
    }
}
