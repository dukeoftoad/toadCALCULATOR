// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.data

import kotlinx.coroutines.flow.Flow

/** App theme preference. Dynamic color is intentionally not offered. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Persisted user settings, exposed as hot Flows. */
interface SettingsRepository {
    /** App theme. */
    val theme: Flow<ThemeMode>

    /** Display precision in significant digits: one of 6, 8, 10, 12, 15. */
    val precision: Flow<Int>

    /** Angle mode applied on cold start. */
    val angleMode: Flow<AngleModePref>

    /** Whether key presses produce haptic feedback (also gated by system settings). */
    val haptics: Flow<Boolean>

    suspend fun setTheme(value: ThemeMode)

    suspend fun setPrecision(value: Int)

    suspend fun setAngleMode(value: AngleModePref)

    suspend fun setHaptics(value: Boolean)
}
