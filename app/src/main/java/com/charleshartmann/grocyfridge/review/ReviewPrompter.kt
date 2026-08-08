package com.charleshartmann.grocyfridge.review

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await

private val Context.reviewPromptDataStore: DataStore<Preferences> by preferencesDataStore(name = "review_prompt_prefs")

private object Keys {
    val SYNC_COUNT = intPreferencesKey("review_prompt_sync_count")
    val REQUESTED = booleanPreferencesKey("review_prompt_requested")
}

private const val SYNCS_BEFORE_FIRST_ASK = 3

object ReviewPrompter {
    suspend fun maybeRequestReview(activity: Activity) {
        var shouldRequest = false
        activity.applicationContext.reviewPromptDataStore.edit { prefs ->
            val alreadyRequested = prefs[Keys.REQUESTED] ?: false
            val count = (prefs[Keys.SYNC_COUNT] ?: 0) + 1
            prefs[Keys.SYNC_COUNT] = count
            if (!alreadyRequested && count >= SYNCS_BEFORE_FIRST_ASK) {
                prefs[Keys.REQUESTED] = true
                shouldRequest = true
            }
        }
        if (!shouldRequest) return
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, reviewInfo).await()
        }
    }
}
