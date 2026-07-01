package com.machikoro.client.ui.game.ui.resolving_effects

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.ui.game.ui.CoinBadge

@Composable
fun EffectsResolutionColumn(
    earnedCoins: Int,
    cards: List<CardType>,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp), // keep 0
        modifier = modifier
    ) {

    CoinBadge(earnedCoins, modifier = Modifier.size(36.dp))


    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        LazyColumn(
            modifier = Modifier.width(140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                CardsStack(cards)
            }
        }
    }
    }
}


@Preview
@Composable
private fun EffectsResolutionColumnPreview() {

    EffectsResolutionColumn(
        earnedCoins = 5,
        cards = listOf(
            CardType.APPLE_ORCHARD,
            CardType.FRUIT_AND_VEGETABLE_MARKET,
            CardType.MINE,
            CardType.CAFE,
            CardType.APPLE_ORCHARD,
            CardType.CAFE
        )
    )
}