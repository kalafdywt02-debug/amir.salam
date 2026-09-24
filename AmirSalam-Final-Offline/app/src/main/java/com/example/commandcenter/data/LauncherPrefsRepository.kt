package com.example.commandcenter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore by preferencesDataStore("amirsalam_launcher_prefs")

// Colors are stored as ARGB Long (0xFFRRGGBB) so they round-trip cleanly
// through DataStore without needing a Compose dependency in this file.
data class LauncherAppearance(
    val columns: Int = 4,
    val showSearchBar: Boolean = true,
    val backgroundColor: Long = 0xFF0B0D12,
    val accentColor: Long = 0xFF8DE1BE
)

class LauncherPrefsRepository(private val context: Context) {
    private val columnsKey = intPreferencesKey("columns")
    private val showSearchKey = booleanPreferencesKey("show_search")
    private val backgroundKey = longPreferencesKey("background_color")
    private val accentKey = longPreferencesKey("accent_color")

    val appearance: Flow<LauncherAppearance> = context.launcherDataStore.data.map { prefs ->
        LauncherAppearance(
            columns = prefs[columnsKey] ?: 4,
            showSearchBar = prefs[showSearchKey] ?: true,
            backgroundColor = prefs[backgroundKey] ?: 0xFF0B0D12,
            accentColor = prefs[accentKey] ?: 0xFF8DE1BE
        )
    }

    suspend fun setColumns(value: Int) {
        context.launcherDataStore.edit { it[columnsKey] = value.coerceIn(3, 6) }
    }

    suspend fun setShowSearchBar(value: Boolean) {
        context.launcherDataStore.edit { it[showSearchKey] = value }
    }

    suspend fun setBackgroundColor(value: Long) {
        context.launcherDataStore.edit { it[backgroundKey] = value }
    }

    suspend fun setAccentColor(value: Long) {
        context.launcherDataStore.edit { it[accentKey] = value }
    }
}
