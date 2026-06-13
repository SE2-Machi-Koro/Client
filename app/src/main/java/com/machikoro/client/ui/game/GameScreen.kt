package com.machikoro.client.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.draw.alpha
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

private val OWN_CARDS_VIEW_DELAY = 10000L
private val MARKETPLACE_VIEW_DELAY = 10000L

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
    onAccuse: (accusedPlayerId: Int) -> Unit = {},
    canAccuse: Boolean = true,
    modifier: Modifier = Modifier
) {
    ShakeDetector(
        enabled = state.gameStatus == GameStatus.IN_PROGRESS,
        onShake = onShake,
    )

    // Cheating accusation (#280): the Accuse action in the player-inspection
    // dialog opens this confirmation. Accusing wrongly costs a coin, so we
    // always confirm first.
    var accuseTargetId by remember { mutableStateOf<String?>(null) }
    val accuseTarget = accuseTargetId?.let { id -> state.players.firstOrNull { it.id == id } }
    if (accuseTarget != null) {
        AlertDialog(
            onDismissRequest = { accuseTargetId = null },
            title = { Text("Accuse ${accuseTarget.displayName}?") },
            text = {
                Text(
                    "Bet that ${accuseTarget.displayName} used the Insider Trading cheat. " +
                            "If you're wrong, you lose a coin."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accuseTarget.id.toIntOrNull()?.let(onAccuse)
                        accuseTargetId = null
                    }
                ) {
                    Text("Accuse")
                }
            },
            dismissButton = {
                TextButton(onClick = { accuseTargetId = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    var showLeaveDialog by remember { mutableStateOf(false) }
    var showOwnCards by remember { mutableStateOf(false) }
    var showMarketplace by remember { mutableStateOf(false) }

    val phaseTimerTime =
        when {
            state.isBuyingPhase -> 30
            state.gamePhase == GamePhase.ROLL_DICE -> 20
            state.gamePhase == GamePhase.RESOLVE_EFFECTS -> 25
            else -> 0
        }

    val cardsTimerTime =
        when {
            showOwnCards -> 10
            showMarketplace -> 10
            else -> 0
        }

    val isCardViewPossible = ((state.gamePhase == GamePhase.ROLL_DICE || state.gamePhase == GamePhase.BUY_OR_BUILD )
            && !state.isActivePlayer)

    LaunchedEffect(showOwnCards) {
        if (showOwnCards) {
            showMarketplace = false
            delay(OWN_CARDS_VIEW_DELAY)
            showOwnCards = false
        }
    }
    LaunchedEffect(showMarketplace) {
        if (showMarketplace) {
            showOwnCards = false
            delay(MARKETPLACE_VIEW_DELAY)
            showMarketplace = false
        }
    }
    LaunchedEffect(isCardViewPossible) {
        if (!isCardViewPossible) {
            showOwnCards = false
            showMarketplace = false
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
                            onAccusePlayer = { accuseTargetId = it },
                            canAccuse = canAccuse,
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

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(IntrinsicSize.Max),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        GamePhaseBanner(
                            phase = state.gamePhase,
                            text = state.customDisplayText
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {

                            // phase timer stays alive
                            if (phaseTimerTime > 0) {
                                Box(
                                    modifier = Modifier.alpha(
                                        if (cardsTimerTime > 0) 0f else 1f
                                    )
                                ) {
                                    DecreasingLineTimer(phaseTimerTime)
                                }
                            }

                            // cards timer overlay
                            if (cardsTimerTime > 0) {
                                DecreasingLineTimer(cardsTimerTime)
                            }
                        }
                    }
                }
            },

            // =====================================
            // LEFT
            // =====================================
            leftContent = {
                Box(modifier = Modifier.fillMaxHeight()) {

                    Box(modifier = Modifier.align(Alignment.Center)
                    ) {
                        if(state.gamePhase != GamePhase.ROLL_DICE) {
                            state.diceResult?.let { DiceResultDisplay(dice = it) }
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        MarketplaceButton(
                            onClick = {
                                showMarketplace = !showMarketplace
                            },
                            enabled = isCardViewPossible
                        )

                    }
                }
            },

            // =====================================
            // CENTER
            // =====================================
            centerContent = {

                Box(modifier = Modifier.align(Alignment.Center)) {
                    if(
                        showOwnCards
                        && isCardViewPossible
                        && !showMarketplace
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Image(
                                painter = painterResource(id = R.drawable.rotated_arrow),
                                contentDescription = "Arrow",
                                modifier = Modifier
                                    .clickable {
                                        showOwnCards = false
                                    }
                                    .size(35.dp)
                            )
                            BigPlayerCardsDisplay(
                                state
                            )
                        }
                    }

                    else if(
                        showMarketplace
                        && isCardViewPossible
                        && !showOwnCards
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            MarketplaceSection(state.marketplace)
                        }
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
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .width(140.dp)
                ) {

                    PlayerCoinField(
                        state = state,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 45.dp)
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
                                    modifier = Modifier
                                        .semantics {
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
                                    modifier = Modifier
                                        .semantics {
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

        //Small own cards at the button
        if(isCardViewPossible && !showOwnCards && !showMarketplace) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 90.dp),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Image(
                    painter = painterResource(id = R.drawable.arrow_button),
                    contentDescription = "Arrow",
                    modifier = Modifier
                        .clickable {
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
    GamePhase.BUY_OR_BUILD -> "Buy card"
    // RESOLVE_EFFECTS auto-advances now (#302) — no manual "Resolve effects" button.
    GamePhase.NONE,
    GamePhase.ROLL_DICE,
    GamePhase.RESOLVE_EFFECTS,
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