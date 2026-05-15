package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.LandmarkType

/**
 * One landmark slot for a player, with its built / unbuilt state, as carried
 * in the `/app/game.sync` reconnect snapshot.
 */
data class PlayerLandmarkState(
    val landmarkType: LandmarkType,
    val isBuilt: Boolean,
)
