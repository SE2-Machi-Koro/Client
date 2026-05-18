package com.machikoro.client.network.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DebugDtosTest {
    @Test
    fun fillLobbyRequestHoldsLobbyCode() {
        val request = FillLobbyRequest(lobbyCode = "ABC123")
        assertEquals("ABC123", request.lobbyCode)
    }

    @Test
    fun fillLobbyRequestEqualityIsValueBased() {
        val a = FillLobbyRequest(lobbyCode = "XYZ")
        val b = FillLobbyRequest(lobbyCode = "XYZ")
        assertEquals(a, b)
    }

    @Test
    fun fillLobbyRequestInequalityOnDifferentCode() {
        val a = FillLobbyRequest(lobbyCode = "AAA")
        val b = FillLobbyRequest(lobbyCode = "BBB")
        assertNotEquals(a, b)
    }

    @Test
    fun fillLobbyRequestCopyProducesCorrectResult() {
        val original = FillLobbyRequest(lobbyCode = "OLD")
        val copy = original.copy(lobbyCode = "NEW")
        assertEquals("NEW", copy.lobbyCode)
        assertEquals("OLD", original.lobbyCode)
    }
}