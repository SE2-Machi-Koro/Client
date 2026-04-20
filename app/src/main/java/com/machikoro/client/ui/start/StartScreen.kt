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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.model.state.ConnectionStatus
import com.machikoro.client.model.state.LobbyStatus
import com.machikoro.client.model.state.StartScreenState
import com.machikoro.client.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.R

@Composable
fun StartScreen(
    state: StartScreenState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
            // ...existing code...
            Image(
                painter = painterResource(id = R.drawable.background_left),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomStart).offset(x = -20.dp, y = 30.dp) // optional
            )

            // ...existing code...
            Image(
                painter = painterResource(id = R.drawable.background_right),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 15.dp, y = 30.dp) // optional
            )

            // ...existing code...
            Text(
                text = "MACHI KORO",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 55.dp)
            )

            // ...existing code...
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

            // ...existing code...
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Connection status: ${state.connectionStatus.toDisplayText()}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Lobby/start: ${state.lobbyStatus.toDisplayText()}",
                    style = MaterialTheme.typography.bodyMedium, // test
                    color = MaterialTheme.colorScheme.primary // test
                )
            }
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
