package com.charleshartmann.grocyfridge.data

import com.charleshartmann.grocyfridge.model.DetectedFoodItem
import com.charleshartmann.grocyfridge.model.GrocyLocation
import com.charleshartmann.grocyfridge.model.GrocyProduct
import com.charleshartmann.grocyfridge.model.InventoryRequest
import com.charleshartmann.grocyfridge.model.ProposedChange
import com.charleshartmann.grocyfridge.model.StorageLocation
import kotlin.math.max

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

    suspend fun applyChanges(changes: List<ProposedChange>) {
        changes.filter { it.included }.forEach { change ->
            val productId = change.productId ?: createProduct(change)
            api.inventoryProduct(
                productId,
                InventoryRequest(newAmount = change.count, locationId = change.locationId)
            )
        }
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

    private companion object {
        val packContainers = setOf("bag", "box", "pack", "package")
    }
}
