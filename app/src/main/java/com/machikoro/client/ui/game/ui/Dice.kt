package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.ui.shared.ActionButton
import kotlinx.coroutines.delay

private const val DICE_ANIMATION_INTERVAL_MS = 300L
private val DICE_FACES = listOf(
    R.drawable.game_dice_1,
    R.drawable.game_dice_2,
    R.drawable.game_dice_3,
    R.drawable.game_dice_4,
    R.drawable.game_dice_5,
    R.drawable.game_dice_6
)

fun diceDrawableFor(value: Int): Int =
    DICE_FACES.getOrElse(value - 1) { R.drawable.game_dice_perspective }

@Composable
fun DiceAnimationDisplay(modifier: Modifier = Modifier) {
    var currentFaceIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(DICE_ANIMATION_INTERVAL_MS)
            currentFaceIndex = (0..5).random()
        }
    }

    Image(
        painter = painterResource(id = DICE_FACES[currentFaceIndex]),
        contentDescription = "Dice rolling",
        modifier = modifier.size(64.dp)
    )
}

@Composable
fun DiceResultDisplay(
    dice: List<Int>,
    modifier: Modifier = Modifier
) {
    val sum = dice.sum()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show each die with its actual face
        dice.forEach { value ->
            Image(
                painter = painterResource(id = diceDrawableFor(value)),
                contentDescription = "Dice showing $value",
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text = "= $sum",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun DiceSection(
    state: GameScreenState,
    onRollDice: (diceCount: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(bottom = 32.dp)
    ) {
        // Dice display area
        when {
            state.isRolling -> {
                // Show one or two animated dice depending on selected count
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val diceCount = if (state.hasTrainStation) 2 else 1
                    repeat(diceCount) {
                        DiceAnimationDisplay()
                    }
                }
            }
            state.diceResult != null -> DiceResultDisplay(dice = state.diceResult)
        }

        if (state.isActivePlayer && state.gameStatus == GameStatus.IN_PROGRESS) {
            var selectedDiceCount by remember(state.roundNumber) { mutableIntStateOf(1) }

            // 1🎲 / 2🎲 toggle buttons when Train Station is built
            if (state.hasTrainStation) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2).forEach { count ->
                        Button(
                            onClick = { selectedDiceCount = count },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedDiceCount == count)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedDiceCount == count)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("$count 🎲")
                        }
                    }
                }
            }

            // Roll Dice button with static dice image before rolling
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!state.isRolling && state.diceResult == null) {
                    Image(
                        painter = painterResource(id = R.drawable.game_dice_perspective),
                        contentDescription = "Dice",
                        modifier = Modifier.size(48.dp)
                    )
                }
                ActionButton(
                    onClick = { onRollDice(if (state.hasTrainStation) selectedDiceCount else 1) },
                    enabled = !state.isRolling,
                    label = if (state.diceResult == null) "Roll Dice" else "Roll Dice Again",
                    modifier = Modifier.semantics {
                        contentDescription = "Roll Dice"
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun DiceAnimationPreview() {
    DiceAnimationDisplay()
}