package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.machikoro.client.ui.game.SIDE_CONTENT_OFFSET
import com.machikoro.client.ui.shared.AnimatedItem
import com.machikoro.client.ui.shared.AnimationType

@Composable
internal fun BuyingPhaseShop(
    state: GameScreenState,
    items: List<ShopItem>,
    onPurchaseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    recommendedCardType: CardType? = null
) {
    // displays purchased card after SUCCESS
    if(state.purchaseState == PurchaseState.SUCCESS) {
        state.purchaseFeedbackItemType?.let {
            AnimatedItem(
                delayMillis = 0,
                animationType = AnimationType.Bounce
            ) {
                PurchaseDisplay(
                    modifier = modifier.offset(y = (0).dp),
                    drawable = drawableForPlayerCard(it),
                    name = state.activePlayerUsername,
                    isActive = state.isActivePlayer
                )
            }
        }
    } else {
        if(state.isActivePlayer) {
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
                ) {

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("Landmarks")
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    }

                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                            ) {
                            BasicText("Establishments")
                        }
                    }

                    items(establishments.chunked(4)) { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        } else {
            BasicText(
                state.activePlayerUsername + " is deciding what card to buy",
                modifier = modifier.offset(y = (-SIDE_CONTENT_OFFSET).dp))
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

        remainingQuantity?.let { count ->
            if (!isAlreadyOwnedPurple) {
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

private fun drawableForPlayerCard(cardType: String): Int =
    when (cardType) {
        CardType.WHEAT_FIELD.name -> R.drawable.card_wheat_field
        CardType.RANCH.name -> R.drawable.card_ranch
        CardType.FOREST.name -> R.drawable.card_forest
        CardType.MINE.name -> R.drawable.card_mine
        CardType.APPLE_ORCHARD.name -> R.drawable.card_apple_orchard
        CardType.BAKERY.name -> R.drawable.card_bakery
        CardType.CONVENIENCE_STORE.name -> R.drawable.card_convenience_store
        CardType.CHEESE_FACTORY.name -> R.drawable.card_cheese_factory
        CardType.FURNITURE_FACTORY.name -> R.drawable.card_furniture_factory
        CardType.FRUIT_AND_VEGETABLE_MARKET.name ->
            R.drawable.card_fruit_and_vegetable_market
        CardType.CAFE.name -> R.drawable.card_cafe
        CardType.FAMILY_RESTAURANT.name -> R.drawable.card_family_restaurant
        CardType.STADIUM.name -> R.drawable.card_stadium
        CardType.TV_STATION.name -> R.drawable.card_tv_station
        CardType.BUSINESS_CENTER.name -> R.drawable.card_business_center
        LandmarkType.TRAIN_STATION.name -> R.drawable.landmark_train_station
        LandmarkType.SHOPPING_MALL.name -> R.drawable.landmark_shopping_mall
        LandmarkType.AMUSEMENT_PARK.name -> R.drawable.landmark_amusement_park
        LandmarkType.RADIO_TOWER.name -> R.drawable.landmark_radio_tower

        else -> R.drawable.card_wheat_field
    }

@Composable
private fun PurchaseDisplay(
    name: String,
    isActive: Boolean,
    drawable: Int,
    modifier: Modifier = Modifier,
    width: Dp = 155.dp,
    height: Dp = 175.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(
            label =
                if(isActive) "You have purchased:"
                else name + " has purchased:"
        )
        Image(
            painter = painterResource(
                drawable
            ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(width)
                .height(height)
                .semantics { }
        )
    }
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
