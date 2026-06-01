package com.machikoro.client.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.network.leaderboard.LeaderboardEntry
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.AnimatedItem
import com.machikoro.client.ui.shared.AnimationType
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.shared.Header
import com.machikoro.client.ui.shared.RegularInfoText
import com.machikoro.client.ui.theme.ButtonBlueDark
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PrimaryBeigeLight
import com.machikoro.client.ui.theme.TextBlueDark

// Medal colors for top 3 ranks
private val GoldColor = Color(0xFFD4AF37)
private val SilverColor = Color(0xFFC0C0C0)
private val BronzeColor = Color(0xFFCD7F32)

@Composable
fun LeaderboardScreen(
    state: LeaderboardState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
) {
    // Reload each time the screen is entered so data reflects current login session
    LaunchedEffect(Unit) { onRetry() }

    Box(modifier = Modifier.fillMaxSize()) {
        Background(R.drawable.home_screen_background)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header("LEADERBOARD")

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.85f),
            ) {
                when (state) {
                    is LeaderboardState.Loading -> LoadingContent()
                    is LeaderboardState.Error -> ErrorContent(state.message, onRetry)
                    is LeaderboardState.Success -> {
                        if (state.entries.isEmpty()) {
                            EmptyContent()
                        } else {
                            // Slide the table up when data arrives
                            AnimatedItem(delayMillis = 0, animationType = AnimationType.SlideUp) {
                                TableContent(state.entries)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ActionButton(label = "Back", onClick = onBackClick)
        }
    }
}

@Composable
private fun TableContent(entries: List<LeaderboardEntry>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryBeigeLight.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        TableHeaderRow()

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            color = ButtonBlueDark.copy(alpha = 0.4f),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(entries) { index, entry ->
                TableEntryRow(entry = entry, isEven = index % 2 == 0)
            }
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#",
            modifier = Modifier.width(52.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextBlueDark,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Player",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextBlueDark,
        )
        Text(
            text = "Wins",
            modifier = Modifier.width(70.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextBlueDark,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Games Played",
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextBlueDark,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TableEntryRow(entry: LeaderboardEntry, isEven: Boolean) {
    val rowBackground = if (isEven) Color.Transparent else ButtonBlueDark.copy(alpha = 0.06f)
    val rankColor = when (entry.rank) {
        1 -> GoldColor
        2 -> SilverColor
        3 -> BronzeColor
        else -> TextBlueDark
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.rank.toString(),
            modifier = Modifier.width(52.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = rankColor,
            fontSize = if (entry.rank <= 3) 18.sp else 16.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = entry.username,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextBlueDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = entry.totalWins.toString(),
            modifier = Modifier.width(70.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextBlueDark,
            textAlign = TextAlign.Center,
        )
        Text(
            text = entry.totalGamesPlayed.toString(),
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextBlueDark,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ButtonBlueDark)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RegularInfoText("Could not load leaderboard")
        Spacer(modifier = Modifier.height(8.dp))
        RegularInfoText(message)
        Spacer(modifier = Modifier.height(16.dp))
        ActionButton(label = "Retry", onClick = onRetry)
    }
}

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        RegularInfoText("No winners yet — be the first!")
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun LeaderboardScreenSuccessPreview() {
    ClientTheme {
        LeaderboardScreen(
            state = LeaderboardState.Success(
                entries = listOf(
                    LeaderboardEntry(rank = 1, userId = 1, username = "alice", totalWins = 17, totalGamesPlayed = 30),
                    LeaderboardEntry(rank = 2, userId = 2, username = "bob", totalWins = 12, totalGamesPlayed = 25),
                    LeaderboardEntry(rank = 3, userId = 3, username = "charlie", totalWins = 8, totalGamesPlayed = 20),
                    LeaderboardEntry(rank = 4, userId = 4, username = "diana", totalWins = 5, totalGamesPlayed = 18),
                    LeaderboardEntry(rank = 5, userId = 5, username = "eve", totalWins = 3, totalGamesPlayed = 12),
                )
            ),
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 430)
@Composable
private fun LeaderboardScreenLoadingPreview() {
    ClientTheme {
        LeaderboardScreen(
            state = LeaderboardState.Loading,
            onBackClick = {},
            onRetry = {},
        )
    }
}