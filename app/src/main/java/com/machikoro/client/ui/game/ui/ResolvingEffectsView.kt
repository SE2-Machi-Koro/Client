package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.ShopItemColor
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.ui.game.ui.resolving_effects.CardsStack
import com.machikoro.client.ui.theme.ButtonBorderBeige
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.TextBlueDark

@Composable
fun ResolvingEffectsView(
    state: GameScreenState,
    modifier: Modifier = Modifier,
) {
    val triggeredEffects = remember(state) { state.triggeredEffects() }
    val localPlayerId = state.players.firstOrNull { it.isCurrentPlayer }?.id?.toIntOrNull()
        ?: state.myUserId

    if (triggeredEffects.isEmpty()) {
        Text(
            text = "No triggered establishments",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier
        )
    } else {
        TriggeredEffectsBoard(
            effects = triggeredEffects,
            players = state.players,
            localPlayerId = localPlayerId,
            modifier = modifier
        )
    }
}
@Composable
private fun TriggeredEffectsBoard(
    effects: List<TriggeredEffectUi>,
    players: List<PlayerCoinState>,
    localPlayerId: Int?,
    modifier: Modifier = Modifier,
) {
    val activePlayerId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull()
    val activePlayerName = players.firstOrNull { it.id.toIntOrNull() == activePlayerId }
        ?.displayName ?: "active player"

    val localEffects = effects.filter { it.playerId == localPlayerId }
    val isLocalActivePlayer = localPlayerId != null && localPlayerId == activePlayerId

    if (!isLocalActivePlayer) {
        PersonalEffectsDetailView(
            effects = localEffects,
            activePlayerName = activePlayerName,
            modifier = modifier
        )
        return
    }

    val redEffects = effects.filter { it.color == ShopItemColor.RED }
    val purpleEffects = effects.filter { it.color == ShopItemColor.PURPLE }
    val stadiumEffects = purpleEffects.filter { it.cardType == CardType.STADIUM }
    val tvStationEffects = purpleEffects.filter { it.cardType == CardType.TV_STATION }
    val businessCenterEffects = purpleEffects.filter { it.cardType == CardType.BUSINESS_CENTER }

    if (tvStationEffects.isNotEmpty() && effects.size == tvStationEffects.size) {
        TvStationChoosePlayerView(
            effect = tvStationEffects.first(),
            players = players.filter { it.id.toIntOrNull() != activePlayerId },
            modifier = modifier
        )
        return
    }

    if (businessCenterEffects.isNotEmpty() && effects.size == businessCenterEffects.size) {
        BusinessCenterChoosePlayerView(
            effect = businessCenterEffects.first(),
            players = players.filter { it.id.toIntOrNull() != activePlayerId },
            modifier = modifier
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top
    ) {
        players.forEach { player ->
            val playerId = player.id.toIntOrNull()
            val playerEffects = effects.filter { it.playerId == playerId }

            when {
                playerId == activePlayerId && stadiumEffects.isNotEmpty() -> {
                    StadiumGainStack(
                        effect = stadiumEffects.first(),
                        payingPlayers = players.filter { it.id.toIntOrNull() != activePlayerId }
                    )
                }

                playerId != activePlayerId && stadiumEffects.isNotEmpty() -> {
                    StadiumLossStack()
                }

                playerEffects.isNotEmpty() -> {
                    TriggeredPlayerStack(
                        effects = playerEffects,
                        isPositive = playerEffects.any { it.color == ShopItemColor.RED } ||
                                playerId == activePlayerId ||
                                playerEffects.any { it.color == ShopItemColor.BLUE }
                    )
                }

                playerId == activePlayerId && redEffects.isNotEmpty() -> {
                    ActivePlayerTransferStack(
                        amount = redEffects.sumOf { it.totalIncome }
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .width(155.dp)
                            .height(175.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalEffectsDetailView(
    effects: List<TriggeredEffectUi>,
    activePlayerName: String,
    modifier: Modifier = Modifier,
) {
    val redEffects = effects.filter { it.color == ShopItemColor.RED }
    val bankEffects = effects.filter {
        it.color == ShopItemColor.BLUE || it.color == ShopItemColor.GREEN
    }
    val shownEffects = redEffects.ifEmpty { bankEffects }.ifEmpty { effects }
    val coinDelta = shownEffects.sumOf { it.totalIncome }

    val subtitle = when {
        redEffects.isNotEmpty() -> "from $activePlayerName"
        bankEffects.isNotEmpty() -> "from the bank"
        shownEffects.isEmpty() -> "no establishments triggered"
        else -> "activated now"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.width(270.dp)
    ) {
        Text(
            text = "Your outcome",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = subtitle,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        IncomeWithCoin(
            amount = coinDelta,
            isPositive = true
        )

        CardsStack(
            cards = shownEffects.stackedCards(),
            modifier = Modifier.width(155.dp)
        )
    }
}

@Composable
private fun TriggeredPlayerStack(
    effects: List<TriggeredEffectUi>,
    isPositive: Boolean,
) {
    val totalCoins = effects.sumOf { it.totalIncome }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IncomeWithCoin(
            amount = totalCoins,
            isPositive = isPositive
        )

        CardsStack(
            cards = effects.stackedCards(),
            modifier = Modifier.width(155.dp)
        )
    }
}

@Composable
private fun ActivePlayerTransferStack(
    amount: Int,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .width(155.dp)
            .padding(top = 12.dp)
    ) {
        IncomeWithCoin(
            amount = amount,
            isPositive = false
        )
    }
}

@Composable
private fun StadiumGainStack(
    effect: TriggeredEffectUi,
    payingPlayers: List<PlayerCoinState>,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IncomeWithCoin(
            amount = 2 * payingPlayers.size,
            isPositive = true
        )

        CardsStack(
            cards = listOf(effect.cardType),
            modifier = Modifier.width(155.dp)
        )
    }
}

@Composable
private fun StadiumLossStack() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IncomeWithCoin(
            amount = 2,
            isPositive = false
        )

        Box(
            modifier = Modifier
                .width(155.dp)
                .height(175.dp)
        )
    }
}

@Composable
private fun TvStationChoosePlayerView(
    effect: TriggeredEffectUi,
    players: List<PlayerCoinState>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Activated card",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            CardsStack(
                cards = listOf(effect.cardType),
                modifier = Modifier.width(155.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose player to steal 5 coins",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            players.forEach { player ->
                TvStationPlayerChoice(player)
            }
        }
    }
}

@Composable
private fun BusinessCenterChoosePlayerView(
    effect: TriggeredEffectUi,
    players: List<PlayerCoinState>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Activated card",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            CardsStack(
                cards = listOf(effect.cardType),
                modifier = Modifier.width(155.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose opponent to exchange with",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            players.forEach { player ->
                TvStationPlayerChoice(player)
            }
        }
    }
}

@Composable
private fun TvStationPlayerChoice(
    player: PlayerCoinState,
) {
    Row(
        modifier = Modifier
            .width(180.dp)
            .background(Color.White, RoundedCornerShape(13.dp))
            .border(2.dp, ButtonBorderBeige, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = player.displayName,
            color = TextBlueDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Balance",
                color = TextBlueDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            CoinBadge(amount = player.coins, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun TvStationActivePlayerResultStack(
    effect: TriggeredEffectUi,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IncomeWithCoin(
            amount = 5,
            isPositive = true
        )

        CardsStack(
            cards = listOf(effect.cardType),
            modifier = Modifier.width(155.dp)
        )
    }
}

@Composable
private fun TvStationPayingPlayerResultStack(
    receivingPlayerName: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IncomeWithCoin(
            amount = 5,
            isPositive = false
        )

        PayingPlayersList(
            listOf(receivingPlayerName)
        )
    }
}

@Composable
private fun TvStationResultView(
    effect: TriggeredEffectUi,
    players: List<PlayerCoinState>,
    activePlayerId: Int,
    payingPlayerId: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Top
    ) {
        players.forEach { player ->
            val playerId = player.id.toIntOrNull()

            when (playerId) {
                activePlayerId -> {
                    TvStationActivePlayerResultStack(
                        effect = effect
                    )
                }

                payingPlayerId -> {
                    val activePlayerName =
                        players.firstOrNull { it.id.toIntOrNull() == activePlayerId }
                            ?.displayName ?: "Player"

                    TvStationPayingPlayerResultStack(
                        receivingPlayerName = activePlayerName
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .width(155.dp)
                            .height(175.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PayingPlayersList(
    playerNames: List<String>,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        playerNames.forEach { name ->
            PayingPlayerPill(name)
        }
    }
}

@Composable
private fun PayingPlayerPill(
    playerName: String,
) {
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFFC5163D), RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = playerName,
            color = Color(0xFF8A1738),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
@Composable
private fun IncomeWithCoin(
    amount: Int,
    isPositive: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (isPositive) "+" else "-",
            color = if (isPositive) Color(0xFF8BC56A) else Color(0xFFC5163D),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.offset(y = (-4).dp)
        )

        CoinBadge(amount = amount)
    }
}
private data class TriggeredEffectUi(
    val playerId: Int,
    val playerName: String,
    val cardType: CardType,
    val cardName: String,
    val quantity: Int,
    val effectText: String,
    val color: ShopItemColor,
    val resolveOrder: Int,
    val incomeAmount: Int,
    )

private val TriggeredEffectUi.totalIncome: Int
    get() = incomeAmount * quantity

private fun List<TriggeredEffectUi>.stackedCards(): List<CardType> =
    flatMap { effect -> List(effect.quantity.coerceAtLeast(1)) { effect.cardType } }

/**
 * Best-effort local preview of establishments that should visually light up for
 * this roll. The server remains authoritative for real coin movement, including
 * Shopping Mall bonuses, partial red-card payments, and non-coin purple effects.
 */
private fun GameScreenState.triggeredEffects(): List<TriggeredEffectUi> {
    val rolledTotal = diceResult?.sum() ?: return emptyList()
    val activePlayerDatabaseId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull()

    val cardCatalog = shopItems
        .ifEmpty { ShopCatalog.defaultItems }
        .filter { it.purchaseType == PurchaseType.ESTABLISHMENT }
        .mapNotNull { item ->
            val cardType = runCatching { CardType.valueOf(item.type) }.getOrNull()
            cardType?.let { it to item }
        }
        .toMap()

    return players.flatMapIndexed { playerOrder, player ->
        val playerId = player.id.toIntOrNull() ?: return@flatMapIndexed emptyList()

        val ownedCards = playerCards[playerId].orEmpty()

        ownedCards.mapNotNull { ownedCard ->
            val item = cardCatalog[ownedCard.cardType] ?: return@mapNotNull null

            if (ownedCard.quantity <= 0) return@mapNotNull null
            if (rolledTotal !in item.activationNumbers) return@mapNotNull null
            if (!item.color.triggersFor(playerId, activePlayerDatabaseId)) return@mapNotNull null

            TriggeredEffectUi(
                playerId = playerId,
                playerName = player.displayName,
                cardType = ownedCard.cardType,
                cardName = item.displayName,
                quantity = ownedCard.quantity,
                effectText = item.effectText,
                color = item.color,
                resolveOrder = item.color.resolvePriority * 100 + playerOrder,
                incomeAmount = ownedCard.cardType.coinEffectAmount(ownedCards),
            )
        }
    }.sortedWith(
        compareBy<TriggeredEffectUi> { it.resolveOrder }
            .thenBy { it.playerId }
            .thenBy { it.cardName }
    )
}

private fun ShopItemColor.triggersFor(
    ownerPlayerId: Int,
    activePlayerId: Int?,
): Boolean = when (this) {
    ShopItemColor.RED -> activePlayerId != null && ownerPlayerId != activePlayerId
    ShopItemColor.BLUE -> true
    ShopItemColor.GREEN -> ownerPlayerId == activePlayerId
    ShopItemColor.PURPLE -> ownerPlayerId == activePlayerId
    ShopItemColor.LANDMARK -> false
}

private val ShopItemColor.resolvePriority: Int
    get() = when (this) {
        ShopItemColor.RED -> 0
        ShopItemColor.BLUE -> 1
        ShopItemColor.GREEN -> 2
        ShopItemColor.PURPLE -> 3
        ShopItemColor.LANDMARK -> 4
    }

private fun CardType.coinEffectAmount(ownedCards: List<PlayerCardState>): Int = when (this) {
    CardType.WHEAT_FIELD -> 1
    CardType.RANCH -> 1
    CardType.FOREST -> 1
    CardType.MINE -> 5
    CardType.APPLE_ORCHARD -> 3
    CardType.BAKERY -> 1
    CardType.CONVENIENCE_STORE -> 3
    CardType.CHEESE_FACTORY -> 3 * ownedCards.quantityOf(CardType.RANCH)
    CardType.FURNITURE_FACTORY -> 3 * (
        ownedCards.quantityOf(CardType.FOREST) + ownedCards.quantityOf(CardType.MINE)
    )
    CardType.FRUIT_AND_VEGETABLE_MARKET -> 2 * (
        ownedCards.quantityOf(CardType.WHEAT_FIELD) + ownedCards.quantityOf(CardType.APPLE_ORCHARD)
    )
    CardType.CAFE -> 1
    CardType.FAMILY_RESTAURANT -> 2
    CardType.STADIUM -> 2
    CardType.TV_STATION -> 5
    CardType.BUSINESS_CENTER -> 0
}

private fun List<PlayerCardState>.quantityOf(cardType: CardType): Int =
    filter { it.cardType == cardType }.sumOf { it.quantity }

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsRedCardsPreview() {
    ClientTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF5A321E))
                .padding(24.dp)
        ) {
            ResolvingEffectsView(
                state = GameScreenState(
                    gameId = 1,
                    gamePhase = GamePhase.RESOLVE_EFFECTS,
                    connectionStatus = ConnectionStatus.CONNECTED,
                    players = listOf(
                        PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = true),
                        PlayerCoinState("2", "Player2", 4),
                        PlayerCoinState("3", "Player3", 5),
                    ),
                    diceResult = listOf(4, 5),
                    activePlayerId = 1,
                    myUserId = 1,
                    gameStatus = GameStatus.IN_PROGRESS,
                    purchaseState = PurchaseState.IDLE,
                    playerCards = mapOf(
                        2 to listOf(PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 1)),
                        3 to listOf(PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 1)),
                    )
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsBlueCardsPreview() {
    ClientTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF5A321E))
                .padding(24.dp)
        ) {
            ResolvingEffectsView(
                state = GameScreenState(
                    gameId = 1,
                    gamePhase = GamePhase.RESOLVE_EFFECTS,
                    connectionStatus = ConnectionStatus.CONNECTED,
                    players = listOf(
                        PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = true),
                        PlayerCoinState("2", "Player2", 4),
                        PlayerCoinState("3", "Player3", 5),
                    ),
                    diceResult = listOf(1),
                    activePlayerId = 1,
                    myUserId = 1,
                    gameStatus = GameStatus.IN_PROGRESS,
                    purchaseState = PurchaseState.IDLE,
                    playerCards = mapOf(
                        1 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                        2 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 2)),
                        3 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                    )
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsGreenCardsPreview() {
    ClientTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF5A321E))
                .padding(24.dp)
        ) {
            ResolvingEffectsView(
                state = GameScreenState(
                    gameId = 1,
                    gamePhase = GamePhase.RESOLVE_EFFECTS,
                    connectionStatus = ConnectionStatus.CONNECTED,
                    players = listOf(
                        PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = true),
                        PlayerCoinState("2", "Player2", 4),
                    ),
                    diceResult = listOf(2),
                    activePlayerId = 1,
                    myUserId = 1,
                    gameStatus = GameStatus.IN_PROGRESS,
                    purchaseState = PurchaseState.IDLE,
                    playerCards = mapOf(
                        1 to listOf(PlayerCardState(CardType.BAKERY, quantity = 2)),
                        2 to listOf(PlayerCardState(CardType.BAKERY, quantity = 2)),
                    )
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsTvStationResultPreview() {
    ClientTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF5A321E))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            TvStationResultView(
                effect = TriggeredEffectUi(
                    playerId = 1,
                    playerName = "You",
                    cardType = CardType.TV_STATION,
                    cardName = "TV Station",
                    quantity = 1,
                    effectText = "Take 5 coins from any one player, on your turn only.",
                    color = ShopItemColor.PURPLE,
                    resolveOrder = 300,
                    incomeAmount = 5
                ),
                players = listOf(
                    PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = true),
                    PlayerCoinState("2", "Player2", 4),
                    PlayerCoinState("3", "Player3", 5),
                    PlayerCoinState("4", "Player4", 7),
                ),
                activePlayerId = 1,
                payingPlayerId = 3
            )
        }
    }
}
