package com.machikoro.client.ui.win

import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import junit.framework.TestCase.assertEquals
import org.junit.Test

class WinnerResolverTest {

    @Test
    fun resolveWinnerNameReturnsMatchingPlayerName() {
        val state = GameScreenState.initial().copy(
            winnerId = 11,
            players = listOf(
                PlayerCoinState(
                    id = "11",
                    displayName = "Alice",
                    coins = 5
                )
            )
        )

        assertEquals(
            "Alice",
            resolveWinnerName(state)
        )
    }

    @Test
    fun resolveWinnerNameReturnsDefaultWhenNoWinnerIdSet() {
        val state = GameScreenState.initial().copy(
            winnerId = null,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5)
            )
        )

        assertEquals("Winner", resolveWinnerName(state))
    }

    @Test
    fun resolveWinnerNameReturnsDefaultWhenWinnerNotInPlayerList() {
        val state = GameScreenState.initial().copy(
            winnerId = 99,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5)
            )
        )

        assertEquals("Winner", resolveWinnerName(state))
    }

    @Test
    fun resolveWinnerNameReturnsCorrectPlayerFromMultiplePlayers() {
        val state = GameScreenState.initial().copy(
            winnerId = 2,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
                PlayerCoinState(id = "3", displayName = "Charlie", coins = 3),
            )
        )

        assertEquals("Bob", resolveWinnerName(state))
    }

    @Test
    fun resolveRankedPlayersWinnerIsAlwaysFirst() {
        val state = GameScreenState.initial().copy(
            winnerId = 2,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
                PlayerCoinState(id = "3", displayName = "Charlie", coins = 3),
            ),
            playerLandmarks = mapOf(
                1 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
                2 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = true) },
                3 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
            )
        )

        val ranked = resolveRankedPlayers(state)
        assertEquals("Bob", ranked.first().first)
    }

    @Test
    fun resolveRankedPlayersSortsOthersByBuiltLandmarksDescending() {
        val state = GameScreenState.initial().copy(
            winnerId = 1,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
                PlayerCoinState(id = "3", displayName = "Charlie", coins = 3),
            ),
            playerLandmarks = mapOf(
                1 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = true) },
                2 to listOf(
                    PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
                ),
                3 to listOf(
                    PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
                ),
            )
        )

        val ranked = resolveRankedPlayers(state)
        assertEquals("Alice", ranked[0].first)
        assertEquals("Bob", ranked[1].first)
        assertEquals("Charlie", ranked[2].first)
    }

    @Test
    fun resolveRankedPlayersReturnsAllPlayers() {
        val state = GameScreenState.initial().copy(
            winnerId = 1,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
                PlayerCoinState(id = "3", displayName = "Charlie", coins = 3),
                PlayerCoinState(id = "4", displayName = "Diana", coins = 7),
            ),
            playerLandmarks = emptyMap()
        )

        val ranked = resolveRankedPlayers(state)
        assertEquals(4, ranked.size)
    }

    @Test
    fun resolveRankedPlayersHandlesNoWinnerId() {
        val state = GameScreenState.initial().copy(
            winnerId = null,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
            ),
            playerLandmarks = emptyMap()
        )

        val ranked = resolveRankedPlayers(state)
        assertEquals(2, ranked.size)
    }

    // resolveGameRankedPlayers tests

    @Test
    fun resolveGameRankedPlayersWinnerIsPlacementOne() {
        val state = GameScreenState.initial().copy(
            winnerId = 2,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
                PlayerCoinState(id = "3", displayName = "Charlie", coins = 3),
            ),
            playerLandmarks = mapOf(
                1 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
                2 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = true) },
                3 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
            )
        )

        val ranked = resolveGameRankedPlayers(state)
        assertEquals(1, ranked.first().placement)
        assertEquals("Bob", ranked.first().displayName)
    }

    @Test
    fun resolveGameRankedPlayersSortsOthersByBuiltLandmarksDescending() {
        val state = GameScreenState.initial().copy(
            winnerId = 1,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
                PlayerCoinState(id = "3", displayName = "Charlie", coins = 3),
            ),
            playerLandmarks = mapOf(
                1 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = true) },
                2 to listOf(
                    PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
                ),
                3 to listOf(
                    PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
                ),
            )
        )

        val ranked = resolveGameRankedPlayers(state)
        assertEquals("Alice", ranked[0].displayName)
        assertEquals("Bob", ranked[1].displayName)
        assertEquals("Charlie", ranked[2].displayName)
    }

    @Test
    fun resolveGameRankedPlayersAssignsCorrectPlacements() {
        val state = GameScreenState.initial().copy(
            winnerId = 1,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
                PlayerCoinState(id = "3", displayName = "Charlie", coins = 3),
            ),
            playerLandmarks = emptyMap()
        )

        val ranked = resolveGameRankedPlayers(state)
        assertEquals(1, ranked[0].placement)
        assertEquals(2, ranked[1].placement)
        assertEquals(3, ranked[2].placement)
    }

    @Test
    fun resolveGameRankedPlayersReturnsAllPlayers() {
        val state = GameScreenState.initial().copy(
            winnerId = 1,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
                PlayerCoinState(id = "3", displayName = "Charlie", coins = 3),
                PlayerCoinState(id = "4", displayName = "Diana", coins = 7),
            ),
            playerLandmarks = emptyMap()
        )

        val ranked = resolveGameRankedPlayers(state)
        assertEquals(4, ranked.size)
    }

    @Test
    fun resolveGameRankedPlayersReportsCorrectBuiltLandmarkCount() {
        val state = GameScreenState.initial().copy(
            winnerId = 1,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
            ),
            playerLandmarks = mapOf(
                1 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = true) },
                2 to listOf(
                    PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
                ),
            )
        )

        val ranked = resolveGameRankedPlayers(state)
        assertEquals(4, ranked[0].builtLandmarks) // Alice: all 4 built
        assertEquals(1, ranked[1].builtLandmarks) // Bob: 1 built
    }

    @Test
    fun resolveGameRankedPlayersHandlesNoWinnerId() {
        val state = GameScreenState.initial().copy(
            winnerId = null,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
            ),
            playerLandmarks = emptyMap()
        )

        val ranked = resolveGameRankedPlayers(state)
        assertEquals(2, ranked.size)
    }
}