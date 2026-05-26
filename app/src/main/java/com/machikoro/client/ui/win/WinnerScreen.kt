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
import com.machikoro.client.ui.theme.ClientTheme

/*
This file contains the WinScreen composable, which displays the end-of-game
screen with the winners' profiles and action buttons. The screen includes
a background, a header, and animated visibility for the player profile cards.
The WinScreen composable takes a list of player names as input and displays
them in a row with animations. It also includes buttons for navigating back
to the home screen or starting a new game.
 */
// TODO: replace list of strings with actual player data model
@Composable
fun GameEndScreen(players: List<String>) {
    if(players.size !in 2..4) {
        return
    }
    Box(modifier = Modifier.fillMaxSize())
    {
        Background(R.drawable.game_end)

        //Content
        Column(
            modifier = Modifier
                .fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Header("Game Over")

            Spacer(modifier = Modifier.padding(17.dp))

            //Player cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    20.dp,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                players.forEachIndexed { i, player ->
                    Box(
                        contentAlignment = Alignment.TopCenter
                    ) {

                        AnimatedItem(500 + 500 * i, AnimationType.Bounce) {
                            PlayerProfileCard(player, i + 1)
                        }
                        if(i == 0) {
                            AnimatedItem(
                                delayMillis = 500 + 500 * players.size, // show crown after all cards are shown
                                animationType = AnimationType.Bounce
                            ) {
                                Box(
                                    modifier = Modifier.offset(y = (-28).dp)
                                ) {
                                    Crown()
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            //Action buttons
            AnimatedItem(delayMillis = 1000 + players.size * 1000, animationType = AnimationType.SlideUp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton("Back to home screen", null)
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun WinScreenPreview() {
    ClientTheme {
        // Dummy data for preview TODO: replace with actual data
        GameEndScreen(listOf<String>( "HsDHDHsssaaassaasasasassaDH", "Name 4"))
    }
}
