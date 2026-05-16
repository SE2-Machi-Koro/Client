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
        "card_wheat_field" -> R.drawable.card_wheat_field_image
        "card_ranch" -> R.drawable.card_ranch_image
        "card_forest" -> R.drawable.card_forest_image
        "card_mine" -> R.drawable.card_mine_image
        "card_apple_orchard" -> R.drawable.card_apple_orchard_image
        "card_bakery" -> R.drawable.card_bakery_image
        "card_convenience_store" -> R.drawable.card_convenince_store_image
        "card_cheese_factory" -> R.drawable.card_cheese_factory_image
        "card_furniture_factory" -> R.drawable.card_furniture_factory_image
        "card_fruit_and_vegetable_market" -> R.drawable.card_fruit_and_vegetable_marke_image
        "card_cafe" -> R.drawable.card_cafe_image
        "card_family_restaurant" -> R.drawable.card_family_restaurant_image
        "card_stadium" -> R.drawable.card_stadium_image
        "card_tv_station" -> R.drawable.card_tv_station_image
        "landmark_train_station" -> R.drawable.landmark_train_station_image
        "landmark_shopping_mall" -> R.drawable.landmark_shopping_mall_image
        "landmark_amusement_park" -> R.drawable.landmark_amusement_park_image
        "landmark_radio_tower" -> R.drawable.landmark_radio_tower_image
        else -> R.drawable.card_bakery_image
    }
}
