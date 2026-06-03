package com.machikoro.client.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.BuildConfig
import com.machikoro.client.R
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.ui.cheat.ShakeDetector
import com.machikoro.client.ui.game.ui.BuyingPhaseShop
import com.machikoro.client.ui.game.ui.DiceAnimationDisplay
import com.machikoro.client.ui.game.ui.DiceResultDisplay
import com.machikoro.client.ui.game.ui.GamePhaseBanner
import com.machikoro.client.ui.game.ui.InitializationLoadingOverlay
import com.machikoro.client.ui.game.ui.MarketplaceSection
import com.machikoro.client.ui.game.ui.PlayersTopBar
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.shared.SecondaryActionButton
import com.machikoro.client.ui.theme.ClientTheme



@Composable
fun GameScreen(
    state: GameScreenState,
    onPurchaseClick: (String) -> Unit = {},
    onRollDice: () -> Unit = {},
    onTurnFlowAction: () -> Unit = {},
    onLeaveGame: () -> Unit = {},
    onEndGame: () -> Unit = {},
    cheatRecommendation: CardType? = null,
    onShake: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Insider Trading cheat (#203): shake during play to surface a local best-buy hint.
    ShakeDetector(
        enabled = state.gameStatus == GameStatus.IN_PROGRESS,
        onShake = onShake,
    )
    var showLeaveDialog by remember { mutableStateOf(false) }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },

            title = {
                Text("Leave Game?")
            },

            text = {
                Text(
                    "The game will keep running. You can resume it from the home screen."
                )
            },

            confirmButton = {
                Row {
                    // Leave button
                    TextButton(
                        onClick = {
                            showLeaveDialog = false
                            onLeaveGame()
                        }
                    ) {
                        Text("Leave")
                    }

                    // Debug End Game button
                    if (
                        BuildConfig.DEBUG &&
                        state.gameStatus == GameStatus.IN_PROGRESS &&
                        state.gameId != null &&
                        state.myUserId != null
                    ) {
                        TextButton(
                            onClick = {
                                showLeaveDialog = false
                                onEndGame()
                            }
                        ) {
                            Text(
                                text = "End game",
                                color = Color.Red
                            )
                        }
                    }
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                    }
                ) {
                    Text("Stay")
                }
            }
        )
    }
    Background()
    // === WHOLE SCREEN ===
    Box(
        modifier = modifier.fillMaxSize()
            .padding(all = 12.dp)
    ) {
        // === START OF TOP BAR ===
        Box(
            modifier = modifier.fillMaxSize()
                .align(Alignment.TopCenter)

        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Top row
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Leave button (left)
                    SecondaryActionButton(
                        label = "Leave",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp),
                        onClick = { showLeaveDialog = true }
                    )

                    // Top bar (centered)
                    PlayersTopBar(
                        players = state.players,
                        playerLandmarks = state.playerLandmarks,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Round indicator (right)
                    state.roundNumber?.let { round ->
                        RoundIndicator(
                            round = round,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Banner under row
                GamePhaseBanner(
                    phase = state.gamePhase,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = state.customDisplayText
                )
            }
        }
        // === END OF TOP BAR ===

        if (state.isBuyingPhase) {
            BuyingPhaseShop(
                state = state,
                items = state.shopItems.ifEmpty { ShopCatalog.defaultItems },
                onPurchaseClick = onPurchaseClick,
                recommendedCardType = cheatRecommendation,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        // todo: change "false" upon adding "marketplace" button to open it on player's click,
        //  make sure that this action if possible during allowed phases only
        //  see sketches
        if (false) {
            MarketplaceSection(
                marketplace = state.marketplace,
                recommendedCardType = cheatRecommendation,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isRolling -> DiceAnimationDisplay()
                state.diceResult != null -> DiceResultDisplay(dice = state.diceResult)
            }

            if (state.gamePhase == GamePhase.ROLL_DICE && state.isActivePlayer && state.gameStatus == GameStatus.IN_PROGRESS) {
                ActionButton(
                    onClick = onRollDice,
                    enabled = !state.isRolling,
                    label = if (state.diceResult == null) "Würfeln" else "Nochmal würfeln",
                    leftIcon = R.drawable.game_dice_perspective,
                    modifier = Modifier.semantics {
                        contentDescription = "Würfeln"
                    }
                )

            }
            // todo: adjust to buttons in sketches, remove as turnFlowLabel
            val turnFlowLabel = state.turnFlowActionLabel()
            if (
                turnFlowLabel != null &&
                state.isActivePlayer &&
                state.gameStatus == GameStatus.IN_PROGRESS
            ) {
                ActionButton(
                    onClick = onTurnFlowAction,
                    modifier = Modifier.semantics {
                        contentDescription = turnFlowLabel
                    },
                    label = turnFlowLabel,
                )
            }
        }
    }

        // Debug-only (#191): force-end the game to reach the winner screen without
        // playing through. Subtle, like the home "Purge DB" control, and compiled out
        // of release builds. Visible only while in a running game the local player is in;
        // the server still enforces admin auth + membership.

        //shows a loading indicator when the game is not in progress and the connection status is not disconnected
        InitializationLoadingOverlay(
            connectionStatus = state.connectionStatus,
            gameStatus = state.gameStatus
        )
    }

private fun GameScreenState.turnFlowActionLabel(): String? = when (gamePhase) {
    GamePhase.RESOLVE_EFFECTS -> "Resolve effects"
    GamePhase.BUY_OR_BUILD -> "End turn"
    GamePhase.NONE,
    GamePhase.ROLL_DICE,
    GamePhase.END_TURN -> null
}

@Composable
private fun RoundIndicator(
    round: Int,
    modifier: Modifier = Modifier
) {
        Text(
            text = "Round $round",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.White,
            modifier = modifier
        )
    }

 // === PREVIEWS ===

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun GameScreenRollDicePreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.ROLL_DICE,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun GameScreenRollingPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.ROLL_DICE,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
                isRolling = true,
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun GameScreenRollDiceNotActivePreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.ROLL_DICE,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 2,
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 600)
@Composable
private fun GameScreenReconnectSnapshotPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.ROLL_DICE,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                diceResult = listOf(3, 4),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun GameScreenBuyOrBuildPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.BUY_OR_BUILD,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                diceResult = listOf(8),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
                roundNumber = 4,
                playerLandmarks = previewLandmarks(),
                marketplace = previewMarketplace(),
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun GameScreenNonePreview() {
    ClientTheme {
        GameScreen(state = GameScreenState.initial())
    }
}

private fun previewPlayers() = listOf(
    PlayerCoinState(
        id = "1",
        displayName = "You",
        coins = 6,
        isCurrentPlayer = true,
        isActivePlayer = true
    ),
    PlayerCoinState(
        id = "2",
        displayName = "SoupCube",
        coins = 3
    ),
    PlayerCoinState(
        id = "3",
        displayName = "doniliks",
        coins = 0
    )
)

private fun previewLandmarks() = mapOf(
    1 to listOf(
        PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
        PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = true),
        PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
        PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
    ),
    2 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
    3 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
)

private fun previewMarketplace() = mapOf(
    CardType.WHEAT_FIELD to 6,
    CardType.BAKERY to 5,
    CardType.CAFE to 6,
    CardType.CONVENIENCE_STORE to 4,
    CardType.FOREST to 6,
)
