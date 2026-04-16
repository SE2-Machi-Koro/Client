package com.machikoro.client.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.model.state.ConnectionStatus
import com.machikoro.client.model.state.LobbyStatus
import com.machikoro.client.model.state.StartScreenState
import com.machikoro.client.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun StartScreen(
    state: StartScreenState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary // test
        )
        Text(
            text = "Connection status: ${state.connectionStatus.toDisplayText()}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Lobby/start: ${state.lobbyStatus.toDisplayText()}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary // test
        )
    }
}

private fun LobbyStatus.toDisplayText(): String = when (this) {
    LobbyStatus.PLACEHOLDER -> "placeholder"
    LobbyStatus.WAITING_FOR_PLAYERS -> "waiting for players"
    LobbyStatus.READY -> "ready"
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() {
    ClientTheme {
        StartScreen(
            state = StartScreenState.placeholder().copy(
                connectionStatus = ConnectionStatus.CONNECTED
            )
        )
    }
}
