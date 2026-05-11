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

@Composable
fun GameOverOneWinner(winnerName: String, roundsNumber: Int) {
    Box(modifier = Modifier.fillMaxSize())
    {
      Background(R.drawable.frame)
        //Content
        Column(
            modifier = Modifier.fillMaxSize().padding(all = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header("Congratulations to...")
            //Player cards
            Row(
                modifier = Modifier.fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            )
            {
                AnimatedItem(delayMillis = 500, animationType = AnimationType.Bounce) {
                    PlayerProfileCard(winnerName, 1)
                }
                AnimatedItem(delayMillis = 1000, animationType = AnimationType.Fade) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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


