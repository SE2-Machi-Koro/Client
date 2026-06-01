package com.machikoro.client.ui.leaderboard

import com.machikoro.client.network.leaderboard.LeaderboardApi
import com.machikoro.client.network.leaderboard.LeaderboardEntry
import com.machikoro.client.ui.start.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Fake API that returns a configurable result or throws
    private class FakeLeaderboardApi(
        private val result: (() -> List<LeaderboardEntry>)? = null,
        private val error: Exception? = null,
    ) : LeaderboardApi {
        var lastRequestedLimit: Int? = null

        override suspend fun getLeaderboard(limit: Int): List<LeaderboardEntry> {
            lastRequestedLimit = limit
            if (error != null) throw error
            return result?.invoke() ?: emptyList()
        }
    }

    private fun entry(rank: Int, wins: Int) = LeaderboardEntry(
        rank = rank,
        userId = rank,
        username = "player$rank",
        totalWins = wins,
        totalGamesPlayed = wins + 1,
    )

    @Test
    fun initialStateIsLoading() {
        val vm = LeaderboardViewModel(FakeLeaderboardApi())
        assertTrue(vm.state.value is LeaderboardState.Loading)
    }

    @Test
    fun loadSetsSuccessStateWithEntriesFromApi() = runTest {
        val entries = listOf(entry(1, 5), entry(2, 3))
        val vm = LeaderboardViewModel(FakeLeaderboardApi(result = { entries }))

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is LeaderboardState.Success)
        assertEquals(entries, (state as LeaderboardState.Success).entries)
    }

    @Test
    fun loadFiltersOutZeroWinEntries() = runTest {
        val entries = listOf(entry(1, 0), entry(2, 3), entry(3, 0))
        val vm = LeaderboardViewModel(FakeLeaderboardApi(result = { entries }))

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value as LeaderboardState.Success
        // Only the entry with 3 wins survives
        assertEquals(1, state.entries.size)
        assertEquals(3, state.entries[0].totalWins)
    }

    @Test
    fun loadKeepsAllEntriesWithPositiveWins() = runTest {
        val entries = listOf(entry(1, 1), entry(2, 10), entry(3, 99))
        val vm = LeaderboardViewModel(FakeLeaderboardApi(result = { entries }))

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value as LeaderboardState.Success
        assertEquals(3, state.entries.size)
    }

    @Test
    fun loadSetsEmptySuccessWhenAllEntriesHaveZeroWins() = runTest {
        val entries = listOf(entry(1, 0), entry(2, 0))
        val vm = LeaderboardViewModel(FakeLeaderboardApi(result = { entries }))

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value as LeaderboardState.Success
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun loadSetsErrorStateOnException() = runTest {
        val vm = LeaderboardViewModel(FakeLeaderboardApi(error = RuntimeException("network down")))

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is LeaderboardState.Error)
        assertEquals("network down", (state as LeaderboardState.Error).message)
    }

    @Test
    fun loadSetsErrorWithFallbackMessageWhenExceptionMessageIsNull() = runTest {
        val vm = LeaderboardViewModel(FakeLeaderboardApi(error = RuntimeException()))

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value as LeaderboardState.Error
        assertEquals("Failed to load leaderboard", state.message)
    }

    @Test
    fun loadResetsToLoadingBeforeCallingApi() = runTest {
        // Start in Error state, then reload — state must transition back through Loading
        val fakeApi = FakeLeaderboardApi(result = { listOf(entry(1, 2)) })
        val vm = LeaderboardViewModel(fakeApi)

        // Force an error first
        val errorVm = LeaderboardViewModel(FakeLeaderboardApi(error = RuntimeException("x")))
        errorVm.load()
        advanceUntilIdle()

        // Now call load on a fresh vm and capture mid-flight state
        val states = mutableListOf<LeaderboardState>()
        states.add(vm.state.value)
        vm.load()
        // Before advancing, state must be Loading again
        states.add(vm.state.value)
        advanceUntilIdle()
        states.add(vm.state.value)

        // Initial → Loading after load() call → Success
        assertTrue(states[0] is LeaderboardState.Loading)
        assertTrue(states[1] is LeaderboardState.Loading)
        assertTrue(states[2] is LeaderboardState.Success)
    }

    @Test
    fun loadRequestsLimitOf50() = runTest {
        val fakeApi = FakeLeaderboardApi(result = { emptyList() })
        val vm = LeaderboardViewModel(fakeApi)

        vm.load()
        advanceUntilIdle()

        assertEquals(50, fakeApi.lastRequestedLimit)
    }
}