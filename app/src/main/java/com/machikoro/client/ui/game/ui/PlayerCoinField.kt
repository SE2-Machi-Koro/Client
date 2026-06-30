package com.machikoro.client.ui.game.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.ui.game.ui.coin_animation.CoinChangeHighlight
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun PlayerCoinField(
    state: GameScreenState,
    highlight: CoinChangeHighlight = CoinChangeHighlight.NONE,
    onCoinPositioned: (playerId: Int, center: Offset) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val currentPlayer = state.players.firstOrNull { it.isCurrentPlayer }
    val coins = currentPlayer?.coins ?: 0
    val amountColor = animateColorAsState(
        targetValue = when (highlight) {
            CoinChangeHighlight.NONE -> Color.White
            CoinChangeHighlight.GAIN -> Color(0xFF63C174)
            CoinChangeHighlight.LOSS -> Color(0xFFFF5573)
        },
        label = "current player coin highlight",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)    ) {
        Text(
            text = "$coins",
            color = amountColor.value,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
        )
        Image(
            painter = painterResource(id = R.drawable.game_coins_decor),
            contentDescription = "Coins",
            modifier = Modifier
                .size(75.dp)
                .onGloballyPositioned { coordinates ->
                    currentPlayer?.id?.toIntOrNull()?.let { playerId ->
                        onCoinPositioned(
                            playerId,
                            coordinates.boundsInRoot().center,
                        )
                    }
                }
        )
    }
}
