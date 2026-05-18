package com.machikoro.client.network.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DebugDtosTest {
    @Test
    fun fillLobbyRequestHoldsLobbyCode() {
        val request = FillLobbyRequest(lobbyCode = "ABC123")
        assertEquals("ABC123", request.lobbyCode)
    }

    @Test
    fun fillLobbyRequestDefaultsCountToNull() {
        val request = FillLobbyRequest(lobbyCode = "ABC123")
        assertNull(request.count)
    }

    @Test
    fun fillLobbyRequestAcceptsExplicitCount() {
        val request = FillLobbyRequest(lobbyCode = "ABC123", count = 2)
        assertEquals(2, request.count)
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
    fun fillLobbyRequestInequalityOnDifferentCount() {
        val a = FillLobbyRequest(lobbyCode = "AAA", count = 1)
        val b = FillLobbyRequest(lobbyCode = "AAA", count = 3)
        assertNotEquals(a, b)
    }

    @Test
    fun fillLobbyRequestCopyProducesCorrectResult() {
        val original = FillLobbyRequest(lobbyCode = "OLD", count = 1)
        val copy = original.copy(lobbyCode = "NEW", count = 3)
        assertEquals("NEW", copy.lobbyCode)
        assertEquals(3, copy.count)
        assertEquals("OLD", original.lobbyCode)
    }

    @Test
    fun resetLobbyRequestHoldsLobbyCode() {
        val request = ResetLobbyRequest(lobbyCode = "ABC123")
        assertEquals("ABC123", request.lobbyCode)
    }

    @Test
    fun resetLobbyRequestEqualityIsValueBased() {
        val a = ResetLobbyRequest(lobbyCode = "XYZ")
        val b = ResetLobbyRequest(lobbyCode = "XYZ")
        assertEquals(a, b)
    }

    @Test
    fun resetLobbyRequestInequalityOnDifferentCode() {
        val a = ResetLobbyRequest(lobbyCode = "AAA")
        val b = ResetLobbyRequest(lobbyCode = "BBB")
        assertNotEquals(a, b)
    }

    @Test
    fun resetLobbyRequestCopyProducesCorrectResult() {
        val original = ResetLobbyRequest(lobbyCode = "OLD")
        val copy = original.copy(lobbyCode = "NEW")
        assertEquals("NEW", copy.lobbyCode)
        assertEquals("OLD", original.lobbyCode)
    }
}