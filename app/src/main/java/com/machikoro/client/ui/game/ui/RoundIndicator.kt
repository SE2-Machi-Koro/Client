package com.machikoro.client.ui.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun RoundIndicator(
    round: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Round $round",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = Color.White,
        modifier = modifier
    )
}