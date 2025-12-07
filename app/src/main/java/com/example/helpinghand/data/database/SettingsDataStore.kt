package com.example.helpinghand.data.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1) Extension DataStore on Context
val Context.settingsDataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(
    name = "user_settings"
)

// 2) Keys
class SettingsRepository(
private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_DYNAMIC_THEME = booleanPreferencesKey("dynamic_theme")
    }

    val darkModeEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_DARK_MODE] ?: false }

    // Keep track of whether user has seen onboarding before
    private val KEY_ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")

    val onboardingShown: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_ONBOARDING_SHOWN] ?: false }

    suspend fun setOnboardingShown() {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_SHOWN] = true
        }
    }

    val dynamicThemeEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_DYNAMIC_THEME] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = enabled
        }
    }

    suspend fun setDynamicTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_THEME] = enabled
        }
    }
}
