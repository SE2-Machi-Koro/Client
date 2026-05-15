package com.machikoro.client.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Rule
import org.junit.Test

/**
 * Verifies the GameScreen renders every field of a /app/game.sync reconnect
 * snapshot — round number, marketplace supply, per-player landmark build state
 * and the last dice roll — so a reconnecting player sees the full board.
 */
class GameScreenSnapshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** A single-player mid-game snapshot — one player keeps every assertion unambiguous. */
    private fun reconnectState() = GameScreenState(
        connectionStatus = ConnectionStatus.CONNECTED,
        gamePhase = GamePhase.BUY_OR_BUILD,
        players = listOf(
            PlayerCoinState(id = "1", displayName = "You", coins = 9, isCurrentPlayer = true)
        ),
        diceResult = listOf(8),
        activePlayerId = 1,
        myUserId = 1,
        gameStatus = GameStatus.IN_PROGRESS,
        roundNumber = 4,
        playerLandmarks = mapOf(
            1 to listOf(
                PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = true),
                PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
                PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
            )
        ),
        marketplace = mapOf(CardType.WHEAT_FIELD to 6, CardType.BAKERY to 5),
    )

    @Test
    fun rendersRoundNumberFromSnapshot() {
        composeTestRule.setContent { ClientTheme { GameScreen(state = reconnectState()) } }

        composeTestRule.onNodeWithText("Round 4").assertIsDisplayed()
    }

    @Test
    fun rendersMarketplaceSupplyFromSnapshot() {
        composeTestRule.setContent { ClientTheme { GameScreen(state = reconnectState()) } }

        composeTestRule.onNodeWithText("Marketplace").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Wheat Field: 6 in stock").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Bakery: 5 in stock").assertIsDisplayed()
    }

    @Test
    fun rendersPlayerLandmarkBuildStateFromSnapshot() {
        composeTestRule.setContent { ClientTheme { GameScreen(state = reconnectState()) } }

        composeTestRule.onNodeWithContentDescription("Train Station: built").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Amusement Park: not built").assertIsDisplayed()
    }

    @Test
    fun rendersLastDiceRollFromSnapshot() {
        composeTestRule.setContent { ClientTheme { GameScreen(state = reconnectState()) } }

        composeTestRule.onNodeWithContentDescription("Würfelergebnis: 8").assertIsDisplayed()
    }
}
