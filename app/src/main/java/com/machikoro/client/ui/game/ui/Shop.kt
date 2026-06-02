package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.ui.game.GameScreen
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PanelBorder
import com.machikoro.client.ui.theme.PrimaryOrange
import com.machikoro.client.ui.theme.TextBlueDark

private val SHOP_CARD_SHAPE = RoundedCornerShape(8.dp)
val RecommendedHighlight = Color(0xFF00C853)

@Composable
internal fun BuyingPhaseShop(
    state: GameScreenState,
    items: List<ShopItem>,
    onPurchaseClick: (String) -> Unit,
    recommendedCardType: CardType? = null,
    modifier: Modifier = Modifier
) {
    val landmarks = remember(items, state.playerLandmarks, state.players) {
        items
            .filter { it.purchaseType == PurchaseType.LANDMARK }
            .filterNot { state.isKnownBuiltLandmark(it) }
            .sortedBy { it.cost }
    }
    val establishments = remember(items) {
        items
            .filter { it.purchaseType == PurchaseType.ESTABLISHMENT }
            .sortedWith(compareBy<ShopItem> { it.cost }.thenBy { it.activationText }.thenBy { it.displayName })
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(292.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShopColumn(
                title = "Landmarks",
                modifier = Modifier.width(238.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = landmarks, key = { it.type }) { item ->
                        ShopImageTile(
                            item = item,
                            state = state,
                            onPurchaseClick = onPurchaseClick,
                            isRecommended = item.type == recommendedCardType?.name
                        )
                    }
                }
            }

            VerticalDivider(color = PanelBorder)

            ShopColumn(
                title = "Establishments",
                modifier = Modifier.weight(1f)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = establishments, key = { it.type }) { item ->
                        ShopImageTile(
                            item = item,
                            state = state,
                            onPurchaseClick = onPurchaseClick,
                            isRecommended = item.type == recommendedCardType?.name
                        )
                    }
                }
            }
        }

        state.purchaseMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = state.purchaseState.toFeedbackColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ShopColumn(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextBlueDark,
            maxLines = 1
        )
        content()
    }
}

@Composable
private fun ShopImageTile(
    item: ShopItem,
    state: GameScreenState,
    onPurchaseClick: (String) -> Unit,
    isRecommended: Boolean,
    modifier: Modifier = Modifier
) {
    val canPurchase = state.canPurchaseItem(item) &&
        (state.purchaseState == PurchaseState.IDLE || state.purchaseState == PurchaseState.ERROR)
    val isFeedbackItem = state.purchaseFeedbackItemType == item.type
    val borderColor = when {
        isFeedbackItem && state.purchaseState == PurchaseState.SUCCESS -> PrimaryOrange
        isFeedbackItem && state.purchaseState == PurchaseState.PENDING -> MaterialTheme.colorScheme.primary
        isRecommended -> RecommendedHighlight
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .width(110.dp)
            .height(131.dp)
            .alpha(if (state.canPurchaseItem(item)) 1f else 0.45f)
            .border(2.dp, borderColor, SHOP_CARD_SHAPE)
            .clickable(enabled = canPurchase) { onPurchaseClick(item.type) }
            .semantics {
                contentDescription = "${item.displayName}: ${item.cost} coins, activates on ${item.activationText}. ${item.effectText}" +
                    if (isRecommended) ", recommended" else ""
            }
    ) {
        Image(
            painter = painterResource(id = ShopImageResolver.drawableForShopItem(item)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

internal fun GameScreenState.shouldShowBuyingPhaseShop(): Boolean =
    isBuyingPhase &&
        isActivePlayer &&
        gameStatus == GameStatus.IN_PROGRESS &&
        gameId != null

private fun GameScreenState.canPurchaseItem(item: ShopItem): Boolean =
    item.isAvailable &&
        hasEnoughKnownCoinsFor(item) &&
        !isKnownBuiltLandmark(item)

private fun GameScreenState.hasEnoughKnownCoinsFor(item: ShopItem): Boolean {
    val activePlayerCoins = players.firstOrNull { it.isActivePlayer }?.coins
    return activePlayerCoins == null || activePlayerCoins >= item.cost
}

private fun GameScreenState.isKnownBuiltLandmark(item: ShopItem): Boolean {
    if (item.purchaseType != PurchaseType.LANDMARK) return false
    val activePlayerId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull() ?: return false
    val landmarkType = runCatching { LandmarkType.valueOf(item.type) }.getOrNull() ?: return false
    return playerLandmarks[activePlayerId].orEmpty().any {
        it.landmarkType == landmarkType && it.isBuilt
    }
}

@Composable
private fun PurchaseState.toFeedbackColor(): Color = when (this) {
    PurchaseState.ERROR -> MaterialTheme.colorScheme.error
    PurchaseState.SUCCESS -> PrimaryOrange
    else -> TextBlueDark
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun BuyingPhaseShopPreview() {
    ClientTheme {
        GameScreen(state = previewBuyingPhaseState())
    }
}

private fun previewBuyingPhaseState() = GameScreenState(
    gameId = 1,
    gamePhase = GamePhase.BUY_OR_BUILD,
    connectionStatus = ConnectionStatus.CONNECTED,
    players = listOf(
        PlayerCoinState(
            id = "1",
            displayName = "You",
            coins = 6,
            isCurrentPlayer = true,
            isActivePlayer = true
        )
    ),
    purchaseState = PurchaseState.IDLE,
    myUserId = 1,
    activePlayerId = 1,
    roundNumber = 4,
    gameStatus = GameStatus.IN_PROGRESS,
    playerLandmarks = mapOf(
        1 to listOf(
            PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
            PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = false),
            PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
            PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
        )
    )
)
