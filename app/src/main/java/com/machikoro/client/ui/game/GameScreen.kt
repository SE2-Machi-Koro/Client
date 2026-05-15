package com.machikoro.client.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme
import kotlinx.coroutines.delay

private const val BANNER_COLOR_ANIMATION_DURATION_MS = 300
private const val DICE_ANIMATION_INTERVAL_MS = 100L
private val DICE_FACES = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

@Composable
fun GameScreen(
    state: GameScreenState,
    onRollDice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        CoinDisplay(
            players = state.players,
            playerLandmarks = state.playerLandmarks,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        if (state.gamePhase != GamePhase.NONE) {
            GamePhaseBanner(
                phase = state.gamePhase,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = coinDisplayTopPadding(state.players))
            )
        }

        state.roundNumber?.let { round ->
            RoundIndicator(
                round = round,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
            )
        }

        if (state.marketplace.isNotEmpty()) {
            MarketplaceSection(
                marketplace = state.marketplace,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isRolling -> DiceAnimationDisplay()
                state.diceResult != null -> DiceResultDisplay(dice = state.diceResult)
            }

            if (state.gamePhase == GamePhase.ROLL_DICE && state.isActivePlayer) {
                Button(
                    onClick = onRollDice,
                    enabled = !state.isRolling,
                    modifier = Modifier.semantics {
                        contentDescription = "Würfeln"
                    }
                ) {
                    Text(
                        text = if (state.diceResult == null) "🎲 Würfeln" else "🎲 Nochmal würfeln",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DiceAnimationDisplay(modifier: Modifier = Modifier) {
    var currentFaceIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(DICE_ANIMATION_INTERVAL_MS)
            currentFaceIndex = (0..5).random()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        modifier = modifier.semantics {
            contentDescription = "Würfelt..."
        }
    ) {
        Text(
            text = DICE_FACES[currentFaceIndex],
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun DiceResultDisplay(
    dice: List<Int>,
    modifier: Modifier = Modifier
) {
    val sum = dice.sum()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        modifier = modifier.semantics {
            contentDescription = "Würfelergebnis: $sum"
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dice.forEach { value ->
                Text(
                    text = DICE_FACES.getOrElse(value - 1) { value.toString() },
                    style = MaterialTheme.typography.displaySmall
                )
            }
            if (dice.size > 1) {
                Text(
                    text = "= $sum",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RoundIndicator(
    round: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Text(
            text = "Round $round",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CoinDisplay(
    players: List<PlayerCoinState>,
    playerLandmarks: Map<Int, List<PlayerLandmarkState>>,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        items(items = players, key = { it.id }) { player ->
            PlayerCoinBadge(
                player = player,
                landmarks = playerLandmarks[player.id.toIntOrNull()].orEmpty(),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun PlayerCoinBadge(
    player: PlayerCoinState,
    landmarks: List<PlayerLandmarkState>,
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
            .widthIn(min = 118.dp, max = 180.dp)
            .semantics { contentDescription = "${player.displayName}: ${player.coins} coins" }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(modifier = Modifier.padding(end = 8.dp))
                Column {
                    Text(
                        text = player.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "${player.coins} coins",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
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

/**
 * Marketplace supply rendered from the reconnect snapshot — one chip per card
 * type still in stock, scrollable horizontally so all 15 types fit on a phone.
 */
@Composable
private fun MarketplaceSection(
    marketplace: Map<CardType, Int>,
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
                    MarketplaceCardChip(type = type, count = count)
                }
            }
        }
    }
}

@Composable
private fun MarketplaceCardChip(
    type: CardType,
    count: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .widthIn(min = 96.dp)
            .semantics { contentDescription = "${type.toDisplayText()}: $count in stock" }
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

@Composable
private fun CoinIcon(modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFFFD54F),
        contentColor = Color(0xFF5D4100),
        modifier = modifier.size(28.dp)
    ) {
        Text(
            text = "\$",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

private fun coinDisplayTopPadding(players: List<PlayerCoinState>) =
    if (players.isEmpty()) 0.dp else 92.dp

@Composable
private fun GamePhaseBanner(
    phase: GamePhase,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = phase.toBannerColor(),
        animationSpec = tween(durationMillis = BANNER_COLOR_ANIMATION_DURATION_MS),
        label = "GamePhaseBannerColor"
    )
    Surface(color = animatedColor, modifier = modifier.fillMaxWidth()) {
        Text(
            text = phase.toDisplayText(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun GamePhase.toBannerColor(): Color = when (this) {
    GamePhase.NONE -> Color.Transparent
    GamePhase.ROLL_DICE -> MaterialTheme.colorScheme.primary
    GamePhase.RESOLVE_EFFECTS -> MaterialTheme.colorScheme.secondary
    GamePhase.BUY_OR_BUILD -> MaterialTheme.colorScheme.tertiary
    GamePhase.END_TURN -> MaterialTheme.colorScheme.error
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun GameScreenRollDicePreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gamePhase = GamePhase.ROLL_DICE,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                myUserId = 1,
                activePlayerId = 1,
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun GameScreenRollingPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gamePhase = GamePhase.ROLL_DICE,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                myUserId = 1,
                activePlayerId = 1,
                isRolling = true,
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun GameScreenRollDiceNotActivePreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gamePhase = GamePhase.ROLL_DICE,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                myUserId = 1,
                activePlayerId = 2,
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 600)
@Composable
private fun GameScreenReconnectSnapshotPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gamePhase = GamePhase.BUY_OR_BUILD,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                diceResult = listOf(8),
                myUserId = 1,
                activePlayerId = 1,
                roundNumber = 4,
                playerLandmarks = previewLandmarks(),
                marketplace = previewMarketplace(),
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun GameScreenNonePreview() {
    ClientTheme {
        GameScreen(state = GameScreenState.initial())
    }
}

private fun previewPlayers() = listOf(
    PlayerCoinState(
        id = "1",
        displayName = "You",
        coins = 6,
        isCurrentPlayer = true,
        isActivePlayer = true
    ),
    PlayerCoinState(
        id = "2",
        displayName = "SoupCube",
        coins = 3
    ),
    PlayerCoinState(
        id = "3",
        displayName = "doniliks",
        coins = 0
    )
)

private fun previewLandmarks() = mapOf(
    1 to listOf(
        PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
        PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = true),
        PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
        PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
    ),
    2 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
    3 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
)

private fun previewMarketplace() = mapOf(
    CardType.WHEAT_FIELD to 6,
    CardType.BAKERY to 5,
    CardType.CAFE to 6,
    CardType.CONVENIENCE_STORE to 4,
    CardType.FOREST to 6,
)
