package com.machikoro.client.ui.game

import com.machikoro.client.R

/**
 * Central mapping from server/catalog image keys to Android drawables.
 *
 * Some resource file names still contain historical typos. Keep those quirks
 * isolated here so shop rendering and tests share the same safe fallback path.
 */
internal object ShopImageResolver {
    const val FALLBACK_IMAGE_KEY = "card_bakery"

    fun drawableFor(imageKey: String): Int = when (imageKey) {
        "card_wheat_field" -> R.drawable.card_wheat_field
        "card_ranch" -> R.drawable.card_ranch
        "card_forest" -> R.drawable.card_forest
        "card_mine" -> R.drawable.card_mine
        "card_apple_orchard" -> R.drawable.card_apple_orchard
        "card_bakery" -> R.drawable.card_bakery
        "card_convenience_store" -> R.drawable.card_convenience_store
        "card_cheese_factory" -> R.drawable.card_cheese_factory
        "card_furniture_factory" -> R.drawable.card_furniture_factory
        "card_fruit_and_vegetable_market" -> R.drawable.card_fruit_and_vegetable_market
        "card_cafe" -> R.drawable.card_cafe
        "card_family_restaurant" -> R.drawable.card_family_restaurant
        "card_stadium" -> R.drawable.card_stadium
        "card_tv_station" -> R.drawable.card_tv_station
        "card_business_center" -> R.drawable.card_business_center
        "landmark_train_station" -> R.drawable.landmark_train_station
        "landmark_shopping_mall" -> R.drawable.landmark_shopping_mall
        "landmark_amusement_park" -> R.drawable.landmark_amusement_park
        "landmark_radio_tower" -> R.drawable.landmark_radio_tower
        else -> R.drawable.card_bakery
    }
}
