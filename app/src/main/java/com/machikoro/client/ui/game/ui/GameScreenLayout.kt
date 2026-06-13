package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun GameScreenLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable BoxScope.() -> Unit = {},
    leftContent: @Composable BoxScope.() -> Unit = {},
    centerContent: @Composable BoxScope.() -> Unit = {},
    rightContent: @Composable BoxScope.() -> Unit = {},
) {

    var maxTopBarHeightPx by remember {
        mutableIntStateOf(0)
    }

    var leftWidthPx by remember {
        mutableIntStateOf(0)
    }

    var rightWidthPx by remember {
        mutableIntStateOf(0)
    }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 42.dp)
    ) {

        // TOP BAR
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(
                    end = 24.dp,
                    bottom = 12.dp
                )
                .onSizeChanged {
                    if (it.height > maxTopBarHeightPx) {
                        maxTopBarHeightPx = it.height
                    }
                }
        ) {
            topBar()
        }

        // LEFT
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    end = 8.dp,
                    bottom = 32.dp
                )
                .onSizeChanged {
                    if (leftWidthPx != it.width) {
                        leftWidthPx = it.width
                    }
                }
        ) {
            leftContent()
        }

        // RIGHT
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    end = 12.dp,
                    start = 8.dp,
                    bottom = 24.dp
                )
                .onSizeChanged {
                    if (rightWidthPx != it.width) {
                        rightWidthPx = it.width
                    }
                }
        ) {
            rightContent()
        }

        // CENTER
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = with(density) {
                        maxTopBarHeightPx.toDp() + 12.dp
                    },
                    start = with(density) {
                        leftWidthPx.toDp() + 8.dp
                    },
                    end = with(density) {
                        rightWidthPx.toDp() + 20.dp
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            centerContent()
        }
    }
}