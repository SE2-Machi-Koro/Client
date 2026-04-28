package com.machikoro.client.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme

private const val BANNER_COLOR_ANIMATION_DURATION_MS = 300

@Composable
fun GameScreen(
    state: GameScreenState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.gamePhase != GamePhase.NONE) {
            GamePhaseBanner(
                phase = state.gamePhase,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun GamePhaseBanner(
    phase: GamePhase,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = phase.toBannerColor(),
        animationSpec = tween(durationMillis = BANNER_COLOR_ANIMATION_DURATION_MS),
        label = "GamePhaseBannerColor"
    )
    Surface(
        color = animatedColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = phase.toDisplayText(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun GamePhase.toBannerColor(): Color = when (this) {
    GamePhase.NONE -> Color.Transparent
    GamePhase.ROLL_DICE -> MaterialTheme.colorScheme.primary
    GamePhase.RESOLVE_EFFECTS -> MaterialTheme.colorScheme.secondary
    GamePhase.BUY_OR_BUILD -> MaterialTheme.colorScheme.tertiary
    GamePhase.END_TURN -> MaterialTheme.colorScheme.error
}

@Preview(showBackground = true, widthDp = 412, heightDp = 200)
@Composable
private fun GameScreenRollDicePreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gamePhase = GamePhase.ROLL_DICE,
                connectionStatus = ConnectionStatus.CONNECTED
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 200)
@Composable
private fun GameScreenBuyOrBuildPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gamePhase = GamePhase.BUY_OR_BUILD,
                connectionStatus = ConnectionStatus.CONNECTED
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 200)
@Composable
private fun GameScreenNonePreview() {
    ClientTheme {
        GameScreen(state = GameScreenState.initial())
    }
}
