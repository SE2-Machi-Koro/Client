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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
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
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.ui.cheat.ShakeDetector
import com.machikoro.client.ui.game.ui.BigPlayerCardsDisplay
import com.machikoro.client.ui.game.ui.BuyingPhaseShop
import com.machikoro.client.ui.game.ui.ChatOverlay
import com.machikoro.client.ui.game.ui.DiceResultDisplay
import com.machikoro.client.ui.game.ui.DiceSection
import com.machikoro.client.ui.game.ui.GamePhaseBanner
import com.machikoro.client.ui.game.ui.GameScreenLayout
import com.machikoro.client.ui.game.ui.InitializationLoadingOverlay
import com.machikoro.client.ui.game.ui.MarketplaceButton
import com.machikoro.client.ui.game.ui.MarketplaceSection
import com.machikoro.client.ui.game.ui.PlayerCardsDisplay
import com.machikoro.client.ui.game.ui.PlayerCoinField
import com.machikoro.client.ui.game.ui.PlayersTopBar
import com.machikoro.client.ui.game.ui.ResolvingEffectsView
import com.machikoro.client.ui.game.ui.RoundIndicator
import com.machikoro.client.ui.game.ui.withResolvingEffectsPreviewCoins
import com.machikoro.client.ui.game.ui.coin_animation.CoinChangeHighlight
import com.machikoro.client.ui.game.ui.coin_animation.CoinTransferOverlay
import com.machikoro.client.ui.game.ui.coin_animation.CoinTransferUi
import com.machikoro.client.ui.game.ui.coin_animation.buildCoinTransfers
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.shared.DecreasingLineTimer
import com.machikoro.client.ui.shared.SecondaryActionButton
import com.machikoro.client.ui.theme.ButtonBeigeLight
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PrimaryBlueDark
import com.machikoro.client.ui.theme.PrimaryOrange
import com.machikoro.client.ui.theme.TextBlueDark
import kotlinx.coroutines.delay

// delays
private val OWN_CARDS_VIEW_DELAY = 10000L
private val MARKETPLACE_VIEW_DELAY = 10000L
private val SHOP_VIEW_DELAY = 15000L



// offsets
const val SIDE_CONTENT_OFFSET = 35

@Composable
fun GameScreen(
    state: GameScreenState,
    onPurchaseClick: (String) -> Unit = {},
    onBuySelectedClick: () -> Unit = {},
    onRollDice: (diceCount: Int) -> Unit = {},
    onReroll: (diceCount: Int) -> Unit = {},
    onSkipReroll: () -> Unit = {},
    onResolveEffectsAnimationFinished: () -> Unit = {},
    onTurnFlowAction: () -> Unit = {},
    onLeaveGame: () -> Unit = {},
    onEndGame: () -> Unit = {},
    onSendChatMessage: (message: String) -> Unit = {},
    cheatRecommendation: CardType? = null,
    onShake: () -> Unit = {},
    onAccuse: (accusedPlayerId: Int) -> Unit = {},
    canAccuse: Boolean = true,
    // Radio Tower reroll budget (#326): false once the active player has rerolled
    // this turn. Combined with [GameScreenState.canReroll] to show the button.
    canReroll: Boolean = true,
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
    val resolvingEffectsCoinState = remember(state) {
        state.withResolvingEffectsPreviewCoins()
    }
    val currentCoins = remember(state.players) {
        state.players.mapNotNull { player ->
            player.id.toIntOrNull()?.let { it to player.coins }
        }.toMap()
    }
    val resolvedCoins = remember(resolvingEffectsCoinState.players) {
        resolvingEffectsCoinState.players.mapNotNull { player ->
            player.id.toIntOrNull()?.let { it to player.coins }
        }.toMap()
    }
    val coinTransfers = remember(state.gamePhase, currentCoins, resolvedCoins) {
        if (state.gamePhase == GamePhase.RESOLVE_EFFECTS) {
            buildCoinTransfers(
                previousCoins = currentCoins,
                currentCoins = resolvedCoins,
            )
        } else {
            emptyList()
        }
    }
    val animationKey = remember(
        state.gameId,
        state.roundNumber,
        state.activePlayerId,
        state.diceResult,
        coinTransfers,
    ) {
        listOf(
            state.gameId,
            state.roundNumber,
            state.activePlayerId,
            state.diceResult,
            coinTransfers,
        )
    }
    var coinAnimationFinished by remember(animationKey) {
        mutableStateOf(coinTransfers.isEmpty())
    }
    var activeCoinTransfer by remember(animationKey) {
        mutableStateOf<CoinTransferUi?>(null)
    }
    val playerCoinPositions = remember {
        mutableStateMapOf<Int, Offset>()
    }
    val coinHighlights = remember(activeCoinTransfer) {
        buildMap {
            activeCoinTransfer?.fromPlayerId?.let {
                put(it, CoinChangeHighlight.LOSS)
            }
            activeCoinTransfer?.toPlayerId?.let {
                put(it, CoinChangeHighlight.GAIN)
            }
        }
    }
    val coinDisplayState =
        if (coinTransfers.isNotEmpty() && !coinAnimationFinished) {
            state
        } else {
            resolvingEffectsCoinState
        }

    LaunchedEffect(animationKey) {
        coinAnimationFinished = coinTransfers.isEmpty()
        activeCoinTransfer = null
    }

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
            state.isBuyingPhase && state.purchaseState != PurchaseState.SUCCESS  -> (SHOP_VIEW_DELAY / 1000).toInt()
            state.gamePhase == GamePhase.ROLL_DICE -> 20
            // Matches the actual auto-advance dwell so the countdown the player sees
            // is the real time until RESOLVE_EFFECTS ends, not a longer, unrelated number.
            state.gamePhase == GamePhase.RESOLVE_EFFECTS -> 0
            state.purchaseState == PurchaseState.SUCCESS -> 5
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

    val showRadioTowerReroll = state.canReroll && canReroll
    var chatOpen by remember { mutableStateOf(false) }
    var readCount by remember { mutableStateOf(0) }

    LaunchedEffect(chatOpen, state.chatMessages.size) {
        if (chatOpen) {
            readCount = state.chatMessages.size
        }
    }

    LaunchedEffect(showOwnCards) {
        if (showOwnCards) {
            SoundManager.play(GameSound.CARD_FLIP)
            showMarketplace = false
            delay(OWN_CARDS_VIEW_DELAY)
            showOwnCards = false
        }
    }
    LaunchedEffect(showMarketplace) {
        if (showMarketplace) {
            SoundManager.play(GameSound.CARD_FLIP)
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
    LaunchedEffect(state.purchaseState) {
        if (state.purchaseState == PurchaseState.SUCCESS) {
            SoundManager.play(GameSound.PURCHASE)
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
                            players = coinDisplayState.players,
                            playerLandmarks =
                                state.playerLandmarks,
                            playerCards = state.playerCards,
                            onAccusePlayer = { accuseTargetId = it },
                            canAccuse = canAccuse,
                            coinHighlights = coinHighlights,
                            onCoinBadgePositioned = { playerId, center ->
                                playerCoinPositions[playerId] = center
                            },
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
                Box(modifier = Modifier
                    .fillMaxHeight()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = SIDE_CONTENT_OFFSET.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (state.gamePhase != GamePhase.ROLL_DICE) {
                            if (
                                state.gamePhase == GamePhase.RESOLVE_EFFECTS &&
                                !state.isActivePlayer
                            ) {
                                BasicText("${state.activePlayerUsername} rolled:")
                            }
                            state.diceResult?.let {
                                DiceResultDisplay(dice = it,
                                    diceSize = 42.dp,
                                    modifier = Modifier.offset(y = (-10).dp))
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    ) {
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
                        var delayShop = 0L

                    LaunchedEffect(state.isBuyingPhase) {
                        if(state.isBuyingPhase) {
                            delayShop = 5000L
                            delay(delayShop)
                            if(state.purchaseState != PurchaseState.SUCCESS
                                || state.purchaseState != PurchaseState.PENDING) {
                                onTurnFlowAction()
                            } else if(state.purchaseState == PurchaseState.SUCCESS) delayShop = 0L
                        } else delayShop = 0L
                    }
                            BuyingPhaseShop(
                                state = state,
                                items = state.shopItems.ifEmpty { ShopCatalog.defaultItems },
                                onPurchaseClick = onPurchaseClick,
                                recommendedCardType = cheatRecommendation,
                                modifier = Modifier.align(Alignment.Center)
                            )
                    }

                    else if (state.gamePhase == GamePhase.RESOLVE_EFFECTS) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(x = 5.dp, y = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ResolvingEffectsView(state = state,
                                    modifier = Modifier.offset(y = -50.dp))

                                if (
                                    showRadioTowerReroll &&
                                    state.isActivePlayer &&
                                    state.gameStatus == GameStatus.IN_PROGRESS
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ActionButton(
                                            onClick = { onReroll(state.diceResult?.size ?: 1) },
                                            enabled = !state.isRolling,
                                            label = "Reroll",
                                            leftIcon = R.drawable.game_dice_perspective,
                                            modifier = Modifier.semantics {
                                                contentDescription = "Reroll dice"
                                            }
                                        )

                                        SecondaryActionButton(
                                            onClick = onSkipReroll,
                                            enabled = !state.isRolling,
                                            label = "Skip",
                                            modifier = Modifier.semantics {
                                                contentDescription = "Skip reroll"
                                            }
                                        )
                                    }
                                }
                            }
                        } else if (state.gamePhase == GamePhase.ROLL_DICE){
                            DiceSection(
                                state = state,
                                onRollDice = onRollDice,
                                onReroll = onReroll,
                                onSkipReroll = onSkipReroll,
                                canReroll = canReroll,
                                modifier = Modifier.align(Alignment.Center)
                            )
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
                        state = coinDisplayState,
                        highlight = coinDisplayState.players
                            .firstOrNull { it.isCurrentPlayer }
                            ?.id
                            ?.toIntOrNull()
                            ?.let { coinHighlights[it] }
                            ?: CoinChangeHighlight.NONE,
                        onCoinPositioned = { playerId, center ->
                            playerCoinPositions[playerId] = center
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(y = SIDE_CONTENT_OFFSET.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = modifier.align(Alignment.BottomCenter)
                    ) {
                        val turnFlowLabel = state.turnFlowActionLabel()
                        if (
                            state.isActivePlayer &&
                            state.gameStatus == GameStatus.IN_PROGRESS &&
                            state.purchaseState != PurchaseState.SUCCESS
                        ) {
                            turnFlowLabel?.let {
                                ActionButton(
                                    onClick = {
                                        if (state.isBuyingPhase) {
                                            onBuySelectedClick()
                                        } else {
                                            onTurnFlowAction()
                                        }
                                    },
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
        BadgedBox(
            badge = {
                if (readCount < state.chatMessages.size) {
                    Badge(
                        contentColor = PrimaryBlueDark,
                        containerColor = PrimaryOrange
                    ){
                        Text((state.chatMessages.size-readCount).toString())
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            // Floating chat button
            FloatingActionButton(
                onClick = {
                    chatOpen = !chatOpen
                },
                containerColor = ButtonBeigeLight
            ) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Chat",
                    tint = TextBlueDark
                )
            }
        }
        // Chat overlay
        ChatOverlay(
            //used to compare the current player with the chat message sender to highlight own messages
            //displayname == username
            currentPlayer = state.players.firstOrNull { it.id.toIntOrNull() == state.myUserId }?.displayName ?: "",
            open = chatOpen,
            messages = state.chatMessages,
            onSendMessageClick = onSendChatMessage,
            onClose = { chatOpen = false }
        )

        if (state.gamePhase == GamePhase.RESOLVE_EFFECTS && !coinAnimationFinished) {
            CoinTransferOverlay(
                transfers = coinTransfers,
                playerPositions = playerCoinPositions,
                modifier = Modifier.fillMaxSize(),
                onTransferChanged = { activeCoinTransfer = it },
                onFinished = {
                    coinAnimationFinished = true
                    onResolveEffectsAnimationFinished()
                },
            )
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

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenRadioTowerRerollDecisionPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.RESOLVE_EFFECTS,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                diceResult = listOf(6, 6),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
                gameStatus = GameStatus.IN_PROGRESS,
                roundNumber = 4,
                playerLandmarks = mapOf(
                    1 to listOf(
                        PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = true),
                    )
                ),
                marketplace = previewMarketplace(),
            ),
            canReroll = true,
        )
    }
}


@Preview(showBackground = true, widthDp = 915, heightDp = 430)
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
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
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

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
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

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
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

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
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

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
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

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenResolveEffectsPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.RESOLVE_EFFECTS,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                diceResult = listOf(3),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
                roundNumber = 4,
                playerLandmarks = previewLandmarks(),
                marketplace = previewMarketplace(),
                customDisplayText = "Resolving effects"
            ),
            canReroll = true
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenResolveEffectsRedCardsPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.RESOLVE_EFFECTS,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = listOf(
                    PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = true),
                    PlayerCoinState("2", "Player2", 4),
                    PlayerCoinState("3", "Player3", 5),
                    PlayerCoinState("4", "Player4", 7),
                ),
                diceResult = listOf(4, 5),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
                roundNumber = 10,
                playerLandmarks = previewLandmarks(),
                playerCards = mapOf(
                    2 to listOf(
                        PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 1)
                    ),
                    3 to listOf(
                        PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 2)
                    ),
                    4 to listOf(
                        PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 1)
                    )
                ),
                marketplace = previewMarketplace(),
                customDisplayText = "Round outcome"
            ),
            canReroll = false
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenResolveEffectsBlueCardsPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.RESOLVE_EFFECTS,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = listOf(
                    PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = true),
                    PlayerCoinState("2", "Player2", 4),
                    PlayerCoinState("3", "Player3", 5),
                    PlayerCoinState("4", "Player4", 7),
                ),
                diceResult = listOf(1),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
                roundNumber = 10,
                playerLandmarks = previewLandmarks(),
                playerCards = mapOf(
                    1 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                    2 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 2)),
                    3 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                    4 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                ),
                marketplace = previewMarketplace(),
                customDisplayText = "Round outcome"
            ),
            canReroll = false
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenResolveEffectsGreenCardsPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.RESOLVE_EFFECTS,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = listOf(
                    PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = true),
                    PlayerCoinState("2", "Player2", 4),
                    PlayerCoinState("3", "Player3", 5),
                    PlayerCoinState("4", "Player4", 7),
                ),
                diceResult = listOf(2),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
                roundNumber = 10,
                playerLandmarks = previewLandmarks(),
                playerCards = mapOf(
                    1 to listOf(
                        PlayerCardState(CardType.BAKERY, quantity = 2)
                    ),
                    2 to listOf(
                        PlayerCardState(CardType.BAKERY, quantity = 2)
                    ),
                    3 to listOf(
                        PlayerCardState(CardType.BAKERY, quantity = 1)
                    ),
                ),
                marketplace = previewMarketplace(),
                customDisplayText = "Round outcome"
            ),
            canReroll = false
        )
    }
}
@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenResolveEffectsPurpleStadiumPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.RESOLVE_EFFECTS,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = listOf(
                    PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = true),
                    PlayerCoinState("2", "Player2", 4),
                    PlayerCoinState("3", "Player3", 5),
                    PlayerCoinState("4", "Player4", 7),
                ),
                diceResult = listOf(6),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
                roundNumber = 10,
                playerLandmarks = previewLandmarks(),
                playerCards = mapOf(
                    1 to listOf(PlayerCardState(CardType.STADIUM, quantity = 1))
                ),
                marketplace = previewMarketplace(),
                customDisplayText = "Round outcome"
            ),
            canReroll = false
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenResolveEffectsPurpleTvStationPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.RESOLVE_EFFECTS,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = listOf(
                    PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = true),
                    PlayerCoinState("2", "Player2", 4),
                    PlayerCoinState("3", "Player3", 5),
                    PlayerCoinState("4", "Player4", 7),
                ),
                diceResult = listOf(6),
                purchaseState = PurchaseState.IDLE,
                myUserId = 1,
                activePlayerId = 1,
                roundNumber = 10,
                playerLandmarks = previewLandmarks(),
                playerCards = mapOf(
                    1 to listOf(PlayerCardState(CardType.TV_STATION, quantity = 1))
                ),
                marketplace = previewMarketplace(),
                customDisplayText = "Choose player"
            ),
            canReroll = false
        )
    }
}
