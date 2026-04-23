package com.machikoro.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.model.state.ConnectionStatus
import com.machikoro.client.viewmodel.GameViewModel

@Composable
fun GameScreen(vm: GameViewModel) {
    val connectionStatus by vm.connectionStatus.collectAsState()
    val diceResult       by vm.diceResult.collectAsState()
    val canRollTwo       by vm.canRollTwo.collectAsState()

    var selectedDiceCount by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Verbindungsstatus
        Text(
            text = when (connectionStatus) {
                ConnectionStatus.CONNECTED    -> "🟢 Verbunden"
                ConnectionStatus.CONNECTING   -> "🟡 Verbinde..."
                ConnectionStatus.DISCONNECTED -> "🔴 Getrennt"
                ConnectionStatus.ERROR        -> "❌ Fehler"
                ConnectionStatus.IDLE         -> "⚪ Bereit"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(32.dp))

        // Würfelanzahl-Auswahl (nur wenn freigeschaltet)
        if (canRollTwo) {
            Text("Wie viele Würfel?", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(1, 2).forEach { count ->
                    FilterChip(
                        selected = selectedDiceCount == count,
                        onClick = { selectedDiceCount = count },
                        label = { Text("$count Würfel") }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Würfelergebnis
        diceResult?.let { result ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                result.dice.forEach { value ->
                    Text(diceFace(value), fontSize = 64.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (result.dice.size > 1)
                    "${result.dice[0]} + ${result.dice[1]} = ${result.total}"
                else
                    "Ergebnis: ${result.total}",
                style = MaterialTheme.typography.titleLarge
            )
        } ?: Text(
            "Noch nicht gewürfelt",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(32.dp))

        // Würfeln-Button
        Button(
            onClick = { vm.rollDice(selectedDiceCount) },
            enabled = connectionStatus == ConnectionStatus.CONNECTED,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("🎲 Würfeln", fontSize = 18.sp)
        }
    }
}

private fun diceFace(value: Int): String = when (value) {
    1 -> "⚀"; 2 -> "⚁"; 3 -> "⚂"
    4 -> "⚃"; 5 -> "⚄"; 6 -> "⚅"
    else -> "?"
}