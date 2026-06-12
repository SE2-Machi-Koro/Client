package com.machikoro.client.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.machikoro.client.ui.game.ui.BigPlayerCardsDisplay
import com.machikoro.client.ui.game.ui.BuyingPhaseShop
import com.machikoro.client.ui.game.ui.DiceAnimationDisplay
import com.machikoro.client.ui.game.ui.DiceResultDisplay
import com.machikoro.client.ui.game.ui.GamePhaseBanner
import com.machikoro.client.ui.game.ui.GameScreenLayout
import com.machikoro.client.ui.game.ui.InitializationLoadingOverlay
import com.machikoro.client.ui.game.ui.MarketplaceButton
import com.machikoro.client.ui.game.ui.MarketplaceSection
import com.machikoro.client.ui.game.ui.PlayerCardsDisplay
import com.machikoro.client.ui.game.ui.PlayerCoinField
import com.machikoro.client.ui.game.ui.PlayersTopBar
import com.machikoro.client.ui.game.ui.RoundIndicator
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.shared.DecreasingLineTimer
import com.machikoro.client.ui.shared.SecondaryActionButton
import com.machikoro.client.ui.theme.ClientTheme
import kotlinx.coroutines.delay

private val SIDE_CENTER_CONTENT_OFFSET = (-35).dp

@Composable
fun GameScreen(
    state: GameScreenState,
    onPurchaseClick: (String) -> Unit = {},
    onBuySelectedClick: () -> Unit = {},
    onRollDice: (diceCount: Int) -> Unit = {},
    onTurnFlowAction: () -> Unit = {},
    onLeaveGame: () -> Unit = {},
    onEndGame: () -> Unit = {},
    cheatRecommendation: CardType? = null,
    onShake: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ShakeDetector(
        enabled = state.gameStatus == GameStatus.IN_PROGRESS,
        onShake = onShake,
    )

    var showLeaveDialog by remember { mutableStateOf(false) }
    var showOwnCards by remember { mutableStateOf(false) }


    var isOwnCardsDisplayPermitted = ((state.gamePhase == GamePhase.ROLL_DICE || state.gamePhase == GamePhase.BUY_OR_BUILD )
            && !state.isActivePlayer)

    LaunchedEffect(isOwnCardsDisplayPermitted) {
        if (!isOwnCardsDisplayPermitted) {
            showOwnCards = false
        }
    }



    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = {
                Text("Leave Game?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("The game will keep running. You can resume it from the home screen.")

                    // Debug-only in-game tools, stacked in the dialog body so they don't
                    // crowd the Leave/Stay buttons and so neither label wraps/clips in the
                    // dialog's narrow width (#255). Compiled out of release builds.
                    if (BuildConfig.DEBUG && state.gameStatus == GameStatus.IN_PROGRESS) {
                        Column {
                            Text(
                                text = "Debug",
                                style = MaterialTheme.typography.labelMedium,
                            )

                            TextButton(
                                onClick = {
                                    showLeaveDialog = false
                                    onShake()
                                }
                            ) {
                                Text("Insider tip")
                            }

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
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        onLeaveGame()
                    }
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLeaveDialog = false }
                ) {
                    Text("Stay")
                }
            }
        )
    }

    Background()
    Box {


    GameScreenLayout(
        // =====================================
        // TOP BAR
        // =====================================
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    SecondaryActionButton(
                        label = "Leave",
                        modifier = Modifier.align(
                            Alignment.CenterStart
                        ),
                        onClick = {
                            showLeaveDialog = true
                        }
                    )

                    PlayersTopBar(
                        players = state.players,
                        playerLandmarks =
                            state.playerLandmarks,
                        playerCards = state.playerCards,
                        modifier = Modifier.align(
                            Alignment.Center
                        ),
                    )

                    state.roundNumber?.let { round ->
                        RoundIndicator(
                            round = round,
                            modifier = Modifier.align(
                                Alignment.CenterEnd
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                GamePhaseBanner(
                    phase = state.gamePhase,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = state.customDisplayText
                )
            }
        },

        // =====================================
        // LEFT
        // =====================================
        leftContent = {
            Box(modifier = Modifier.fillMaxHeight()) {

                Box(modifier = Modifier.align(Alignment.Center)
                    .offset(y = SIDE_CENTER_CONTENT_OFFSET)
                ) {
                    if(state.gamePhase != GamePhase.ROLL_DICE) {
                        state.diceResult?.let { DiceResultDisplay(dice = it) }
                    }
                }

                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    MarketplaceButton()
                }
            }
        },

        // =====================================
        // CENTER
        // =====================================
        centerContent = {

            Box(modifier = Modifier.align(Alignment.Center)) {

                if(showOwnCards && isOwnCardsDisplayPermitted) {
                    LaunchedEffect(Unit) {
                        delay(10000)
                        showOwnCards = false
                    }
                    Column(
                        modifier = Modifier.align(Alignment.Center)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DecreasingLineTimer(10)
                        Image(
                            painter = painterResource(id = R.drawable.rotated_arrow),
                            contentDescription = "Arrow",
                            modifier = Modifier.clickable {
                                showOwnCards = false
                            }
                                .size(35.dp)
                            )
                        BigPlayerCardsDisplay(
                            state
                        )
                    }
                }

                else if (false) {
                    MarketplaceSection(
                        marketplace = state.marketplace,
                        recommendedCardType = cheatRecommendation,
                        modifier = Modifier.widthIn(max = 260.dp)
                    )
                }

                else if (state.isBuyingPhase) {
                    if(state.isActivePlayer) {
                        BuyingPhaseShop(
                            state = state,
                            items = state.shopItems.ifEmpty { ShopCatalog.defaultItems },
                            onPurchaseClick = onPurchaseClick,
                            recommendedCardType = cheatRecommendation,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else BasicText("Waiting for purchase")
                }

                else if (state.gamePhase == GamePhase.ROLL_DICE) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            state.isRolling -> DiceAnimationDisplay()
                            state.diceResult != null -> DiceResultDisplay(dice = state.diceResult)
                        }

                        if (state.isActivePlayer && state.gameStatus == GameStatus.IN_PROGRESS) {
                            var selectedDiceCount by remember(state.roundNumber) { mutableIntStateOf(1) }

                            if (state.hasTrainStation) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(1, 2).forEach { count ->
                                        Button(
                                            onClick = { selectedDiceCount = count },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedDiceCount == count)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (selectedDiceCount == count)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) {
                                            Text("$count 🎲")
                                        }
                                    }
                                }
                            }

                            ActionButton(
                                onClick = { onRollDice(if (state.hasTrainStation) selectedDiceCount else 1) },
                                enabled = !state.isRolling,
                                label = if (state.diceResult == null) "Würfeln" else "Nochmal würfeln",
                                leftIcon = R.drawable.game_dice_perspective,
                                modifier = Modifier.semantics {
                                    contentDescription = "Würfeln"
                                }
                            )
                        }
                    }
                }
            }
        },
// =====================================
// RIGHT
// =====================================
        rightContent = {
            Box(modifier = Modifier.fillMaxHeight()
                .width(140.dp)
            ) {

            PlayerCoinField(
               state = state,
                modifier = Modifier.align(Alignment.CenterEnd)
                    .offset(y = SIDE_CENTER_CONTENT_OFFSET)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier.align(Alignment.BottomCenter)
            ) {
                val turnFlowLabel = state.turnFlowActionLabel()
                if (
                    state.isActivePlayer &&
                    state.gameStatus == GameStatus.IN_PROGRESS
                ) {
                    turnFlowLabel?.let {
                            ActionButton(
                                onClick = if (state.isBuyingPhase) onBuySelectedClick else onTurnFlowAction,
                                enabled = !state.isBuyingPhase || state.canConfirmSelectedPurchase(),
                                modifier = Modifier.semantics {
                                    contentDescription = turnFlowLabel
                                }
                                    .fillMaxWidth(),
                                label = turnFlowLabel,
                            )
                    }

                            if (state.isBuyingPhase) {
                               SecondaryActionButton(
                                   onClick = onTurnFlowAction,
                                   enabled = state.purchaseState != PurchaseState.PENDING,
                                   modifier = Modifier.semantics {
                                       contentDescription = "Skip"
                                   }
                                       .fillMaxWidth(),
                                   label = "Skip",
                           )
                        }
                    }
                }
            }
        }
    )

        if(isOwnCardsDisplayPermitted && !showOwnCards) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .offset(y = 90.dp),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Image(
                    painter = painterResource(id = R.drawable.arrow_button),
                    contentDescription = "Arrow",
                    modifier = Modifier.clickable {
                        showOwnCards = true
                    }
                        .size(35.dp),

                )
                Spacer(modifier = Modifier.height(4.dp))
                PlayerCardsDisplay(
                    state,
                    modifier = Modifier
                )
            }
        }
    }
        InitializationLoadingOverlay(
            connectionStatus = state.connectionStatus,
            gameStatus = state.gameStatus
        )
    }


private fun GameScreenState.turnFlowActionLabel(): String? = when (gamePhase) {
    GamePhase.RESOLVE_EFFECTS -> "Resolve effects"
    GamePhase.BUY_OR_BUILD -> "Buy card"
    GamePhase.NONE,
    GamePhase.ROLL_DICE,
    GamePhase.END_TURN -> null
}

private fun GameScreenState.canConfirmSelectedPurchase(): Boolean =
    selectedPurchaseItemType != null &&
        purchaseState != PurchaseState.PENDING &&
        purchaseState != PurchaseState.SUCCESS

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
    ),
    PlayerCoinState(
        id = "4",
        displayName = "looooooooooooooooooooooong name",
        coins = 3
    ),
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
