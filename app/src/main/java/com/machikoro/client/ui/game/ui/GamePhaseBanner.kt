package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme


// todo: adjust to figma design colors, shape etc
@Composable
fun GamePhaseBanner(
    phase: GamePhase,
    modifier: Modifier = Modifier,
    text: String? = null
) {


    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        GameButton(
            text = text
                ?.takeIf { it.isNotBlank() && phase == GamePhase.ROLL_DICE }
                ?: GamePhase.ROLL_DICE.toDisplayText(),
            isActive = phase == GamePhase.ROLL_DICE
        )

        GameButton(
            text = text
                ?.takeIf { it.isNotBlank() && phase == GamePhase.RESOLVE_EFFECTS }
                ?: GamePhase.RESOLVE_EFFECTS.toDisplayText(),
            isActive = phase == GamePhase.RESOLVE_EFFECTS
        )

        GameButton(
            text = text
                ?.takeIf { it.isNotBlank() && phase == GamePhase.RESOLVE_EFFECTS }
                ?: GamePhase.RESOLVE_EFFECTS.toDisplayText(),
            isActive = phase == GamePhase.RESOLVE_EFFECTS
        )

    }
}
@Composable
fun GameButton(
    text: String,
    isActive: Boolean,
) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Color(0xCCC4D3DC).copy(
                    alpha = if (isActive) 1f else 0.65f
                )
            )
            .padding(
                horizontal = 24.dp,
                vertical = 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF004E7E).copy(
                    alpha = if (isActive) 1f else 0.65f
                )
            )
        )
    }
}
@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun Prev() {
    ClientTheme {
        GamePhaseBanner(GamePhase.RESOLVE_EFFECTS,
            text = "Choose ")    }
}
