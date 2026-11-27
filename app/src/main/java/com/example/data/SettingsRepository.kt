package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.wonderPlayDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wonderplay_settings"
)

data class AppSettings(
    val soundEnabled: Boolean = true,
    val difficulty: DifficultyLevel = DifficultyLevel.EASY
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val DIFFICULTY = stringPreferencesKey("difficulty")
    }

    val settingsFlow: Flow<AppSettings> = context.wonderPlayDataStore.data.map { prefs ->
        AppSettings(
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            difficulty = DifficultyLevel.fromWire(prefs[Keys.DIFFICULTY] ?: DifficultyLevel.EASY.wireValue)
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.wonderPlayDataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setDifficulty(difficulty: DifficultyLevel) {
        context.wonderPlayDataStore.edit { prefs ->
            prefs[Keys.DIFFICULTY] = difficulty.wireValue
        }
    }
}
