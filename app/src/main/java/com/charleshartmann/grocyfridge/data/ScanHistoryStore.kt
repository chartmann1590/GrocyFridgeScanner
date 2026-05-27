package com.charleshartmann.grocyfridge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.charleshartmann.grocyfridge.model.ScanHistoryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.historyDataStore by preferencesDataStore(name = "scan_history")

class ScanHistoryStore(private val context: Context) {
    private val historyKey = stringPreferencesKey("records")
    private val serializer = ListSerializer(ScanHistoryRecord.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    val records: Flow<List<ScanHistoryRecord>> = context.historyDataStore.data.map { prefs ->
        prefs[historyKey]?.let { encoded ->
            runCatching { json.decodeFromString(serializer, encoded) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun add(record: ScanHistoryRecord) {
        context.historyDataStore.edit { prefs ->
            val current = prefs[historyKey]?.let { encoded ->
                runCatching { json.decodeFromString(serializer, encoded) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[historyKey] = json.encodeToString(serializer, (listOf(record) + current).take(25))
        }
    }

    suspend fun delete(record: ScanHistoryRecord) {
        context.historyDataStore.edit { prefs ->
            val current = prefs[historyKey]?.let { encoded ->
                runCatching { json.decodeFromString(serializer, encoded) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[historyKey] = json.encodeToString(serializer, current.filter { it.timestampMillis != record.timestampMillis })
        }
    }
}
