package com.machikoro.client.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Rule
import org.junit.Test

/**
 * Verifies the in-app cheat/accusation tutorial section (issue #336). The
 * section is rendered as the final page of the rules viewer, so showing it here
 * is what a player reaches from Home -> Rules -> last page.
 */
class CheatRulesContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent() {
        composeTestRule.setContent {
            ClientTheme {
                CheatRulesContent()
            }
        }
    }

    @Test
    fun showsCheatSectionHeading() {
        setContent()
        composeTestRule.onNodeWithText("Cheating & Accusations").assertIsDisplayed()
        composeTestRule.onNodeWithText("Insider Trading", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Accusing a cheater").assertIsDisplayed()
    }

    @Test
    fun explainsCheatActivation() {
        setContent()
        // Both activation paths: shake gesture and the in-game "Insider tip" button.
        composeTestRule.onNodeWithText("shake your phone", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Insider tip", substring = true).assertIsDisplayed()
    }

    @Test
    fun explainsOneTurnValidityAndServerReporting() {
        setContent()
        composeTestRule.onNodeWithText("current turn only", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("reported to the server", substring = true).assertIsDisplayed()
    }

    @Test
    fun explainsAccusationMechanicLimitAndPenalty() {
        setContent()
        composeTestRule.onNodeWithText("Accuse of cheating", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("once per turn", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("lose", substring = true).assertIsDisplayed()
    }
}
