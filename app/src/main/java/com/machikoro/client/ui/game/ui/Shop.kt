package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.domain.model.shop.CardDefinitions
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.ui.game.GameScreen
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PrimaryOrange
import com.machikoro.client.ui.theme.TextBlueDark

private val SHOP_CARD_SHAPE = RoundedCornerShape(8.dp)
val RecommendedHighlight = Color(0xFF00C853)
val SelectedHighlight = Color(0xFFFFD700) // Gold color for selected card

@Composable
internal fun BuyingPhaseShop(
    state: GameScreenState,
    items: List<ShopItem>,
    onPurchaseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    recommendedCardType: CardType? = null
) {

    val landmarks = remember(
        items,
        state.playerLandmarks,
        state.players
    ) {
        items
            .filter { it.purchaseType == PurchaseType.LANDMARK }
            .filterNot { state.isKnownBuiltLandmark(it) }
            .sortedBy { it.cost }
    }

    val establishments = remember(items) {
        CardDefinitions.sortShopItemsByActivation(
            items.filter { it.purchaseType == PurchaseType.ESTABLISHMENT }
        )
    }

    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // LANDMARKS TITLE
            item {
                BasicText("Landmarks")
            }

            // LANDMARKS ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    landmarks.forEach { item ->
                        ShopImageTile(
                            item = item,
                            state = state,
                            onPurchaseClick = onPurchaseClick,
                            isRecommended = item.type == recommendedCardType?.name
                        )
                    }
                }
            }

            // ESTABLISHMENTS TITLE
            item {
                BasicText("Establishments")
            }

            // GRID
            items(establishments.chunked(4)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowItems.forEach { item ->
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
    }
}





@Composable
private fun ShopImageTile(
    item: ShopItem,
    state: GameScreenState,
    onPurchaseClick: (String) -> Unit,
    isRecommended: Boolean = false,
    modifier: Modifier = Modifier
) {
    val canPurchase = state.canPurchaseItem(item)
    val isSelected = state.selectedPurchaseItemType == item.type
    val isFeedbackItem = state.purchaseFeedbackItemType == item.type

    val borderColor = when {
        isFeedbackItem && state.purchaseState == PurchaseState.SUCCESS -> PrimaryOrange
        isFeedbackItem && state.purchaseState == PurchaseState.PENDING -> MaterialTheme.colorScheme.primary
        isSelected -> SelectedHighlight
        isRecommended -> RecommendedHighlight
        else -> Color.Transparent
    }

    // Allow clicking if purchasable OR if already selected (to deselect/change selection)
    val isClickable = canPurchase || isSelected

    val framePadding = Modifier.padding(
        start = 6.dp,
        end = 6.dp,
        bottom = 9.dp
    )

    Box(
        modifier = modifier
            .wrapContentSize()
            .clickable(
                enabled = isClickable,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onPurchaseClick(item.type)
            } .semantics {
                contentDescription = "${item.displayName}: ${item.cost} coins, activates on ${item.activationText}. ${item.effectText}" +
                        if (isRecommended) ", recommended" else ""
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(155.dp)
                .height(175.dp),
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(
                    id = ShopImageResolver.drawableForShopItem(item)
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (canPurchase) 1f else 0.45f)
                    .semantics {
                        contentDescription =
                            "${item.displayName}: ${item.cost} coins, activates on ${item.activationText}. ${item.effectText}" +
                                    if (isRecommended) ", recommended" else ""
                    }
            )

            if (isRecommended && !isSelected) {
                Image(
                    painter = painterResource(R.drawable.card_frame_green),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .matchParentSize()
                        .then(framePadding)
                )
            }

            if (isSelected) {
                Image(
                    painter = painterResource(R.drawable.card_frame),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .matchParentSize()
                        .then(framePadding)
                )
            }
        }
    }
}


internal fun GameScreenState.shouldShowBuyingPhaseShop(): Boolean =
    isBuyingPhase &&
            isActivePlayer &&
            gameStatus == GameStatus.IN_PROGRESS &&
            gameId != null

private fun GameScreenState.canPurchaseItem(item: ShopItem): Boolean =
    item.isAvailable &&
            purchaseState != PurchaseState.PENDING &&
            purchaseState != PurchaseState.SUCCESS &&
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

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun BuyingPhaseShopWithFramePreview() {
    ClientTheme {
        BuyingPhaseShop(
            state = previewBuyingPhaseState().copy(
                pendingPurchaseItemType = "SHOPPING_MALL"
            ),
            items = ShopCatalog.defaultItems,
            onPurchaseClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ShopCardStatesPreview() {
    ClientTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            ShopImageTile(
                item = ShopCatalog.defaultItems[0],
                state = previewBuyingPhaseState(),
                onPurchaseClick = {},
                isRecommended = true
            )

            ShopImageTile(
                item = ShopCatalog.defaultItems[1],
                state = previewBuyingPhaseState().copy(
                    selectedPurchaseItemType =
                        ShopCatalog.defaultItems[1].type
                ),
                onPurchaseClick = {}
            )
        }
    }
}