package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.theme.CardBlueBackground
import com.machikoro.client.ui.theme.CardBlueText
import com.machikoro.client.ui.theme.CardGreenBackground
import com.machikoro.client.ui.theme.CardGreenText
import com.machikoro.client.ui.theme.CardPurpleBackground
import com.machikoro.client.ui.theme.CardPurpleText
import com.machikoro.client.ui.theme.CardRedBackground
import com.machikoro.client.ui.theme.CardRedText
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.Highlight
import com.machikoro.client.ui.theme.PanelBackgroundTransparent
import com.machikoro.client.ui.theme.PanelBorder
import com.machikoro.client.ui.theme.TextBlueDark

private val EFFECT_PANEL_SHAPE = RoundedCornerShape(8.dp)
private val EFFECT_ROW_SHAPE = RoundedCornerShape(8.dp)

@Composable
fun ResolvingEffectsView(
    state: GameScreenState,
    modifier: Modifier = Modifier,
    diceAction: (@Composable () -> Unit)? = null,
) {
    val triggeredEffects = remember(state) { state.triggeredEffects() }
    val activeEffect = triggeredEffects.firstOrNull()

    Column(
        modifier = modifier
            .widthIn(max = 720.dp)
            .clip(EFFECT_PANEL_SHAPE)
            .background(PanelBackgroundTransparent)
            .border(2.dp, PanelBorder, EFFECT_PANEL_SHAPE)
            .padding(16.dp)
            .semantics {
                contentDescription = "Resolving effects for triggered establishments"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        state.diceResult?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DiceResultDisplay(dice = it)
                diceAction?.invoke()
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("Resolving effects")
            Text(
                text = activeEffect?.let { "Now: ${it.playerName} - ${it.cardName}" }
                    ?: "No triggered establishments",
                color = TextBlueDark,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (triggeredEffects.isEmpty()) {
            EmptyTriggeredEffects()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(
                    items = triggeredEffects,
                    key = { "${it.playerId}-${it.cardType.name}" }
                ) { effect ->
                    TriggeredEffectRow(
                        effect = effect,
                        isActive = effect == activeEffect,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTriggeredEffects() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EFFECT_ROW_SHAPE)
            .background(Color.White.copy(alpha = 0.78f))
            .border(1.dp, PanelBorder, EFFECT_ROW_SHAPE)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Waiting for the server to finish resolving this roll.",
            color = TextBlueDark,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TriggeredEffectRow(
    effect: TriggeredEffectUi,
    isActive: Boolean,
) {
    val tint = effect.color.palette()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EFFECT_ROW_SHAPE)
            .background(if (isActive) Highlight else Color.White.copy(alpha = 0.86f))
            .border(2.dp, if (isActive) tint.text else tint.background, EFFECT_ROW_SHAPE)
            .padding(10.dp)
            .semantics {
                contentDescription =
                    "${effect.playerName}, ${effect.cardName}, ${effect.quantity} owned, ${effect.effectText}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.TopStart) {
            CardArtImage(
                drawableResId = ShopImageResolver.drawableForCardType(effect.cardType),
                width = 72.dp,
                height = 86.dp,
            )
            CardQuantityIndicator(
                quantity = effect.quantity,
                isVisible = effect.quantity > 1,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EffectColorPill(effect.color, tint)
                Text(
                    text = effect.playerName,
                    color = TextBlueDark,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Text(
                text = effect.cardName,
                color = TextBlueDark,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = effect.effectText,
                color = TextBlueDark,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EffectColorPill(
    color: ShopItemColor,
    tint: EffectColorPalette,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.background)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = color.displayName,
            color = tint.text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
        )
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
)

private data class EffectColorPalette(
    val background: Color,
    val text: Color,
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
            )
        }
    }.sortedWith(
        compareBy<TriggeredEffectUi> { it.resolveOrder }
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

private val ShopItemColor.displayName: String
    get() = when (this) {
        ShopItemColor.RED -> "Opponent"
        ShopItemColor.BLUE -> "Any turn"
        ShopItemColor.GREEN -> "Active"
        ShopItemColor.PURPLE -> "Major"
        ShopItemColor.LANDMARK -> "Landmark"
    }

private fun ShopItemColor.palette(): EffectColorPalette = when (this) {
    ShopItemColor.BLUE -> EffectColorPalette(CardBlueBackground, CardBlueText)
    ShopItemColor.GREEN -> EffectColorPalette(CardGreenBackground, CardGreenText)
    ShopItemColor.RED -> EffectColorPalette(CardRedBackground, CardRedText)
    ShopItemColor.PURPLE -> EffectColorPalette(CardPurpleBackground, CardPurpleText)
    ShopItemColor.LANDMARK -> EffectColorPalette(PanelBackgroundTransparent, TextBlueDark)
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun ResolvingEffectsViewPreview() {
    ClientTheme {
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
                        displayName = "Mia",
                        coins = 4,
                        isCurrentPlayer = false,
                        isActivePlayer = false
                    )
                ),
                diceResult = listOf(1, 2),
                activePlayerId = 11,
                myUserId = 11,
                gameStatus = GameStatus.IN_PROGRESS,
                purchaseState = PurchaseState.IDLE,
                playerCards = mapOf(
                    1 to listOf(
                        PlayerCardState(CardType.BAKERY, quantity = 2),
                    ),
                    2 to listOf(
                        PlayerCardState(CardType.CAFE, quantity = 1),
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
            modifier = Modifier
                .background(Color(0xFF5A321E))
                .padding(24.dp)
        )
    }
}