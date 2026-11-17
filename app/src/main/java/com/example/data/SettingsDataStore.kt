package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "pixel_kick_settings")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val DURATION_MIN = intPreferencesKey("duration_min")
        val GAME_SPEED = floatPreferencesKey("game_speed")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val DUAL_PAD = booleanPreferencesKey("dual_pad")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val diffName = prefs[Keys.DIFFICULTY] ?: MatchDifficulty.PRO.name
        AppSettings(
            sfxEnabled = prefs[Keys.SFX_ENABLED] ?: true,
            sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.75f,
            defaultDurationMin = prefs[Keys.DURATION_MIN] ?: 3,
            defaultSpeed = prefs[Keys.GAME_SPEED] ?: 1.0f,
            difficulty = MatchDifficulty.entries.firstOrNull { it.name == diffName } ?: MatchDifficulty.PRO,
            dualPadEnabled = prefs[Keys.DUAL_PAD] ?: false
        )
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val currentDiff = prefs[Keys.DIFFICULTY] ?: MatchDifficulty.PRO.name
            val current = AppSettings(
                sfxEnabled = prefs[Keys.SFX_ENABLED] ?: true,
                sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.75f,
                defaultDurationMin = prefs[Keys.DURATION_MIN] ?: 3,
                defaultSpeed = prefs[Keys.GAME_SPEED] ?: 1.0f,
                difficulty = MatchDifficulty.entries.firstOrNull { it.name == currentDiff } ?: MatchDifficulty.PRO,
                dualPadEnabled = prefs[Keys.DUAL_PAD] ?: false
            )
            val updated = transform(current)
            prefs[Keys.SFX_ENABLED] = updated.sfxEnabled
            prefs[Keys.SFX_VOLUME] = updated.sfxVolume
            prefs[Keys.DURATION_MIN] = updated.defaultDurationMin
            prefs[Keys.GAME_SPEED] = updated.defaultSpeed
            prefs[Keys.DIFFICULTY] = updated.difficulty.name
            prefs[Keys.DUAL_PAD] = updated.dualPadEnabled
        }
    }
}
