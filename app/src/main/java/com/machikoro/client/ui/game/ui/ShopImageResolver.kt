package com.machikoro.client.ui.game.ui

import com.machikoro.client.R
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.model.shop.ShopItem

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
        "landmark_train_station_locked" -> R.drawable.landmark_train_station_locked
        "landmark_shopping_mall" -> R.drawable.landmark_shopping_mall
        "landmark_shopping_mall_locked" -> R.drawable.landmark_shopping_mall_locked
        "landmark_amusement_park" -> R.drawable.landmark_amusement_park
        "landmark_amusement_park_locked" -> R.drawable.landmark_amusement_park_locked
        "landmark_radio_tower" -> R.drawable.landmark_radio_tower
        "landmark_radio_tower_locked" -> R.drawable.landmark_radio_tower_locked
        else -> R.drawable.card_bakery
    }

    fun drawableForShopItem(item: ShopItem): Int =
        drawableFor(
            if (item.purchaseType == PurchaseType.LANDMARK) {
                "${item.imageKey}_locked"
            } else {
                item.imageKey
            }
        )
}
