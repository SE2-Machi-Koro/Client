package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.R
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.shared.AnimatedItem
import com.machikoro.client.ui.shared.AnimationType
import com.machikoro.client.ui.theme.TextBlueDark


private val SHOP_CARD_SHAPE = RoundedCornerShape(8.dp)


@Composable
internal fun PlayerCardsDisplay(
    state: GameScreenState,
    modifier: Modifier = Modifier,
) {
    val currentPlayerId =
        state.players
            .firstOrNull { it.isCurrentPlayer }
            ?.id
            ?.toIntOrNull()

    val landmarks =
        state.playerLandmarks[currentPlayerId]
            .orEmpty()
            .take(4)

    val establishments =
        state.playerCards[currentPlayerId]
            .orEmpty()
            .take(4)

    Row(
        modifier = modifier.wrapContentSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            landmarks.forEach {
                LandmarkDisplay(it)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            establishments.forEach {
                CardDisplay(it)
            }
        }
    }
}

@Composable
fun BigPlayerCardsDisplay(
    state: GameScreenState,
    modifier: Modifier = Modifier,
) {
    val currentPlayerId =
        state.players
            .firstOrNull { it.isCurrentPlayer }
            ?.id
            ?.toIntOrNull()

    val landmarks =
        state.playerLandmarks[currentPlayerId]
            .orEmpty()

    val establishments =
        state.playerCards[currentPlayerId]
            .orEmpty()

    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        LazyColumn(
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
                    landmarks.forEach {
                        LandmarkDisplay(it,
                            width = 155.dp,
                            height = 175.dp
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
                    rowItems.forEach {
                        CardDisplay(it,
                        width = 155.dp,
                        height = 175.dp,
                        showCounter = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandmarkDisplay(
    item: PlayerLandmarkState,
    modifier: Modifier = Modifier,
    width: Dp = 90.dp,
    height: Dp = 110.dp
) {
    CardArtImage(
        drawableResId = drawableForPlayerLandmark(item),
        width = width,
        height = height,
        modifier = modifier
    )
}

internal data class LandmarkPurchaseRevealUi(
    val landmarks: List<PlayerLandmarkState>,
    val title: String,
    val message: String,
)

internal fun GameScreenState.landmarkPurchaseRevealUi(
    purchasedLandmark: LandmarkType,
): LandmarkPurchaseRevealUi {
    val activePlayer = players.firstOrNull { it.isActivePlayer }
    val activePlayerId = activePlayer?.id?.toIntOrNull()
    val landmarksByType = playerLandmarks[activePlayerId]
        .orEmpty()
        .associateBy { it.landmarkType }

    val landmarks = LandmarkType.entries.map { landmarkType ->
        val landmark = landmarksByType[landmarkType]
            ?: PlayerLandmarkState(landmarkType = landmarkType, isBuilt = false)

        if (landmarkType == purchasedLandmark) {
            landmark.copy(isBuilt = true)
        } else {
            landmark
        }
    }
    val allLandmarksBuilt = landmarks.all { it.isBuilt }
    val playerName = activePlayer?.displayName ?: "Player"

    return LandmarkPurchaseRevealUi(
        landmarks = landmarks,
        title = if (isActivePlayer) "Your landmarks" else "$playerName's landmarks",
        message = when {
            allLandmarksBuilt && isActivePlayer -> "You won!"
            allLandmarksBuilt -> "$playerName wins!"
            isActivePlayer -> "You are closer to winning!"
            else -> "$playerName is closer to winning!"
        }
    )
}

@Composable
internal fun LandmarkPurchaseReveal(
    state: GameScreenState,
    purchasedLandmark: LandmarkType,
    modifier: Modifier = Modifier,
) {
    val reveal = state.landmarkPurchaseRevealUi(purchasedLandmark)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(reveal.title)
        BasicText(reveal.message)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reveal.landmarks.forEach { landmark ->
                if (landmark.landmarkType == purchasedLandmark) {
                    AnimatedItem(
                        delayMillis = 250,
                        animationType = AnimationType.Bounce
                    ) {
                        LandmarkDisplay(
                            item = landmark,
                            width = 155.dp,
                            height = 175.dp
                        )
                    }
                } else {
                    LandmarkDisplay(
                        item = landmark,
                        width = 155.dp,
                        height = 175.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun CardDisplay(
    item: PlayerCardState,
    modifier: Modifier = Modifier,
    width: Dp = 90.dp,
    height: Dp = 110.dp,
    showCounter: Boolean = false
) {
    Box(
        modifier = modifier.wrapContentSize()
    ) {

        CardArtImage(
            drawableResId = drawableForPlayerCard(item),
            width = width,
            height = height
        )
        if(showCounter) {
            CardQuantityIndicator(
                quantity = item.quantity,
                isVisible = item.quantity > 1,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
internal fun CardArtImage(
    drawableResId: Int,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Image(
        painter = painterResource(id = drawableResId),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(width)
            .height(height)
            .alpha(alpha)
            .clip(SHOP_CARD_SHAPE)
            .semantics {}
    )
}

@Composable
internal fun CardQuantityIndicator(
    quantity: Int,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .alpha(if (isVisible) 1f else 0f)
            .offset(x = (-6).dp, y = (-8).dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = TextBlueDark,
                shape = CircleShape
            )
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = quantity.toString() + "x",
            color = TextBlueDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}


fun drawableForPlayerLandmark(playerLandmarkState: PlayerLandmarkState): Int {
    if (playerLandmarkState.isBuilt) return drawableForLandmarkBuild(playerLandmarkState.landmarkType)
    else return drawableForLandmarkNotBuild(playerLandmarkState.landmarkType)
}

private fun drawableForLandmarkBuild(landmarkType: LandmarkType): Int =
    when (landmarkType) {
        LandmarkType.TRAIN_STATION -> R.drawable.landmark_train_station
        LandmarkType.SHOPPING_MALL -> R.drawable.landmark_shopping_mall
        LandmarkType.AMUSEMENT_PARK -> R.drawable.landmark_amusement_park
        LandmarkType.RADIO_TOWER -> R.drawable.landmark_radio_tower
    }
private fun drawableForLandmarkNotBuild(landmarkType: LandmarkType): Int =
    when (landmarkType) {
        LandmarkType.TRAIN_STATION -> R.drawable.landmark_train_station_locked
        LandmarkType.SHOPPING_MALL -> R.drawable.landmark_shopping_mall_locked
        LandmarkType.AMUSEMENT_PARK -> R.drawable.landmark_amusement_park_locked
        LandmarkType.RADIO_TOWER -> R.drawable.landmark_radio_tower_locked
    }

fun drawableForPlayerCard(playerCardState: PlayerCardState): Int =
    when (playerCardState.cardType) {
        CardType.WHEAT_FIELD -> R.drawable.card_wheat_field
        CardType.RANCH -> R.drawable.card_ranch
        CardType.FOREST -> R.drawable.card_forest
        CardType.MINE -> R.drawable.card_mine
        CardType.APPLE_ORCHARD -> R.drawable.card_apple_orchard
        CardType.BAKERY -> R.drawable.card_bakery
        CardType.CONVENIENCE_STORE -> R.drawable.card_convenience_store
        CardType.CHEESE_FACTORY -> R.drawable.card_cheese_factory
        CardType.FURNITURE_FACTORY -> R.drawable.card_furniture_factory
        CardType.FRUIT_AND_VEGETABLE_MARKET ->
            R.drawable.card_fruit_and_vegetable_market
        CardType.CAFE -> R.drawable.card_cafe
        CardType.FAMILY_RESTAURANT -> R.drawable.card_family_restaurant
        CardType.STADIUM -> R.drawable.card_stadium
        CardType.TV_STATION -> R.drawable.card_tv_station
        CardType.BUSINESS_CENTER -> R.drawable.card_business_center
    }

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun PlayerCardsDisplayPreview() {
    PlayerCardsDisplay(
        state = GameScreenState.initial().copy(
            myUserId = 1,

            playerLandmarks = mapOf(
                1 to listOf(
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.TRAIN_STATION,
                        isBuilt = true
                    ),
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.SHOPPING_MALL,
                        isBuilt = false
                    ),
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.AMUSEMENT_PARK,
                        isBuilt = false
                    ),
                    PlayerLandmarkState(
                        landmarkType = LandmarkType.RADIO_TOWER,
                        isBuilt = true
                    )
                )
            ),

            playerCards = mapOf(
                1 to listOf(
                    PlayerCardState(
                        cardType = CardType.WHEAT_FIELD,
                        quantity = 1
                    ),
                    PlayerCardState(
                        cardType = CardType.BAKERY,
                        quantity = 1

                    ),
                    PlayerCardState(
                        cardType = CardType.CAFE,
                        quantity = 1
                    ),
                    PlayerCardState(
                        cardType = CardType.FOREST,
                        quantity = 1
                    ),
                    PlayerCardState(
                        cardType = CardType.STADIUM,
                        quantity = 1
                    )
                )
            )
        )
    )
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun LandmarkPurchaseRevealPreview() {
    LandmarkPurchaseReveal(
        state = GameScreenState.initial().copy(
            myUserId = 42,
            activePlayerId = 42,
            players = listOf(
                com.machikoro.client.domain.model.state.PlayerCoinState(
                    id = "1",
                    displayName = "Player 1",
                    coins = 4,
                    isCurrentPlayer = true,
                    isActivePlayer = true
                )
            ),
            playerLandmarks = mapOf(
                1 to listOf(
                    PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                    PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
                    PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false)
                )
            )
        ),
        purchasedLandmark = LandmarkType.SHOPPING_MALL
    )
}
