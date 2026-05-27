package com.charleshartmann.grocyfridge.data

import android.util.Log
import com.charleshartmann.grocyfridge.model.DetectedFoodItem
import com.charleshartmann.grocyfridge.model.GrocyLocation
import com.charleshartmann.grocyfridge.model.GrocyProduct
import com.charleshartmann.grocyfridge.model.ConsumeRequest
import com.charleshartmann.grocyfridge.model.GrocyQuantityUnit
import com.charleshartmann.grocyfridge.model.GrocyStockItem
import com.charleshartmann.grocyfridge.model.InventoryRequest
import com.charleshartmann.grocyfridge.model.ProposedChange
import com.charleshartmann.grocyfridge.model.ScanHistoryChange
import com.charleshartmann.grocyfridge.model.StorageLocation
import kotlin.math.max

data class SyncResult(val synced: Int, val skipped: Int)

class GrocyRepository(private val api: GrocyApi) {
    suspend fun buildProposals(
        detectedItems: List<DetectedFoodItem>,
        selectedLocation: StorageLocation
    ): List<ProposedChange> {
        val locationId = ensureLocation(selectedLocation.displayName)
        val quantityUnits = api.quantityUnits()
        val pieceUnit = quantityUnits.firstOrNull { it.name.equals("Piece", ignoreCase = true) }
            ?: error("Grocy quantity unit 'Piece' is missing")
        val packUnit = quantityUnits.firstOrNull { it.name.equals("Pack", ignoreCase = true) }
            ?: pieceUnit
        val products = api.products()
        val stockByProductId = api.stock().associateBy { it.productId }

        return normalizeDetections(detectedItems).map { item ->
            val product = products.firstOrNull { normalizeName(it.name) == normalizeName(item.name) }
            val currentAmount = product?.id?.let { stockByProductId[it]?.amount } ?: 0.0
            ProposedChange(
                detectedName = item.name,
                displayName = product?.name ?: item.name.replaceFirstChar { it.uppercase() },
                count = max(0.0, item.count),
                currentAmount = currentAmount,
                productId = product?.id,
                locationId = locationId,
                unitId = if (item.container in packContainers) packUnit.id else pieceUnit.id,
                isNewProduct = product == null
            )
        }.sortedBy { it.displayName.lowercase() }
    }

    suspend fun applyChanges(changes: List<ProposedChange>): SyncResult {
        val included = changes.filter { it.included }
        Log.i(TAG, "applyChanges: ${included.size} of ${changes.size} changes included")
        var synced = 0
        var skipped = 0
        included.forEachIndexed { index, change ->
            if (change.productId != null && change.delta == 0.0) {
                Log.i(TAG, "  [$index] ${change.displayName}: skipped (already at ${change.count})")
                skipped++
                return@forEachIndexed
            }
            Log.i(TAG, "  [$index] ${change.displayName}: amount=${change.count}, productId=${change.productId}, locationId=${change.locationId}, isNew=${change.isNewProduct}")
            val productId = change.productId ?: createProduct(change)
            Log.d(TAG, "  [$index] inventoryProduct(productId=$productId, newAmount=${change.count})")
            api.inventoryProduct(
                productId,
                InventoryRequest(newAmount = change.count, locationId = change.locationId)
            )
            Log.i(TAG, "  [$index] ${change.displayName}: synced OK")
            synced++
        }
        Log.i(TAG, "applyChanges: $synced synced, $skipped skipped (already up to date)")
        return SyncResult(synced = synced, skipped = skipped)
    }

    suspend fun retrySyncChanges(historyChanges: List<ScanHistoryChange>): SyncResult {
        val included = historyChanges.filter { it.included && it.locationId != 0L }
        Log.i(TAG, "retrySyncChanges: ${included.size} changes to retry")
        var synced = 0
        var skipped = 0
        included.forEachIndexed { index, change ->
            if (change.productId != null && change.newAmount == change.previousAmount) {
                Log.i(TAG, "  [$index] ${change.name}: skipped (already at ${change.newAmount})")
                skipped++
                return@forEachIndexed
            }
            Log.i(TAG, "  [$index] ${change.name}: amount=${change.newAmount}, productId=${change.productId}, locationId=${change.locationId}, isNew=${change.isNewProduct}")
            val productId = change.productId ?: createProductFromHistory(change)
            api.inventoryProduct(
                productId,
                InventoryRequest(newAmount = change.newAmount, locationId = change.locationId)
            )
            Log.i(TAG, "  [$index] ${change.name}: synced OK")
            synced++
        }
        Log.i(TAG, "retrySyncChanges: $synced synced, $skipped skipped")
        return SyncResult(synced = synced, skipped = skipped)
    }

    private suspend fun createProductFromHistory(change: ScanHistoryChange): Long {
        return api.createProduct(
            GrocyProduct(
                name = change.name,
                locationId = change.locationId,
                defaultConsumeLocationId = change.locationId,
                purchaseUnitId = change.unitId,
                stockUnitId = change.unitId
            )
        ).createdObjectId
    }

    private suspend fun ensureLocation(name: String): Long {
        val existing = api.locations().firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing?.id != null) return existing.id
        return api.createLocation(GrocyLocation(name = name)).createdObjectId
    }

    private suspend fun createProduct(change: ProposedChange): Long {
        return api.createProduct(
            GrocyProduct(
                name = change.displayName,
                locationId = change.locationId,
                defaultConsumeLocationId = change.locationId,
                purchaseUnitId = change.unitId,
                stockUnitId = change.unitId
            )
        ).createdObjectId
    }

    private fun normalizeDetections(items: List<DetectedFoodItem>): List<DetectedFoodItem> {
        return items
            .filter { it.name.isNotBlank() && it.count > 0.0 }
            .groupBy { normalizeName(it.name) }
            .map { (_, grouped) ->
                val first = grouped.first()
                first.copy(
                    name = first.name.trim(),
                    count = grouped.sumOf { it.count },
                    confidence = grouped.maxOf { it.confidence }
                )
            }
    }

    private fun normalizeName(value: String): String {
        return value.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .removeSuffix("s")
    }

    suspend fun fetchStock(): List<GrocyStockItem> = api.stock()

    suspend fun fetchLocations(): List<GrocyLocation> = api.locations()

    suspend fun fetchQuantityUnits(): List<GrocyQuantityUnit> = api.quantityUnits()

    suspend fun setProductAmount(productId: Long, newAmount: Double, locationId: Long) {
        Log.i(TAG, "setProductAmount: productId=$productId, newAmount=$newAmount, locationId=$locationId")
        api.inventoryProduct(productId, InventoryRequest(newAmount = newAmount, locationId = locationId))
    }

    suspend fun consumeProduct(productId: Long, amount: Double) {
        Log.i(TAG, "consumeProduct: productId=$productId, amount=$amount")
        api.consumeProduct(productId, ConsumeRequest(amount = amount))
    }

    suspend fun deleteProduct(productId: Long) {
        Log.i(TAG, "deleteProduct: productId=$productId")
        api.deleteProduct(productId)
    }

    private companion object {
        const val TAG = "GrocyRepository"
        val packContainers = setOf("bag", "box", "pack", "package")
    }
}
