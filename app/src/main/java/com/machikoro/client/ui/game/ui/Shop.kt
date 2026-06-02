package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.ShopItemColor
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PurchaseState
val RecommendedHighlight = Color(0xFF00C853)

@Composable
fun BuyingPhaseShop(
    state: GameScreenState,
    items: List<ShopItem>,
    onPurchaseClick: (String) -> Unit,
    recommendedCardType: CardType? = null,
    modifier: Modifier = Modifier
) {
    // Disable buying until game is IN_PROGRESS, both active-player state and a server game id are known.
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
                        state = state,
                        canPurchase = canPurchase && state.canPurchaseItem(item),
                        onPurchaseClick = onPurchaseClick,
                        isRecommended = item.type == recommendedCardType?.name,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }
            }
            state.purchaseMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = state.purchaseState.toFeedbackColor()
                )
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    state: GameScreenState,
    canPurchase: Boolean,
    onPurchaseClick: (String) -> Unit,
    isRecommended: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isPurchaseEnabled =
        canPurchase && (state.purchaseState == PurchaseState.IDLE || state.purchaseState == PurchaseState.ERROR)
    Surface(
        color = if (canPurchase) item.color.toContainerColor() else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = item.color.toContentColor(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .widthIn(min = 140.dp, max = 150.dp)
            .then(
                if (isRecommended) {
                    Modifier.border(3.dp, RecommendedHighlight, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .alpha(if (canPurchase) 1f else 0.72f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Image(
                painter = painterResource(id = ShopImageResolver.drawableFor(item.imageKey)),
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
            if (!canPurchase) {
                Text(
                    text = state.disabledReasonFor(item),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }
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
                Text(text = state.buttonTextFor(item))
            }
        }
    }
}



private fun GameScreenState.canCurrentPlayerPurchase(): Boolean = isActivePlayer && gameStatus == GameStatus.IN_PROGRESS



private fun GameScreenState.canPurchaseItem(item: ShopItem): Boolean =
    item.isAvailable &&
            hasEnoughKnownCoinsFor(item) &&
            !isKnownBuiltLandmark(item)

private fun GameScreenState.disabledReasonFor(item: ShopItem): String = when {
    gameStatus != GameStatus.IN_PROGRESS -> "Game not active"
    !isActivePlayer -> "Waiting"
    gameId == null -> "No game"
    !item.isAvailable -> "Unavailable"
    !hasEnoughKnownCoinsFor(item) -> "Need coins"
    isKnownBuiltLandmark(item) -> "Built"
    else -> "Blocked"
}

private fun GameScreenState.hasEnoughKnownCoinsFor(item: ShopItem): Boolean {
    val activePlayerCoins = players.firstOrNull { it.isActivePlayer }?.coins
    return activePlayerCoins == null || activePlayerCoins >= item.cost
}

private fun GameScreenState.isKnownBuiltLandmark(item: ShopItem): Boolean {
    if (item.purchaseType != PurchaseType.LANDMARK) return false
    val activePlayerId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull() ?: return false
    val landmarkType = runCatching { LandmarkType.valueOf(item.type) }.getOrNull() ?: return false
    return playerLandmarks[activePlayerId].orEmpty().any {
        it.landmarkType == landmarkType && it.isBuilt
    }
}
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

private fun GameScreenState.buttonTextFor(item: ShopItem): String = when {
    purchaseFeedbackItemType != item.type -> "Buy"
    purchaseState == PurchaseState.PENDING -> "Buying"
    purchaseState == PurchaseState.SUCCESS -> "Bought"
    purchaseState == PurchaseState.ERROR -> "Retry"
    else -> "Buy"
}

@Composable
private fun PurchaseState.toFeedbackColor(): Color = when (this) {
    PurchaseState.ERROR -> MaterialTheme.colorScheme.error
    PurchaseState.SUCCESS -> Color(0xFF306514)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

