package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.ui.theme.TextBlueDark
import com.machikoro.client.ui.theme.White
import com.machikoro.client.R
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.ui.theme.PrimaryOrange
import kotlinx.serialization.builtins.NothingSerializer

private val SHOP_CARD_SHAPE = RoundedCornerShape(8.dp)

/**
 * Marketplace supply rendered from the reconnect snapshot — one chip per card
 * type still in stock, scrollable horizontally so all 15 types fit on a phone.
 */
@Composable
fun MarketplaceSection(
    marketplace: Map<CardType, Int>,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        Title("Marketplace")
        Spacer(modifier = Modifier.height(4.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(marketplace.entries.toList()) { entry ->
                CardDisplay(
                    cardType = entry.key,
                    quantity = entry.value,
                    width = 150.dp,
                    height = 175.dp,
                    showCounter = true,
                    modifier = Modifier.
                    padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CardDisplay(
    cardType: CardType,
    quantity: Int,
    modifier: Modifier = Modifier,
    width: Dp = 90.dp,
    height: Dp = 110.dp,
    showCounter: Boolean = false
) {

    Box(
        modifier = modifier
            .wrapContentSize()
    ) {
        val alpha = if (quantity > 0) 1f else 0.70f // if more than 0 left
        Image(
            painter = painterResource(id = drawableForPlayerCard(cardType)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(width)
                .height(height)
                .alpha(alpha)
                .clip(SHOP_CARD_SHAPE)
                .semantics { }
        )
        if(showCounter) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(30.dp)
                    .offset(x = (-6).dp, y = (-8).dp)
                    .clip(CircleShape)
                    .alpha(alpha)
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
                    style = MaterialTheme.typography.labelSmall,
                    )
            }
        }
    }
}
private fun drawableForPlayerCard(cardType: CardType): Int =
    when (cardType) {
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
    }


@Composable
fun MarketplaceButton(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier.clickable(
            enabled = enabled
        ) {
            onClick()
        }
    ) {
        val alpha = if (enabled) 1f else 0.7f
        Column(
            modifier = Modifier.alpha(alpha)
        ) {
            Image(
                painter = painterResource(id = R.drawable.markerplace_icon),
                contentDescription = "",
                modifier = Modifier.size(85.dp)
                    .offset(y = 8.dp)
            )

            Text(
                text = "Marketplace",
                style = MaterialTheme.typography.bodyMedium,
                color = White,
                fontSize = 16.sp,
            )
        }
    }
}


@Composable
private fun Title(
    text: String,
) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Color(0xFFC4D3DC).copy(
                    alpha = 0.8f
                )
            ).widthIn(max = 200.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics {
                contentDescription =
                    text
            },
        contentAlignment = Alignment.Center
    ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF004E7E)
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
