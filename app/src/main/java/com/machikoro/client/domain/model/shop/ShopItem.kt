package com.machikoro.client.domain.model.shop

data class ShopItem(
    val purchaseType: PurchaseType,
    // Must match Server CardType/LandmarkType enum names so purchase requests deserialize.
    val type: String,
    val displayName: String,
    val cost: Int,
    val color: ShopItemColor,
    val establishmentType: String,
    val imageKey: String,
    val isAvailable: Boolean = true
)

enum class PurchaseType {
    ESTABLISHMENT,
    LANDMARK
}

enum class ShopItemColor {
    BLUE,
    GREEN,
    RED,
    PURPLE,
    LANDMARK
}

object ShopCatalog {
    // Local display catalog until the server sends marketplace/supply snapshots.
    val defaultItems = listOf(
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = "WHEAT_FIELD",
            displayName = "Wheat Field",
            cost = 1,
            color = ShopItemColor.BLUE,
            establishmentType = "WHEAT",
            imageKey = "card_wheat_field"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = "RANCH",
            displayName = "Ranch",
            cost = 1,
            color = ShopItemColor.BLUE,
            establishmentType = "COW",
            imageKey = "card_ranch"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = "BAKERY",
            displayName = "Bakery",
            cost = 1,
            color = ShopItemColor.GREEN,
            establishmentType = "BREAD",
            imageKey = "card_bakery"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = "CAFE",
            displayName = "Cafe",
            cost = 2,
            color = ShopItemColor.RED,
            establishmentType = "CUP",
            imageKey = "card_cafe"
        ),
        ShopItem(
            purchaseType = PurchaseType.LANDMARK,
            type = "TRAIN_STATION",
            displayName = "Train Station",
            cost = 4,
            color = ShopItemColor.LANDMARK,
            establishmentType = "LANDMARK",
            imageKey = "landmark_train_station"
        )
    )
}
