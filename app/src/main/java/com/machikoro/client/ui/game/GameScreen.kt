package com.machikoro.client.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.shop.ShopItemColor
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme

private const val BANNER_COLOR_ANIMATION_DURATION_MS = 300

@Composable
fun GameScreen(
    state: GameScreenState,
    onPurchaseClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        CoinDisplay(
            players = state.players,
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

        if (state.isBuyingPhase) {
            // Shop uses a local catalog until the backend exposes marketplace supply.
            BuyingPhaseShop(
                state = state,
                items = ShopCatalog.defaultItems,
                onPurchaseClick = onPurchaseClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = shopTopPadding(state.players))
                    .padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun CoinDisplay(
    players: List<PlayerCoinState>,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty()) {
        return
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = players,
            key = { it.id }
        ) { player ->
            PlayerCoinBadge(
                player = player,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun PlayerCoinBadge(
    player: PlayerCoinState,
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
            .semantics {
                contentDescription = "${player.displayName}: ${player.coins} coins"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            CoinIcon(
                modifier = Modifier.padding(end = 8.dp)
            )
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

private fun coinDisplayTopPadding(players: List<PlayerCoinState>) = if (players.isEmpty()) 0.dp else 68.dp

@Composable
private fun BuyingPhaseShop(
    state: GameScreenState,
    items: List<ShopItem>,
    onPurchaseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Disable buying until both active-player state and a server game id are known.
    val canPurchase = state.canCurrentPlayerPurchase() && state.gameId != null
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (canPurchase) "Shop" else "Shop - waiting for active player",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(verticalAlignment = Alignment.CenterVertically) {
                items(
                    items = items,
                    key = { it.type }
                ) { item ->
                    ShopItemCard(
                        item = item,
                        purchaseState = state.purchaseState,
                        canPurchase = canPurchase,
                        onPurchaseClick = onPurchaseClick,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    purchaseState: PurchaseState,
    canPurchase: Boolean,
    onPurchaseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPurchaseEnabled = canPurchase && item.isAvailable && purchaseState == PurchaseState.IDLE
    Surface(
        color = item.color.toContainerColor(),
        contentColor = item.color.toContentColor(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        modifier = modifier.widthIn(min = 140.dp, max = 150.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Image(
                painter = painterResource(id = item.imageKey.toDrawableRes()),
                contentDescription = item.displayName,
                modifier = Modifier.size(68.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${item.cost} coins",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = item.establishmentType,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onPurchaseClick(item.type) },
                enabled = isPurchaseEnabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = purchaseState.toButtonText())
            }
        }
    }
}

private fun GameScreenState.canCurrentPlayerPurchase(): Boolean =
    players.any { it.isCurrentPlayer && it.isActivePlayer }

private fun shopTopPadding(players: List<PlayerCoinState>) = if (players.isEmpty()) 76.dp else 144.dp

@Composable
private fun ShopItemColor.toContainerColor(): Color = when (this) {
    ShopItemColor.BLUE -> Color(0xFFB5E1E5)
    ShopItemColor.GREEN -> Color(0xFFBEE6A8)
    ShopItemColor.RED -> Color(0xFFE9B1AF)
    ShopItemColor.PURPLE -> Color(0xFFDAB7E8)
    ShopItemColor.LANDMARK -> Color(0xFFE8B68F)
}

@Composable
private fun ShopItemColor.toContentColor(): Color = when (this) {
    ShopItemColor.BLUE -> MaterialTheme.colorScheme.primary
    ShopItemColor.GREEN -> Color(0xFF306514)
    ShopItemColor.RED -> Color(0xFF743A38)
    ShopItemColor.PURPLE -> Color(0xFF431755)
    ShopItemColor.LANDMARK -> Color(0xFF7D3A1E)
}

private fun PurchaseState.toButtonText(): String = when (this) {
    PurchaseState.IDLE -> "Buy"
    PurchaseState.PENDING -> "Buying"
    PurchaseState.SUCCESS -> "Bought"
}

private fun String.toDrawableRes(): Int = when (this) {
    // Maps server-aligned catalog keys to Android drawables.
    "card_wheat_field" -> R.drawable.card_wheat_field_image
    "card_ranch" -> R.drawable.card_ranch_image
    "card_bakery" -> R.drawable.card_bakery_image
    "card_cafe" -> R.drawable.card_cafe_image
    "landmark_train_station" -> R.drawable.landmark_train_station_image
    else -> R.drawable.card_bakery_image
}

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
    Surface(
        color = animatedColor,
        modifier = modifier.fillMaxWidth()
    ) {
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

@Preview(showBackground = true, widthDp = 412, heightDp = 200)
@Composable
private fun GameScreenRollDicePreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.ROLL_DICE,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                purchaseState = PurchaseState.IDLE
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 200)
@Composable
private fun GameScreenBuyOrBuildPreview() {
    ClientTheme {
        GameScreen(
            state = GameScreenState(
                gameId = 1,
                gamePhase = GamePhase.BUY_OR_BUILD,
                connectionStatus = ConnectionStatus.CONNECTED,
                players = previewPlayers(),
                purchaseState = PurchaseState.IDLE
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 200)
@Composable
private fun GameScreenNonePreview() {
    ClientTheme {
        GameScreen(state = GameScreenState.initial())
    }
}

private fun previewPlayers() = listOf(
    PlayerCoinState(
        id = "player-1",
        displayName = "You",
        coins = 6,
        isCurrentPlayer = true,
        isActivePlayer = true
    ),
    PlayerCoinState(
        id = "player-2",
        displayName = "SoupCube",
        coins = 3
    ),
    PlayerCoinState(
        id = "player-3",
        displayName = "doniliks",
        coins = 0
    )
)
