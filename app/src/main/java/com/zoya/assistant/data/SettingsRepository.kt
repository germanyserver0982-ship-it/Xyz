package com.zoya.assistant.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "zoya_settings")

/**
 * Everything user-configurable lives here instead of hardcoded in Gradle/BuildConfig — the
 * Gemini API key in particular, so it's entered once from the in-app Settings screen and
 * persists across rebuilds, matching a normal consumer app's expectations.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val VOICE_NAME = stringPreferencesKey("voice_name")
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { it[Keys.GEMINI_API_KEY].orEmpty() }
    val voiceNameFlow: Flow<String> = context.dataStore.data.map { it[Keys.VOICE_NAME] ?: "Aoede" }

    suspend fun getApiKeyOnce(): String = apiKeyFlow.first()

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { it[Keys.GEMINI_API_KEY] = key.trim() }
    }

    suspend fun saveVoiceName(voice: String) {
        context.dataStore.edit { it[Keys.VOICE_NAME] = voice }
    }
}
