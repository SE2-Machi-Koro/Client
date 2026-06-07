package com.machikoro.client.ui.lobby

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.ui.shared.ArrowTextButton
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.ui.theme.ButtonBeigeLight
import com.machikoro.client.ui.theme.ButtonBlueDark
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.TextBlueDark
import com.machikoro.client.ui.theme.TextWhite
import com.machikoro.client.ui.theme.White
import com.machikoro.client.ui.theme.PanelBackgroundBeige
import com.machikoro.client.ui.theme.PanelBorder
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.shared.Header
import com.machikoro.client.ui.theme.ButtonBorderBeige
import com.machikoro.client.ui.theme.ButtonBorderBlue
import com.machikoro.client.ui.theme.ButtonBorderOrange
import com.machikoro.client.ui.theme.ButtonOrange
import com.machikoro.client.ui.theme.ButtonShadowColor
import com.machikoro.client.ui.theme.PrimaryOrange
import com.machikoro.client.ui.theme.TextBlueLight
import com.machikoro.client.ui.theme.TextOnOrange

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
        players = state.playerList,
        maxPlayers = state.maxPlayers,
        currentUsername = state.loggedInAs,
        // First player in roster has lowest turnOrder, which is the host
        hostUsername = state.playerList.firstOrNull()?.displayName,
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
    modifier: Modifier = Modifier,
    playerNames: List<String>,
    players: List<PlayerCoinState>,
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

    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    // All players must be ready, not just the host
    val startEnabled = isHost && players.size >= 2 && players.all { it.isReady }
    // True when any dummy player (filled via debug) is present in the roster
    val hasDummies = players.any { it.displayName.startsWith("debug_player") }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Background(R.drawable.background_wood)// === BACKGROUND LAYER ===

        // === HEADER ===
        // Main title.
        Header("Lobby",
            modifier = Modifier
                .padding(top = 23.dp)
                .align(Alignment.TopCenter),
            fontSize = 52
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LobbyPanel(
                playerNames = players.map { it.displayName },
                maxPlayers = maxPlayers,
                currentUsername = currentUsername,
                hostUsername = hostUsername,
                isReady = isReady
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isHost) {
                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (playerNames.size < maxPlayers) {
                        Box(
                            modifier = modifier
                                .width(170.dp)
                                .height(55.dp)
                                .background(
                                    color = ButtonBorderBlue,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(bottom = 4.dp)
                        )
                        {
                            Button(
                                onClick = onFillWithDummies,
                                modifier = Modifier
                                    .width(170.dp)
                                    .height(55.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ButtonBlueDark)
                            ) {
                                Text("Fill Dummies",
                                    color = TextWhite,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth())
                            }
                        }
                    } else if (playerNames.size > 1) {
                        Box(
                            modifier = modifier
                                .width(170.dp)
                                .height(55.dp)
                                .background(
                                    color = ButtonBorderBlue,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(bottom = 4.dp)
                        )
                        {
                            Button(
                                onClick = onResetLobby,
                                modifier = Modifier
                                    .width(170.dp)
                                    .height(55.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ButtonBlueDark)
                            ) {
                                Text("Reset Lobby",
                                    color = TextWhite,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Box(
                        modifier = modifier
                            .width(170.dp)
                            .height(55.dp)
                            .background(
                                color = ButtonBorderOrange,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(bottom = 4.dp)
                    )
                    {
                        Button(
                            onClick = onStartGame,
                            enabled = startEnabled,
                            modifier = Modifier
                                .width(170.dp)
                                .height(55.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonOrange,
                                disabledContainerColor = ButtonOrange.copy(alpha = 0.65f)
                            )
                        ) {
                            Text(
                                text = "Start Game",
                                color = TextOnOrange,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        ReadyToggle(
            isReady = isReady,
            onClick = onReadyToggle,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 140.dp, bottom = 12.dp)
        )

        lobbyCode?.let { code ->
            LobbyCodeCopyRow(
                code = code,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 180.dp, start = 85.dp)
            )
        }

        ArrowTextButton(
            label = "Leave Lobby",
            onClick = onLeaveLobby,
            modifier = Modifier.offset(x = 30.dp, y = 28.dp),
            fontSize = 18.sp
        )
    }
}

@Composable
private fun LobbyPanel(
    playerNames: List<String>,
    maxPlayers: Int,
    currentUsername: String?,
    hostUsername: String?,
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(358.dp)
            .background(
                color = PanelBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(bottom = 4.dp, start = 4.dp)
    )
    {
        Card(
            modifier = Modifier.width(350.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackgroundBeige),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    Text(
                        text = "Players",
                        modifier = Modifier.width(200.dp).padding(top = 5.dp, start = 45.dp),
                        color = TextBlueDark,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Status",
                        modifier = Modifier.width(100.dp).padding(top = 5.dp, start = 17.dp),
                        color = TextBlueDark,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                repeat(maxPlayers) { index ->
                    val name = playerNames.getOrNull(index)
                    val isCurrentUser = name != null && name == currentUsername
                    val isHostPlayer = name != null && name == hostUsername

                    val statusText = when {
                        name == null -> ""
                        isCurrentUser && !isReady -> "not ready"
                        else -> "ready"
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        PlayerSlot(
                            name = when {
                                name == null && index == playerNames.size -> "Waiting for players..."
                                name == null -> ""
                                isCurrentUser -> "$name (you)"
                                else -> name
                            },
                            isHost = isHostPlayer
                        )

                        StatusSlot(text = statusText)
                    }
                }
            }
        }
        Image(
            painter = painterResource(id = R.drawable.decor_screw),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = 8.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.decor_screw),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = 8.dp)
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
        colors = CardDefaults.cardColors(containerColor = ButtonBeigeLight),
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
    val isNotReady = text == "not ready"
    val isEmpty = text.isBlank()

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(36.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isEmpty -> Color(0xFFE3DDD2)
                isNotReady -> Color(0xFFE3DDD2)
                else -> ButtonBeigeLight
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNotReady || isEmpty) 1.dp else 5.dp        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                color = if (isNotReady || isEmpty) TextBlueDark.copy(alpha = 0.45f) else TextBlueDark,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
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
            .padding(vertical = 10.dp),
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
            fontWeight = FontWeight.Bold,
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
            colors = CardDefaults.cardColors(containerColor = PanelBackgroundBeige),
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
                    color = TextBlueLight,
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

@Preview(showBackground = true, widthDp = 917, heightDp = 412)
@Composable
private fun LobbyScreenPreview() {
    ClientTheme {
        LobbyScreen(
            players = listOf(PlayerCoinState(id = "1", displayName = "Player1", coins = 3)),
            currentUsername = "Player1",
            hostUsername = "Player1",
            isHost = true,
            isReady = false,
            lobbyCode = "AJ25Z39"
        )
    }
}

@Preview(showBackground = true, widthDp = 917, heightDp = 412)
@Composable
private fun LobbyScreenFullPreview() {
    ClientTheme {
        LobbyScreen(
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Player1", coins = 3, isReady = true),
                PlayerCoinState(id = "2", displayName = "Player2", coins = 3, isReady = true),
                PlayerCoinState(id = "3", displayName = "Player3", coins = 3, isReady = false),
                PlayerCoinState(id = "4", displayName = "Player4", coins = 3, isReady = true),
            ),
            currentUsername = "Player1",
            hostUsername = "Player1",
            isHost = true,
            isReady = true,
            lobbyCode = "AJ25Z39"
        )
    }
}
