package com.charleshartmann.grocyfridge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.bugReportRepoDataStore by preferencesDataStore(name = "feedback_bug_reports")

class BugReportRepo(private val context: Context) {
    private val key = stringPreferencesKey("bug_reports_list")
    private val serializer = ListSerializer(BugReport.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    val reports: Flow<List<BugReport>> = context.bugReportRepoDataStore.data.map { prefs ->
        prefs[key]?.let { encoded ->
            runCatching { json.decodeFromString(serializer, encoded) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun getBugReportsList(): List<BugReport> {
        return reports.first()
    }

    suspend fun updateBugReports(reportsList: List<BugReport>) {
        context.bugReportRepoDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(serializer, reportsList)
        }
    }

    suspend fun saveBugReport(report: BugReport) {
        context.bugReportRepoDataStore.edit { prefs ->
            val current = prefs[key]?.let { encoded ->
                runCatching { json.decodeFromString(serializer, encoded) }.getOrDefault(emptyList())
            } ?: emptyList()
            // updates or adds
            val filtered = current.filter { it.number != report.number }
            prefs[key] = json.encodeToString(serializer, listOf(report) + filtered)
        }
    }
}
