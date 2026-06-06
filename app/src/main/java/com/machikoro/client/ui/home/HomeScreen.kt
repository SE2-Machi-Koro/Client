package com.machikoro.client.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.BuildConfig
import com.machikoro.client.R
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.shared.Header
import com.machikoro.client.ui.shared.SecondaryActionButton
import com.machikoro.client.ui.theme.ButtonBeigeLight
import com.machikoro.client.ui.theme.ButtonBlueDark
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PanelBackgroundBeige
import com.machikoro.client.ui.theme.PanelBackgroundBeigeDark
import com.machikoro.client.ui.theme.PanelBorder
import com.machikoro.client.ui.theme.TextBlueDark
import com.machikoro.client.ui.theme.TextWhite


@Composable
fun HomeScreen(
    username: String? = null,
    joinLobbyCode: String = "",
    showJoinLobbyInput: Boolean = false,
    onJoinLobbyCodeChange: (String) -> Unit = {},
    onJoinLobbyClick: () -> Unit = {},
    onCreateLobbyClick: () -> Unit = {},
    onRankingClick: () -> Unit = {},
    onJoinLobbySubmit: () -> Unit = {},
    joinLobbyError: Boolean = false,
    hasActiveGame: Boolean = false,
    onResumeGameClick: () -> Unit = {},
    onPurgeClick: () -> Unit = {},
    onLogoutClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {

    var showPdfViewer by remember { mutableStateOf(false) }

    if (showPdfViewer) {
        PdfViewerScreen(
            onClose = { showPdfViewer = false }
        )
    } else {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

    Background(R.drawable.background_wood)
    // Root container. Box allows placing elements freely with align().
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 30.dp, start = 30.dp, end = 30.dp)
    ) {
        // === HEADER ===
        // Main title.

            Header("WELCOME",
                modifier = Modifier
                    .padding(top = 24.dp)
                    .align(Alignment.TopCenter),
                fontSize = 56
            )


        // === PROFILE SECTION ===
        // User profile card in the top right corner.
        // Shows the logged-in username in the top right corner.
        SecondaryActionButton(
            label = username ?: "Guest",
            modifier = Modifier.align(Alignment.TopEnd),
            onClick = null,
            leftIcon = R.drawable.login_user_icon
            )

        // Logout affordance in the top-left corner. Issue #106 places the
        // logout action on the authenticated screen; the start screen never
        // shows it because the routing in AppRoot collapses HomeScreen back to
        // StartScreen the moment the session clears.
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                ActionButton(
                    "Logout",
                    onClick = onLogoutClick
                )

                if (BuildConfig.DEBUG) {
                    Text(
                        text = "⚙ Purge DB",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        modifier = Modifier
                            .clickable { onPurgeClick() }
                    )
                }
            }
        }

        // === MAIN ACTION BUTTONS ===
        // Three main lobby actions in the center of the screen.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 20.dp)
                .height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(60.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
            ) {
                // Join lobby card. The code input only appears after clicking the card.
                Column(
                    modifier = Modifier.width(174.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HomeCard(
                        iconRes = R.drawable.home_lobby_join_icon,
                        text = "Join Lobby",
                        isPrimary = false,
                        enabled = !hasActiveGame,
                        onClick = onJoinLobbyClick
                    )
                    if (showJoinLobbyInput) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            JoinLobbyCodeRow(
                                code = joinLobbyCode,
                                onCodeChange = onJoinLobbyCodeChange,
                                onJoinLobbySubmit = onJoinLobbySubmit,
                                isError = joinLobbyError
                            )
                        }
                    }
                }

                // Create lobby card with generated code displayed directly below it.
                Column(
                    modifier = Modifier.width(174.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HomeCard(
                        iconRes = R.drawable.home_lobby_create_icon,
                        text = "Create Lobby",
                        isPrimary = true,
                        enabled = !hasActiveGame,
                        onClick = onCreateLobbyClick
                    )
                }

                Column(
                    modifier = Modifier.width(174.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HomeCard(
                        iconRes = R.drawable.home_resume_game_icon,
                        text = "Resume Game",
                        isPrimary = false,
                        enabled = hasActiveGame,
                        onClick = onResumeGameClick
                    )
                }
            }
        }

        // === BOTTOM MENU ===
        // Clickable menu items for rules, ranking and settings.
        BottomMenuBar(
            onRulesClick = { showPdfViewer = true },
            onRankingClick = onRankingClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
            )
        }
    }
}
}
@Composable
private fun HomeCard(
    iconRes: Int,
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor =
        when {
            !enabled -> ButtonBeigeLight.copy(alpha = 0.55f)
            isPrimary -> ButtonBlueDark
            else -> ButtonBeigeLight
        }

    val textColor =
        when {
            !enabled -> TextBlueDark.copy(alpha = 0.45f)
            isPrimary -> TextWhite
            else -> TextBlueDark
        }

    val contentAlpha = if (enabled) 1f else 0.45f
    val shadowAlpha = if (enabled) 1f else 0.45f

    Box(
        modifier = Modifier
            .width(174.dp)
            .height(144.dp)
    ) {
        // Bottom/side shadow border
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 4.dp)
                .clip(shape)
                .background(PanelBorder.copy(alpha = shadowAlpha))
        )
        // Bottom/side shadow border
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = (-3).dp, y = 4.dp)
                .clip(shape)
                .background(PanelBorder.copy(alpha = shadowAlpha))        )

        // Main card
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(backgroundColor)
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.decor_screw),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 10.dp, y = 7.dp),
                alpha = contentAlpha
            )

            Image(
                painter = painterResource(id = R.drawable.decor_screw),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = 7.dp),
                alpha = contentAlpha
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    alpha = contentAlpha
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun JoinLobbyCodeRow(
    code: String,
    onCodeChange: (String) -> Unit,
    onJoinLobbySubmit: () -> Unit,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .width(137.dp)
                .height(34.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackgroundBeige),
            border = BorderStroke(
                width = if (isError) 2.dp else 0.dp,
                color = if (isError) MaterialTheme.colorScheme.error else Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = { input ->
                        onCodeChange(input.uppercase())
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextBlueDark,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (code.isBlank()) {
                            Text(
                                text = "Code",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0x4D004E7E),
                                maxLines = 1
                            )
                        }

                        innerTextField()
                    }
                )
            }
        }

        // Sends the entered lobby code to the backend.
        Box(
            modifier = Modifier
                .size(34.dp)
                .clickable(
                    enabled = code.isNotBlank(),
                    onClick = onJoinLobbySubmit
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.home_check_icon),
                contentDescription = "Join lobby",
                modifier = Modifier.size(29.dp)
            )
        }
    }
}

@Composable
private fun BottomMenuBar(
    onRulesClick: () -> Unit,
    onRankingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Bottom menu container.
    Card(
        modifier = modifier
            .width(430.dp)
            .height(60.dp),
        shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBackgroundBeigeDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Opens the rules screen / PDF viewer.
            BottomMenuItem(
                iconRes = R.drawable.home_rules_icon,
                text = "Rules",
                onClick = onRulesClick
            )

            // Opens the ranking / leaderboard screen.
            BottomMenuItem(
                iconRes = R.drawable.home_rang_icon,
                text = "Leaderboard",
                onClick = onRankingClick
            )
        }
    }
}

@Composable
private fun BottomMenuItem(
    iconRes: Int,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(42.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ButtonBeigeLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
                //.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextBlueDark,
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun HomeScreenPreview() {
    ClientTheme {
        HomeScreen(
            onLogoutClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun HomeScreenWithJoinLobbyCodePreview() {
    ClientTheme {
        HomeScreen(
            onLogoutClick = {},
            joinLobbyCode = "AJ25Z39",
            showJoinLobbyInput = true,
            onJoinLobbyCodeChange = {},
            onJoinLobbySubmit = {},
        )
    }
}
