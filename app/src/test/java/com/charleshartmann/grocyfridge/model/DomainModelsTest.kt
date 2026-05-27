package com.charleshartmann.grocyfridge.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProposedChangeTest {

    @Test
    fun deltaIsPositiveWhenCountExceedsCurrent() {
        val change = ProposedChange(
            detectedName = "milk", displayName = "Milk",
            count = 5.0, currentAmount = 3.0,
            productId = 1L, locationId = 2L, unitId = 3L
        )
        assertEquals(2.0, change.delta, 0.001)
    }

    @Test
    fun deltaIsNegativeWhenCurrentExceedsCount() {
        val change = ProposedChange(
            detectedName = "milk", displayName = "Milk",
            count = 1.0, currentAmount = 4.0,
            productId = 1L, locationId = 2L, unitId = 3L
        )
        assertEquals(-3.0, change.delta, 0.001)
    }

    @Test
    fun deltaIsZeroWhenEqual() {
        val change = ProposedChange(
            detectedName = "milk", displayName = "Milk",
            count = 3.0, currentAmount = 3.0,
            productId = 1L, locationId = 2L, unitId = 3L
        )
        assertEquals(0.0, change.delta, 0.001)
    }

    @Test
    fun defaultsToIncludedTrue() {
        val change = ProposedChange(
            detectedName = "x", displayName = "X",
            count = 1.0, currentAmount = 0.0,
            productId = null, locationId = 1L, unitId = 1L
        )
        assertTrue(change.included)
    }

    @Test
    fun defaultsToNotNewProduct() {
        val change = ProposedChange(
            detectedName = "x", displayName = "X",
            count = 1.0, currentAmount = 0.0,
            productId = null, locationId = 1L, unitId = 1L
        )
        assertFalse(change.isNewProduct)
    }

    @Test
    fun copyPreservesFields() {
        val original = ProposedChange(
            detectedName = "chips", displayName = "Chips",
            count = 3.0, currentAmount = 1.0,
            productId = 10L, locationId = 2L, unitId = 3L,
            included = true, isNewProduct = false
        )
        val modified = original.copy(included = false, count = 5.0)

        assertFalse(modified.included)
        assertEquals(5.0, modified.count, 0.001)
        assertEquals("Chips", modified.displayName)
        assertEquals(10L, modified.productId)
        assertEquals(4.0, modified.delta, 0.001)
    }
}

class AppSettingsTest {

    @Test
    fun isCompleteWhenBothFieldsPresent() {
        val settings = AppSettings("https://grocy.example.com", "abc123")
        assertTrue(settings.isComplete)
    }

    @Test
    fun isNotCompleteWhenUrlBlank() {
        val settings = AppSettings("", "abc123")
        assertFalse(settings.isComplete)
    }

    @Test
    fun isNotCompleteWhenApiKeyBlank() {
        val settings = AppSettings("https://grocy.example.com", "")
        assertFalse(settings.isComplete)
    }

    @Test
    fun isNotCompleteWhenBothBlank() {
        val settings = AppSettings()
        assertFalse(settings.isComplete)
    }

    @Test
    fun isNotCompleteWhenWhitespaceOnly() {
        val settings = AppSettings("   ", "   ")
        assertFalse(settings.isComplete)
    }
}

class StorageLocationTest {

    @Test
    fun fridgeHasCorrectDisplayName() {
        assertEquals("Fridge", StorageLocation.Fridge.displayName)
    }

    @Test
    fun cupboardsHasCorrectDisplayName() {
        assertEquals("Cupboards", StorageLocation.Cupboards.displayName)
    }

    @Test
    fun hasExactlyTwoEntries() {
        assertEquals(2, StorageLocation.entries.size)
    }
}

class ScanStateTest {

    @Test
    fun reviewStateHoldsData() {
        val changes = listOf(
            ProposedChange("milk", "Milk", 2.0, 1.0, 1L, 2L, 3L)
        )
        val state = ScanState.Review("/path/to/photo.jpg", changes)

        assertEquals("/path/to/photo.jpg", state.imagePath)
        assertEquals(1, state.changes.size)
        assertEquals("Milk", state.changes.first().displayName)
    }

    @Test
    fun errorStateHoldsMessage() {
        val state = ScanState.Error("Something went wrong")
        assertEquals("Something went wrong", state.message)
    }

    @Test
    fun reviewStateCopyChanges() {
        val original = ScanState.Review(
            "/photo.jpg",
            listOf(ProposedChange("milk", "Milk", 2.0, 1.0, 1L, 2L, 3L))
        )
        val modified = original.copy(
            changes = original.changes.map { it.copy(count = 5.0) }
        )

        assertEquals(5.0, modified.changes.first().count, 0.001)
    }
}

class ScanHistoryRecordTest {

    @Test
    fun serializationRoundTrip() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val record = ScanHistoryRecord(
            timestampMillis = 1700000000000L,
            location = "Fridge",
            imagePath = "/photo.jpg",
            changes = listOf(
                ScanHistoryChange("Milk", 1.0, 3.0, true),
                ScanHistoryChange("Chips", 2.0, 0.0, false)
            )
        )
        val encoded = json.encodeToString(ScanHistoryRecord.serializer(), record)
        val decoded = json.decodeFromString(ScanHistoryRecord.serializer(), encoded)

        assertEquals(record.timestampMillis, decoded.timestampMillis)
        assertEquals(record.location, decoded.location)
        assertEquals(record.imagePath, decoded.imagePath)
        assertEquals(2, decoded.changes.size)
        assertEquals("Milk", decoded.changes[0].name)
        assertEquals(1.0, decoded.changes[0].previousAmount, 0.001)
        assertEquals(3.0, decoded.changes[0].newAmount, 0.001)
        assertTrue(decoded.changes[0].included)
        assertFalse(decoded.changes[1].included)
    }

    @Test
    fun listSerializationRoundTrip() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val records = listOf(
            ScanHistoryRecord(1000L, "Fridge", "/a.jpg", emptyList()),
            ScanHistoryRecord(2000L, "Cupboards", "/b.jpg", listOf(ScanHistoryChange("Rice", 0.0, 2.0, true)))
        )
        val serializer = kotlinx.serialization.builtins.ListSerializer(ScanHistoryRecord.serializer())
        val encoded = json.encodeToString(serializer, records)
        val decoded = json.decodeFromString(serializer, encoded)

        assertEquals(2, decoded.size)
        assertEquals("Cupboards", decoded[1].location)
    }

    @Test
    fun scanHistoryChangeDeserializesWithoutNewFields() {
        val json = Json { ignoreUnknownKeys = true }
        val old = """{"name":"Milk","previousAmount":1.0,"newAmount":3.0,"included":true}"""
        val change = json.decodeFromString(ScanHistoryChange.serializer(), old)
        assertEquals("Milk", change.name)
        assertNull(change.productId)
        assertEquals(0L, change.locationId)
        assertEquals(0L, change.unitId)
        assertFalse(change.isNewProduct)
    }

    @Test
    fun scanHistoryChangeSerializesNewFields() {
        val json = Json { ignoreUnknownKeys = true }
        val change = ScanHistoryChange("Milk", 1.0, 3.0, true, productId = 10L, locationId = 2L, unitId = 3L, isNewProduct = false)
        val encoded = json.encodeToString(ScanHistoryChange.serializer(), change)
        val decoded = json.decodeFromString(ScanHistoryChange.serializer(), encoded)
        assertEquals(10L, decoded.productId)
        assertEquals(2L, decoded.locationId)
        assertEquals(3L, decoded.unitId)
        assertFalse(decoded.isNewProduct)
    }

    @Test
    fun pendingStatusSerializesCorrectly() {
        val json = Json { ignoreUnknownKeys = true }
        val record = ScanHistoryRecord(
            timestampMillis = 1700000000000L,
            location = "Fridge",
            imagePath = "/photo.jpg",
            changes = emptyList(),
            status = ScanStatus.PENDING
        )
        val encoded = json.encodeToString(ScanHistoryRecord.serializer(), record)
        val decoded = json.decodeFromString(ScanHistoryRecord.serializer(), encoded)
        assertEquals(ScanStatus.PENDING, decoded.status)
    }
}
