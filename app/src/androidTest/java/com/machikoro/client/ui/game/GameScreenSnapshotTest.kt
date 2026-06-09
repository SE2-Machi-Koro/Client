package com.machikoro.client.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.PurchaseState
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

    /** A mid-game snapshot with synchronized card and landmark ownership for each player. */
    private fun reconnectState() = GameScreenState(
        connectionStatus = ConnectionStatus.CONNECTED,
        gamePhase = GamePhase.BUY_OR_BUILD,
        players = listOf(
            PlayerCoinState(id = "1", displayName = "You", coins = 9, isCurrentPlayer = true),
            PlayerCoinState(id = "2", displayName = "Opponent", coins = 4),
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
            ),
            2 to listOf(
                PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = false),
                PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = true),
                PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
                PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
            )
        ),
        playerCards = mapOf(
            1 to listOf(
                PlayerCardState(CardType.WHEAT_FIELD, quantity = 1),
                PlayerCardState(CardType.BAKERY, quantity = 2),
            ),
            2 to listOf(
                PlayerCardState(CardType.CAFE, quantity = 2),
                PlayerCardState(CardType.FOREST, quantity = 1),
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
    fun doesNotRenderOwnedPlayerCardsInTopBar() {
        composeTestRule.setContent { ClientTheme { GameScreen(state = reconnectState()) } }

        composeTestRule.onNodeWithContentDescription("Owned by You: Wheat Field, 1 card").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Owned by You: Bakery, 2 cards").assertDoesNotExist()
    }

    @Test
    fun doesNotReintroduceTopBarCardsWhenSnapshotChanges() {
        var state by mutableStateOf(reconnectState())
        composeTestRule.setContent { ClientTheme { GameScreen(state = state) } }

        composeTestRule.runOnIdle {
            state = state.copy(
                playerCards = mapOf(
                    1 to listOf(
                        PlayerCardState(CardType.WHEAT_FIELD, quantity = 3),
                        PlayerCardState(CardType.CAFE, quantity = 1),
                    )
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("Owned by You: Wheat Field, 3 cards").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Owned by You: Cafe, 1 card").assertDoesNotExist()
    }

    @Test
    fun opensPlayerCardWindowFromOpponentCoinBadgeOnly() {
        composeTestRule.setContent { ClientTheme { GameScreen(state = reconnectState()) } }

        composeTestRule.onNodeWithContentDescription("Inspect Opponent").performScrollTo().performClick()

        composeTestRule.onNodeWithContentDescription("Player cards window for Opponent").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Opponent owns Cafe, quantity 2").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Opponent landmark Shopping Mall: built").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Inspect You in player cards window").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("You owns Bakery, quantity 2").assertDoesNotExist()
    }

    @Test
    fun doesNotOpenPlayerCardWindowForCurrentPlayer() {
        composeTestRule.setContent { ClientTheme { GameScreen(state = reconnectState()) } }

        composeTestRule.onNodeWithContentDescription("Inspect You").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Player cards window for You").assertDoesNotExist()
    }

    @Test
    fun updatesOpenPlayerCardWindowWhenSnapshotChanges() {
        var state by mutableStateOf(reconnectState())
        composeTestRule.setContent { ClientTheme { GameScreen(state = state) } }

        composeTestRule.onNodeWithContentDescription("Inspect Opponent").performScrollTo().performClick()

        composeTestRule.runOnIdle {
            state = state.copy(
                playerCards = state.playerCards + (
                    2 to listOf(
                        PlayerCardState(CardType.CAFE, quantity = 3),
                        PlayerCardState(CardType.TV_STATION, quantity = 1),
                    )
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("Opponent owns Cafe, quantity 3").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Opponent owns TV Station, quantity 1").assertIsDisplayed()
    }

    @Test
    fun rendersLastDiceRollFromSnapshot() {
        composeTestRule.setContent { ClientTheme { GameScreen(state = reconnectState()) } }

        composeTestRule.onNodeWithContentDescription("Würfelergebnis: 8").assertIsDisplayed()
    }
}
