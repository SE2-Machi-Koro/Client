package com.machikoro.client.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.LobbyStatus
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme

private val SecondaryActionShape = RoundedCornerShape(8.dp)
private val SecondaryActionColor = Color(0xFF64B5F6)
private val SecondaryActionTextColor = Color.Black

@Composable
fun StartScreen(
    state: StartScreenState,
    registerDialogState: RegisterDialogState,
    loginDialogState: LoginDialogState,
    logoutState: LogoutState,
    onRegisterUsernameChange: (String) -> Unit,
    onRegisterPasswordChange: (String) -> Unit,
    onRegisterSubmit: () -> Unit,
    onRegisterDialogReset: () -> Unit,
    onLoginUsernameChange: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit,
    onLoginSubmit: () -> Unit,
    onLoginDialogReset: () -> Unit,
    onLogoutSubmit: () -> Unit,
    onStartGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val showPdfViewer = remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

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
            Button(
                onClick = { showPdfViewer.value = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                shape = SecondaryActionShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryActionColor
                )
            ) {
                Text(
                    text = "Rules",
                    color = SecondaryActionTextColor,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            LobbyControls(
                state = state,
                logoutState = logoutState,
                onStartGame = onStartGame,
                onShowRegisterDialog = { showRegisterDialog = true },
                onShowLoginDialog = { showLoginDialog = true },
                onLogoutSubmit = onLogoutSubmit,
            )
            if (showRegisterDialog) {
                RegisterDialog(
                    state = registerDialogState,
                    onUsernameChange = onRegisterUsernameChange,
                    onPasswordChange = onRegisterPasswordChange,
                    onSubmit = onRegisterSubmit,
                    onDismiss = {
                        showRegisterDialog = false
                        onRegisterDialogReset()
                    },
                )
            }

            if (showLoginDialog) {
                LoginDialog(
                    state = loginDialogState,
                    onUsernameChange = onLoginUsernameChange,
                    onPasswordChange = onLoginPasswordChange,
                    onSubmit = onLoginSubmit,
                    onDismiss = {
                        showLoginDialog = false
                        onLoginDialogReset()
                    },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.BackgroundImages() {
    Image(
        painter = painterResource(id = R.drawable.background_left),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = (-20).dp, y = 30.dp)
    )
    Image(
        painter = painterResource(id = R.drawable.background_right),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 15.dp, y = 30.dp)
    )
}

@Composable
private fun BoxScope.TitleHeader() {
    Text(
        text = "MACHI KORO",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 55.dp)
    )
}

@Composable
private fun LobbyControls(
    state: StartScreenState,
    logoutState: LogoutState,
    onStartGame: () -> Unit,
    onShowRegisterDialog: () -> Unit,
    onShowLoginDialog: () -> Unit,
    onLogoutSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Players: ${state.playerList.size}/${state.maxPlayers}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Connection status: ${state.connectionStatus.toDisplayText()}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Lobby/start: ${state.lobbyStatus.toDisplayText()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (state.loggedInAs == null) {
            SecondaryActionButton(
                text = "Register",
                onClick = onShowRegisterDialog,
            )
            SecondaryActionButton(
                text = "Login",
                onClick = onShowLoginDialog,
            )
        } else {
            Text(
                text = "Logged in as ${state.loggedInAs}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            SecondaryActionButton(
                text = if (logoutState.submitting) "Logging out…" else "Logout",
                onClick = onLogoutSubmit,
                enabled = !logoutState.submitting,
            )
        }
        if (state.isHost) {
            HostStartGameButton(enabled = state.playerList.size >= 2, onStartGame = onStartGame)
        }
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = SecondaryActionShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = SecondaryActionColor
        )
    ) {
        Text(
            text = text,
            color = SecondaryActionTextColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun HostStartGameButton(enabled: Boolean, onStartGame: () -> Unit) {
    Button(
        onClick = onStartGame,
        enabled = enabled,
        modifier = Modifier.padding(top = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.LightGray,
        )
    ) {
        Text(
            text = "Start Game",
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
            registerDialogState = RegisterDialogState(),
            loginDialogState = LoginDialogState(),
            logoutState = LogoutState(),
            onRegisterUsernameChange = {},
            onRegisterPasswordChange = {},
            onRegisterSubmit = {},
            onRegisterDialogReset = {},
            onLoginUsernameChange = {},
            onLoginPasswordChange = {},
            onLoginSubmit = {},
            onLoginDialogReset = {},
            onLogoutSubmit = {},
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 917,
    heightDp = 412
)
@Composable
private fun StartScreenAuthenticatedPreview() {
    ClientTheme {
        StartScreen(
            state = StartScreenState.placeholder().copy(
                connectionStatus = ConnectionStatus.CONNECTED,
                loggedInAs = "alice",
            ),
            registerDialogState = RegisterDialogState(),
            loginDialogState = LoginDialogState(),
            logoutState = LogoutState(),
            onRegisterUsernameChange = {},
            onRegisterPasswordChange = {},
            onRegisterSubmit = {},
            onRegisterDialogReset = {},
            onLoginUsernameChange = {},
            onLoginPasswordChange = {},
            onLoginSubmit = {},
            onLoginDialogReset = {},
            onLogoutSubmit = {},
        )
    }
}
