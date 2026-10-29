// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toadcalculator.R
import com.toadcalculator.ui.calculator.CalculatorScreen
import com.toadcalculator.ui.history.HistorySheet
import com.toadcalculator.ui.settings.SettingsScreen
import com.toadcalculator.ui.theme.ToadCalculatorTheme
import com.toadcalculator.viewmodel.CalculatorViewModel
import com.toadcalculator.viewmodel.SettingsViewModel

/**
 * Root of the single-activity UI. Applies the persisted theme, hosts the
 * calculator (home) and settings screens as simple state-based navigation,
 * plus the history bottom sheet and the SCI toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    ToadCalculatorTheme(themeMode = settingsState.theme) {
        AppScaffold(
            settingsViewModel = settingsViewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(settingsViewModel: SettingsViewModel) {
    val calculatorViewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory)
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val history by calculatorViewModel.history.collectAsStateWithLifecycle()
    val calculatorState by calculatorViewModel.uiState.collectAsStateWithLifecycle()
    val scientific = calculatorState.scientific
    val historyCd = stringResource(R.string.cd_history)
    val sciCd = stringResource(R.string.cd_toggle_scientific)
    val settingsCd = stringResource(R.string.cd_open_settings)
    val backCd = stringResource(R.string.cd_back)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (showSettings) R.string.settings_title else R.string.app_name,
                        ),
                    )
                },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(
                            onClick = { showSettings = false },
                            modifier = Modifier
                                .testTag(AppTags.BACK)
                                .semantics { contentDescription = backCd },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
                actions = {
                    if (!showSettings) {
                        TextButton(
                            onClick = { calculatorViewModel.toggleScientific() },
                            modifier = Modifier
                                .testTag(AppTags.TOGGLE_SCIENTIFIC)
                                .semantics { contentDescription = sciCd },
                        ) {
                            Text(
                                text = "SCI",
                                color = if (scientific) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .testTag(AppTags.OPEN_SETTINGS)
                                .semantics { contentDescription = settingsCd },
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null)
                        }
                        IconButton(
                            onClick = { showHistory = true },
                            modifier = Modifier
                                .testTag(AppTags.OPEN_HISTORY)
                                .semantics { contentDescription = historyCd },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        if (showSettings) {
            SettingsScreen(
                state = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                onTheme = settingsViewModel::setTheme,
                onPrecision = settingsViewModel::setPrecision,
                onAngleMode = settingsViewModel::setAngleMode,
                onHaptics = settingsViewModel::setHaptics,
                onClearHistory = settingsViewModel::clearHistory,
                onOpenHistorySheet = { showHistory = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            CalculatorScreen(
                viewModel = calculatorViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    HistorySheet(
        entries = history,
        visible = showHistory,
        onDismiss = { showHistory = false },
        onLoad = { expression ->
            calculatorViewModel.loadExpression(expression)
            showHistory = false
        },
        onDelete = calculatorViewModel::deleteHistoryEntry,
        onClearAll = calculatorViewModel::clearHistory,
    )
}

object AppTags {
    const val OPEN_HISTORY = "open_history"
    const val TOGGLE_SCIENTIFIC = "toggle_scientific"
    const val OPEN_SETTINGS = "open_settings"
    const val BACK = "back"
}
