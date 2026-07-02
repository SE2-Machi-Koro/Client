package com.machikoro.client.ui.game.ui.resolving_effects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.CardType
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import com.machikoro.client.R
import com.machikoro.client.ui.shared.AnimatedItem
import com.machikoro.client.ui.shared.AnimationType

@Composable
fun CardsStack(
    cards: List<CardType>,
    modifier: Modifier = Modifier,
    animateCards: Boolean = !LocalInspectionMode.current,
    initialDelayMillis: Int = 0,
    onPositioned: (Offset) -> Unit = {},
) {
    val overlap = 36.dp
    val cardHeight = 165.dp
    val cardWidth = 140.dp
    val shadowWidth = cardWidth

    Box(
        modifier = modifier
            .height(cardHeight + ((cards.size - 1) * overlap))
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInRoot().center)
            }
    ) {
        cards.forEachIndexed { index, card ->
            val yOffset = overlap * index

            val cardContent: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .offset(y = yOffset)
                        .width(cardWidth)
                        .height(cardHeight)
                ) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (cardWidth - shadowWidth) / 4,
                                    y = (-1.5).dp
                                )
                                .width(shadowWidth)
                                .height(7.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x44000000),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp
                                    )
                                )
                        )
                    }

                    CardDisplay(
                        cardType = card,
                        width = cardWidth,
                        height = cardHeight
                    )
                }
            }

            if (animateCards) {
                AnimatedItem(
                    delayMillis = initialDelayMillis + index * 120,
                    animationType = AnimationType.Bounce,
                    content = cardContent
                )
            } else {
                cardContent()
            }
        }
    }
}



@Composable
fun CardDisplay(
    cardType: CardType,
    modifier: Modifier = Modifier,
    width: Dp = 140.dp,
    height: Dp = 165.dp,
) {
    Box(
        modifier = modifier.wrapContentSize()
    ) {

        Image(
            painter = painterResource(
                id = drawableForPlayerCard(cardType)
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

fun drawableForPlayerCard(
    cardType: CardType
): Int =
    when (cardType) {

        CardType.WHEAT_FIELD ->
            R.drawable.card_wheat_field

        CardType.RANCH ->
            R.drawable.card_ranch

        CardType.FOREST ->
            R.drawable.card_forest

        CardType.MINE ->
            R.drawable.card_mine

        CardType.APPLE_ORCHARD ->
            R.drawable.card_apple_orchard

        CardType.BAKERY ->
            R.drawable.card_bakery

        CardType.CONVENIENCE_STORE ->
            R.drawable.card_convenience_store

        CardType.CHEESE_FACTORY ->
            R.drawable.card_cheese_factory

        CardType.FURNITURE_FACTORY ->
            R.drawable.card_furniture_factory

        CardType.FRUIT_AND_VEGETABLE_MARKET ->
            R.drawable.card_fruit_and_vegetable_market

        CardType.CAFE ->
            R.drawable.card_cafe

        CardType.FAMILY_RESTAURANT ->
            R.drawable.card_family_restaurant

        CardType.STADIUM ->
            R.drawable.card_stadium

        CardType.TV_STATION ->
            R.drawable.card_tv_station
    }

@Preview(showBackground = true, widthDp = 180, heightDp = 360)
@Composable
private fun AnimatedCardsStackPreview() {
    CardsStack(
        cards = listOf(
            CardType.BAKERY,
            CardType.BAKERY,
            CardType.BAKERY,
        ),
        animateCards = true
    )
}
