package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun GameScreenLayout(
    modifier: Modifier = Modifier,

    topBar: @Composable BoxScope.() -> Unit = {},
    leftContent: @Composable BoxScope.() -> Unit = {},
    centerContent: @Composable BoxScope.() -> Unit = {},
    rightContent: @Composable BoxScope.() -> Unit = {}
) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = 42.dp,
                end = 8.dp
            )
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // =====================================
            // TOP BAR
            // =====================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                topBar()
            }

            // =====================================
            // CONTENT AREA
            // =====================================
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                // LEFT
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentWidth()
                ) {
                    leftContent()
                }

                // CENTER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    centerContent()
                }

                // RIGHT
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentWidth()
                ) {
                    rightContent()
                }
            }
        }
    }
}
