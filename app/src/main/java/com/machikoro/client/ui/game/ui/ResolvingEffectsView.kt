package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.ShopItemColor
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.domain.model.state.triggeredEstablishmentsForCurrentRoll
import com.machikoro.client.ui.game.ui.resolving_effects.CardsStack
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.TextBlueDark

private const val CUP_ESTABLISHMENT_TYPE = "CUP"
private const val BREAD_ESTABLISHMENT_TYPE = "BREAD"

@Composable
fun ResolvingEffectsView(
    state: GameScreenState,
    modifier: Modifier = Modifier,
    onEffectCardPositioned: (playerId: Int, center: Offset) -> Unit = { _, _ -> },
) {
    val triggeredEffects = remember(state) { state.triggeredEffects() }

    if (triggeredEffects.isEmpty()) {
        BasicText(
            "No establishments triggered",
            modifier = modifier.offset(y = 5.dp)
        )
        return
    }

    TriggeredEffectsBoard(
        effects = triggeredEffects,
        players = state.players,
        onEffectCardPositioned = onEffectCardPositioned,
        modifier = modifier
    )
}

internal fun GameScreenState.withResolvingEffectsPreviewCoins(): GameScreenState {
    if (gamePhase != GamePhase.RESOLVE_EFFECTS) return this

    val triggeredEffects = triggeredEffects()
    if (triggeredEffects.isEmpty()) return this

    val activePlayerId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull()
    val activePlayerName = players.firstOrNull { it.id.toIntOrNull() == activePlayerId }
        ?.displayName ?: "Active player"
    val outcomeItems = buildOutcomeItems(
        effects = triggeredEffects,
        players = players,
        activePlayerId = activePlayerId,
        activePlayerName = activePlayerName
    )
    if (outcomeItems.isEmpty()) return this

    val coinDeltasByPlayer = outcomeItems
        .groupBy { it.playerId }
        .mapValues { (_, outcomes) ->
            outcomes.sumOf { if (it.isPositive) it.amount else -it.amount }
        }

    return copy(
        players = players.map { player ->
            val playerId = player.id.toIntOrNull()
            val coinDelta = coinDeltasByPlayer[playerId] ?: return@map player
            player.copy(coins = (player.coins + coinDelta).coerceAtLeast(0))
        }
    )
}

@Composable
private fun TriggeredEffectsBoard(
    effects: List<TriggeredEffectUi>,
    players: List<PlayerCoinState>,
    onEffectCardPositioned: (playerId: Int, center: Offset) -> Unit,
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
            .offset(x = (-6).dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top
    ) {
        val playersWithOutcomes = players.mapNotNull { player ->
            val playerId = player.id.toIntOrNull()
            val playerItems = outcomeItems.filter { it.playerId == playerId }
            player.takeIf { playerItems.isNotEmpty() }?.let { it to playerItems }
        }

        playersWithOutcomes.forEachIndexed { playerIndex, (_, playerItems) ->
            PlayerOutcomeColumn(
                outcomes = playerItems,
                initialDelayMillis = playerIndex * 180,
                onEffectCardPositioned = onEffectCardPositioned
            )
        }
    }
}

@Composable
private fun PlayerOutcomeColumn(
    outcomes: List<PlayerOutcomeUi>,
    initialDelayMillis: Int = 0,
    onEffectCardPositioned: (playerId: Int, center: Offset) -> Unit = { _, _ -> },
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(140.dp)
    ) {
        outcomes.forEachIndexed { outcomeIndex, outcome ->
            OutcomeStack(
                outcome = outcome,
                initialDelayMillis = initialDelayMillis + outcomeIndex * 250,
                onEffectCardPositioned = onEffectCardPositioned
            )
        }
    }
}

@Composable
private fun OutcomeStack(
    outcome: PlayerOutcomeUi,
    initialDelayMillis: Int,
    onEffectCardPositioned: (playerId: Int, center: Offset) -> Unit,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (outcome.amount != 0) {
            IncomeWithCoin(
                amount = outcome.amount,
                isPositive = outcome.isPositive
            )
        }

        outcome.playerName?.let { playerName ->
            OutcomePlayerPill(
                playerName = playerName,
                isReceiver = outcome.isPositive
            )
        }

        if (outcome.cards.isNotEmpty()) {
            CardsStack(
                cards = outcome.cards,
                modifier = Modifier.width(140.dp),
                initialDelayMillis = initialDelayMillis,
                onPositioned = { center ->
                    onEffectCardPositioned(outcome.playerId, center)
                }
            )
        }
    }
}

@Composable
private fun OutcomePlayerPill(
    playerName: String,
    isReceiver: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isReceiver) Color(0xFF238B45) else Color(0xFFC5163D)

    Row(
        modifier = modifier
            .width(134.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.login_user_icon),
            contentDescription = null,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = playerName,
            color = borderColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, widthDp = 170, heightDp = 110)
@Composable
private fun OutcomePlayerPillsPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutcomePlayerPill(
            playerName = "Player 1",
            isReceiver = false
        )
        OutcomePlayerPill(
            playerName = "Player 2",
            isReceiver = true
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
            .width(140.dp)
            .height(165.dp),
        contentAlignment = Alignment.Center
    ) {
        CardsStack(
            cards = listOf(cardType),
            modifier = Modifier.width(140.dp)
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

// TODO: Wire TV Station and Business Center interaction states once the server exposes
// selectable purple-card effects. For now these composables are preview scaffolding.
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
            players.filterNot { it.isActivePlayer }.forEach { player ->
                PlayerChoicePill(
                    name = player.displayName,
                    coins = player.coins,
                    enabled = player.coins > 0,
                    onClick = {
                        // TODO: Handle TV Station player selection when server action is available.
                    }
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

            /*PurpleCardsQueueView(
                purpleCards = listOf(CardType.BUSINESS_CENTER),
                activeCard = CardType.BUSINESS_CENTER
            )*/
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
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(155.dp)
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
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
        CoinForEffects(
            amount = amount,
            isPositive
        )
    }
}

private data class PlayerOutcomeUi(
    val playerId: Int,
    val amount: Int,
    val isPositive: Boolean,
    val cards: List<CardType>,
    val playerName: String? = null,
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

    val stadiumAmountPerOpponent = stadiumEffects.sumOf { it.incomeAmount * it.quantity }

    val stadiumLosses = if (stadiumAmountPerOpponent > 0) {
        opponents.mapNotNull { opponent ->
            val opponentId = opponent.id.toIntOrNull() ?: return@mapNotNull null
            val paidAmount = minOf(stadiumAmountPerOpponent, opponent.coins)
            if (paidAmount <= 0) return@mapNotNull null

            PlayerOutcomeUi(
                playerId = opponentId,
                amount = paidAmount,
                isPositive = false,
                cards = emptyList(),
                playerName = opponent.displayName
            )
        }
    } else {
        emptyList()
    }

    val stadiumGainAmount = stadiumLosses.sumOf { it.amount }

    val stadiumGain = if (stadiumGainAmount > 0 && stadiumEffects.isNotEmpty()) {
        listOf(
            PlayerOutcomeUi(
                playerId = activePlayerId,
                amount = stadiumGainAmount,
                isPositive = true,
                cards = stadiumEffects.stackedCards(),
                playerName = activePlayerName
            )
        )
    } else {
        emptyList()
    }

    var remainingActivePlayerCoins = activePlayer?.coins?.coerceAtLeast(0) ?: 0

    data class RedPayment(
        val effect: TriggeredEffectUi,
        val amount: Int,
    )

    val redPayments = redEffects.mapNotNull { effect ->
        val paidAmount = minOf(effect.totalIncome, remainingActivePlayerCoins)
        remainingActivePlayerCoins -= paidAmount
        if (paidAmount <= 0) return@mapNotNull null
        RedPayment(effect, paidAmount)
    }

    val redReceiverOutcomes = redPayments.map { payment ->
        PlayerOutcomeUi(
            playerId = payment.effect.playerId,
            amount = payment.amount,
            isPositive = true,
            cards = payment.effect.stackedCards(),
            playerName = payment.effect.playerName
        )
    }

    val activeRedPayments = redPayments.map { payment ->
        PlayerOutcomeUi(
            playerId = activePlayerId,
            amount = payment.amount,
            isPositive = false,
            cards = emptyList(),
            playerName = activePlayerName
        )
    }

    val bankIncomeOutcomes = regularIncomeEffects.map { effect ->
        PlayerOutcomeUi(
            playerId = effect.playerId,
            amount = effect.totalIncome,
            isPositive = true,
            cards = effect.stackedCards(),
            playerName = effect.playerName
        )
    }

    // TV Station steals 5 coins from a random opponent; pick the richest as a local preview guess
    val tvStationEffects = effects.filter { it.cardType == CardType.TV_STATION }
    val tvStationSteal = tvStationEffects.sumOf { it.incomeAmount * it.quantity }
    val tvStationTarget = if (tvStationSteal > 0) opponents.maxByOrNull { it.coins } else null
    val tvStationLoss = tvStationTarget?.let { opponent ->
        val opponentId = opponent.id.toIntOrNull() ?: return@let null
        val paid = minOf(tvStationSteal, opponent.coins)
        if (paid <= 0) return@let null
        PlayerOutcomeUi(
            playerId = opponentId,
            amount = paid,
            isPositive = false,
            cards = emptyList(),
            playerName = opponent.displayName
        )
    }
    val tvStationGain = tvStationLoss?.let { loss ->
        PlayerOutcomeUi(
            playerId = activePlayerId,
            amount = loss.amount,
            isPositive = true,
            cards = tvStationEffects.stackedCards(),
            playerName = activePlayerName
        )
    }

    return activeRedPayments +
            redReceiverOutcomes +
            stadiumGain +
            stadiumLosses +
            bankIncomeOutcomes +
            listOfNotNull(tvStationGain) +
            listOfNotNull(tvStationLoss)
}

/**
 * Best-effort local preview of establishments that should visually light up for
 * this roll. The server remains authoritative for real coin movement; this
 * preview mirrors Shopping Mall bonuses and caps theft at visible coin balances.
 * TV Station picks the richest opponent as a local preview guess; the server result is authoritative.
 */
private fun GameScreenState.triggeredEffects(): List<TriggeredEffectUi> {
    return triggeredEstablishmentsForCurrentRoll().map { triggered ->
        val item = triggered.item
        val ownedCard = triggered.ownedCard

        TriggeredEffectUi(
            playerId = triggered.playerId,
            playerName = triggered.playerName,
            cardType = ownedCard.cardType,
            cardName = item.displayName,
            quantity = ownedCard.quantity,
            effectText = item.effectText,
            color = item.color,
            resolveOrder = item.color.resolvePriority * 100 + triggered.playerOrder,
            incomeAmount = ownedCard.cardType.coinEffectAmount(triggered.ownedCards) +
                shoppingMallBonus(triggered.playerId, item),
        )
    }.sortedWith(
        compareBy<TriggeredEffectUi> { it.resolveOrder }
            .thenBy { it.playerId }
            .thenBy { it.cardName }
    )
}

@Composable
private fun CoinForEffects(
    amount: Int? = null,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(42.dp)) {
        Image(
            painter = painterResource(R.drawable.coin),
            contentDescription = "Coin",
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )
        Text(
            text = (if (isPositive) "+" else "-") +
                    amount?.toString().orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF744300),
            fontSize = 20.sp,
            modifier = Modifier
                .offset(y = (-4).dp)
                .align(Alignment.Center)
        )
    }
}
private fun GameScreenState.shoppingMallBonus(
    playerId: Int,
    item: ShopItem,
): Int {
    if (
        item.establishmentType != CUP_ESTABLISHMENT_TYPE &&
        item.establishmentType != BREAD_ESTABLISHMENT_TYPE
    ) {
        return 0
    }
    val hasShoppingMall = playerLandmarks[playerId].orEmpty().any {
        it.landmarkType == LandmarkType.SHOPPING_MALL && it.isBuilt
    }
    return if (hasShoppingMall) 1 else 0
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
}

private fun TriggeredEffectUi.stackedCards(): List<CardType> =
    List(quantity.coerceAtLeast(1)) { cardType }
private fun List<PlayerCardState>.quantityOf(cardType: CardType): Int =
    filter { it.cardType == cardType }.sumOf { it.quantity }

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsNoTriggeredCardsPreview() {
    ClientTheme {
        ResolvingEffectsPreviewContainer {
            ResolvingEffectsView(
                state = resolvingEffectsPreviewState(
                    myUserId = 1,
                    activePlayerId = 1,
                    diceResult = listOf(6, 6),
                    playerCards = mapOf(
                        1 to listOf(
                            PlayerCardState(CardType.BAKERY, quantity = 1),
                            PlayerCardState(CardType.CAFE, quantity = 1),
                        ),
                        2 to listOf(
                            PlayerCardState(CardType.WHEAT_FIELD, quantity = 1),
                        )
                    )
                )
            )
        }
    }
}

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
