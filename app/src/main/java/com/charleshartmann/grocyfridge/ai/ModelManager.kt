package com.charleshartmann.grocyfridge.ai

import android.content.Context
import com.charleshartmann.grocyfridge.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed interface ModelState {
    data object NotDownloaded : ModelState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : ModelState
    data object Ready : ModelState
    data class Error(val message: String) : ModelState
}

class ModelManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    val modelFile: File
        get() = File(context.filesDir, "models/${BuildConfig.MODEL_FILE}")

    private val _modelState = MutableStateFlow<ModelState>(
        if (isModelReady()) ModelState.Ready else ModelState.NotDownloaded
    )
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    fun isModelReady(): Boolean = modelFile.exists() && modelFile.length() > 100_000_000

    val modelSizeBytes: Long
        get() = if (modelFile.exists()) modelFile.length() else 0L

    fun refreshState() {
        _modelState.value = if (isModelReady()) ModelState.Ready else ModelState.NotDownloaded
    }

    suspend fun ensureModel(): File = withContext(Dispatchers.IO) {
        if (isModelReady()) {
            _modelState.value = ModelState.Ready
            return@withContext modelFile
        }

        downloadModel()
        modelFile
    }

    suspend fun downloadModel() {
        withContext(Dispatchers.IO) {
            try {
                _modelState.value = ModelState.Downloading(0f, 0L, 0L)
                modelFile.parentFile?.mkdirs()
                val url = "https://huggingface.co/${BuildConfig.MODEL_REPO}/resolve/main/${BuildConfig.MODEL_FILE}?download=true"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _modelState.value = ModelState.Error("Download failed: HTTP ${response.code}")
                        error("Model download failed: HTTP ${response.code}")
                    }
                    val body = response.body ?: run {
                        _modelState.value = ModelState.Error("Download failed: empty response")
                        error("Model download failed: empty response")
                    }
                    val totalBytes = body.contentLength()
                    val tempFile = File(modelFile.parentFile, "${modelFile.name}.download")
                    var downloaded = 0L
                    body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                val progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                                _modelState.value = ModelState.Downloading(progress, downloaded, totalBytes)
                            }
                        }
                    }
                    if (tempFile.length() <= 100_000_000) {
                        tempFile.delete()
                        _modelState.value = ModelState.Error("Downloaded file was too small to be valid")
                        error("Model download was too small to be valid")
                    }
                    tempFile.renameTo(modelFile)
                    _modelState.value = ModelState.Ready
                }
            } catch (e: Exception) {
                if (_modelState.value !is ModelState.Error) {
                    _modelState.value = ModelState.Error(e.message ?: "Model download failed")
                }
                throw e
            }
        }
    }
}
