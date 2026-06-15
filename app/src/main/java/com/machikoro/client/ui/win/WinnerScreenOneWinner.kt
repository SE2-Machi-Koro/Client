package com.machikoro.client.ui.win

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.AnimatedItem
import com.machikoro.client.ui.shared.AnimationType
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.shared.Header
import com.machikoro.client.ui.shared.SecondaryActionButton
import com.machikoro.client.ui.theme.ClientTheme
import kotlinx.coroutines.delay

private const val PLAYER_CARD_DELAY = 500
private const val PLAYER_VISIBLE_TIME = 1500L

private const val WINNER_DELAY = 500
private const val BUTTONS_DELAY = 4000
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WinnerScreen(
    rankedPlayers: List<Pair<String, Int>>,
    roundsNumber: Int,
    onBackHome: () -> Unit,
    onViewLeaderboard: () -> Unit = {},
) {

    var showAllPlayers by remember {
        mutableStateOf(true)
    }
    var showCrown by remember {
        mutableStateOf(false)
    }

    var showWinnerOnly by remember {
        mutableStateOf(false)
    }

    var showButtons by remember {
        mutableStateOf(false)
    }

    var headerText by remember {
        mutableStateOf("Game Over!")
    }

    LaunchedEffect(Unit) {
        delay(
            ((rankedPlayers.size + 1) * PLAYER_CARD_DELAY).toLong()
        )
        // Show crown on winner
        showCrown = true

        // Wait delay * player count
        delay(
            PLAYER_VISIBLE_TIME *
                    rankedPlayers.size
        )

        // Remove all players
        showAllPlayers = false
        headerText = "Congratulations to..."
        // Show winner screen
        showWinnerOnly = true

        // Show buttons
        delay(BUTTONS_DELAY.toLong())
        showButtons = true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Background(R.drawable.game_end)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            key(headerText) {
                AnimatedItem(
                    delayMillis = 0,
                    animationType = AnimationType.Bounce
                ) {
                    Header(headerText)
                }
            }

            Spacer(
                modifier = Modifier.padding(16.dp)
            )
            if(rankedPlayers.isNotEmpty()) {
            // ALL PLAYERS FIRST
            if (showAllPlayers) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterHorizontally
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    rankedPlayers.forEachIndexed { index, (name, _) ->

                        AnimatedItem(
                            delayMillis = PLAYER_CARD_DELAY + (index * PLAYER_CARD_DELAY),
                            animationType = AnimationType.Bounce
                        ) {
                            Box(
                                contentAlignment = Alignment.TopCenter
                            ) {

                                PlayerProfileCard(name = name,
                                    place = index + 1
                                )

                                if (index == 0 && showCrown) {

                                    AnimatedItem(
                                        delayMillis = 0,
                                        animationType = AnimationType.Bounce
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier.offset(
                                                    y = (-28).dp
                                                )
                                        ) {
                                            Crown()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showWinnerOnly) {
                AnimatedItem(
                    delayMillis = WINNER_DELAY,
                    animationType = AnimationType.Bounce
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            contentAlignment = Alignment.TopCenter
                        ) {
                            val winner = rankedPlayers.firstOrNull()
                            winner?.let { it ->
                                PlayerProfileCard(
                                    name = it.first,
                                    place = 1
                                )
                            }

                            Box(
                                modifier = Modifier.offset(y = (-28).dp)
                            ) {
                                Crown()
                            }
                        }

                        Spacer(
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        BasicText(
                            "Won the game in\n$roundsNumber rounds!"
                        )
                    }
                }
            }
            }
            Spacer(
                modifier = Modifier.weight(1f)
            )

            // BUTTONS
            if (showButtons) {
                AnimatedItem(
                    delayMillis = 0,
                    animationType =
                        AnimationType.SlideUp
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        ActionButton(
                            label =
                                "Back home",
                            onClick =
                                onBackHome
                        )

                        SecondaryActionButton(
                            label =
                                "View Leaderboard",
                            onClick =
                                onViewLeaderboard
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WinnerScreenScreenPreview() {
    ClientTheme {
        WinnerScreen(
            roundsNumber = 5,
            onBackHome = {},
            onViewLeaderboard = {},
            rankedPlayers = listOf(

            )
        )
    }
}