package com.machikoro.client.ui.lobby

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.ui.theme.*
import androidx.compose.ui.text.style.TextAlign
import com.machikoro.client.domain.model.state.LobbyScreenState

@Composable
fun LobbyScreen(
    state: LobbyScreenState,
    isReady: Boolean = false,
    onReadyToggle: () -> Unit = {},
    onStartGame: () -> Unit = {},
    onLeaveLobby: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LobbyScreen(
        playerNames = state.playerList,
        maxPlayers = state.maxPlayers,
        currentUsername = state.loggedInAs,
        hostUsername = state.playerList.firstOrNull(), // temporary: first player = host
        isHost = state.isHost,
        isReady = state.isReady,
        onReadyToggle = onReadyToggle,
        onStartGame = onStartGame,
        onLeaveLobby = onLeaveLobby,
        modifier = modifier
    )
}

@Composable
fun LobbyScreen(
    playerNames: List<String>,
    maxPlayers: Int = 4,
    currentUsername: String? = null,
    hostUsername: String? = null,
    isHost: Boolean = false,
    isReady: Boolean = false,
    onReadyToggle: () -> Unit = {},
    onStartGame: () -> Unit = {},
    onLeaveLobby: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val startEnabled = isHost && playerNames.size >= 2

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(White)
    ) {
        // === BACKGROUND LAYER ===
        // Decorative background image on the bottom left.
        Image(
            painter = painterResource(id = R.drawable.background_left),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = -15.dp, y = 25.dp)
                .blur(3.5.dp)
        )

        // Decorative background image on the bottom right.
        Image(
            painter = painterResource(id = R.drawable.background_right),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 15.dp, y = 25.dp)
                .blur(3.5.dp)
        )

        // White transparent overlay for better readability.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.7f))
        )

        // === HEADER ===
        // Main title.
        Text(
            text = "Lobby",
            style = MaterialTheme.typography.headlineMedium,
            color = TextBlueDark,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = "Spielerliste",
                    modifier = Modifier.width(220.dp),
                    color = TextBlueDark,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Status",
                    modifier = Modifier.width(90.dp),
                    color = TextBlueDark,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            repeat(maxPlayers) { index ->
                val name = playerNames.getOrNull(index)
                val isCurrentUser = name != null && name == currentUsername
                val isHostPlayer = name != null && name == hostUsername

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    PlayerSlot(
                        name = when {
                            name == null -> ""
                            isCurrentUser -> "$name (ich)"
                            else -> name
                        },
                        isHost = isHostPlayer
                    )

                    StatusSlot(
                        text = if (name == null) "" else if (isCurrentUser && !isReady) "nicht bereit" else "bereit"
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onStartGame,
                enabled = startEnabled,
                modifier = Modifier
                    .width(320.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonBlueDark,
                    disabledContainerColor = ButtonBlueDark.copy(alpha = 0.65f)
                )
            ) {
                Text("Spiel starten", color = TextWhite, style =  MaterialTheme.typography.labelLarge)
            }
        }

        ReadyToggle(
            isReady = isReady,
            onClick = onReadyToggle,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 140.dp, top = 40.dp)
        )

        LeaveLobbyButton(
            onClick = onLeaveLobby,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 65.dp, bottom = 38.dp)
        )
    }
}
@Composable
private fun PlayerSlot(name: String, isHost: Boolean) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(36.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ButtonBlueLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = TextBlueDark,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            if (isHost) {
                Spacer(modifier = Modifier.width(4.dp))

                Image(
                    painter = painterResource(id = R.drawable.lobby_host_icon),
                    contentDescription = "Host",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusSlot(text: String) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(36.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ButtonBlueGrey),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = text, color = TextBlueDark, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ReadyToggle(
    isReady: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(90.dp)
            .background(ButtonBlueDark, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "bereit",
            color = TextWhite,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // === SWITCH CONTAINER ===
        Box(
            modifier = Modifier
                .width(37.dp)
                .height(60.dp)
                .background(Color.LightGray, RoundedCornerShape(25.dp)),
            contentAlignment = if (isReady) Alignment.TopCenter else Alignment.BottomCenter
        ) {
            // === ICON BUTTON ===
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(30.dp)
                    .background(Color.White, shape = RoundedCornerShape(50))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = if (isReady)
                            R.drawable.lobby_ready_status_icon
                        else
                            R.drawable.lobby_not_ready_status_icon
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(45.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "nicht\nbereit",
            color = TextWhite,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LeaveLobbyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(95.dp)
            .height(95.dp)
            .background(
                color = ButtonBlueGrey,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.lobby_leave),
            contentDescription = "Lobby verlassen",
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Lobby\nverlassen",
            modifier = Modifier.fillMaxWidth(),
            color = TextBlueDark,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, widthDp = 917, heightDp = 412)
@Composable
private fun LobbyScreenPreview() {
    ClientTheme {
        LobbyScreen(
            playerNames = listOf("Spieler1"),
            currentUsername = "Spieler1",
            hostUsername = "Spieler1",
            isHost = true,
            isReady = false
        )
    }
}

@Preview(showBackground = true, widthDp = 917, heightDp = 412)
@Composable
private fun LobbyScreenFullPreview() {
    ClientTheme {
        LobbyScreen(
            playerNames = listOf("Spieler1", "Spieler2", "Spieler3", "Spieler4"),
            currentUsername = "Spieler1",
            hostUsername = "Spieler1",
            isHost = true,
            isReady = true
        )
    }
}