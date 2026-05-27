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
import com.charleshartmann.grocyfridge.data.SyncResult
import com.charleshartmann.grocyfridge.data.ScanHistoryStore
import com.charleshartmann.grocyfridge.data.SettingsStore
import com.charleshartmann.grocyfridge.model.AppSettings
import com.charleshartmann.grocyfridge.model.GrocyLocation
import com.charleshartmann.grocyfridge.model.GrocyQuantityUnit
import com.charleshartmann.grocyfridge.model.GrocyStockItem
import com.charleshartmann.grocyfridge.model.ProposedChange
import com.charleshartmann.grocyfridge.model.ScanHistoryChange
import com.charleshartmann.grocyfridge.model.ScanHistoryRecord
import com.charleshartmann.grocyfridge.model.ScanState
import com.charleshartmann.grocyfridge.model.ScanStatus
import com.charleshartmann.grocyfridge.model.StorageLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val message: String
)

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

    private val _connectionTest = MutableStateFlow<ConnectionTestResult?>(null)
    val connectionTest: StateFlow<ConnectionTestResult?> = _connectionTest.asStateFlow()

    val history: StateFlow<List<ScanHistoryRecord>> = historyStore.records.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val modelState: StateFlow<ModelState> = modelManager.modelState

    private val _inventoryStock = MutableStateFlow<List<GrocyStockItem>>(emptyList())
    val inventoryStock: StateFlow<List<GrocyStockItem>> = _inventoryStock.asStateFlow()

    private val _inventoryLocations = MutableStateFlow<Map<Long, String>>(emptyMap())
    val inventoryLocations: StateFlow<Map<Long, String>> = _inventoryLocations.asStateFlow()

    private val _inventoryUnits = MutableStateFlow<Map<Long, String>>(emptyMap())
    val inventoryUnits: StateFlow<Map<Long, String>> = _inventoryUnits.asStateFlow()

    private val _inventoryLoading = MutableStateFlow(false)
    val inventoryLoading: StateFlow<Boolean> = _inventoryLoading.asStateFlow()

    private val _inventoryError = MutableStateFlow<String?>(null)
    val inventoryError: StateFlow<String?> = _inventoryError.asStateFlow()

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

    fun testConnection(url: String, apiKey: String) {
        viewModelScope.launch {
            _connectionTest.value = ConnectionTestResult(true, "Testing...")
            try {
                val api = GrocyClientFactory.create(url.trim().trimEnd('/'), apiKey.trim())
                val info = api.systemInfo()
                val version = info.grocyVersion?.Version ?: info.version
                _connectionTest.value = ConnectionTestResult(
                    isSuccess = true,
                    message = if (version != null) "Connected! Grocy version: $version" else "Connected successfully!"
                )
                Log.i(TAG, "Connection test passed: $info")
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("401") == true -> "Authentication failed — check your API key"
                    e.message?.contains("404") == true -> "Server found but Grocy API not detected — check URL"
                    e.message?.contains("Unable to resolve") == true -> "Cannot reach server — check URL and network"
                    e.message?.contains("Connection refused") == true -> "Connection refused — server may be down"
                    else -> "Connection failed: ${e.message}"
                }
                _connectionTest.value = ConnectionTestResult(isSuccess = false, message = msg)
                Log.w(TAG, "Connection test failed: $msg", e)
            }
        }
    }

    fun clearConnectionTest() {
        _connectionTest.value = null
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
                val rawMsg = throwable.message ?: "Photo analysis failed."
                _uiState.update {
                    it.copy(scanState = ScanState.Error(rawMsg))
                }
                saveFailedScan(imagePath, rawMsg)
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
        val recordTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val historyChanges = review.changes.map {
                ScanHistoryChange(
                    name = it.displayName,
                    previousAmount = it.currentAmount,
                    newAmount = it.count,
                    included = it.included,
                    productId = it.productId,
                    locationId = it.locationId,
                    unitId = it.unitId,
                    isNewProduct = it.isNewProduct
                )
            }
            historyStore.add(
                ScanHistoryRecord(
                    timestampMillis = recordTimestamp,
                    location = uiState.value.selectedLocation.displayName,
                    imagePath = review.imagePath,
                    changes = historyChanges,
                    status = ScanStatus.PENDING
                )
            )
            Log.i(TAG, "Saved PENDING history record at $recordTimestamp")

            try {
                val result = repository(currentSettings).applyChanges(review.changes)
                historyStore.update(recordTimestamp) { it.copy(status = ScanStatus.SUCCESS) }
                Log.i(TAG, "Sync succeeded, updated record to SUCCESS")
                val message = when {
                    result.synced == 0 && result.skipped > 0 ->
                        "Inventory already up to date — ${result.skipped} item${if (result.skipped != 1) "s" else ""} unchanged."
                    result.skipped > 0 ->
                        "Synced ${result.synced} change${if (result.synced != 1) "s" else ""}, ${result.skipped} already up to date."
                    else ->
                        "Synced ${result.synced} inventory change${if (result.synced != 1) "s" else ""} to Grocy."
                }
                _uiState.update {
                    it.copy(
                        scanState = ScanState.Idle,
                        isSaving = false,
                        lastSyncMessage = message
                    )
                }
            } catch (throwable: Throwable) {
                val errorMsg = extractErrorMessage(throwable)
                historyStore.update(recordTimestamp) {
                    it.copy(status = ScanStatus.FAILED, errorMessage = errorMsg.take(500))
                }
                Log.e(TAG, "Sync failed, updated record to FAILED: $errorMsg", throwable)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        scanState = ScanState.Error(errorMsg)
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
        val hasSyncData = record.changes.isNotEmpty() &&
            record.changes.any { it.included && it.locationId != 0L }

        if (hasSyncData) {
            retrySyncOnly(record)
        } else {
            viewModelScope.launch { historyStore.delete(record) }
            analyzePhoto(record.imagePath)
        }
    }

    private fun retrySyncOnly(record: ScanHistoryRecord) {
        val currentSettings = settings.value
        if (!currentSettings.isComplete) {
            _uiState.update { it.copy(scanState = ScanState.Error("Grocy URL and API key are required.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            historyStore.update(record.timestampMillis) {
                it.copy(status = ScanStatus.PENDING, errorMessage = null)
            }
            Log.i(TAG, "Retrying sync for record at ${record.timestampMillis}")

            try {
                val result = repository(currentSettings).retrySyncChanges(record.changes)
                historyStore.update(record.timestampMillis) { it.copy(status = ScanStatus.SUCCESS) }
                Log.i(TAG, "Sync retry succeeded for record at ${record.timestampMillis}")
                val message = when {
                    result.synced == 0 && result.skipped > 0 ->
                        "Inventory already up to date — ${result.skipped} item${if (result.skipped != 1) "s" else ""} unchanged."
                    result.skipped > 0 ->
                        "Synced ${result.synced} change${if (result.synced != 1) "s" else ""}, ${result.skipped} already up to date."
                    else ->
                        "Synced ${result.synced} inventory change${if (result.synced != 1) "s" else ""} to Grocy."
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        lastSyncMessage = message
                    )
                }
            } catch (throwable: Throwable) {
                val errorMsg = extractErrorMessage(throwable)
                historyStore.update(record.timestampMillis) {
                    it.copy(status = ScanStatus.FAILED, errorMessage = errorMsg.take(500))
                }
                Log.e(TAG, "Sync retry failed: $errorMsg", throwable)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        scanState = ScanState.Error(errorMsg)
                    )
                }
            }
        }
    }

    fun loadInventory() {
        val currentSettings = settings.value
        if (!currentSettings.isComplete) return
        viewModelScope.launch {
            _inventoryLoading.value = true
            _inventoryError.value = null
            try {
                val repo = repository(currentSettings)
                val stock = repo.fetchStock()
                val locations = repo.fetchLocations()
                val units = repo.fetchQuantityUnits()
                _inventoryStock.value = stock.sortedBy { it.product?.name?.lowercase() }
                _inventoryLocations.value = locations.associate { (it.id ?: 0L) to it.name }
                _inventoryUnits.value = units.associate { it.id to it.name }
                Log.i(TAG, "Loaded inventory: ${stock.size} items")
            } catch (throwable: Throwable) {
                val msg = extractErrorMessage(throwable)
                _inventoryError.value = msg
                Log.e(TAG, "Failed to load inventory: $msg", throwable)
            } finally {
                _inventoryLoading.value = false
            }
        }
    }

    fun updateProductAmount(productId: Long, newAmount: Double, locationId: Long) {
        val currentSettings = settings.value
        viewModelScope.launch {
            try {
                repository(currentSettings).setProductAmount(productId, newAmount, locationId)
                Log.i(TAG, "Updated product $productId amount to $newAmount")
                loadInventory()
            } catch (throwable: Throwable) {
                _inventoryError.value = extractErrorMessage(throwable)
            }
        }
    }

    fun consumeProduct(productId: Long, amount: Double) {
        val currentSettings = settings.value
        viewModelScope.launch {
            try {
                repository(currentSettings).consumeProduct(productId, amount)
                Log.i(TAG, "Consumed $amount of product $productId")
                loadInventory()
            } catch (throwable: Throwable) {
                _inventoryError.value = extractErrorMessage(throwable)
            }
        }
    }

    fun deleteProduct(productId: Long) {
        val currentSettings = settings.value
        viewModelScope.launch {
            try {
                repository(currentSettings).deleteProduct(productId)
                Log.i(TAG, "Deleted product $productId")
                loadInventory()
            } catch (throwable: Throwable) {
                _inventoryError.value = extractErrorMessage(throwable)
            }
        }
    }

    fun clearInventoryError() {
        _inventoryError.value = null
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

    private fun extractErrorMessage(throwable: Throwable): String {
        if (throwable is HttpException) {
            val code = throwable.code()
            val body = try {
                throwable.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            Log.w(TAG, "HTTP $code error body: $body")
            return if (!body.isNullOrBlank()) "HTTP $code: $body" else "HTTP $code: ${throwable.message()}"
        }
        return throwable.message ?: "Grocy sync failed."
    }

    companion object {
        private const val TAG = "GrocyFridgeVM"
    }
}

fun Context.captureFile(): java.io.File {
    val dir = java.io.File(cacheDir, "captures").apply { mkdirs() }
    return java.io.File(dir, "scan-${System.currentTimeMillis()}.jpg")
}
