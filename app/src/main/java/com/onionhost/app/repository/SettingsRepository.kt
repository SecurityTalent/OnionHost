package com.onionhost.app.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "onionhost_settings")

class SettingsRepository @Inject constructor(
    private val context: Context
) {
    companion object {
        val KEY_AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
        val KEY_DEFAULT_PORT = intPreferencesKey("default_port")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_RATE_LIMIT = intPreferencesKey("rate_limit")
    }

    val autoStartOnBoot: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_START_BOOT] ?: false
    }

    val defaultPort: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_PORT] ?: 8080
    }

    val rateLimitPerMin: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_RATE_LIMIT] ?: 120
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_START_BOOT] = enabled
        }
    }

    suspend fun setDefaultPort(port: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_PORT] = port
        }
    }

    suspend fun setRateLimit(limit: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RATE_LIMIT] = limit
        }
    }
}
