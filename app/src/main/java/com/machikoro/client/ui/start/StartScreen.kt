package com.machikoro.client.ui.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.model.state.BackendHealth
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.ui.home.PdfViewerScreen
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.Header
import com.machikoro.client.ui.shared.MainScreenBackground
import com.machikoro.client.ui.shared.SecondaryActionButton
import com.machikoro.client.ui.theme.ClientTheme


private val HealthChipShape = RoundedCornerShape(12.dp)
private val HealthChipBackground = Color.White.copy(alpha = 0.85f)
private val HealthUpColor = Color(0xFF4CAF50)
private val HealthDownColor = Color(0xFFE53935)
private val HealthUnknownColor = Color(0xFFB0BEC5)

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
    modifier: Modifier = Modifier
) {
   // val showPdfViewer = remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

   /* if (showPdfViewer.value) {
        PdfViewerScreen(
            onClose = { showPdfViewer.value = false }
        )
    } else {*/
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            MainScreenBackground()
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = 30.dp, start = 30.dp, end = 30.dp)
            ) {
            BackendHealthChip(
                health = state.backendHealth,
                modifier = Modifier
                    .align(Alignment.TopStart)
            )
            Header(
                label = "Machi Koro",
                modifier = Modifier
                .align(Alignment.TopCenter),
                fontSize = 52)

            /*SecondaryActionButton(
                label = "Rules",
                onClick = { showPdfViewer.value = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )*/

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                ActionButton(
                    label = "Login",
                    onClick = {
                        onLoginDialogReset()
                        showLoginDialog = true
                    },
                    fontSize = 24
                )
                ActionButton(
                    label = "Register",
                    // Reset dialog VM state before opening so the dialog can't
                    // surface leftover values from a previous successful
                    // submission — the route flips to HomeScreen on success
                    // before the user can dismiss the dialog, so the dismissal
                    // reset never fires and the Activity-scoped VM keeps its
                    // stale state until the user comes back to StartScreen.
                    onClick = {
                        onRegisterDialogReset()
                        showRegisterDialog = true
                    },
                    fontSize = 24
                )

            }

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
private fun BackendHealthChip(
    health: BackendHealth,
    modifier: Modifier = Modifier,
) {
    val (dotColor, label) = when (health) {
        BackendHealth.UP -> HealthUpColor to "Connected"
        BackendHealth.DOWN -> HealthDownColor to "Backend unreachable"
        BackendHealth.UNKNOWN -> HealthUnknownColor to "Checking…"
    }
    Row(
        modifier = modifier
            .clip(HealthChipShape)
            .background(HealthChipBackground)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.Black,
            style = MaterialTheme.typography.labelMedium,
        )
    }
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
                connectionStatus = ConnectionStatus.CONNECTED,
                backendHealth = BackendHealth.UP,
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
                backendHealth = BackendHealth.DOWN,
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
