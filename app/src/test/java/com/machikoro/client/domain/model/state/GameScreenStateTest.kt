package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.LandmarkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameScreenStateTest {
    @Test
    fun initialUsesNoneGamePhaseAndIdleConnectionStatus() {
        val state = GameScreenState.initial()

        assertEquals(GamePhase.NONE, state.gamePhase)
        assertEquals(ConnectionStatus.IDLE, state.connectionStatus)
        assertEquals(emptyList<PlayerCoinState>(), state.players)
        assertEquals(null, state.gameId)
        assertEquals(null, state.diceResult)
        assertEquals(null, state.activePlayerId)
        assertEquals(null, state.myUserId)
        assertEquals(false, state.isActivePlayer)
        assertEquals(PurchaseState.IDLE, state.purchaseState)
        assertEquals(null, state.selectedPurchaseItemType)
        assertEquals(null, state.pendingPurchaseItemType)
        assertEquals(null, state.purchaseFeedbackItemType)
        assertEquals(null, state.purchaseMessage)
        assertEquals(false, state.isBuyingPhase)
    }

    @Test
    fun buyOrBuildPhaseIsBuyingPhase() {
        val state = GameScreenState.initial().copy(gamePhase = GamePhase.BUY_OR_BUILD)

        assertEquals(true, state.isBuyingPhase)
    }

    @Test
    fun hasTrainStationUsesActivePlayersDatabasePlayerIdWhenUserIdDiffers() {
        val state = trainStationState(
            playerLandmarks = mapOf(
                ACTIVE_PLAYER_DATABASE_ID to listOf(
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.TRAIN_STATION,
                        isBuilt = true,
                    ),
                ),
            ),
        )

        assertTrue(state.hasTrainStation)
    }

    @Test
    fun hasTrainStationIsFalseWhenActivePlayerHasNoBuiltTrainStation() {
        val state = trainStationState(
            playerLandmarks = mapOf(
                ACTIVE_PLAYER_DATABASE_ID to listOf(
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.TRAIN_STATION,
                        isBuilt = false,
                    ),
                ),
                ACTIVE_USER_ID to listOf(
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.TRAIN_STATION,
                        isBuilt = true,
                    ),
                ),
            ),
        )

        assertFalse(state.hasTrainStation)
    }

    @Test
    fun hasTrainStationIsFalseWhenNoPlayerIsMarkedActive() {
        val state = GameScreenState.initial().copy(
            activePlayerId = ACTIVE_USER_ID,
            myUserId = ACTIVE_USER_ID,
            players = listOf(
                PlayerCoinState(
                    id = ACTIVE_PLAYER_DATABASE_ID.toString(),
                    displayName = "Waiting Player",
                    coins = 4,
                    isCurrentPlayer = true,
                    isActivePlayer = false,
                ),
            ),
            playerLandmarks = mapOf(
                ACTIVE_PLAYER_DATABASE_ID to listOf(
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.TRAIN_STATION,
                        isBuilt = true,
                    ),
                ),
            ),
        )

        assertFalse(state.hasTrainStation)
    }

    @Test
    fun hasTrainStationIsFalseWhenActivePlayerIdCannotBeMappedToDatabaseId() {
        val state = GameScreenState.initial().copy(
            activePlayerId = ACTIVE_USER_ID,
            myUserId = ACTIVE_USER_ID,
            players = listOf(
                PlayerCoinState(
                    id = "player-$ACTIVE_PLAYER_DATABASE_ID",
                    displayName = "Active Player",
                    coins = 4,
                    isCurrentPlayer = true,
                    isActivePlayer = true,
                ),
            ),
            playerLandmarks = mapOf(
                ACTIVE_PLAYER_DATABASE_ID to listOf(
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.TRAIN_STATION,
                        isBuilt = true,
                    ),
                ),
            ),
        )

        assertFalse(state.hasTrainStation)
    }

    @Test
    fun hasTrainStationIsFalseWhenActivePlayerLandmarksAreMissing() {
        val state = trainStationState(playerLandmarks = emptyMap())

        assertFalse(state.hasTrainStation)
    }

    @Test
    fun hasTrainStationIgnoresBuiltNonTrainStationLandmarks() {
        val state = trainStationState(
            playerLandmarks = mapOf(
                ACTIVE_PLAYER_DATABASE_ID to listOf(
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.SHOPPING_MALL,
                        isBuilt = true,
                    ),
                ),
            ),
        )

        assertFalse(state.hasTrainStation)
    }

    private fun trainStationState(
        playerLandmarks: Map<Int, List<PlayerLandmarkState>>,
    ): GameScreenState =
        GameScreenState.initial().copy(
            activePlayerId = ACTIVE_USER_ID,
            myUserId = ACTIVE_USER_ID,
            players = listOf(
                PlayerCoinState(
                    id = ACTIVE_PLAYER_DATABASE_ID.toString(),
                    displayName = "Active Player",
                    coins = 4,
                    isCurrentPlayer = true,
                    isActivePlayer = true,
                ),
                PlayerCoinState(
                    id = OTHER_PLAYER_DATABASE_ID.toString(),
                    displayName = "Waiting Player",
                    coins = 3,
                    isCurrentPlayer = false,
                    isActivePlayer = false,
                ),
            ),
            playerLandmarks = playerLandmarks,
        )

    private companion object {
        const val ACTIVE_USER_ID = 202
        const val ACTIVE_PLAYER_DATABASE_ID = 11
        const val OTHER_PLAYER_DATABASE_ID = 22
    }
}
