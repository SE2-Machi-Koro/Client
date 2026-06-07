package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
            .padding(top = 8.dp,
                start = 42.dp,
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
                    .padding(
                        end = 24.dp,
                        bottom = 12.dp
                    )
            ) {
                topBar()
            }

            // =====================================
            // CONTENT AREA
            // =====================================
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // LEFT
                Box(
                    modifier = Modifier.wrapContentWidth()
                        .offset(y = (-20).dp)
                        .padding(end = 8.dp)
                ) {
                    leftContent()
                }

                // CENTER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    centerContent()
                }

                // RIGHT
                Box(
                    modifier = Modifier.wrapContentWidth()
                        .offset(y = (-20).dp)
                        .padding(
                            end = 12.dp,
                            start = 8.dp
                        )

                ) {
                    rightContent()
                }
            }
        }
    }
}
