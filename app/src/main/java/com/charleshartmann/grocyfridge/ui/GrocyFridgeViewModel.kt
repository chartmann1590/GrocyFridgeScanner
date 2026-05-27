package com.charleshartmann.grocyfridge.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charleshartmann.grocyfridge.ai.LiteRtFoodImageAnalyzer
import com.charleshartmann.grocyfridge.ai.ModelManager
import com.charleshartmann.grocyfridge.ai.ModelState
import com.charleshartmann.grocyfridge.data.GrocyClientFactory
import com.charleshartmann.grocyfridge.data.GrocyRepository
import com.charleshartmann.grocyfridge.data.ScanHistoryStore
import com.charleshartmann.grocyfridge.data.SettingsStore
import com.charleshartmann.grocyfridge.model.AppSettings
import com.charleshartmann.grocyfridge.model.ProposedChange
import com.charleshartmann.grocyfridge.model.ScanHistoryChange
import com.charleshartmann.grocyfridge.model.ScanHistoryRecord
import com.charleshartmann.grocyfridge.model.ScanState
import com.charleshartmann.grocyfridge.model.ScanStatus
import com.charleshartmann.grocyfridge.model.StorageLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val selectedLocation: StorageLocation = StorageLocation.Fridge,
    val scanState: ScanState = ScanState.Idle,
    val isSaving: Boolean = false,
    val lastSyncMessage: String? = null
)

class GrocyFridgeViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val historyStore = ScanHistoryStore(application)
    private val modelManager = ModelManager(application)

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState

    val history: StateFlow<List<ScanHistoryRecord>> = historyStore.records.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val modelState: StateFlow<ModelState> = modelManager.modelState

    val onboardingComplete: StateFlow<Boolean> = settingsStore.onboardingComplete.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            settingsStore.save(settings)
        }
    }

    fun selectLocation(location: StorageLocation) {
        _uiState.update { it.copy(selectedLocation = location) }
    }

    fun analyzePhoto(imagePath: String) {
        val currentSettings = settings.value
        if (!currentSettings.isComplete) {
            _uiState.update { it.copy(scanState = ScanState.Error("Grocy URL and API key are required.")) }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(scanState = ScanState.PreparingModel, lastSyncMessage = null) }
                Log.i(TAG, "Preparing model for image: $imagePath")
                val modelFile = modelManager.ensureModel()
                _uiState.update { it.copy(scanState = ScanState.Analyzing) }
                Log.i(TAG, "Analyzing image...")
                val analyzer = LiteRtFoodImageAnalyzer(modelFile)
                val detectionResult = analyzer.analyze(imagePath)
                Log.i(TAG, "Detection result: ${detectionResult.items.size} items found")
                val repository = repository(currentSettings)
                val changes = repository.buildProposals(detectionResult.items, uiState.value.selectedLocation)
                Log.i(TAG, "Built ${changes.size} proposals")
                _uiState.update { it.copy(scanState = ScanState.Review(imagePath, changes)) }
            } catch (throwable: Throwable) {
                Log.e(TAG, "Photo analysis failed", throwable)
                val errorMsg = throwable.message ?: "Photo analysis failed."
                _uiState.update {
                    it.copy(scanState = ScanState.Error(errorMsg))
                }
                saveFailedScan(imagePath, errorMsg)
            }
        }
    }

    fun updateChange(index: Int, transform: (ProposedChange) -> ProposedChange) {
        _uiState.update { state ->
            val review = state.scanState as? ScanState.Review ?: return@update state
            val changes = review.changes.toMutableList()
            changes[index] = transform(changes[index])
            state.copy(scanState = review.copy(changes = changes))
        }
    }

    fun applyReview() {
        val currentSettings = settings.value
        val review = uiState.value.scanState as? ScanState.Review ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository(currentSettings).applyChanges(review.changes)
                historyStore.add(
                    ScanHistoryRecord(
                        timestampMillis = System.currentTimeMillis(),
                        location = uiState.value.selectedLocation.displayName,
                        imagePath = review.imagePath,
                        changes = review.changes.map {
                            ScanHistoryChange(
                                name = it.displayName,
                                previousAmount = it.currentAmount,
                                newAmount = it.count,
                                included = it.included
                            )
                        },
                        status = ScanStatus.SUCCESS
                    )
                )
                val appliedCount = review.changes.count { it.included }
                _uiState.update {
                    it.copy(
                        scanState = ScanState.Idle,
                        isSaving = false,
                        lastSyncMessage = "Synced $appliedCount inventory changes to Grocy."
                    )
                }
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        scanState = ScanState.Error(throwable.message ?: "Grocy sync failed.")
                    )
                }
            }
        }
    }

    fun cancelReview() {
        _uiState.update { it.copy(scanState = ScanState.Idle) }
    }

    fun clearError() {
        _uiState.update { it.copy(scanState = ScanState.Idle) }
    }

    fun deleteHistoryRecord(record: ScanHistoryRecord) {
        viewModelScope.launch {
            historyStore.delete(record)
        }
    }

    fun retryScan(record: ScanHistoryRecord) {
        viewModelScope.launch {
            historyStore.delete(record)
        }
        analyzePhoto(record.imagePath)
    }

    fun downloadModel() {
        viewModelScope.launch {
            modelManager.downloadModel()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsStore.markOnboardingComplete()
        }
    }

    val modelSizeBytes: Long
        get() = modelManager.modelSizeBytes

    private fun repository(settings: AppSettings): GrocyRepository {
        return GrocyRepository(GrocyClientFactory.create(settings.grocyUrl, settings.grocyApiKey))
    }

    private fun saveFailedScan(imagePath: String, errorMessage: String) {
        viewModelScope.launch {
            historyStore.add(
                ScanHistoryRecord(
                    timestampMillis = System.currentTimeMillis(),
                    location = uiState.value.selectedLocation.displayName,
                    imagePath = imagePath,
                    changes = emptyList(),
                    status = ScanStatus.FAILED,
                    errorMessage = errorMessage.take(500)
                )
            )
            Log.i(TAG, "Saved failed scan to history: $imagePath")
        }
    }

    companion object {
        private const val TAG = "GrocyFridgeVM"
    }
}

fun Context.captureFile(): java.io.File {
    val dir = java.io.File(cacheDir, "captures").apply { mkdirs() }
    return java.io.File(dir, "scan-${System.currentTimeMillis()}.jpg")
}
