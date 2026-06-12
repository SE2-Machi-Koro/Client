package com.machikoro.client.ui.win

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.machikoro.client.ui.shared.SecondaryActionButton
import com.machikoro.client.ui.theme.ClientTheme

/*
 * Displays the final game-specific leaderboard after a match ends.
 * Players are ranked by placement: rank 1 = winner (built all 4 landmarks),
 * remaining players sorted by number of built landmarks (descending).
 * Uses PlayerProfileCard to stay visually consistent with the winner screen.
 */
@Composable
fun GameLeaderboardScreen(
    rankedPlayers: List<RankedGamePlayer>,
    onBackHome: () -> Unit,
    onViewGlobalLeaderboard: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Background(R.drawable.game_end)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header("Game Results")

            Spacer(modifier = Modifier.padding(17.dp))

            // Player profile cards — reuse PlayerProfileCard (supports placements 1–4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    20.dp,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rankedPlayers.forEach { player ->
                    Box(contentAlignment = Alignment.TopCenter) {
                        AnimatedItem(
                            delayMillis = 300 + 400 * (player.placement - 1),
                            animationType = AnimationType.Bounce,
                        ) {
                            PlayerProfileCard(
                                name = player.displayName,
                                place = player.placement,
                            )
                        }
                        // Crown on the winner card
                        if (player.placement == 1) {
                            AnimatedItem(
                                delayMillis = 300 + 400 * rankedPlayers.size,
                                animationType = AnimationType.Bounce,
                            ) {
                                Box(modifier = Modifier.offset(y = (-28).dp)) {
                                    Crown()
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedItem(
                delayMillis = 500 + 400 * rankedPlayers.size,
                animationType = AnimationType.SlideUp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ActionButton(label = "Back to home screen", onClick = onBackHome)
                    SecondaryActionButton(
                        label = "View Global Leaderboard",
                        onClick = onViewGlobalLeaderboard,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameLeaderboardScreenFourPlayersPreview() {
    ClientTheme {
        GameLeaderboardScreen(
            rankedPlayers = listOf(
                RankedGamePlayer(placement = 1, displayName = "Alice", builtLandmarks = 4),
                RankedGamePlayer(placement = 2, displayName = "Bob", builtLandmarks = 3),
                RankedGamePlayer(placement = 3, displayName = "Charlie", builtLandmarks = 2),
                RankedGamePlayer(placement = 4, displayName = "Diana", builtLandmarks = 1),
            ),
            onBackHome = {},
            onViewGlobalLeaderboard = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameLeaderboardScreenTwoPlayersPreview() {
    ClientTheme {
        GameLeaderboardScreen(
            rankedPlayers = listOf(
                RankedGamePlayer(placement = 1, displayName = "Alice", builtLandmarks = 4),
                RankedGamePlayer(placement = 2, displayName = "Bob", builtLandmarks = 2),
            ),
            onBackHome = {},
            onViewGlobalLeaderboard = {},
        )
    }
}