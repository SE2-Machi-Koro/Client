package com.machikoro.client.ui.lobby

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.ui.theme.ButtonBlueDark
import com.machikoro.client.ui.theme.ButtonBlueGrey
import com.machikoro.client.ui.theme.ButtonBlueLight
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.TextBlueDark
import com.machikoro.client.ui.theme.TextWhite
import com.machikoro.client.ui.theme.White

@Composable
fun LobbyScreen(
    state: LobbyScreenState,
    lobbyCode: String?,
    onReadyToggle: () -> Unit = {},
    onStartGame: () -> Unit = {},
    onLeaveLobby: () -> Unit = {},
    onFillWithDummies: () -> Unit = {},
    onResetLobby: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    LobbyScreen(
        playerNames = state.playerList,
        maxPlayers = state.maxPlayers,
        currentUsername = state.loggedInAs,
        // TODO: Replace first-player-as-host fallback once backend exposes host information.
        hostUsername = state.playerList.firstOrNull(),
        isHost = state.isHost,
        isReady = state.isReady,
        lobbyCode = lobbyCode,
        onReadyToggle = onReadyToggle,
        onStartGame = onStartGame,
        onLeaveLobby = onLeaveLobby,
        onFillWithDummies = onFillWithDummies,
        onResetLobby = onResetLobby,
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
    lobbyCode: String? = null,
    onReadyToggle: () -> Unit = {},
    onStartGame: () -> Unit = {},
    onLeaveLobby: () -> Unit = {},
    onFillWithDummies: () -> Unit = {},
    onResetLobby: () -> Unit = {},
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
                    text = "Players",
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
                            isCurrentUser -> "$name (you)"
                            else -> name
                        },
                        isHost = isHostPlayer
                    )

                    // TODO: Replace placeholder ready state once backend exposes readiness per player.
                    val statusText = when {
                        name == null -> ""
                        isCurrentUser && !isReady -> "not ready"
                        else -> "ready"
                    }

                    StatusSlot(text = statusText)
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
                Text("Start Game", color = TextWhite, style =  MaterialTheme.typography.labelLarge)
            }

            // Debug helper: fill remaining slots so the host can start without real players
            if (isHost && playerNames.size < maxPlayers) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onFillWithDummies,
                    modifier = Modifier
                        .width(320.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonBlueGrey)
                ) {
                    Text("[Debug] Fill with dummies", color = TextBlueDark, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Debug helper: remove all non-host players so the host can start fresh
            if (isHost && playerNames.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onResetLobby,
                    modifier = Modifier
                        .width(320.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonBlueGrey)
                ) {
                    Text("[Debug] Reset lobby", color = TextBlueDark, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        ReadyToggle(
            isReady = isReady,
            onClick = onReadyToggle,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 140.dp, top = 40.dp)
        )

        lobbyCode?.let { code ->
            LobbyCodeCopyRow(
                code = code,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 180.dp, start = 95.dp)
            )
        }

        LeaveLobbyButton(
            onClick = onLeaveLobby,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 25.dp, start = 30.dp)
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
            text = "ready",
            color = White,
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
            text = "not\nready",
            color = White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LobbyCodeCopyRow(
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun copyLobbyCodeToClipboard() {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText("Lobby Code", code)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Lobby code copied", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier
                .width(140.dp)
                .height(40.dp)
                .clickable { copyLobbyCodeToClipboard() },
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0x4D004E7E),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.weight(1f))

                Image(
                    painter = painterResource(id = R.drawable.home_copy_icon),
                    contentDescription = "Copy lobby code",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
@Composable
private fun LeaveLobbyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(105.dp)
            .height(42.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ButtonBlueDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.lobby_leave),
                contentDescription = "Lobby verlassen",
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Leave Lobby",
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 917, heightDp = 412)
@Composable
private fun LobbyScreenPreview() {
    ClientTheme {
        LobbyScreen(
            playerNames = listOf("Player1"),
            currentUsername = "Player1",
            hostUsername = "Player",
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
            playerNames = listOf("Player1", "Player2", "Player3", "Player4"),
            currentUsername = "Player1",
            hostUsername = "Player1",
            isHost = true,
            isReady = true
        )
    }
}
