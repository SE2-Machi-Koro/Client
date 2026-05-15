package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.LandmarkType

/**
 * Human-readable labels for snapshot enums. `WHEAT_FIELD` → "Wheat Field".
 * Kept here alongside the other `toDisplayText()` helpers so the UI never
 * renders raw SCREAMING_SNAKE_CASE enum names.
 */
fun LandmarkType.toDisplayText(): String = name.humanizeEnumName()

fun CardType.toDisplayText(): String = name.humanizeEnumName()

private fun String.humanizeEnumName(): String =
    split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
