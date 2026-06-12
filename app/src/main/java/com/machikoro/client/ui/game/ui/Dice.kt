package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.DecreasingLineTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val DICE_ANIMATION_INTERVAL_MS = 100L // faster change
private const val DICE_ANIMATION_DURATION_MS = 5000L // animate for fixed 5s
private const val ROLL_TIMER_SECONDS = 5
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
fun DiceAnimationDisplay(
    animating: Boolean,
    intervalMs: Long = DICE_ANIMATION_INTERVAL_MS,
    modifier: Modifier = Modifier
) {
    var currentFaceIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(animating) {
        if (animating) {
            // Keep changing faces while animating is true. This loop is cancelable when
            // the composable's LaunchedEffect key changes (animating -> false) or
            // when the composition leaves.
            while (animating && isActive) {
                currentFaceIndex = (0..5).random()
                delay(intervalMs)
            }
        }
    }

    Image(
        painter = painterResource(id = DICE_FACES[currentFaceIndex]),
        contentDescription = "Dice rolling",
        modifier = modifier.size(64.dp)
    )
}

@Composable
fun DiceCountSelector(
    diceCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = diceDrawableFor(diceCount)),
        contentDescription = "Select $diceCount dice",
        modifier = modifier
            .size(80.dp)
            .border(
                width = if (isSelected) 4.dp else 2.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
            .clickable(
                enabled = true,
                onClick = onClick,
                role = Role.Button
            )
            .alpha(if (isSelected) 1f else 0.6f)
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
        dice.forEach { value ->
            Image(
                painter = painterResource(id = diceDrawableFor(value)),
                contentDescription = "Dice showing $value",
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text = "$sum",
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
    // Only show the dice UI during the ROLL_DICE phase
    if (state.gamePhase != GamePhase.ROLL_DICE) return

    var isAnimating by remember { mutableStateOf(false) }
    // ephemeral UI selection and frozen choice when user taps a die-count
    var selectedDiceCount by remember(state.roundNumber) { mutableStateOf<Int?>(null) }
    var frozenDiceCount by remember(state.roundNumber) { mutableStateOf<Int?>(null) }
    // track whether the player has used the radio-tower reroll this round
    var hasRerolled by remember(state.roundNumber) { mutableStateOf(false) }

    // number of dice to animate: prefer server result size, else frozen, else selected, else server requested
    val animationDiceCount = state.diceResult?.size ?: frozenDiceCount ?: selectedDiceCount ?: state.requestedDiceCount

    // react to server dice result changes: when server clears result reset selection/flags
    LaunchedEffect(state.diceResult) {
        if (state.diceResult == null) {
            selectedDiceCount = null
            frozenDiceCount = null
            hasRerolled = false
        }
        // when diceResult != null we intentionally do not copy it locally; the UI reads state.diceResult directly
    }

    // run animation for fixed duration when triggered
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            delay(DICE_ANIMATION_DURATION_MS)
            isAnimating = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(bottom = 32.dp)
    ) {
        when {
                isAnimating -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(animationDiceCount) {
                        DiceAnimationDisplay(animating = true)
                    }
                }
            }
            state.diceResult != null -> {
                // show authoritative server result when available and not animating
                DiceResultDisplay(dice = state.diceResult)
            }
            else -> {
                // nothing to show
            }
        }

        // only show controls when it's the rolling phase and player may act
        if (state.isActivePlayer &&
            state.gameStatus == GameStatus.IN_PROGRESS &&
            !isAnimating
        ) {
            // if player has train station, offer a 1/2 selector (numbers)
            if (state.hasTrainStation) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    listOf(1, 2).forEach { count ->
                        // simple numeric selector with border highlight
                        val isSelected = (selectedDiceCount ?: state.requestedDiceCount) == count
                        DiceCountSelector(
                            diceCount = count,
                            isSelected = isSelected,
                            onClick = {
                                selectedDiceCount = count
                                frozenDiceCount = count
                            }
                        )
                    }
                }
            }
            // show a timer above the roll/reroll control (UI only)
            DecreasingLineTimer(ROLL_TIMER_SECONDS)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.diceResult == null) {
                    Image(
                        painter = painterResource(id = R.drawable.game_dice_perspective),
                        contentDescription = "Dice",
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Roll button: visible when server has not provided a result yet
                if (state.diceResult == null) {
                    val chosen = frozenDiceCount ?: selectedDiceCount ?: state.requestedDiceCount
                    ActionButton(
                        onClick = {
                            // freeze selection, start local animation and request roll
                            frozenDiceCount = chosen
                            isAnimating = true
                            onRollDice(chosen)
                        },
                        enabled = true,
                        label = "Roll Dice",
                        modifier = Modifier.semantics {
                            contentDescription = "Roll Dice"
                        }
                    )
                }

                val showResult = !isAnimating && state.diceResult != null

                // Reroll button: only offered when server has produced a result and player has radio tower
                if (showResult && state.hasRadioTower && !hasRerolled) {
                    val chosen = frozenDiceCount ?: selectedDiceCount ?: state.requestedDiceCount
                    ActionButton(
                        onClick = {
                            // mark reroll used, freeze choice (if not already), start animation and request reroll
                            hasRerolled = true
                            frozenDiceCount = chosen
                            isAnimating = true
                            onRollDice(chosen)
                        },
                        enabled = true,
                        label = "Roll Dice Again",
                        modifier = Modifier.semantics {
                            contentDescription = "Reroll Dice"
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun DiceAnimationPreview() {
    DiceAnimationDisplay(animating = true, intervalMs = 80)
}