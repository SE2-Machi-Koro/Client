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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.AnimatedItem
import com.machikoro.client.ui.shared.AnimationType
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.shared.Header
import com.machikoro.client.ui.shared.RegularInfoText
import com.machikoro.client.ui.shared.SecondaryActionButton
import com.machikoro.client.ui.theme.ClientTheme

/*
This file contains the GameOverOneWinner composable,
which displays the end-of-game screen when there is
only one winner. The screen includes a background,
a header, and an animated player profile card for
the winner. It also displays the number of rounds
it took for the winner to win the game. Runner-up
players are shown with a staggered animation after
the winner card appears. Buttons are displayed in a
column to prevent overflow on narrow screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameOverOneWinner(
    winnerName: String,
    roundsNumber: Int,
    onBackHome: () -> Unit,
    onViewLeaderboard: () -> Unit = {},
    rankedPlayers: List<Pair<String, Int>> = emptyList(),
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Background(R.drawable.game_end)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header("Congratulations to...")

            Spacer(modifier = Modifier.padding(17.dp))

            // Winner card with crown and round info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    40.dp,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    AnimatedItem(
                        delayMillis = 500,
                        animationType = AnimationType.Bounce
                    ) {
                        PlayerProfileCard(winnerName, 1)
                    }
                    AnimatedItem(
                        delayMillis = 1000,
                        animationType = AnimationType.Bounce
                    ) {
                        Box(modifier = Modifier.offset(y = (-28).dp)) {
                            Crown()
                        }
                    }
                }

                AnimatedItem(
                    delayMillis = 2000,
                    animationType = AnimationType.Fade
                ) {
                    RegularInfoText("won the game in \n$roundsNumber rounds!")
                }
            }

            // Runner-up players appear with staggered delay after winner card
            if (rankedPlayers.size > 1) {
                Spacer(modifier = Modifier.padding(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        16.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rankedPlayers.drop(1).forEachIndexed { index, (name, _) ->
                        AnimatedItem(
                            delayMillis = 3000 + index * 500,
                            animationType = AnimationType.Bounce
                        ) {
                            PlayerProfileCard(name, index + 2)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedItem(
                delayMillis = 5000,
                animationType = AnimationType.SlideUp
            ) {
                // Buttons in a Column to prevent overflow on narrow screens
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ActionButton("Back to home", onBackHome)
                    SecondaryActionButton("View Leaderboard", onViewLeaderboard)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameOverOnePlayerPreview() {
    ClientTheme {
        GameOverOneWinner(
            winnerName = "Alice",
            roundsNumber = 5,
            onBackHome = {},
            onViewLeaderboard = {},
            rankedPlayers = listOf(
                "Alice" to 4,
                "Bob" to 2,
                "Charlie" to 1,
                "Diana" to 0,
            )
        )
    }
}