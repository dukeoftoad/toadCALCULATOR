// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toadcalculator.data.AngleModePref
import com.toadcalculator.data.HistoryRepository
import com.toadcalculator.data.SettingsRepository
import com.toadcalculator.data.SettingsDefaults
import com.toadcalculator.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: ThemeMode = SettingsDefaults.THEME,
    val precision: Int = SettingsDefaults.PRECISION,
    val angleMode: AngleModePref = SettingsDefaults.ANGLE_MODE,
    val haptics: Boolean = SettingsDefaults.HAPTICS,
)

/** Settings screen ViewModel: one StateFlow over all preferences + action callbacks. */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            settingsRepository.theme,
            settingsRepository.precision,
            settingsRepository.angleMode,
            settingsRepository.haptics,
        ) { theme, precision, angle, haptics ->
            SettingsUiState(theme, precision, angle, haptics)
        }.onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun setTheme(value: ThemeMode) = launchSet { settingsRepository.setTheme(value) }
    fun setPrecision(value: Int) = launchSet { settingsRepository.setPrecision(value) }
    fun setAngleMode(value: AngleModePref) = launchSet { settingsRepository.setAngleMode(value) }
    fun setHaptics(value: Boolean) = launchSet { settingsRepository.setHaptics(value) }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clearAll() }
    }

    private fun launchSet(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as com.toadcalculator.ToadApplication
                SettingsViewModel(
                    settingsRepository = app.graph.settingsRepository,
                    historyRepository = app.graph.historyRepository,
                )
            }
        }
    }
}
