package com.machikoro.client.domain.model.state

/**
 * Result of a cheating accusation (#280), surfaced to the player as a toast.
 * Player names are resolved from the current roster when the server's
 * ACCUSATION_RESULT frame arrives.
 */
data class AccusationResult(
    val caught: Boolean,
    val accuserName: String,
    val accusedName: String,
)
