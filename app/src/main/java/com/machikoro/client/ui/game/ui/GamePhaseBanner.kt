package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PrimaryBeigeLight
import com.machikoro.client.ui.theme.PrimaryOrangeDark


// todo: adjust to figma design colors, shape etc
@Composable
fun GamePhaseBanner(
    phase: GamePhase,
    modifier: Modifier = Modifier,
    text: String? = null
) {
    // adjusted to show extra hints e.g. "Chose card to buy or skip"
   val textToDisplay = if(text.isNullOrEmpty()) phase.toDisplayText() else text
    Surface(color = PrimaryBeigeLight, modifier = modifier.fillMaxWidth()) {
        Text(
            text = textToDisplay,
            style = MaterialTheme.typography.headlineSmall,
            color = PrimaryOrangeDark,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun Prev() {
    ClientTheme {
GamePhaseBanner(GamePhase.RESOLVE_EFFECTS)    }
}
