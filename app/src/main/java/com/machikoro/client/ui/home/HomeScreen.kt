package com.machikoro.client.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.ui.theme.ButtonBlueDark
import com.machikoro.client.ui.theme.ButtonBlueLight
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.TextBlueDark
import com.machikoro.client.ui.theme.TextWhite
import com.machikoro.client.ui.theme.White

@Composable
fun HomeScreen(
    joinLobbyCode: String = "",
    showJoinLobbyInput: Boolean = false,
    onJoinLobbyCodeChange: (String) -> Unit = {},
    onJoinLobbyClick: () -> Unit = {},
    onCreateLobbyClick: () -> Unit = {},
    onPublicLobbiesClick: () -> Unit = {},
    onRulesClick: () -> Unit = {},
    onRankingClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onJoinLobbySubmit: () -> Unit = {},
    joinLobbyError: Boolean = false,
    hasActiveGame: Boolean = false,
    onResumeGameClick: () -> Unit = {},
    onPurgeClick: () -> Unit = {},
    onLogoutClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    // Root container. Box allows placing elements freely with align().
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(White)
    ) {
        // === BACKGROUND LAYER ===
        // Decorative background image on the bottom left.
        Image(
            painter = painterResource(id = R.drawable.background_left),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomStart)
               .offset(x = 0.dp, y = 53.dp)
                .blur(3.5.dp)
        )

        // Decorative background image on the bottom right.
        Image(
            painter = painterResource(id = R.drawable.background_right),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 15.dp, y = 53.dp)
                .blur(3.5.dp)
        )

        // White transparent overlay for better readability.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 0.dp, y = 40.dp)
                .background(Color.White.copy(alpha = 0.7f))
        )

        // === HEADER ===
        // Main title.
        Text(
            text = "MACHI KORO",
            style = MaterialTheme.typography.headlineMedium,
            color = TextBlueDark,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp),
        )

        // === PROFILE SECTION ===
        // User profile card in the top right corner.
        ProfileCard(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 30.dp)
        )

        // Logout affordance in the top-left corner. Issue #106 places the
        // logout action on the authenticated screen; the start screen never
        // shows it because the routing in AppRoot collapses HomeScreen back to
        // StartScreen the moment the session clears.
        LogoutButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 20.dp, start = 30.dp)
        )

        // Small dev-only button — clears all games/lobbies from the DB
        Text(
            text = "⚙ Purge DB",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Red.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 70.dp, start = 32.dp)
                .clickable { onPurgeClick() }
        )

        // === MAIN ACTION BUTTONS ===
        // Three main lobby actions in the center of the screen.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 20.dp)
                .height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(60.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
            ) {
                // Join lobby card. The code input only appears after clicking the card.
                Column(
                    modifier = Modifier.width(150.dp),
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

                        JoinLobbyCodeRow(
                            code = joinLobbyCode,
                            onCodeChange = onJoinLobbyCodeChange,
                            onJoinLobbySubmit = onJoinLobbySubmit,
                            isError = joinLobbyError
                        )
                    }
                }

                // Create lobby card with generated code displayed directly below it.
                Column(
                    modifier = Modifier.width(150.dp),
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

                HomeCard(
                    iconRes = R.drawable.home_lobby_join_icon,
                    text = "Resume Game",
                    isPrimary = false,
                    enabled = hasActiveGame,
                    onClick = onResumeGameClick
                )
            }
        }

        // === BOTTOM MENU ===
        // Clickable menu items for rules, ranking and settings.
        BottomMenuBar(
            onRulesClick = onRulesClick,
            onRankingClick = onRankingClick,
            onSettingsClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ButtonBlueDark,
            contentColor = TextWhite,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(
            text = "Logout",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
        )
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
    // The primary card is highlighted with dark blue.
    val backgroundColor = if (isPrimary) ButtonBlueDark else ButtonBlueLight
    val textColor = if (isPrimary) TextWhite else TextBlueDark

    // Button is used as a card because it is already clickable and supports elevation.
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(160.dp)
            .height(140.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Action icon.
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(70.dp)
            )

            // Action text. It stays in one line and becomes shortened if needed.
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

@Composable
private fun JoinLobbyCodeRow(
    code: String,
    onCodeChange: (String) -> Unit,
    onJoinLobbySubmit: () -> Unit,
    isError: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Card(
            modifier = Modifier
                .width(110.dp)
                .height(34.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = White),
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
        Card(
            modifier = Modifier
                .size(34.dp)
                .clickable(
                    enabled = code.isNotBlank(),
                    onClick = onJoinLobbySubmit
                ),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_check_icon),
                    contentDescription = "Join lobby",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    modifier: Modifier = Modifier
) {
    // Small card showing user avatar, username and edit icon.
    Card(
        modifier = modifier
            .width(150.dp)
            .height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
            ) {
            // User avatar icon.
            Image(
                painter = painterResource(id = R.drawable.login_user_icon),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )

            // Placeholder username.
            Text(
                text = "NN",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextBlueDark
            )

            // Edit icon. Later this can open a name edit dialog.
            Image(
                painter = painterResource(id = R.drawable.home_edit_icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BottomMenuBar(
    onRulesClick: () -> Unit,
    onRankingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Bottom menu container.
    Card(
        modifier = modifier
            .width(475.dp)
            .height(50.dp),
        shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp),
        colors = CardDefaults.cardColors(containerColor = White),
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

            // Opens the settings screen.
            BottomMenuItem(
                iconRes = R.drawable.home_settings_icon,
                text = "Settings",
                onClick = onSettingsClick
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
    // Single clickable menu item inside the bottom bar.
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Menu icon.
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(30.dp)
        )

        // Menu label.
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextBlueDark,
            maxLines = 1
        )
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
