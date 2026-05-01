package com.machikoro.client.ui.start

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Rule
import org.junit.Test

class LoginDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun submitButtonIsDisabledWhenFieldsAreBlank() {
        composeTestRule.setContent {
            ClientTheme {
                LoginDialog(
                    state = LoginDialogState(),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Login").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun submitButtonIsEnabledWhenBothFieldsFilled() {
        composeTestRule.setContent {
            ClientTheme {
                LoginDialog(
                    state = LoginDialogState(username = "alice", password = "hunter2"),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Login").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun errorMessageIsRenderedWhenPresent() {
        composeTestRule.setContent {
            ClientTheme {
                LoginDialog(
                    state = LoginDialogState(
                        username = "alice",
                        password = "wrong",
                        errorMessage = "Invalid username or password",
                    ),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Invalid username or password").assertIsDisplayed()
    }

    @Test
    fun successStateShowsLoggedInMessageAndCloseButton() {
        composeTestRule.setContent {
            ClientTheme {
                LoginDialog(
                    state = LoginDialogState(loggedInAs = "alice"),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Logged in as alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").assertIsDisplayed().assertIsEnabled()
    }
}
