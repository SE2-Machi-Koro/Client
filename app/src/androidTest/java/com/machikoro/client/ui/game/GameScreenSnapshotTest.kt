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
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.domain.model.state.toDisplayText
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
        gameId = 1,
        purchaseState = PurchaseState.IDLE,
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

    @Test
    fun rendersPlayerNamesAndCoinCountsCorrectly() {
        val state = GameScreenState(
            gameId = 1,
            connectionStatus = ConnectionStatus.CONNECTED,
            gamePhase = GamePhase.ROLL_DICE,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 4, isCurrentPlayer = true, isActivePlayer = true),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 3),
            ),
            activePlayerId = 1,
            myUserId = 1,
            gameStatus = GameStatus.IN_PROGRESS,
            purchaseState = PurchaseState.IDLE,
        )

        composeTestRule.setContent { ClientTheme { GameScreen(state = state) } }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 coins").assertIsDisplayed()  // alice's coins
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun rendersAllFourLandmarksUnbuiltOnGameStart() {
        val state = GameScreenState(
            gameId = 1,
            connectionStatus = ConnectionStatus.CONNECTED,
            gamePhase = GamePhase.BUY_OR_BUILD,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "You", coins = 4, isCurrentPlayer = true, isActivePlayer = true)
            ),
            playerLandmarks = mapOf(
                1 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) }
            ),
            activePlayerId = 1,
            myUserId = 1,
            gameStatus = GameStatus.IN_PROGRESS,
            purchaseState = PurchaseState.IDLE,
        )

        composeTestRule.setContent { ClientTheme { GameScreen(state = state) } }

        LandmarkType.entries.forEach { type ->
            composeTestRule.onNodeWithContentDescription("${type.toDisplayText()}: not built").assertIsDisplayed()
        }
    }

    @Test
    fun rendersBuiltAndUnbuiltLandmarksCorrectly() {
        val state = GameScreenState(
            connectionStatus = ConnectionStatus.CONNECTED,
            gamePhase = GamePhase.BUY_OR_BUILD,
            players = listOf(
                // single player to keep assertions unambiguous
                PlayerCoinState(id = "1", displayName = "You", coins = 5, isCurrentPlayer = true)
            ),
            activePlayerId = 1,
            myUserId = 1,
            gameStatus = GameStatus.IN_PROGRESS,
            playerLandmarks = mapOf(
                1 to listOf(
                    // mix of built and unbuilt landmarks
                    PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
                )
            ),
            purchaseState = PurchaseState.IDLE,
            gameId = 1
        )

        composeTestRule.setContent { ClientTheme { GameScreen(state = state) } }

        // Positive assertions: built landmarks
        composeTestRule.onNodeWithContentDescription("Train Station: built").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Amusement Park: built").assertIsDisplayed()

        // Positive assertions: unbuilt landmarks
        composeTestRule.onNodeWithContentDescription("Shopping Mall: not built").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Radio Tower: not built").assertIsDisplayed()

        // Optional negatives: ensure no incorrect labels
        composeTestRule.onNodeWithContentDescription("Train Station: not built").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Shopping Mall: built").assertDoesNotExist()
    }

    @Test
    fun marketplaceDisplaysCorrectCardCountsAfterSync() {
        val state = GameScreenState(
            gameId = 1,
            connectionStatus = ConnectionStatus.CONNECTED,
            gamePhase = GamePhase.BUY_OR_BUILD,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "You", coins = 4, isCurrentPlayer = true)
            ),
            marketplace = mapOf(
                CardType.WHEAT_FIELD to 6,
                CardType.BAKERY to 5,
                CardType.CAFE to 4,
            ),
            activePlayerId = 1,
            myUserId = 1,
            gameStatus = GameStatus.IN_PROGRESS,
            purchaseState = PurchaseState.IDLE,
        )

        composeTestRule.setContent { ClientTheme { GameScreen(state = state) } }

        composeTestRule.onNodeWithContentDescription("Wheat Field: 6 in stock").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Bakery: 5 in stock").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Cafe: 4 in stock").assertIsDisplayed()
    }
}
