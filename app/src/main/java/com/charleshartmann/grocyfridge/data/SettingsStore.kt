package com.charleshartmann.grocyfridge.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.charleshartmann.grocyfridge.BuildConfig
import com.charleshartmann.grocyfridge.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val urlKey = stringPreferencesKey("grocy_url")
    private val onboardingKey = booleanPreferencesKey("onboarding_complete")
    private val apiKeyAlias = "grocy_api_key"
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secrets",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            grocyUrl = prefs[urlKey].orEmpty().ifBlank { BuildConfig.DEFAULT_GROCY_URL },
            grocyApiKey = encryptedPrefs.getString(apiKeyAlias, null).orEmpty()
                .ifBlank { BuildConfig.DEFAULT_GROCY_API_KEY }
        )
    }

    val onboardingComplete: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[onboardingKey] ?: false
    }

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[urlKey] = settings.grocyUrl.trim().trimEnd('/')
        }
        encryptedPrefs.edit()
            .putString(apiKeyAlias, settings.grocyApiKey.trim())
            .apply()
    }

    suspend fun markOnboardingComplete() {
        context.settingsDataStore.edit { prefs ->
            prefs[onboardingKey] = true
        }
    }
}
