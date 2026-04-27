package com.machikoro.client.ui.start
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.LobbyStatus
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.R

@Composable
private fun PlayerCountDisplay(playerCount: Int, maxPlayers: Int) {
    Text(
        text = "$playerCount/$maxPlayers ready",
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun StartGameButton(isEnabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.padding(top = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
        )
    ) {
        Text(
            text = "Start Game",
            color = if (isEnabled) Color.White else Color.LightGray,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun LobbyInfoColumn(state: StartScreenState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PlayerCountDisplay(state.playerList.size, state.maxPlayers)
        Text(
            text = "Connection status: ${state.connectionStatus.toDisplayText()}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Lobby/start: ${state.lobbyStatus.toDisplayText()}",
            style = MaterialTheme.typography.bodyMedium, // test
            color = MaterialTheme.colorScheme.primary // test
        )
        // Host-only Start Game button
        if (state.isHost) {
            val enabled = state.playerList.size >= 2
            StartGameButton(
                isEnabled = enabled,
                onClick = { /* Implement start game logic here or trigger ViewModel event */ }
            )
        }
    }
}

@Composable
fun StartScreen(
    state: StartScreenState,
    modifier: Modifier = Modifier
) {
    val showPdfViewer = remember { mutableStateOf(false) }

    if (showPdfViewer.value) {
        PdfViewerScreen(
            onClose = { showPdfViewer.value = false }
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.background_left),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomStart).offset(x = -20.dp, y = 30.dp) // optional
            )

            Image(
                painter = painterResource(id = R.drawable.background_right),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 15.dp, y = 30.dp) // optional
            )

            Text(
                text = "MACHI KORO",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 55.dp)
            )

            Button(
                onClick = { showPdfViewer.value = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF64B5F6)
                )
            ) {
                Text(
                    text = "Rules",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            LobbyInfoColumn(state)
        }
    }
}

private fun LobbyStatus.toDisplayText(): String = when (this) {
    LobbyStatus.PLACEHOLDER -> "placeholder"
    LobbyStatus.WAITING_FOR_PLAYERS -> "waiting for players"
    LobbyStatus.READY -> "ready"
}

@Preview(
    showBackground = true,
    widthDp = 917,
    heightDp = 412
)
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
