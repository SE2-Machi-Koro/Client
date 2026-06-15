package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.ShopItemColor
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun ResolvingEffectsView(
    state: GameScreenState,
    modifier: Modifier = Modifier,
    diceAction: (@Composable () -> Unit)? = null,
) {
    val triggeredEffects = remember(state) { state.triggeredEffects() }

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
            activePlayerId = state.activePlayerId,
            modifier = modifier
        )
    }
}
@Composable
private fun TriggeredEffectsBoard(
    effects: List<TriggeredEffectUi>,
    activePlayerId: Int?,
    modifier: Modifier = Modifier,
) {
    val redEffects = effects.filter { it.color == ShopItemColor.RED }
    val otherEffects = effects.filter { it.color != ShopItemColor.RED }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(46.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        redEffects
            .groupBy { it.playerId }
            .forEach { (_, playerEffects) ->
                TriggeredPlayerStack(
                    effects = playerEffects,
                    isPositive = true
                )
            }

        otherEffects
            .groupBy { it.playerId }
            .forEach { (playerId, playerEffects) ->
                TriggeredPlayerStack(
                    effects = playerEffects,
                    isPositive = playerId == activePlayerId
                )
            }

        if (redEffects.isNotEmpty()) {
            ActivePlayerTransferStack(
                payingPlayerNames = redEffects.map { it.playerName }.distinct(),
                amount = redEffects.sumOf { it.incomeAmount * it.quantity }
            )
        }
    }
}

@Composable
private fun TriggeredPlayerStack(
    effects: List<TriggeredEffectUi>,
    isPositive: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IncomeWithCoin(
            amount = effects.sumOf { it.incomeAmount * it.quantity },
            isPositive = isPositive
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-18).dp),
        ) {
            effects.forEach { effect ->
                Box(contentAlignment = Alignment.TopStart) {
                    CardArtImage(
                        drawableResId = ShopImageResolver.drawableForCardType(effect.cardType),
                        width = 155.dp,
                        height = 175.dp,
                    )

                    CardQuantityIndicator(
                        quantity = effect.quantity,
                        isVisible = effect.quantity > 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePlayerTransferStack(
    payingPlayerNames: List<String>,
    amount: Int,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(bottom = 111.dp)
    ) {
        IncomeWithCoin(
            amount = amount,
            isPositive = false
        )

        PayingPlayersList(payingPlayerNames)
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
            text = "${if (isPositive) "+" else "-"}$amount x",
            color = if (isPositive) Color(0xFF8BC56A) else Color(0xFFC5163D),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .offset(y = (-4).dp)
        )

        CoinBadge(amount = 1)
    }
}

@Composable
private fun CoinBadge(
    amount: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(36.dp)) {
        Image(
            painter = painterResource(R.drawable.coin),
            contentDescription = "Coin",
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )

        Text(
            text = amount.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF744300),
            fontSize = 18.sp,
            modifier = Modifier
                .offset(y = (-4).dp)
                .align(Alignment.Center)
        )
    }
}


private fun previewIncomeText(
    effects: List<TriggeredEffectUi>,
    isActivePlayer: Boolean,
): String {
    val amount = effects.sumOf { it.quantity }
    return if (isActivePlayer) "+$amount x" else "-$amount x"
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

        playerCards[playerId].orEmpty().mapNotNull { ownedCard ->
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
                incomeAmount = ownedCard.cardType.coinEffectAmount(),
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

private fun CardType.coinEffectAmount(): Int = when (this) {
    CardType.CAFE -> 1
    CardType.FAMILY_RESTAURANT -> 3
    CardType.BAKERY -> 1
    CardType.CONVENIENCE_STORE -> 3
    CardType.WHEAT_FIELD -> 1
    CardType.FOREST -> 1
    else -> 1
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsViewPreview() {
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
                        PlayerCoinState(
                            id = "1",
                            displayName = "You",
                            coins = 6,
                            isCurrentPlayer = true,
                            isActivePlayer = true
                        ),
                        PlayerCoinState(
                            id = "2",
                            displayName = "Player2",
                            coins = 4,
                            isCurrentPlayer = false,
                            isActivePlayer = false
                        ),
                        PlayerCoinState(
                            id = "3",
                            displayName = "Player3",
                            coins = 5,
                            isCurrentPlayer = false,
                            isActivePlayer = false
                        )
                    ),
                    diceResult = listOf(4, 5),
                    activePlayerId = 1,
                    myUserId = 1,
                    gameStatus = GameStatus.IN_PROGRESS,
                    purchaseState = PurchaseState.IDLE,
                    playerCards = mapOf(
                        1 to listOf(
                            PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 2),
                        ),
                        2 to listOf(
                            PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 1),
                        ),
                        3 to listOf(
                            PlayerCardState(CardType.FAMILY_RESTAURANT, quantity = 1),
                        )
                    ),
                    playerLandmarks = mapOf(
                        1 to listOf(
                            PlayerLandmarkState(
                                landmarkType = LandmarkType.RADIO_TOWER,
                                isBuilt = true
                            )
                        )
                    )
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}