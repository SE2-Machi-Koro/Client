package com.machikoro.client.ui.win

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.ui.ActionButton
import com.machikoro.client.ui.AnimatedItem
import com.machikoro.client.ui.AnimationType
import com.machikoro.client.ui.Background
import com.machikoro.client.ui.Header
import com.machikoro.client.ui.RegularInfoText
import com.machikoro.client.ui.theme.ClientTheme

/*
This file contains the GameOverOneWinner composable,
which displays the end-of-game screen when there is
only one winner. The screen includes a background,
a header, and an animated player profile card for
the winner. It also displays the number of rounds
it took for the winner to win the game. Finally,
it includes buttons for navigating back to the home
screen or starting a new game, which are also animated
for visibility.
 */
@Composable
fun GameOverOneWinner(winnerName: String, roundsNumber: Int) {
    Box(modifier = Modifier.fillMaxSize())
    {
      Background(R.drawable.game_end)
        //Content
        Column(
            modifier = Modifier.fillMaxSize().padding(all = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header("Congratulations to...")
            Row(
                modifier = Modifier.fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            )
            {
                //Player card
                AnimatedItem(delayMillis = 500, animationType = AnimationType.Bounce) {
                    PlayerProfileCard(winnerName, 1)
                }
                AnimatedItem(delayMillis = 1000, animationType = AnimationType.Fade) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        //TODO: later add more info here like score, coins, etc
                        RegularInfoText("won the game in \n$roundsNumber rounds!")
                    }
                }
            }

            AnimatedItem(delayMillis = 5000, animationType = AnimationType.SlideUp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton("Back to home screen", null)
                    ActionButton("Play again", null)
                }
            }
        }
    }
}


    @Preview(showBackground = true)
    @Composable
    fun GameOverOnePlayerPreview() {
        ClientTheme {
            // Dummy data for preview
            GameOverOneWinner(winnerName = "Alice", roundsNumber = 5)
        }
    }


