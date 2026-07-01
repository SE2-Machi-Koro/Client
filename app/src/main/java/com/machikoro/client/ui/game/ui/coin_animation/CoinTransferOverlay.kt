package com.machikoro.client.ui.game.ui.coin_animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.machikoro.client.ui.game.ui.CoinBadge
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val TransferDurationMillis = 900
private const val TransferPauseMillis = 200L
private const val CardStackLeadInMillis = 1_200L

@Composable
fun CoinTransferOverlay(
    transfers: List<CoinTransferUi>,
    playerPositions: Map<Int, Offset>,
    effectCardPositions: Map<Int, Offset>,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    val density = LocalDensity.current
    val coinRadiusPx = with(density) { 18.dp.toPx() }
    val positionsReady = transfers.all { transfer ->
        (
            transfer.fromPlayerId?.let(playerPositions::containsKey)
                ?: transfer.toPlayerId?.let(effectCardPositions::containsKey)
                ?: true
            ) &&
            (transfer.toPlayerId == null || playerPositions.containsKey(transfer.toPlayerId))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val bankPosition = with(density) {
            Offset(maxWidth.toPx() / 2f, maxHeight.toPx() / 2f)
        }
        val progress = remember { Animatable(0f) }
        var activeTransfer by remember { mutableStateOf<CoinTransferUi?>(null) }

        LaunchedEffect(transfers, positionsReady) {
            if (transfers.isEmpty()) {
                activeTransfer = null
                onFinished()
                return@LaunchedEffect
            }
            if (!positionsReady) return@LaunchedEffect

            delay(CardStackLeadInMillis)
            transfers.forEach { transfer ->
                activeTransfer = transfer
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = TransferDurationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
                delay(TransferPauseMillis)
            }

            activeTransfer = null
            onFinished()
        }

        activeTransfer?.let { transfer ->
            val start = transfer.fromPlayerId
                ?.let(playerPositions::get)
                ?: transfer.toPlayerId?.let(effectCardPositions::get)
                ?: bankPosition
            val end = transfer.toPlayerId
                ?.let(playerPositions::get)
                ?: bankPosition
            val position = lerp(start, end, progress.value)
            val highlight = if (transfer.toPlayerId == null) {
                CoinChangeHighlight.LOSS
            } else {
                CoinChangeHighlight.GAIN
            }

            Box(modifier = Modifier.fillMaxSize()) {
                CoinBadge(
                    amount = transfer.amount,
                    highlight = highlight,
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (position.x - coinRadiusPx).roundToInt(),
                            y = (position.y - coinRadiusPx).roundToInt(),
                        )
                    },
                )
            }
        }
    }
}
