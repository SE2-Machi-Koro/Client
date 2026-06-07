package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.state.toDisplayText
import kotlin.collections.get
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.machikoro.client.R

private val SURFACE_COLOR = Color(0xFF8F7365)


// todo: adjust to current figma design, keep landmark badges

@Composable
fun PlayersTopBar(
    players: List<PlayerCoinState>,
    playerLandmarks: Map<Int, List<PlayerLandmarkState>>,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(
            bottomStart = 10.dp,
            bottomEnd = 12.dp
        ),
        color = SURFACE_COLOR,
        shadowElevation = 3.dp,
        modifier = modifier
            .wrapContentSize()
            .semantics {
                contentDescription = ""},
    ) {
        LazyRow(
            modifier = modifier.wrapContentWidth()
                .padding(bottom = 2.dp, end = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(
                items = players,
                key = { it.id }
            ) { player ->

                val playerId = player.id.toIntOrNull()

                PlayerCoinBadge(
                    player = player,
                    landmarks = playerLandmarks[playerId].orEmpty()
                )
            }
        }
    }
}

@Composable
private fun PlayerCoinBadge(
    player: PlayerCoinState,
    landmarks: List<PlayerLandmarkState>,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        player.isActivePlayer -> Color(0xFFFFFFFF)
        else -> Color(0xB3FFFFFF)
    }

    val textColor = Color(0xFF004E7E)

    val scale = if(player.isActivePlayer) 1.0f else 0.95f
    val fontSize = if(player.isActivePlayer) 18.sp else 16.sp
    Box(modifier = Modifier.scale(scale)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = backgroundColor,
                shadowElevation = 3.dp,
                modifier = modifier
                    .wrapContentSize()
                    .widthIn(max = 140.dp)
                    .semantics {
                        contentDescription =
                            "${player.displayName}, ${player.coins} coins"
                    },

                ) {
                Column(
                    modifier = modifier
                        .padding(horizontal = 28.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val name = if (player.isCurrentPlayer) "You" else player.displayName
                    // Player name
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // Landmarks
                    LandmarkRow(landmarks)
                }

            }
        val opacity = if (player.isCurrentPlayer) 0f else 1f
            // Coin
            CoinBadge(
                amount = player.coins,
                 modifier = Modifier.align(Alignment.BottomEnd)
                 .offset(x = 15.dp, y = 12.dp)
                 .alpha(opacity)
            )
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
