package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.model.state.GameScreenState

@Composable
fun PlayerCoinField(
    state: GameScreenState,
    modifier: Modifier = Modifier
) {
    val coins = state
        .players.
        first { it.isCurrentPlayer }
        .coins
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$coins",
            color = Color.White,
            fontSize = 40.sp,
        )
        Image(
            painter = painterResource(id = R.drawable.player_coins),
            contentDescription = "Coins",
            modifier = Modifier.size(120.dp)
                .offset(y = -20.dp)
        )


    }
}
