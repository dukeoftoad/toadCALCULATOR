// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Angle mode preference persisted by name. */
enum class AngleModePref { DEG, RAD }

private const val STORE_NAME = "toadcalculator_settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)

private object Keys {
    val THEME = stringPreferencesKey("theme")
    val PRECISION = intPreferencesKey("precision")
    val ANGLE_MODE = stringPreferencesKey("angle_mode")
    val HAPTICS = booleanPreferencesKey("haptics")
}

object SettingsDefaults {
    val THEME = ThemeMode.SYSTEM
    const val PRECISION = 12
    val ANGLE_MODE = AngleModePref.DEG
    const val HAPTICS = true
}

class DataStoreSettingsRepository(
    private val context: Context,
) : SettingsRepository {

    override val theme: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: SettingsDefaults.THEME.name) }
            .getOrDefault(SettingsDefaults.THEME)
    }

    override val precision: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        val v = prefs[Keys.PRECISION] ?: SettingsDefaults.PRECISION
        if (v in ALLOWED_PRECISIONS) v else SettingsDefaults.PRECISION
    }

    override val angleMode: Flow<AngleModePref> = context.settingsDataStore.data.map { prefs ->
        runCatching { AngleModePref.valueOf(prefs[Keys.ANGLE_MODE] ?: SettingsDefaults.ANGLE_MODE.name) }
            .getOrDefault(SettingsDefaults.ANGLE_MODE)
    }

    override val haptics: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.HAPTICS] ?: SettingsDefaults.HAPTICS
    }

    override suspend fun setTheme(value: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = value.name }
    }

    override suspend fun setPrecision(value: Int) {
        require(value in ALLOWED_PRECISIONS) { "Unsupported precision: $value" }
        context.settingsDataStore.edit { it[Keys.PRECISION] = value }
    }

    override suspend fun setAngleMode(value: AngleModePref) {
        context.settingsDataStore.edit { it[Keys.ANGLE_MODE] = value.name }
    }

    override suspend fun setHaptics(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.HAPTICS] = value }
    }

    companion object {
        val ALLOWED_PRECISIONS = setOf(6, 8, 10, 12, 15)
    }
}
