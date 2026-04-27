package com.machikoro.client.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.ui.theme.*

@Composable
fun HomeScreen(
    // Callbacks passed from outside, e.g. from Navigation or ViewModel.
    // This keeps the UI separated from the app logic.
    onJoinLobbyClick: () -> Unit = {},
    onCreateLobbyClick: () -> Unit = {},
    onPublicLobbiesClick: () -> Unit = {},
    onRulesClick: () -> Unit = {},
    onRankingClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
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
                .offset(x = -15.dp, y = 25.dp)
                .blur(3.5.dp)
        )

        // Decorative background image on the bottom right.
        Image(
            painter = painterResource(id = R.drawable.background_right),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 15.dp, y = 25.dp)
                .blur(3.5.dp)
        )

        // White transparent overlay for better readability.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.65f))
        )

        // === HEADER ===
        // Main title.
        Text(
            text = "WILLKOMMEN",
            style = MaterialTheme.typography.headlineMedium,
            color = TextBlueDark,
            modifier = Modifier.padding(start = 90.dp, top = 29.dp)
        )

        // Subtitle below the title.
        Text(
            text = "Lass uns spielen!",
            style = MaterialTheme.typography.headlineMedium,
            color = TextBlueLight,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 90.dp, top = 65.dp)
        )

        // === PROFILE SECTION ===
        // User profile card in the top right corner.
        ProfileCard(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 29.dp, end = 90.dp)
        )

        // === MAIN ACTION BUTTONS ===
        // Three main lobby actions in the center of the screen.
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 50.dp),
            horizontalArrangement = Arrangement.spacedBy(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeCard(
                iconRes = R.drawable.home_lobby_join_icon,
                text = "Lobby beitreten",
                isPrimary = false,
                onClick = onJoinLobbyClick
            )

            HomeCard(
                iconRes = R.drawable.home_lobby_create_icon,
                text = "Lobby erstellen",
                isPrimary = true,
                onClick = onCreateLobbyClick
            )

            HomeCard(
                iconRes = R.drawable.home_lobby_public_icon,
                text = "Öffentliche Lobbys",
                isPrimary = false,
                onClick = onPublicLobbiesClick
            )
        }

        // === BOTTOM MENU ===
        // Clickable menu items for rules, ranking and settings.
        BottomMenuBar(
            onRulesClick = onRulesClick,
            onRankingClick = onRankingClick,
            onSettingsClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp)
        )
    }
}

@Composable
private fun HomeCard(
    iconRes: Int,
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    // The primary card is highlighted with dark blue.
    val backgroundColor = if (isPrimary) ButtonBlueDark else ButtonBlueLight
    val textColor = if (isPrimary) TextWhite else TextBlueDark

    // Button is used as a card because it is already clickable and supports elevation.
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(140.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
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
                modifier = Modifier.size(74.dp)
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
private fun ProfileCard(
    modifier: Modifier = Modifier
) {
    // Small card showing user avatar, username and edit icon.
    Card(
        modifier = modifier
            .width(171.dp)
            .height(55.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ButtonBlueGrey),
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
                modifier = Modifier.size(40.dp)
            )

            // Placeholder username.
            Text(
                text = "NN",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlueDark
            )

            // Edit icon. Later this can open a name edit dialog.
            Image(
                painter = painterResource(id = R.drawable.home_edit_icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
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
            .height(60.dp),
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        colors = CardDefaults.cardColors(containerColor = ButtonBlueGrey),
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
                text = "Regeln",
                onClick = onRulesClick
            )

            // Opens the ranking / leaderboard screen.
            BottomMenuItem(
                iconRes = R.drawable.home_rang_icon,
                text = "Rangliste",
                onClick = onRankingClick
            )

            // Opens the settings screen.
            BottomMenuItem(
                iconRes = R.drawable.home_settings_icon,
                text = "Einstellungen",
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
            modifier = Modifier.size(35.dp)
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

@Preview(
    showBackground = true,
    widthDp = 917,
    heightDp = 412
)
@Composable
private fun HomeScreenPreview() {
    ClientTheme {
        HomeScreen()
    }
}