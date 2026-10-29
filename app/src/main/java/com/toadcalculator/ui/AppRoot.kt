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
import com.toadcalculator.ui.tools.AgeScreen
import com.toadcalculator.ui.tools.BmiScreen
import com.toadcalculator.ui.tools.DateDiffScreen
import com.toadcalculator.ui.tools.ToolDestination
import com.toadcalculator.ui.tools.ToolsHomeScreen
import com.toadcalculator.ui.tools.UnitConverterScreen
import com.toadcalculator.ui.theme.ToadCalculatorTheme
import com.toadcalculator.viewmodel.CalculatorViewModel
import com.toadcalculator.viewmodel.SettingsViewModel

enum class AppScreen { CALCULATOR, SETTINGS, TOOLS }

/**
 * Root of the single-activity UI. Applies the persisted theme and hosts the
 * calculator (home), settings, and the four offline tools, plus the history
 * bottom sheet. Navigation is simple state — no navigation library needed.
 */
@Composable
fun AppRoot() {
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    ToadCalculatorTheme(themeMode = settingsState.theme) {
        AppScaffold(settingsViewModel = settingsViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(settingsViewModel: SettingsViewModel) {
    val calculatorViewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory)
    var screen by rememberSaveable { mutableStateOf(AppScreen.CALCULATOR) }
    var activeTool by rememberSaveable { mutableStateOf<ToolDestination?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val history by calculatorViewModel.history.collectAsStateWithLifecycle()
    val calculatorState by calculatorViewModel.uiState.collectAsStateWithLifecycle()
    val scientific = calculatorState.scientific
    val historyCd = stringResource(R.string.cd_history)
    val sciCd = stringResource(R.string.cd_toggle_scientific)
    val settingsCd = stringResource(R.string.cd_open_settings)
    val toolsCd = stringResource(R.string.cd_open_tools)
    val backCd = stringResource(R.string.cd_back)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            screen == AppScreen.SETTINGS -> stringResource(R.string.settings_title)
                            screen == AppScreen.TOOLS && activeTool != null ->
                                com.toadcalculator.ui.tools.toolTitle(activeTool!!)
                            screen == AppScreen.TOOLS -> stringResource(R.string.tools_title)
                            else -> stringResource(R.string.app_name)
                        },
                    )
                },
                navigationIcon = {
                    if (screen != AppScreen.CALCULATOR) {
                        IconButton(
                            onClick = {
                                if (screen == AppScreen.TOOLS && activeTool != null) {
                                    activeTool = null
                                } else {
                                    screen = AppScreen.CALCULATOR
                                    activeTool = null
                                }
                            },
                            modifier = Modifier
                                .testTag(AppTags.BACK)
                                .semantics { contentDescription = backCd },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
                actions = {
                    if (screen == AppScreen.CALCULATOR) {
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
                        TextButton(
                            onClick = { screen = AppScreen.TOOLS },
                            modifier = Modifier
                                .testTag(AppTags.OPEN_TOOLS)
                                .semantics { contentDescription = toolsCd },
                        ) {
                            Text(text = "TOOLS", fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { screen = AppScreen.SETTINGS },
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
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            screen == AppScreen.SETTINGS -> SettingsScreen(
                state = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                onTheme = settingsViewModel::setTheme,
                onPrecision = settingsViewModel::setPrecision,
                onAngleMode = settingsViewModel::setAngleMode,
                onHaptics = settingsViewModel::setHaptics,
                onClearHistory = settingsViewModel::clearHistory,
                onOpenHistorySheet = { showHistory = true },
                modifier = contentModifier,
            )
            screen == AppScreen.TOOLS -> when (activeTool) {
                ToolDestination.UNIT_CONVERTER -> UnitConverterScreen(modifier = contentModifier)
                ToolDestination.BMI -> BmiScreen(modifier = contentModifier)
                ToolDestination.AGE -> AgeScreen(modifier = contentModifier)
                ToolDestination.DATE_DIFF -> DateDiffScreen(modifier = contentModifier)
                null -> ToolsHomeScreen(
                    onOpen = { activeTool = it },
                    modifier = contentModifier,
                )
            }
            else -> CalculatorScreen(viewModel = calculatorViewModel, modifier = contentModifier)
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
    const val OPEN_TOOLS = "open_tools"
    const val BACK = "back"
}
