package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.model.shop.CardDefinitions
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.domain.model.state.isAlreadyOwnedPurpleEstablishment
import com.machikoro.client.domain.model.state.isShopItemAvailableFromMarketplace
import com.machikoro.client.domain.model.state.remainingMarketplaceQuantityFor
import com.machikoro.client.ui.game.GameScreen
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.CardPurpleBackground
import com.machikoro.client.ui.theme.CardPurpleText
import com.machikoro.client.ui.theme.PrimaryOrange
import com.machikoro.client.ui.theme.TextBlueDark
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight

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
    val isSuccessFeedback = isFeedbackItem && state.purchaseState == PurchaseState.SUCCESS
    val remainingQuantity = state.remainingMarketplaceQuantityFor(item)
    val isAlreadyOwnedPurple = state.isAlreadyOwnedPurpleEstablishment(item)
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
            }
            .semantics {
                contentDescription =
                    "${item.displayName}: ${item.cost} coins, activates on ${item.activationText}. ${item.effectText}" +
                            remainingQuantity?.let { ", $it remaining" }.orEmpty() +
                            (if (remainingQuantity == 0) ", unavailable" else "") +
                            (if (isAlreadyOwnedPurple) ", already owned" else "") +
                            (if (isRecommended) ", recommended" else "")
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(155.dp)
                .height(175.dp),
            contentAlignment = Alignment.Center
        ) {
            CardArtImage(
                drawableResId = ShopImageResolver.drawableForShopItem(item),
                width = 155.dp,
                height = 175.dp,
                alpha = if (canPurchase) 1f else 0.45f
            )
        }

        if (isRecommended && !isSelected && !isSuccessFeedback) {
            Image(
                painter = painterResource(R.drawable.card_frame_green),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .matchParentSize()
                    .then(framePadding)
            )
        }

        if (isSelected  || isSuccessFeedback) {
            Image(
                painter = painterResource(R.drawable.card_frame),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .matchParentSize()
                    .then(framePadding)
            )
        }

        remainingQuantity?.let { count ->
                if(!isAlreadyOwnedPurple) {
                CardQuantityIndicator(
                    quantity = count,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }

        if (isAlreadyOwnedPurple) {
            OwnedCardIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        y = (-15).dp,
                    )
            )
        }
    }
}

@Composable
private fun OwnedCardIndicator(modifier: Modifier = Modifier) {
    Text(
        text = "Owned",
        color = CardPurpleText,
        fontSize = 14.sp,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .padding(top = 6.dp, end = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CardPurpleBackground)
            .border(2.dp, CardPurpleText, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .semantics {
                contentDescription = "Already owned card indicator"
            }
    )
}

internal fun GameScreenState.shouldShowBuyingPhaseShop(): Boolean =
    isBuyingPhase &&
            isActivePlayer &&
            gameStatus == GameStatus.IN_PROGRESS &&
            gameId != null

private fun GameScreenState.canPurchaseItem(item: ShopItem): Boolean =
    isShopItemAvailableFromMarketplace(item) &&
            purchaseState != PurchaseState.PENDING &&
            purchaseState != PurchaseState.SUCCESS &&
            hasEnoughKnownCoinsFor(item) &&
            !isAlreadyOwnedPurpleEstablishment(item) &&
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
private fun GameScreenRollingPhasePreview() {
    ClientTheme {
        GameScreen(
            state = previewBuyingPhaseState().copy(
                gamePhase = GamePhase.ROLL_DICE,
                purchaseState = PurchaseState.IDLE
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenBuyingPhasePreview() {
    ClientTheme {
        GameScreen(
            state = previewBuyingPhaseState().copy(
                gamePhase = GamePhase.BUY_OR_BUILD,
                purchaseState = PurchaseState.IDLE
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenPurchasePendingPreview() {
    ClientTheme {
        GameScreen(
            state = previewBuyingPhaseState().copy(
                gamePhase = GamePhase.BUY_OR_BUILD,
                purchaseState = PurchaseState.PENDING,
                selectedPurchaseItemType = CardType.BAKERY.name,
                purchaseFeedbackItemType = CardType.BAKERY.name
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenPurchaseSuccessPreview() {
    ClientTheme {
        GameScreen(
            state = previewBuyingPhaseState().copy(
                gamePhase = GamePhase.BUY_OR_BUILD,
                purchaseState = PurchaseState.SUCCESS,
                purchaseFeedbackItemType = CardType.BAKERY.name
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun GameScreenNotActivePlayerPreview() {
    ClientTheme {
        GameScreen(
            state = previewBuyingPhaseState().copy(
                gamePhase = GamePhase.BUY_OR_BUILD,
                activePlayerId = 2,
                players = listOf(
                    PlayerCoinState("1", "You", 6, isCurrentPlayer = true, isActivePlayer = false),
                    PlayerCoinState("2", "Lev", 8, isCurrentPlayer = false, isActivePlayer = true)
                )
            )
        )
    }
}
