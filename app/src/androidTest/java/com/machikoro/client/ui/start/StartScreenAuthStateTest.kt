package com.machikoro.client.ui.start

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Rule
import org.junit.Test

class StartScreenAuthStateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unauthenticatedShowsRegisterAndLoginAndHidesLoggedInBanner() {
        composeTestRule.setContent {
            ClientTheme {
                StartScreen(
                    state = StartScreenState.placeholder(),
                    registerDialogState = RegisterDialogState(),
                    loginDialogState = LoginDialogState(),
                    logoutState = LogoutState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                    onLoginUsernameChange = {},
                    onLoginPasswordChange = {},
                    onLoginSubmit = {},
                    onLoginDialogReset = {},
                    onLogoutSubmit = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Register").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
        composeTestRule.onNodeWithText("Logout").assertDoesNotExist()
        composeTestRule.onNodeWithText("Logged in as alice").assertDoesNotExist()
    }

    @Test
    fun authenticatedShowsLoggedInBannerAndLogoutAndHidesRegisterAndLogin() {
        composeTestRule.setContent {
            ClientTheme {
                StartScreen(
                    state = StartScreenState.placeholder().copy(loggedInAs = "alice"),
                    registerDialogState = RegisterDialogState(),
                    loginDialogState = LoginDialogState(),
                    logoutState = LogoutState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                    onLoginUsernameChange = {},
                    onLoginPasswordChange = {},
                    onLoginSubmit = {},
                    onLoginDialogReset = {},
                    onLogoutSubmit = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Logged in as alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Logout").assertIsDisplayed()
        composeTestRule.onNodeWithText("Register").assertDoesNotExist()
        composeTestRule.onNodeWithText("Login").assertDoesNotExist()
    }
}
