package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun ResolvingEffectsView(
    state: GameScreenState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        state.diceResult?.let {
            DiceResultDisplay(dice = it)
        }

        BasicText("Resolving effects...")

        BasicText("Triggered establishments")
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsViewPreview() {
    ClientTheme {
        ResolvingEffectsView(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.RESOLVE_EFFECTS,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = listOf(
                    PlayerCoinState(
                        id = "1",
                        displayName = "You",
                        coins = 6,
                        isCurrentPlayer = true,
                        isActivePlayer = true
                    )
                ),
                diceResult = listOf(2),
                purchaseState = PurchaseState.IDLE
            ),
            modifier = Modifier
                .background(Color(0xFF5A321E))
                .padding(24.dp)
        )
    }
}