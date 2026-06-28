package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.machikoro.client.R
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.shop.CardDefinitions
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.shared.SecondaryActionButton
import kotlinx.coroutines.delay
import kotlin.collections.chunked
import kotlin.collections.forEach

private val SURFACE_COLOR = Color(0xFF8F7365)

private const val PlayerInventoryAutoDismissMillis = 12_000L

@Composable
fun PlayersTopBar(
    players: List<PlayerCoinState>,
    playerLandmarks: Map<Int, List<PlayerLandmarkState>>,
    playerCards: Map<Int, List<PlayerCardState>>,
    onAccusePlayer: (playerId: String) -> Unit = {},
    canAccuse: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty()) return

    val inspectablePlayers = players.filterNot { it.isCurrentPlayer }
    var inspectedPlayerId by remember { mutableStateOf<String?>(null) }
    val inspectedPlayer = inspectedPlayerId?.let { selectedId ->
        inspectablePlayers.firstOrNull { player -> player.id == selectedId }
    }

    Surface(
        shape = RoundedCornerShape(
            bottomStart = 10.dp,
            bottomEnd = 12.dp
        ),
        color = SURFACE_COLOR,
        shadowElevation = 3.dp,
        modifier = modifier.wrapContentSize()
    ) {
        LazyRow(
            modifier = Modifier
                .wrapContentWidth()
                .padding(bottom = 2.dp, end = 5.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(items = players, key = { it.id }) { player ->
                val playerSnapshotId = player.snapshotId()

                PlayerCoinBadge(
                    player = player,
                    landmarks = playerLandmarks[playerSnapshotId].orEmpty(),
                    canInspect = !player.isCurrentPlayer,
                    onInspect = { inspectedPlayerId = player.id }
                )
            }
        }
    }

    inspectedPlayer?.let { selectedPlayer ->
        LaunchedEffect(selectedPlayer.id) {
            delay(PlayerInventoryAutoDismissMillis)
            inspectedPlayerId = null
        }

        PlayerInventoryDialog(
            players = inspectablePlayers,
            selectedPlayer = selectedPlayer,
            playerLandmarks = playerLandmarks,
            playerCards = playerCards,
            onPlayerSelected = { inspectedPlayerId = it.id },
            onDismiss = { inspectedPlayerId = null },
            canAccuse = canAccuse,
            onAccuse = {
                inspectedPlayerId = null
                onAccusePlayer(selectedPlayer.id)
            }
        )
    }
}

@Composable
private fun PlayerCoinBadge(
    player: PlayerCoinState,
    landmarks: List<PlayerLandmarkState>,
    canInspect: Boolean,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        player.isActivePlayer -> Color(0xFFFFFFFF)
        else -> Color(0xB3FFFFFF)
    }

    val textColor = Color(0xFF004E7E)

    val displayName = if (player.isCurrentPlayer) "You" else player.displayName
    val scale = if (player.isActivePlayer) 1.0f else 0.95f
    val fontSize = if (player.isActivePlayer) 18.sp else 16.sp

    Box(
        modifier = modifier.scale(scale)
            .clickable(enabled = canInspect, onClick = onInspect)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = backgroundColor,
            shadowElevation = 3.dp,
            modifier = Modifier
                .wrapContentSize()
                .widthIn(max = 140.dp)
                .semantics {
                    contentDescription = if (canInspect) {
                        "Inspect ${player.displayName}"
                    } else {
                        "${player.displayName}, ${player.coins} coins"
                    }
                }
        ) {
            Column(
                modifier = modifier
                    .padding(horizontal = 28.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                LandmarkRow(landmarks)
            }
        }

        val opacity = if (player.isCurrentPlayer) 0f else 1f
        CoinBadge(
            amount = player.coins,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 15.dp, y = 12.dp)
                .alpha(opacity)
        )
    }
}

@Composable
private fun PlayerInventoryDialog(
    players: List<PlayerCoinState>,
    selectedPlayer: PlayerCoinState,
    playerLandmarks: Map<Int, List<PlayerLandmarkState>>,
    playerCards: Map<Int, List<PlayerCardState>>,
    onPlayerSelected: (PlayerCoinState) -> Unit,
    onDismiss: () -> Unit,
    canAccuse: Boolean = true,
    onAccuse: () -> Unit = {}
) {
    val selectedSnapshotId = selectedPlayer.snapshotId()
    val landmarks = playerLandmarks[selectedSnapshotId].orEmpty()
    val cards = playerCards[selectedSnapshotId].orEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF8F7365),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .fillMaxHeight(0.95f)
                .semantics {
                    contentDescription = "Player cards window for ${selectedPlayer.displayName}"
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // One accusation per turn (issue #280) — disabled until the next
                    // turn once the local player has used theirs.
                    SecondaryActionButton(
                        label = if (!canAccuse) "Accused this turn"
                        else if (players.size == 1)
                            "Accuse of \n cheating"
                        else "Accuse of cheating",
                        onClick = onAccuse,
                        enabled = canAccuse,
                        modifier = Modifier.semantics {
                            contentDescription = "Accuse of cheating"
                        }
                            .align(Alignment.CenterStart),
                        fontSize = 20,
                    )
                    if(players.size == 1) {
                        PlayerSelectorRow(
                            players = players,
                            selectedPlayer = selectedPlayer,
                            onPlayerSelected = onPlayerSelected,
                            modifier = Modifier.align(Alignment.Center),
                            )
                    }
                    ActionButton(
                        label = "Close",
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        fontSize = 20

                    )
                }
                if(players.size > 1) {
                PlayerSelectorRow(
                    players = players,
                    selectedPlayer = selectedPlayer,
                    onPlayerSelected = onPlayerSelected,
                )
}
                CompositionLocalProvider(
                    LocalOverscrollFactory provides null
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ESTABLISHMENTS TITLE
                        item {
                            BasicText("Landmarks")
                        }
                        // LANDMARKS ROW
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                landmarks.forEach { item ->
                                    PlayerInventoryLandmarkCard(item.landmarkType, item.isBuilt)
                                }
                            }
                        }
                        // ESTABLISHMENTS TITLE
                        item {
                            BasicText("Establishments")
                        }

                        // GRID
                        items(cards.visibleInDisplayOrder().chunked(4)) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                rowItems.forEach { item ->
                                    PlayerInventoryCard(item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun PlayerSelectorRow(
    players: List<PlayerCoinState>,
    selectedPlayer: PlayerCoinState,
    onPlayerSelected: (PlayerCoinState) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        players.forEach { player ->
            PlayerSelectorButton(
                player = player,
                selected = player.id == selectedPlayer.id,
                onClick = { onPlayerSelected(player) },
                enabled = (players.size > 1)
            )
        }
    }
}

@Composable
private fun PlayerInventoryLandmarkCard(
    landmarkType: LandmarkType,
    built: Boolean
) {
    val landmarkName = landmarkType.toDisplayText()
    val stateLabel = if (built) "built" else "not built"

    Image(
        painter = painterResource(landmarkDrawableFor(landmarkType, built)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(170.dp)
            .height(195.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics {
                contentDescription = "landmark $landmarkName: $stateLabel"
            }
    )
}

@Composable
private fun PlayerInventoryCard(
    card: PlayerCardState
) {
    val cardName = card.cardType.toDisplayText()

    Box(
        modifier = Modifier
            .width(170.dp)
            .height(195.dp)
            .semantics {
                contentDescription = "$cardName, quantity ${card.quantity}"
            }
    ) {
        Image(
            painter = painterResource(ShopImageResolver.drawableForCardType(card.cardType)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
        )

        CardQuantityBadge(
            quantity = card.quantity,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-6).dp, y = (-8).dp)
        )
    }
}

@Composable
private fun CardQuantityBadge(
    quantity: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = Color(0xFF004E7E),
                shape = CircleShape
            )
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${quantity}x",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF004E7E),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}


private fun List<PlayerCardState>.visibleInDisplayOrder(): List<PlayerCardState> {
    val cardsByType = filter { it.quantity > 0 }.associateBy { it.cardType }
    return CardDefinitions.sortCardTypesByActivation(cardsByType.keys).mapNotNull { cardsByType[it] }
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
fun CoinBadge(
    amount: Int? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(36.dp)) {
        Image(
            painter = painterResource(R.drawable.coin),
            contentDescription = "Coin",
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )

        Text(
            text = amount?.toString().orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF744300),
            fontSize = 18.sp,
            modifier = Modifier
                .offset(y = (-4).dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun PlayerSelectorButton(
    player: PlayerCoinState,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Color.White.copy(
                    alpha = if (selected) 1f else 0.65f
                )
            ).widthIn(max = 200.dp)

        .clickable(onClick = onClick,
                enabled = enabled)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics {
                contentDescription =
                    "Inspect ${player.displayName} in player cards window"
            },
            contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.login_user_icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = (if (selected) "Cards of " else "" ) + player.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF004E7E).copy(
                        alpha = if (selected) 1f else 0.65f
                    )
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}