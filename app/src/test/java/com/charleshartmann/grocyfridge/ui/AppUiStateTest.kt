package com.charleshartmann.grocyfridge.ui

import com.charleshartmann.grocyfridge.model.ProposedChange
import com.charleshartmann.grocyfridge.model.ScanState
import com.charleshartmann.grocyfridge.model.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUiStateTest {

    @Test
    fun defaultStateIsIdleWithFridgeSelected() {
        val state = AppUiState()
        assertEquals(StorageLocation.Fridge, state.selectedLocation)
        assertEquals(ScanState.Idle, state.scanState)
        assertFalse(state.isSaving)
        assertNull(state.lastSyncMessage)
    }

    @Test
    fun copyChangesLocation() {
        val state = AppUiState().copy(selectedLocation = StorageLocation.Cupboards)
        assertEquals(StorageLocation.Cupboards, state.selectedLocation)
    }

    @Test
    fun copySetsErrorState() {
        val state = AppUiState().copy(scanState = ScanState.Error("test error"))
        assertEquals("test error", (state.scanState as ScanState.Error).message)
    }

    @Test
    fun copySetsPreparingModel() {
        val state = AppUiState().copy(scanState = ScanState.PreparingModel)
        assertEquals(ScanState.PreparingModel, state.scanState)
    }

    @Test
    fun copySetsAnalyzing() {
        val state = AppUiState().copy(scanState = ScanState.Analyzing)
        assertEquals(ScanState.Analyzing, state.scanState)
    }

    @Test
    fun copySetsReviewState() {
        val changes = listOf(
            ProposedChange("milk", "Milk", 3.0, 1.0, 1L, 2L, 3L)
        )
        val state = AppUiState().copy(
            scanState = ScanState.Review("/photo.jpg", changes)
        )

        val review = state.scanState as ScanState.Review
        assertEquals("/photo.jpg", review.imagePath)
        assertEquals(1, review.changes.size)
        assertEquals("Milk", review.changes.first().displayName)
    }

    @Test
    fun copySetsSavingAndSyncMessage() {
        val state = AppUiState().copy(
            isSaving = true,
            lastSyncMessage = "Synced 3 changes."
        )
        assertTrue(state.isSaving)
        assertEquals("Synced 3 changes.", state.lastSyncMessage)
    }

    @Test
    fun copyClearsSyncMessageWithNull() {
        val state = AppUiState(lastSyncMessage = "old message").copy(lastSyncMessage = null)
        assertNull(state.lastSyncMessage)
    }
}

class ViewModelUpdateChangeLogicTest {

    private val sampleChanges = listOf(
        ProposedChange("milk", "Milk", 3.0, 1.0, 10L, 2L, 3L),
        ProposedChange("chips", "Chips", 2.0, 0.0, null, 2L, 3L, isNewProduct = true)
    )

    @Test
    fun updateChangeAtIndex() {
        val changes = sampleChanges.toMutableList()
        changes[0] = changes[0].copy(count = 5.0)

        assertEquals(5.0, changes[0].count, 0.001)
        assertEquals(2.0, changes[1].count, 0.001)
        assertEquals("Chips", changes[1].displayName)
    }

    @Test
    fun toggleIncludedOnChange() {
        val changes = sampleChanges.toMutableList()
        changes[1] = changes[1].copy(included = false)

        assertTrue(changes[0].included)
        assertFalse(changes[1].included)
    }

    @Test
    fun editDisplayNameOnChange() {
        val changes = sampleChanges.toMutableList()
        changes[0] = changes[0].copy(displayName = "Whole Milk")

        assertEquals("Whole Milk", changes[0].displayName)
    }

    @Test
    fun countChangeUpdatesDelta() {
        val change = sampleChanges[0].copy(count = 10.0)
        assertEquals(9.0, change.delta, 0.001)
    }

    @Test
    fun filterIncludedChanges() {
        val changes = listOf(
            ProposedChange("milk", "Milk", 3.0, 1.0, 10L, 2L, 3L, included = true),
            ProposedChange("chips", "Chips", 2.0, 0.0, null, 2L, 3L, included = false),
            ProposedChange("eggs", "Eggs", 6.0, 2.0, 11L, 2L, 3L, included = true)
        )

        val included = changes.filter { it.included }
        assertEquals(2, included.size)
        assertEquals("Milk", included[0].displayName)
        assertEquals("Eggs", included[1].displayName)
    }

    @Test
    fun countIncludedChanges() {
        val changes = listOf(
            ProposedChange("a", "A", 1.0, 0.0, 1L, 1L, 1L, included = true),
            ProposedChange("b", "B", 1.0, 0.0, 2L, 1L, 1L, included = false),
            ProposedChange("c", "C", 1.0, 0.0, 3L, 1L, 1L, included = true)
        )
        assertEquals(2, changes.count { it.included })
    }

    @Test
    fun anyIncludedReturnsTrue() {
        val changes = listOf(
            ProposedChange("a", "A", 1.0, 0.0, 1L, 1L, 1L, included = false),
            ProposedChange("b", "B", 1.0, 0.0, 2L, 1L, 1L, included = true)
        )
        assertTrue(changes.any { it.included })
    }

    @Test
    fun anyIncludedReturnsFalseWhenAllExcluded() {
        val changes = listOf(
            ProposedChange("a", "A", 1.0, 0.0, 1L, 1L, 1L, included = false),
            ProposedChange("b", "B", 1.0, 0.0, 2L, 1L, 1L, included = false)
        )
        assertFalse(changes.any { it.included })
    }

    @Test
    fun mapChangesToHistoryChanges() {
        val changes = listOf(
            ProposedChange("milk", "Milk", 5.0, 2.0, 1L, 2L, 3L, included = true),
            ProposedChange("chips", "Chips", 3.0, 1.0, null, 2L, 3L, included = false)
        )
        val history = changes.map {
            com.charleshartmann.grocyfridge.model.ScanHistoryChange(
                name = it.displayName,
                previousAmount = it.currentAmount,
                newAmount = it.count,
                included = it.included
            )
        }

        assertEquals(2, history.size)
        assertEquals("Milk", history[0].name)
        assertEquals(2.0, history[0].previousAmount, 0.001)
        assertEquals(5.0, history[0].newAmount, 0.001)
        assertTrue(history[0].included)
        assertFalse(history[1].included)
    }
}
