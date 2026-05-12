package com.machikoro.client.ui.start

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Rule
import org.junit.Test

class RegisterDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun submitButtonIsDisabledWhenFieldsAreBlank() {
        composeTestRule.setContent {
            ClientTheme {
                RegisterDialog(
                    state = RegisterDialogState(),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Register").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun submitButtonIsEnabledWhenBothFieldsFilled() {
        composeTestRule.setContent {
            ClientTheme {
                RegisterDialog(
                    state = RegisterDialogState(username = "alice", password = "hunter2"),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Register").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun errorMessageIsRenderedWhenPresent() {
        composeTestRule.setContent {
            ClientTheme {
                RegisterDialog(
                    state = RegisterDialogState(
                        username = "alice",
                        password = "hunter2",
                        errorMessage = "Username 'alice' is already taken",
                    ),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Username 'alice' is already taken").assertIsDisplayed()
    }

    @Test
    fun successStateShowsRegisteredMessageAndCloseButton() {
        composeTestRule.setContent {
            ClientTheme {
                RegisterDialog(
                    state = RegisterDialogState(registeredUsername = "alice"),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Registered as alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").assertIsDisplayed().assertIsEnabled()
    }
}
