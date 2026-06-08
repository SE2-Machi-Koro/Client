package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

   Row(
       modifier = modifier.wrapContentSize()
   ) {
       CompositionLocalProvider(
           LocalOverscrollFactory provides null
       ) {

           LazyVerticalGrid(
               columns = GridCells.Fixed(2), // 2 per row
               modifier = Modifier
                   .weight(1f),
           ) {

               items(landmarks) { landmark ->
                   Box(
                       modifier = Modifier.padding(top = 8.dp)
                   ) {
                       LandmarkDisplay(
                           landmark,
                           width = 155.dp,
                           height = 180.dp
                       )
                   }
               }
           }
       }
       CompositionLocalProvider(
           LocalOverscrollFactory provides null
       ) {
           LazyVerticalGrid(
               columns = GridCells.Fixed(2), // 2 per row
               modifier = Modifier
                   .weight(1f),
           ) {
               items(establishments) { establishment ->
                   Box(
                       modifier = Modifier.padding(top = 8.dp)
                   ) {
                       CardDisplay(
                           establishment,
                           width = 155.dp,
                           height = 180.dp,
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
    Image(
        painter = painterResource(id = drawableForPlayerLandmark(item)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(width)
            .height(height)
            .clip(SHOP_CARD_SHAPE)
            .semantics {}
    )
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

        Image(
            painter = painterResource(id = drawableForPlayerCard(item)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(SHOP_CARD_SHAPE)
                .semantics { }
        )
        if(showCounter) {
            val alpha = if (item.quantity > 1) 1f else 0f // if more that 1 card
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(30.dp)
                    .alpha(alpha)
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
                    text = item.quantity.toString() + "x",
                    color = TextBlueDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
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