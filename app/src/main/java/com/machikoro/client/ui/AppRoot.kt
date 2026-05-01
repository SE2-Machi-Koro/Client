package com.machikoro.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.ui.game.GameScreen
import com.machikoro.client.ui.start.StartScreen
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun AppRoot(
    gameScreenState: GameScreenState,
    startScreenState: StartScreenState,
    registerDialogState: RegisterDialogState,
    onRegisterUsernameChange: (String) -> Unit,
    onRegisterPasswordChange: (String) -> Unit,
    onRegisterSubmit: () -> Unit,
    onRegisterDialogReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (gameScreenState.gamePhase != GamePhase.NONE) {
        GameScreen(state = gameScreenState, modifier = modifier)
    } else {
        StartScreen(
            state = startScreenState,
            registerDialogState = registerDialogState,
            onRegisterUsernameChange = onRegisterUsernameChange,
            onRegisterPasswordChange = onRegisterPasswordChange,
            onRegisterSubmit = onRegisterSubmit,
            onRegisterDialogReset = onRegisterDialogReset,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true, widthDp = 917, heightDp = 412)
@Composable
private fun AppRootStartScreenPreview() {
    ClientTheme {
        AppRoot(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder(),
            registerDialogState = RegisterDialogState(),
            onRegisterUsernameChange = {},
            onRegisterPasswordChange = {},
            onRegisterSubmit = {},
            onRegisterDialogReset = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 917, heightDp = 412)
@Composable
private fun AppRootGameScreenPreview() {
    ClientTheme {
        AppRoot(
            gameScreenState = GameScreenState.initial().copy(gamePhase = GamePhase.ROLL_DICE),
            startScreenState = StartScreenState.placeholder(),
            registerDialogState = RegisterDialogState(),
            onRegisterUsernameChange = {},
            onRegisterPasswordChange = {},
            onRegisterSubmit = {},
            onRegisterDialogReset = {},
        )
    }
}
