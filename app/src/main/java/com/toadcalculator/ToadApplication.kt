// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator

import android.app.Application
import com.toadcalculator.data.AppDatabase
import com.toadcalculator.data.DataStoreSettingsRepository
import com.toadcalculator.data.HistoryRepository
import com.toadcalculator.data.RoomHistoryRepository
import com.toadcalculator.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application holding the hand-written dependency graph (manual constructor
 * injection — no DI framework; see WORKLOG Phase 0 for the rationale).
 */
class ToadApplication : Application() {

    /**
     * Process-wide scope for fire-and-forget work that must outlive composition —
     * the silent update check (StartupUpdateCheck), launched on app open. Using an
     * application scope (not a composition scope) keeps the network attempt off
     * Compose's idle clock, the same pattern toadFILES uses for its startup check.
     */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val graph: AppGraph by lazy {
        val db = AppDatabase.get(this)
        AppGraph(
            historyRepository = RoomHistoryRepository(db.historyDao()),
            settingsRepository = DataStoreSettingsRepository(this),
        )
    }
}

/** Simple immutable holder for the app-wide singletons. */
class AppGraph(
    val historyRepository: HistoryRepository,
    val settingsRepository: SettingsRepository,
)
