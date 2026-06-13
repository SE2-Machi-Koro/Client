package com.machikoro.client.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PanelBackgroundBeige
import com.machikoro.client.ui.theme.PrimaryOrange
import com.machikoro.client.ui.theme.TextBlueDark

/**
 * In-app tutorial page documenting the hidden "Insider Trading" cheat and the
 * accusation mechanic (client issue #336). Rendered as the final page of the
 * rules viewer ([PdfViewerScreen]) so a player paging through the tutorial
 * discovers it naturally. The wording mirrors the live in-game labels
 * ("Insider tip", "Accuse of cheating") so the docs match what players see.
 */
@Composable
fun CheatRulesContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Cheating & Accusations",
            color = PrimaryOrange,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackgroundBeige),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionTitle("Insider Trading (the cheat)")
                BodyLine(
                    "On your own turn you can secretly peek at the best card to buy: " +
                        "shake your phone, or tap the “Insider tip” button. The " +
                        "strongest affordable establishment is highlighted in green in the shop.",
                )
                BodyLine("The tip is valid for the current turn only.")
                BodyLine(
                    "Using it is reported to the server — other players can accuse you " +
                        "of cheating, so use it at your own risk.",
                )

                SectionTitle("Accusing a cheater")
                BodyLine(
                    "Suspect someone cheated? Tap that player to inspect them, then tap " +
                        "“Accuse of cheating”.",
                )
                BodyLine("You can accuse only once per turn.")
                BodyLine(
                    "If you are right, the cheater is penalised. If you are wrong, you lose " +
                        "a coin — so accuse carefully.",
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TextBlueDark,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun BodyLine(text: String) {
    Text(
        text = text,
        color = TextBlueDark,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Preview(showBackground = true)
@Composable
private fun CheatRulesContentPreview() {
    ClientTheme {
        Background(R.drawable.background_wood)
        CheatRulesContent()
    }
}
