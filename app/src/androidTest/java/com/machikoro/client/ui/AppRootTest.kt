package com.machikoro.client.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Rule
import org.junit.Test

private const val START_SCREEN_TITLE = "MACHI KORO"

class AppRootTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsStartScreenWhenGamePhaseIsNone() {
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    gameScreenState = GameScreenState.initial(),
                    startScreenState = StartScreenState.placeholder(),
                    registerDialogState = RegisterDialogState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                )
            }
        }

        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(GamePhase.ROLL_DICE.toDisplayText()).assertDoesNotExist()
    }

    @Test
    fun showsGameScreenWhenGamePhaseIsNotNone() {
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    gameScreenState = GameScreenState.initial().copy(gamePhase = GamePhase.ROLL_DICE),
                    startScreenState = StartScreenState.placeholder(),
                    registerDialogState = RegisterDialogState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                )
            }
        }

        composeTestRule.onNodeWithText(GamePhase.ROLL_DICE.toDisplayText()).assertIsDisplayed()
        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertDoesNotExist()
    }

    @Test
    fun swapsScreenWhenGamePhaseTransitions() {
        var phase by mutableStateOf(GamePhase.NONE)
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    gameScreenState = GameScreenState.initial().copy(gamePhase = phase),
                    startScreenState = StartScreenState.placeholder(),
                    registerDialogState = RegisterDialogState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                )
            }
        }

        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertIsDisplayed()

        phase = GamePhase.BUY_OR_BUILD
        composeTestRule.onNodeWithText(GamePhase.BUY_OR_BUILD.toDisplayText()).assertIsDisplayed()
        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertDoesNotExist()

        phase = GamePhase.NONE
        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(GamePhase.BUY_OR_BUILD.toDisplayText()).assertDoesNotExist()
    }
}
