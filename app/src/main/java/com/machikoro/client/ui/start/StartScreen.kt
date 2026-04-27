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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layoutId
import com.machikoro.client.R
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.LobbyStatus
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun StartScreen(
    state: StartScreenState,
    modifier: Modifier = Modifier,
    onStartGame: () -> Unit
) {
    val showPdfViewer = remember { mutableStateOf(false) }

    if (showPdfViewer.value) {
        PdfViewerScreen(
            onClose = { showPdfViewer.value = false }
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            BackgroundImages()
            TitleHeader()
            RulesButton(onClick = { showPdfViewer.value = true })
            LobbyControls(state = state, onStartGame = onStartGame)
        }
    }
}

@Composable
private fun BackgroundImages() {
    Image(
        painter = painterResource(id = R.drawable.background_left),
        contentDescription = null,
        modifier = Modifier.layoutId("backgroundLeft").offset(x = -20.dp, y = 30.dp)
    )
    Image(
        painter = painterResource(id = R.drawable.background_right),
        contentDescription = null,
        modifier = Modifier.layoutId("backgroundRight").offset(x = 15.dp, y = 30.dp)
    )
}

@Composable
private fun TitleHeader() {
    Text(
        text = "MACHI KORO",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.layoutId("titleHeader").padding(top = 55.dp)
    )
}

@Composable
private fun RulesButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.layoutId("rulesButton").padding(top = 16.dp, end = 16.dp),
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
}

@Composable
private fun LobbyControls(state: StartScreenState, onStartGame: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Player count display
        Text(
            text = "${state.playerList.size}/${state.maxPlayers} ready",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Connection status: ${state.connectionStatus}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Lobby/start: ${state.lobbyStatus.toDisplayText()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (state.isHost) {
            HostStartGameButton(enabled = state.playerList.size >= 2, onStartGame = onStartGame)
        }
    }
}

@Composable
private fun HostStartGameButton(enabled: Boolean, onStartGame: () -> Unit) {
    Button(
        onClick = onStartGame,
        enabled = enabled,
        modifier = Modifier.padding(top = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
        )
    ) {
        Text(
            text = "Start Game",
            color = if (enabled) Color.White else Color.LightGray,
            style = MaterialTheme.typography.labelLarge
        )
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
            ),
            onStartGame = {}
        )
    }
}
