// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator

import android.content.Context
import com.toadcalculator.data.AppDatabase
import com.toadcalculator.data.DataStoreSettingsRepository
import com.toadcalculator.data.HistoryRepository
import com.toadcalculator.data.RoomHistoryRepository
import com.toadcalculator.data.SettingsRepository

/**
 * Hand-written dependency graph (manual constructor injection).
 *
 * Decision: no DI framework. Hilt would add ~200 KB to the APK and a kapt/ksp
 * build step for a graph this small; see WORKLOG for the full rationale.
 */
class AppGraph(context: Context) {
    private val database: AppDatabase = AppDatabase.get(context)

    val historyRepository: HistoryRepository = RoomHistoryRepository(database.historyDao())
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context)
}
