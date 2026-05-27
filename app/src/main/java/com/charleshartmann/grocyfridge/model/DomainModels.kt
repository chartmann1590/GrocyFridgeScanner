package com.charleshartmann.grocyfridge.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class StorageLocation(val displayName: String) {
    Fridge("Fridge"),
    Cupboards("Cupboards")
}

@Serializable
data class DetectedFoodItem(
    val name: String,
    val count: Double,
    val container: String = "unknown",
    val confidence: Double = 0.0
)

@Serializable
data class FoodDetectionResult(
    val items: List<DetectedFoodItem> = emptyList()
)

data class ProposedChange(
    val detectedName: String,
    val displayName: String,
    val count: Double,
    val currentAmount: Double,
    val productId: Long?,
    val locationId: Long,
    val unitId: Long,
    val included: Boolean = true,
    val isNewProduct: Boolean = false
) {
    val delta: Double get() = count - currentAmount
}

@Serializable
data class GrocyProduct(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    @SerialName("location_id") val locationId: Long? = null,
    @SerialName("default_consume_location_id") val defaultConsumeLocationId: Long? = null,
    @SerialName("qu_id_purchase") val purchaseUnitId: Long? = null,
    @SerialName("qu_id_stock") val stockUnitId: Long? = null,
    @SerialName("min_stock_amount") val minStockAmount: Double = 0.0,
    @SerialName("default_best_before_days") val defaultBestBeforeDays: Int = 0,
    @SerialName("default_best_before_days_after_open") val defaultBestBeforeDaysAfterOpen: Int = 0,
    @SerialName("enable_tare_weight_handling") val enableTareWeightHandling: Int = 0,
    @SerialName("tare_weight") val tareWeight: Double = 0.0,
    @SerialName("not_check_stock_fulfillment_for_recipes") val notCheckStockFulfillmentForRecipes: Int = 0,
    @SerialName("should_not_be_frozen") val shouldNotBeFrozen: Int = 0
)

@Serializable
data class GrocyLocation(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    @SerialName("is_freezer") val isFreezer: Int = 0,
    val active: Int = 1
)

@Serializable
data class GrocyQuantityUnit(
    val id: Long,
    val name: String,
    @SerialName("name_plural") val namePlural: String? = null,
    val active: Int = 1
)

@Serializable
data class GrocyStockItem(
    @SerialName("product_id") val productId: Long,
    val amount: Double = 0.0,
    val product: GrocyProduct? = null
)

@Serializable
data class CreatedObjectResponse(
    @SerialName("created_object_id") val createdObjectId: Long
)

@Serializable
data class InventoryRequest(
    @SerialName("new_amount") val newAmount: Double,
    @SerialName("best_before_date") val bestBeforeDate: String = "2999-12-31",
    @SerialName("location_id") val locationId: Long,
    @SerialName("stock_label_type") val stockLabelType: Int = 1,
    val note: String = "Updated by fridge scanner photo"
)

data class AppSettings(
    val grocyUrl: String = "",
    val grocyApiKey: String = ""
) {
    val isComplete: Boolean get() = grocyUrl.isNotBlank() && grocyApiKey.isNotBlank()
}

@Serializable
enum class ScanStatus {
    SUCCESS, FAILED
}

@Serializable
data class ScanHistoryRecord(
    val timestampMillis: Long,
    val location: String,
    val imagePath: String,
    val changes: List<ScanHistoryChange>,
    val status: ScanStatus = ScanStatus.SUCCESS,
    val rawLlmResponse: String? = null,
    val errorMessage: String? = null
)

@Serializable
data class ScanHistoryChange(
    val name: String,
    val previousAmount: Double,
    val newAmount: Double,
    val included: Boolean
)

sealed interface ScanState {
    data object Idle : ScanState
    data object PreparingModel : ScanState
    data object Analyzing : ScanState
    data class Review(val imagePath: String, val changes: List<ProposedChange>) : ScanState
    data class Error(val message: String) : ScanState
}
