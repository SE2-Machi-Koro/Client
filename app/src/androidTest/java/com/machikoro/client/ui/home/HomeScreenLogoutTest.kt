package com.machikoro.client.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenLogoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsLogoutButton() {
        composeTestRule.setContent {
            ClientTheme {
                HomeScreen(onLogoutClick = {}, onGoToLobbyClick = {})
            }
        }

        composeTestRule.onNodeWithText("Logout").assertIsDisplayed()
    }

    @Test
    fun clickingLogoutInvokesCallback() {
        var clicks = 0
        composeTestRule.setContent {
            ClientTheme {
                HomeScreen(
                    onLogoutClick = { clicks += 1 },
                    onGoToLobbyClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Logout").performClick()

        assertEquals(1, clicks)
    }
}
