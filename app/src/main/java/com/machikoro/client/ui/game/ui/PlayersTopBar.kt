package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.toDisplayText
import kotlinx.coroutines.delay
import kotlin.collections.get

private const val PlayerInventoryAutoDismissMillis = 12_000L

// todo: adjust to current figma design, keep landmark badges
@Composable
fun PlayersTopBar(
    players: List<PlayerCoinState>,
    playerLandmarks: Map<Int, List<PlayerLandmarkState>>,
    playerCards: Map<Int, List<PlayerCardState>>,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty()) return
    var inspectedPlayerId by remember { mutableStateOf<String?>(null) }
    val inspectedPlayer = inspectedPlayerId?.let { id ->
        players.firstOrNull { player -> player.id == id }
    }

    LazyRow(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        items(items = players, key = { it.id }) { player ->
            val playerSnapshotId = player.snapshotId()
            PlayerCoinBadge(
                player = player,
                landmarks = playerLandmarks[playerSnapshotId].orEmpty(),
                cards = playerCards[playerSnapshotId].orEmpty(),
                onInspect = { inspectedPlayerId = player.id },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }

    inspectedPlayer?.let { selectedPlayer ->
        LaunchedEffect(selectedPlayer.id) {
            delay(PlayerInventoryAutoDismissMillis)
            inspectedPlayerId = null
        }

        PlayerInventoryDialog(
            players = players,
            selectedPlayer = selectedPlayer,
            playerLandmarks = playerLandmarks,
            playerCards = playerCards,
            onPlayerSelected = { inspectedPlayerId = it.id },
            onDismiss = { inspectedPlayerId = null }
        )
    }
}

@Composable
private fun PlayerCoinBadge(
    player: PlayerCoinState,
    landmarks: List<PlayerLandmarkState>,
    cards: List<PlayerCardState>,
    onInspect: () -> Unit,
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
                    maxLines = 1,
                    modifier = Modifier
                        .clickable(onClick = onInspect)
                        .semantics {
                            contentDescription = "Inspect ${player.displayName}"
                        }
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
private fun PlayerInventoryDialog(
    players: List<PlayerCoinState>,
    selectedPlayer: PlayerCoinState,
    playerLandmarks: Map<Int, List<PlayerLandmarkState>>,
    playerCards: Map<Int, List<PlayerCardState>>,
    onPlayerSelected: (PlayerCoinState) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedSnapshotId = selectedPlayer.snapshotId()
    val landmarks = playerLandmarks[selectedSnapshotId].orEmpty()
    val cards = playerCards[selectedSnapshotId].orEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(min = 320.dp, max = 560.dp)
                .semantics {
                    contentDescription = "Player cards window for ${selectedPlayer.displayName}"
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PlayerInventoryHeader(
                    playerName = selectedPlayer.displayName,
                    onDismiss = onDismiss
                )

                PlayerSelectorRow(
                    players = players,
                    selectedPlayer = selectedPlayer,
                    onPlayerSelected = onPlayerSelected
                )

                PlayerInventoryLandmarks(
                    playerName = selectedPlayer.displayName,
                    landmarks = landmarks
                )

                PlayerInventoryCards(
                    playerName = selectedPlayer.displayName,
                    cards = cards
                )
            }
        }
    }
}

@Composable
private fun PlayerInventoryHeader(
    playerName: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = playerName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        TextButton(onClick = onDismiss) {
            Text("Close")
        }
    }
}

@Composable
private fun PlayerSelectorRow(
    players: List<PlayerCoinState>,
    selectedPlayer: PlayerCoinState,
    onPlayerSelected: (PlayerCoinState) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        players.forEach { player ->
            PlayerSelectorButton(
                player = player,
                selected = player.id == selectedPlayer.id,
                onClick = { onPlayerSelected(player) }
            )
        }
    }
}

@Composable
private fun PlayerSelectorButton(
    player: PlayerCoinState,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Inspect ${player.displayName} in player cards window"
            }
    ) {
        Text(
            text = player.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PlayerInventoryLandmarks(
    playerName: String,
    landmarks: List<PlayerLandmarkState>
) {
    val landmarksByType = landmarks.associateBy { it.landmarkType }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Landmarks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LandmarkType.entries.forEach { type ->
                val built = landmarksByType[type]?.isBuilt == true
                PlayerInventoryLandmarkCard(
                    playerName = playerName,
                    landmarkType = type,
                    built = built
                )
            }
        }
    }
}

@Composable
private fun PlayerInventoryLandmarkCard(
    playerName: String,
    landmarkType: LandmarkType,
    built: Boolean
) {
    val landmarkName = landmarkType.toDisplayText()
    val stateLabel = if (built) "built" else "not built"
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .widthIn(min = 112.dp, max = 128.dp)
            .semantics {
                contentDescription = "$playerName landmark $landmarkName: $stateLabel"
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Image(
                painter = painterResource(landmarkDrawableFor(landmarkType, built)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
            )

            Text(
                text = landmarkName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Text(
                text = if (built) "Built" else "Locked",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlayerInventoryCards(
    playerName: String,
    cards: List<PlayerCardState>
) {
    val visibleCards = cards.visibleInDisplayOrder()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Establishments",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (visibleCards.isEmpty()) {
            Text(
                text = "No establishments",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleCards.forEach { card ->
                    PlayerInventoryCard(
                        playerName = playerName,
                        card = card
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerInventoryCard(
    playerName: String,
    card: PlayerCardState
) {
    val cardName = card.cardType.toDisplayText()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .widthIn(min = 120.dp, max = 136.dp)
            .semantics {
                contentDescription = "$playerName owns $cardName, quantity ${card.quantity}"
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Image(
                painter = painterResource(ShopImageResolver.drawableForCardType(card.cardType)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(72.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
            )

            Text(
                text = cardName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
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

private fun PlayerCoinState.snapshotId(): Int? = id.toIntOrNull()

private fun landmarkDrawableFor(
    landmarkType: LandmarkType,
    built: Boolean
): Int {
    val imageKey = "landmark_${landmarkType.name.lowercase()}"
    return ShopImageResolver.drawableFor(
        if (built) imageKey else "${imageKey}_locked"
    )
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
private fun CoinIcon(
    amount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFFFD54F),
        contentColor = Color(0xFF5D4100),
        modifier = modifier.size(28.dp)
    ) {
        Text(
            text = "$amount",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
