package com.machikoro.client.ui.leaderboard

import com.machikoro.client.network.leaderboard.LeaderboardEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardStateTest {

    private val sampleEntry = LeaderboardEntry(
        rank = 1,
        userId = 1,
        username = "alice",
        totalWins = 5,
        totalGamesPlayed = 8,
    )

    @Test
    fun loadingStateIsNotSuccessOrError() {
        val state: LeaderboardState = LeaderboardState.Loading
        assertTrue(state !is LeaderboardState.Success)
        assertTrue(state !is LeaderboardState.Error)
    }

    @Test
    fun successStateStoresEntries() {
        val state = LeaderboardState.Success(listOf(sampleEntry))
        assertEquals(1, state.entries.size)
        assertEquals("alice", state.entries[0].username)
    }

    @Test
    fun successStateWithEmptyListIsValid() {
        val state = LeaderboardState.Success(emptyList())
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun errorStateStoresMessage() {
        val state = LeaderboardState.Error("something went wrong")
        assertEquals("something went wrong", state.message)
    }

    @Test
    fun twoSuccessStatesWithSameEntriesAreEqual() {
        val a = LeaderboardState.Success(listOf(sampleEntry))
        val b = LeaderboardState.Success(listOf(sampleEntry))
        assertEquals(a, b)
    }

    @Test
    fun twoErrorStatesWithSameMessageAreEqual() {
        assertEquals(
            LeaderboardState.Error("err"),
            LeaderboardState.Error("err"),
        )
    }
}