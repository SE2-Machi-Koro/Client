package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.ConnectionStatus


@Composable
fun InitializationLoadingOverlay(
    connectionStatus: ConnectionStatus,
    gameStatus: GameStatus?,
    modifier: Modifier = Modifier
) {
    val isInitializing = connectionStatus != ConnectionStatus.CONNECTED || gameStatus != GameStatus.IN_PROGRESS

    if (isInitializing) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .widthIn(min = 200.dp, max = 280.dp)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Initializing Game...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            connectionStatus == ConnectionStatus.CONNECTING -> "Connecting to server..."
                            connectionStatus == ConnectionStatus.DISCONNECTED -> "Reconnecting..."
                            connectionStatus == ConnectionStatus.ERROR -> "Connection error - retrying..."
                            gameStatus == null -> "Loading game state..."
                            else -> "Game loading..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
