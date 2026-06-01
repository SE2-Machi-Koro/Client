package com.machikoro.client.ui.game

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.model.state.toDisplayText


/**
 * Marketplace supply rendered from the reconnect snapshot — one chip per card
 * type still in stock, scrollable horizontally so all 15 types fit on a phone.
 */
@Composable
fun MarketplaceSection(
    marketplace: Map<CardType, Int>,
    recommendedCardType: CardType? = null,
    modifier: Modifier = Modifier
) {
    val entries = CardType.entries.mapNotNull { type ->
        marketplace[type]?.let { count -> type to count }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Marketplace",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = entries, key = { it.first }) { (type, count) ->
                    MarketplaceCardChip(
                        type = type,
                        count = count,
                        isRecommended = type == recommendedCardType,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketplaceCardChip(
    type: CardType,
    count: Int,
    isRecommended: Boolean = false
) {
    val description = "${type.toDisplayText()}: $count in stock" +
            if (isRecommended) ", recommended" else ""
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .widthIn(min = 96.dp)
            .then(
                if (isRecommended) {
                    Modifier.border(3.dp, RecommendedHighlight, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = description }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = type.toDisplayText(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                text = "×$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

