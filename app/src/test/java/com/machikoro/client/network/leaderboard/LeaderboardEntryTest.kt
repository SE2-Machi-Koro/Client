package com.machikoro.client.network.leaderboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LeaderboardEntryTest {

    private fun entry() = LeaderboardEntry(
        rank = 1,
        userId = 42,
        username = "alice",
        totalWins = 10,
        totalGamesPlayed = 15,
    )

    @Test
    fun constructorStoresAllFields() {
        val e = entry()
        assertEquals(1, e.rank)
        assertEquals(42, e.userId)
        assertEquals("alice", e.username)
        assertEquals(10, e.totalWins)
        assertEquals(15, e.totalGamesPlayed)
    }

    @Test
    fun equalEntriesAreEqual() {
        assertEquals(entry(), entry())
    }

    @Test
    fun entriesDifferingByRankAreNotEqual() {
        assertNotEquals(entry(), entry().copy(rank = 2))
    }

    @Test
    fun entriesDifferingByUsernameAreNotEqual() {
        assertNotEquals(entry(), entry().copy(username = "bob"))
    }

    @Test
    fun entriesDifferingByWinsAreNotEqual() {
        assertNotEquals(entry(), entry().copy(totalWins = 0))
    }

    @Test
    fun copyProducesIndependentInstance() {
        val original = entry()
        val copy = original.copy(totalWins = 99)
        assertEquals(10, original.totalWins)
        assertEquals(99, copy.totalWins)
    }

    @Test
    fun toStringContainsUsername() {
        val str = entry().toString()
        assertTrue(str.contains("alice"))
    }

    // Helper pulled in without importing Assert.assertTrue
    private fun assertTrue(value: Boolean) = assertEquals(true, value)
}