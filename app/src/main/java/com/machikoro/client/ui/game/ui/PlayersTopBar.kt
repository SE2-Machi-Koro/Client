package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.toDisplayText
import kotlin.collections.get

// todo: adjust to current figma design, keep landmark badges
@Composable
fun PlayersTopBar(
    players: List<PlayerCoinState>,
    playerLandmarks: Map<Int, List<PlayerLandmarkState>>,
    playerCards: Map<Int, List<PlayerCardState>>,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty()) return
    LazyRow(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        items(items = players, key = { it.id }) { player ->
            val playerSnapshotId = player.id.toIntOrNull()
            PlayerCoinBadge(
                player = player,
                landmarks = playerLandmarks[playerSnapshotId].orEmpty(),
                cards = playerCards[playerSnapshotId].orEmpty(),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
@Composable
private fun PlayerCoinBadge(
    player: PlayerCoinState,
    landmarks: List<PlayerLandmarkState>,
    cards: List<PlayerCardState>,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        player.isCurrentPlayer -> MaterialTheme.colorScheme.primary
        player.isActivePlayer -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        player.isCurrentPlayer -> MaterialTheme.colorScheme.onPrimary
        player.isActivePlayer -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 3.dp,
        modifier = modifier
            .widthIn(min = 138.dp, max = 232.dp)
            .semantics { contentDescription = "${player.displayName}: ${player.coins} coins" }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(
                    amount = player.coins,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = player.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            if (cards.hasVisibleCards()) {
                OwnedCardRow(
                    playerName = player.displayName,
                    cards = cards,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (landmarks.isNotEmpty()) {
                LandmarkRow(
                    landmarks = landmarks,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun OwnedCardRow(
    playerName: String,
    cards: List<PlayerCardState>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        cards.visibleInDisplayOrder().forEach { card ->
            OwnedCardChip(
                playerName = playerName,
                card = card
            )
        }
    }
}

@Composable
private fun OwnedCardChip(
    playerName: String,
    card: PlayerCardState
) {
    val cardName = card.cardType.toDisplayText()
    val cardUnit = if (card.quantity == 1) "card" else "cards"
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(6.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .widthIn(min = 58.dp, max = 72.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .semantics {
                contentDescription = "Owned by $playerName: $cardName, ${card.quantity} $cardUnit"
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            Image(
                painter = painterResource(ShopImageResolver.drawableForCardType(card.cardType)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(34.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
            )
            Text(
                text = "x${card.quantity}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun List<PlayerCardState>.hasVisibleCards(): Boolean =
    any { it.quantity > 0 }

private fun List<PlayerCardState>.visibleInDisplayOrder(): List<PlayerCardState> {
    val cardsByType = filter { it.quantity > 0 }.associateBy { it.cardType }
    return CardType.entries.mapNotNull { cardsByType[it] }
}

/**
 * Compact built/unbuilt indicator for a player's four landmarks, rendered in a
 * fixed order so the columns stay stable across players and snapshots.
 */
@Composable
private fun LandmarkRow(
    landmarks: List<PlayerLandmarkState>,
    modifier: Modifier = Modifier
) {
    val byType = landmarks.associateBy { it.landmarkType }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LandmarkType.entries.forEach { type ->
            val built = byType[type]?.isBuilt == true
            LandmarkPip(type = type, built = built)
        }
    }
}

@Composable
private fun LandmarkPip(
    type: LandmarkType,
    built: Boolean
) {
    val pipColor = if (built) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val builtLabel = if (built) "built" else "not built"
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(color = pipColor, shape = RoundedCornerShape(3.dp))
            .semantics { contentDescription = "${type.toDisplayText()}: $builtLabel" }
    ) {
        Text(
            text = type.name.take(1),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (built) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.surface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
private fun CoinBadge(
    amount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(36.dp),
    ) {

        Image(
            painter = painterResource(R.drawable.coin),
            contentDescription = "Coin",
            modifier = Modifier.fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )

        Text(
            text = amount.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF744300),
            fontSize = 18.sp,
            modifier = Modifier.offset(y = (-4).dp)
            .align(Alignment.Center),
        )
    }
}
