package com.machikoro.client.ui.game.ui

import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandmarkPurchaseRevealTest {
    @Test
    fun landmarkCardsUseNormalSizeWhenEnoughWidthIsAvailable() {
        assertEquals(155.dp, landmarkRevealCardWidth(700.dp))
    }

    @Test
    fun landmarkCardsShrinkEquallyToFitNarrowLayouts() {
        assertEquals(119.dp, landmarkRevealCardWidth(500.dp))
    }

    @Test
    fun activePlayerRevealShowsAllLandmarksAndBuildsPurchasedLandmark() {
        val state = state(
            myUserId = 42,
            landmarks = listOf(
                PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true)
            )
        )

        val reveal = state.landmarkPurchaseRevealUi(LandmarkType.SHOPPING_MALL)

        assertEquals("Your landmarks", reveal.title)
        assertEquals("You are closer to winning!", reveal.message)
        assertEquals(LandmarkType.entries, reveal.landmarks.map { it.landmarkType })
        assertTrue(
            reveal.landmarks.first { it.landmarkType == LandmarkType.SHOPPING_MALL }.isBuilt
        )
    }

    @Test
    fun passivePlayerRevealNamesTheActivePlayer() {
        val state = state(
            myUserId = 7,
            landmarks = emptyList()
        )

        val reveal = state.landmarkPurchaseRevealUi(LandmarkType.AMUSEMENT_PARK)

        assertEquals("Player 1's landmarks", reveal.title)
        assertEquals("Player 1 is closer to winning!", reveal.message)
    }

    @Test
    fun activePlayerWithOneLandmarkLeftGetsEncouragement() {
        val state = state(
            myUserId = 42,
            landmarks = listOf(
                PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = true)
            )
        )

        val reveal = state.landmarkPurchaseRevealUi(LandmarkType.AMUSEMENT_PARK)

        assertEquals("Only 1 landmark left - keep going!", reveal.message)
    }

    @Test
    fun passivePlayerSeesWhenActivePlayerHasAlmostFinished() {
        val state = state(
            myUserId = 7,
            landmarks = listOf(
                PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = true)
            )
        )

        val reveal = state.landmarkPurchaseRevealUi(LandmarkType.AMUSEMENT_PARK)

        assertEquals("Player 1 has almost finished!", reveal.message)
    }

    @Test
    fun finalLandmarkShowsCompletionMessage() {
        val state = state(
            myUserId = 42,
            landmarks = LandmarkType.entries.map {
                PlayerLandmarkState(
                    landmarkType = it,
                    isBuilt = it != LandmarkType.RADIO_TOWER
                )
            }
        )

        val reveal = state.landmarkPurchaseRevealUi(LandmarkType.RADIO_TOWER)

        assertEquals("You won!", reveal.message)
        assertTrue(reveal.landmarks.all { it.isBuilt })
    }

    private fun state(
        myUserId: Int,
        landmarks: List<PlayerLandmarkState>,
    ): GameScreenState = GameScreenState.initial().copy(
        myUserId = myUserId,
        activePlayerId = 42,
        players = listOf(
            PlayerCoinState(
                id = "100",
                displayName = "Player 1",
                coins = 10,
                isActivePlayer = true
            )
        ),
        playerLandmarks = mapOf(100 to landmarks)
    )
}
