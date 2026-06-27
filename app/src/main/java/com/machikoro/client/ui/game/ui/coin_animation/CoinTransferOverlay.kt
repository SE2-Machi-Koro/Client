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

private const val TransferDurationMillis = 700
private const val TransferPauseMillis = 150L

@Composable
fun CoinTransferOverlay(
    transfers: List<CoinTransferUi>,
    playerPositions: Map<Int, Offset>,
    modifier: Modifier = Modifier,
    onTransferChanged: (CoinTransferUi?) -> Unit = {},
    onFinished: () -> Unit = {},
) {
    val density = LocalDensity.current
    val coinRadiusPx = with(density) { 18.dp.toPx() }
    val positionsReady = transfers.all { transfer ->
        transfer.toPlayerId?.let(playerPositions::containsKey) == true &&
            (transfer.fromPlayerId == null || playerPositions.containsKey(transfer.fromPlayerId))
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
                onTransferChanged(null)
                onFinished()
                return@LaunchedEffect
            }
            if (!positionsReady) return@LaunchedEffect

            transfers.forEach { transfer ->
                activeTransfer = transfer
                onTransferChanged(transfer)
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
            onTransferChanged(null)
            onFinished()
        }

        activeTransfer?.let { transfer ->
            val start = transfer.fromPlayerId
                ?.let(playerPositions::get)
                ?: bankPosition
            val end = transfer.toPlayerId
                ?.let(playerPositions::get)
                ?: return@let
            val position = lerp(start, end, progress.value)

            Box(modifier = Modifier.fillMaxSize()) {
                CoinBadge(
                    amount = transfer.amount,
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
