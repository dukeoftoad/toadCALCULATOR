// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.fakes

import com.toadcalculator.data.AngleModePref
import com.toadcalculator.data.HistoryEntry
import com.toadcalculator.data.HistoryRepository
import com.toadcalculator.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeHistoryRepository : HistoryRepository {
    val entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<HistoryEntry>> = entries

    override suspend fun add(expression: String, result: String) {
        if (expression.isBlank()) return
        entries.value = entries.value + HistoryEntry(
            id = nextId++,
            expression = expression,
            result = result,
            timestamp = System.currentTimeMillis(),
        )
    }

    override suspend fun delete(entry: HistoryEntry) {
        entries.value = entries.value.filterNot { it.id == entry.id }
    }

    override suspend fun clearAll() {
        entries.value = emptyList()
    }
}

class FakeSettingsRepository : SettingsRepository {
    private val _precision = MutableStateFlow(12)
    private val _angleMode = MutableStateFlow(AngleModePref.DEG)
    private val _haptics = MutableStateFlow(true)

    override val precision: Flow<Int> = _precision
    override val angleMode: Flow<AngleModePref> = _angleMode
    override val haptics: Flow<Boolean> = _haptics

    override suspend fun setPrecision(value: Int) {
        _precision.value = value
    }

    override suspend fun setAngleMode(value: AngleModePref) {
        _angleMode.value = value
    }

    override suspend fun setHaptics(value: Boolean) {
        _haptics.value = value
    }
}
