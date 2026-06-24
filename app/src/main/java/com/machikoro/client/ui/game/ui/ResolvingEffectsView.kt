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
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.machikoro.client.R

@Composable
fun ResolvingEffectsView(
    state: GameScreenState,
    modifier: Modifier = Modifier,
) {
    val triggeredEffects = remember(state) { state.triggeredEffects() }

    TriggeredEffectsBoard(
        effects = triggeredEffects,
        players = state.players,
        modifier = modifier
    )
}

@Composable
private fun TriggeredEffectsBoard(
    effects: List<TriggeredEffectUi>,
    players: List<PlayerCoinState>,
    modifier: Modifier = Modifier,
) {
    val activePlayerId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull()
    val activePlayerName = players.firstOrNull { it.id.toIntOrNull() == activePlayerId }
        ?.displayName ?: "Active player"

    val outcomeItems = remember(effects, players) {
        buildOutcomeItems(
            effects = effects,
            players = players,
            activePlayerId = activePlayerId,
            activePlayerName = activePlayerName
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .offset(x = (-10).dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top
    ) {
        players.forEach { player ->
            val playerId = player.id.toIntOrNull()
            val playerItems = outcomeItems.filter { it.playerId == playerId }

            PlayerOutcomeColumn(
                outcomes = playerItems
            )
        }
    }
}

@Composable
private fun PlayerOutcomeColumn(
    outcomes: List<PlayerOutcomeUi>,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(155.dp)
    ) {
        if (outcomes.isEmpty()) {
            IncomeWithCoin(amount = 0, isPositive = true)
            Box(
                modifier = Modifier
                    .width(155.dp)
                    .height(175.dp)
            )
        } else {
            outcomes.forEach { outcome ->
                OutcomeStack(outcome)
            }
        }
    }
}

@Composable
private fun OutcomeStack(
    outcome: PlayerOutcomeUi,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IncomeWithCoin(
            amount = outcome.amount,
            isPositive = outcome.isPositive
        )

        CardsStack(
            cards = outcome.cards,
            modifier = Modifier.width(155.dp)
        )
    }
}

@Composable
private fun PurpleCardsQueueView(
    purpleCards: List<CardType>,
    activeCard: CardType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        purpleCards.forEach { card ->
            FramedEffectCard(
                cardType = card,
                isSelected = card == activeCard
            )
        }
    }
}

@Composable
private fun FramedEffectCard(
    cardType: CardType,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(155.dp)
            .height(175.dp),
        contentAlignment = Alignment.Center
    ) {
        CardsStack(
            cards = listOf(cardType),
            modifier = Modifier.width(155.dp)
        )

        if (isSelected) {
            Image(
                painter = painterResource(R.drawable.card_frame),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .matchParentSize()
                    .padding(
                        start = 6.dp,
                        end = 6.dp,
                        bottom = 9.dp
                    )
            )
        }
    }
}

@Composable
private fun PurpleTvStationChoiceView(
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
                text = "Choose player",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            PurpleCardsQueueView(
                purpleCards = listOf(CardType.TV_STATION),
                activeCard = CardType.TV_STATION
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            players.forEach { player ->
                PlayerChoicePill(
                    name = player.displayName,
                    coins = player.coins,
                    enabled = player.coins >= 5
                )
            }
        }
    }
}

@Composable
private fun PurpleTvStationOutcomeView(
    players: List<PlayerCoinState>,
    activePlayerId: Int,
    payingPlayerId: Int,
    modifier: Modifier = Modifier,
) {
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

            PlayerOutcomeColumn(
                outcomes = when (playerId) {
                    activePlayerId -> listOf(
                        PlayerOutcomeUi(
                            playerId = activePlayerId,
                            amount = 5,
                            isPositive = true,
                            cards = listOf(CardType.TV_STATION)
                        )
                    )

                    payingPlayerId -> listOf(
                        PlayerOutcomeUi(
                            playerId = payingPlayerId,
                            amount = 5,
                            isPositive = false,
                            cards = emptyList()
                        )
                    )

                    else -> emptyList()
                }
            )
        }
    }
}

@Composable
private fun BusinessCenterChooseOwnCardView(
    ownCards: List<CardType>,
    selectedCard: CardType?,
    opponentName: String,
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
                text = "You",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            PurpleCardsQueueView(
                purpleCards = listOf(CardType.BUSINESS_CENTER),
                activeCard = CardType.BUSINESS_CENTER
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Choose one of your cards to swap with $opponentName",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                ownCards.forEach { card ->
                    FramedEffectCard(
                        cardType = card,
                        isSelected = card == selectedCard
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessCenterChooseOpponentCardView(
    opponentName: String,
    ownSelectedCard: CardType,
    opponentCards: List<CardType>,
    opponentSelectedCard: CardType?,
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
                text = "Your card",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            CardsStack(
                cards = listOf(ownSelectedCard),
                modifier = Modifier.width(155.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$opponentName's cards",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                opponentCards.forEach { card ->
                    FramedEffectCard(
                        cardType = card,
                        isSelected = card == opponentSelectedCard
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessCenterOutcomeView(
    ownReceivedCard: CardType,
    opponentReceivedCard: CardType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "You received",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            CardsStack(
                cards = listOf(ownReceivedCard),
                modifier = Modifier.width(155.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Opponent received",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            CardsStack(
                cards = listOf(opponentReceivedCard),
                modifier = Modifier.width(155.dp)
            )
        }
    }
}

@Composable
private fun PlayerChoicePill(
    name: String,
    coins: Int,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .width(155.dp)
            .background(
                color = if (enabled) Color.White else Color(0xFFD7D0CA),
                shape = RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            color = if (enabled) TextBlueDark else Color(0xFF7A6F69),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        CoinBadge(amount = coins, modifier = Modifier.offset(y = 4.dp))
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

private data class PlayerOutcomeUi(
    val playerId: Int,
    val amount: Int,
    val isPositive: Boolean,
    val cards: List<CardType>,
    val fromPlayerName: String? = null,
)
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

private fun buildOutcomeItems(
    effects: List<TriggeredEffectUi>,
    players: List<PlayerCoinState>,
    activePlayerId: Int?,
    activePlayerName: String,
): List<PlayerOutcomeUi> {
    if (activePlayerId == null) return emptyList()

    val activePlayer = players.firstOrNull { it.id.toIntOrNull() == activePlayerId }
    val opponents = players.filter { it.id.toIntOrNull() != activePlayerId }

    val stadiumEffects = effects.filter { it.cardType == CardType.STADIUM }
    val redEffects = effects.filter { it.color == ShopItemColor.RED }
    val regularIncomeEffects = effects.filter {
        it.color == ShopItemColor.BLUE || it.color == ShopItemColor.GREEN
    }

    val stadiumGain = stadiumEffects.map { effect ->
        PlayerOutcomeUi(
            playerId = activePlayerId,
            amount = effect.incomeAmount * opponents.size * effect.quantity,
            isPositive = true,
            cards = effect.stackedCards()
        )
    }

    val stadiumLosses = if (stadiumEffects.isNotEmpty()) {
        opponents.mapNotNull { opponent ->
            val opponentId = opponent.id.toIntOrNull() ?: return@mapNotNull null
            PlayerOutcomeUi(
                playerId = opponentId,
                amount = stadiumEffects.sumOf { it.incomeAmount * it.quantity },
                isPositive = false,
                cards = emptyList(),
                fromPlayerName = activePlayerName
            )
        }
    } else {
        emptyList()
    }

    val redReceiverOutcomes = redEffects.map { effect ->
        PlayerOutcomeUi(
            playerId = effect.playerId,
            amount = effect.totalIncome,
            isPositive = true,
            cards = effect.stackedCards(),
            fromPlayerName = activePlayerName
        )
    }

    val redTotalPaidByActivePlayer = redEffects.sumOf { it.totalIncome }
    val activeRedPayment = if (redTotalPaidByActivePlayer > 0 && activePlayer != null) {
        listOf(
            PlayerOutcomeUi(
                playerId = activePlayerId,
                amount = redTotalPaidByActivePlayer,
                isPositive = false,
                cards = emptyList()
            )
        )
    } else {
        emptyList()
    }

    val bankIncomeOutcomes = regularIncomeEffects.map { effect ->
        PlayerOutcomeUi(
            playerId = effect.playerId,
            amount = effect.totalIncome,
            isPositive = true,
            cards = effect.stackedCards()
        )
    }

    val unresolvedPurpleOutcomes = effects
        .filter {
            it.color == ShopItemColor.PURPLE &&
                    it.cardType != CardType.STADIUM
        }
        .map { effect ->
            PlayerOutcomeUi(
                playerId = effect.playerId,
                amount = effect.totalIncome,
                isPositive = true,
                cards = effect.stackedCards()
            )
        }

    return activeRedPayment +
            redReceiverOutcomes +
            stadiumGain +
            stadiumLosses +
            bankIncomeOutcomes
}

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

private fun TriggeredEffectUi.stackedCards(): List<CardType> =
    List(quantity.coerceAtLeast(1)) { cardType }
private fun List<PlayerCardState>.quantityOf(cardType: CardType): Int =
    filter { it.cardType == cardType }.sumOf { it.quantity }

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsRedCardsActivePlayerPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 1,
                    activePlayerId = 1,
                    diceResult = listOf(4, 5),
                    playerCards = mapOf(
                        2 to listOf(PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 1)),
                        3 to listOf(PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 2)),
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsRedCardsPassivePlayerPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 2,
                    activePlayerId = 1,
                    diceResult = listOf(4, 5),
                    playerCards = mapOf(
                        2 to listOf(PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 1)),
                        3 to listOf(PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 2)),
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsBlueCardsActivePlayerPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 1,
                    activePlayerId = 1,
                    diceResult = listOf(1),
                    playerCards = mapOf(
                        1 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                        2 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 2)),
                        3 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsBlueCardsPassivePlayerPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 2,
                    activePlayerId = 1,
                    diceResult = listOf(1),
                    playerCards = mapOf(
                        1 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                        2 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 2)),
                        3 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsGreenCardsActivePlayerPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 1,
                    activePlayerId = 1,
                    diceResult = listOf(2),
                    playerCards = mapOf(
                        1 to listOf(PlayerCardState(CardType.BAKERY, quantity = 2)),
                        2 to listOf(PlayerCardState(CardType.BAKERY, quantity = 2)),
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsGreenCardsPassivePlayerPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 2,
                    activePlayerId = 1,
                    diceResult = listOf(2),
                    playerCards = mapOf(
                        1 to listOf(PlayerCardState(CardType.BAKERY, quantity = 2)),
                        2 to listOf(PlayerCardState(CardType.BAKERY, quantity = 2)),
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleStadiumActivePlayerPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 1,
                    activePlayerId = 1,
                    diceResult = listOf(6),
                    playerCards = mapOf(
                        1 to listOf(PlayerCardState(CardType.STADIUM, quantity = 1))
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleStadiumPassivePlayerPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 2,
                    activePlayerId = 1,
                    diceResult = listOf(6),
                    playerCards = mapOf(
                        1 to listOf(PlayerCardState(CardType.STADIUM, quantity = 1))
                    )
                )
            )
        }
    }
}

@Composable
private fun ResolvingEffectsPreviewContainer(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .background(Color(0xFF5A321E))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleStadiumOutcomePreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 1,
                    activePlayerId = 1,
                    diceResult = listOf(6),
                    playerCards = mapOf(
                        1 to listOf(PlayerCardState(CardType.STADIUM, quantity = 1))
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleTvStationChoicePreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            PurpleTvStationChoiceView(
                players = previewPlayersForResolvingEffects(
                    myUserId = 1,
                    activePlayerId = 1
                ).filter { it.id != "1" }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleTvStationChoiceWithDisabledPlayerPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            PurpleTvStationChoiceView(
                players = listOf(
                    PlayerCoinState(id = "2", displayName = "Player2", coins = 4),
                    PlayerCoinState(id = "3", displayName = "Player3", coins = 7),
                    PlayerCoinState(id = "4", displayName = "Player4", coins = 0),
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleTvStationOutcomePreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            PurpleTvStationOutcomeView(
                players = previewPlayersForResolvingEffects(
                    myUserId = 1,
                    activePlayerId = 1
                ),
                activePlayerId = 1,
                payingPlayerId = 3
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleBusinessCenterChooseOwnCardPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            BusinessCenterChooseOwnCardView(
                ownCards = listOf(
                    CardType.WHEAT_FIELD,
                    CardType.BAKERY,
                    CardType.FOREST,
                ),
                selectedCard = CardType.BAKERY,
                opponentName = "Player3"
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleBusinessCenterChooseOpponentCardPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            BusinessCenterChooseOpponentCardView(
                opponentName = "Player3",
                ownSelectedCard = CardType.BAKERY,
                opponentCards = listOf(
                    CardType.RANCH,
                    CardType.CAFE,
                    CardType.CONVENIENCE_STORE,
                ),
                opponentSelectedCard = CardType.RANCH
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleBusinessCenterOutcomePreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            BusinessCenterOutcomeView(
                ownReceivedCard = CardType.RANCH,
                opponentReceivedCard = CardType.BAKERY
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsPurpleMultipleCardsQueuePreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            PurpleCardsQueueView(
                purpleCards = listOf(
                    CardType.STADIUM,
                    CardType.TV_STATION,
                    CardType.BUSINESS_CENTER
                ),
                activeCard = CardType.TV_STATION
            )
        }
    }
}

private fun resolvingEffectsPreviewState(
    myUserId: Int,
    activePlayerId: Int,
    diceResult: List<Int>,
    playerCards: Map<Int, List<PlayerCardState>>,
): GameScreenState {
    return GameScreenState(
        gameId = 1,
        gamePhase = GamePhase.RESOLVE_EFFECTS,
        connectionStatus = ConnectionStatus.CONNECTED,
        players = previewPlayersForResolvingEffects(
            myUserId = myUserId,
            activePlayerId = activePlayerId
        ),
        diceResult = diceResult,
        activePlayerId = activePlayerId,
        myUserId = myUserId,
        gameStatus = GameStatus.IN_PROGRESS,
        purchaseState = PurchaseState.IDLE,
        playerCards = playerCards,
        customDisplayText = "Resolving effects"
    )
}

private fun previewPlayersForResolvingEffects(
    myUserId: Int,
    activePlayerId: Int,
): List<PlayerCoinState> {
    return listOf(
        PlayerCoinState(
            id = "1",
            displayName = "You",
            coins = 6,
            isCurrentPlayer = myUserId == 1,
            isActivePlayer = activePlayerId == 1
        ),
        PlayerCoinState(
            id = "2",
            displayName = "Player2",
            coins = 4,
            isCurrentPlayer = myUserId == 2,
            isActivePlayer = activePlayerId == 2
        ),
        PlayerCoinState(
            id = "3",
            displayName = "Player3",
            coins = 5,
            isCurrentPlayer = myUserId == 3,
            isActivePlayer = activePlayerId == 3
        ),
        PlayerCoinState(
            id = "4",
            displayName = "Player4",
            coins = 7,
            isCurrentPlayer = myUserId == 4,
            isActivePlayer = activePlayerId == 4
        ),
    )
}