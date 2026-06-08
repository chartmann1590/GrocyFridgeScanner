package com.charleshartmann.grocyfridge.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.charleshartmann.grocyfridge.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit

data class OllamaModel(
    val name: String,
    val isRemote: Boolean
)

object DiagnosticsHelper {

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun detectOllamaModels(serverUrl: String?): List<OllamaModel> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        val urls = mutableListOf<Pair<String, Boolean>>()
        // localhost is local/on-device
        urls.add(Pair("http://localhost:11434/api/tags", false))
        // 10.0.2.2 is remote (running on host)
        urls.add(Pair("http://10.0.2.2:11434/api/tags", true))

        if (!serverUrl.isNullOrBlank()) {
            try {
                val uri = URI(serverUrl)
                val host = uri.host
                if (host != null && host != "localhost" && host != "127.0.0.1" && host != "10.0.2.2") {
                    urls.add(Pair("http://$host:11434/api/tags", true))
                }
            } catch (e: Exception) {
                // Ignore malformed URI
            }
        }

        val detected = mutableListOf<OllamaModel>()
        for ((url, isRemote) in urls) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            val modelsArray = json.optJSONArray("models")
                            if (modelsArray != null) {
                                for (i in 0 until modelsArray.length()) {
                                    val modelObj = modelsArray.optJSONObject(i)
                                    val name = modelObj?.optString("name")
                                    if (name != null) {
                                        detected.add(OllamaModel(name, isRemote))
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore failed connection
            }
        }
        detected
    }

    suspend fun gatherDiagnostics(
        context: Context,
        modelName: String,
        isModelOnDevice: Boolean,
        isModelReady: Boolean,
        customServerUrl: String? = null
    ): String {
        val brand = Build.BRAND
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val androidVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT
        val appVersionName = BuildConfig.VERSION_NAME
        val appVersionCode = BuildConfig.VERSION_CODE
        val locale = Locale.getDefault().toString()

        val filesDir = context.filesDir
        val freeSpaceBytes = filesDir.usableSpace
        val totalSpaceBytes = filesDir.totalSpace
        val freeSpaceGb = String.format(Locale.US, "%.2f GB", freeSpaceBytes.toDouble() / (1024 * 1024 * 1024))
        val totalSpaceGb = String.format(Locale.US, "%.2f GB", totalSpaceBytes.toDouble() / (1024 * 1024 * 1024))

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val freeMemMb = String.format(Locale.US, "%.2f MB", memInfo.availMem.toDouble() / (1024 * 1024))
        val totalMemMb = String.format(Locale.US, "%.2f MB", memInfo.totalMem.toDouble() / (1024 * 1024))

        val runningOllamaModels = detectOllamaModels(customServerUrl)
        val ollamaModelsMarkdown = if (runningOllamaModels.isEmpty()) {
            "None detected"
        } else {
            runningOllamaModels.joinToString("\n") { 
                "- **${it.name}** (${if (it.isRemote) "Remote" else "On-Device"})"
            }
        }

        return """
            
            ### System Diagnostics
            - **Device Brand:** $brand
            - **Device Model:** $model
            - **Device Manufacturer:** $manufacturer
            - **Android Version:** $androidVersion (API Level $apiLevel)
            - **App Version:** $appVersionName (Code $appVersionCode)
            - **System Locale:** $locale
            - **Disk Storage:** $freeSpaceGb free / $totalSpaceGb total
            - **System Memory:** $freeMemMb free / $totalMemMb total
            
            ### Model Information
            - **Model Name:** $modelName
            - **Location:** ${if (isModelOnDevice) "On-Device" else "Remote"}
            - **Status:** ${if (isModelReady) "Ready" else "Not Ready"}
            
            ### Detected Ollama Models
            $ollamaModelsMarkdown
        """.trimIndent()
    }
}
