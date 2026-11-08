// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toadcalculator.R
import com.toadcalculator.ui.calculator.CalculatorScreen
import com.toadcalculator.ui.history.HistorySheet
import com.toadcalculator.ui.settings.SettingsScreen
import com.toadcalculator.ui.tools.AgeScreen
import com.toadcalculator.ui.tools.BmiScreen
import com.toadcalculator.ui.tools.DateDiffScreen
import com.toadcalculator.ui.tools.PercentScreen
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

/** Single-line brand wordmark: "toad" in the accent green, rest in onSurface. */
@Composable
private fun BrandTitle() {
    val primary = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = primary, fontWeight = FontWeight.ExtraBold)) {
                append("toad")
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("CALCULATOR")
            }
        },
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
        maxLines = 1,
    )
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

    // System back (gesture/button): drill back through tool → tools home →
    // calculator, mirroring the top-bar arrow. Without this the gesture closed
    // the whole app from any screen. The history sheet intercepts back itself
    // (ModalBottomSheet); this handler also covers the case where the sheet is
    // open over SETTINGS/TOOLS so the first back closes the sheet, not the
    // screen — preventing showHistory from lingering in saved state (B7).
    BackHandler(enabled = screen != AppScreen.CALCULATOR || showHistory) {
        when {
            showHistory -> showHistory = false
            screen == AppScreen.TOOLS && activeTool != null -> activeTool = null
            else -> {
                screen = AppScreen.CALCULATOR
                activeTool = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        screen == AppScreen.SETTINGS -> Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                            maxLines = 1,
                        )
                        screen == AppScreen.TOOLS && activeTool != null -> Text(
                            text = com.toadcalculator.ui.tools.toolTitle(activeTool!!),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                            maxLines = 1,
                        )
                        screen == AppScreen.TOOLS -> Text(
                            text = stringResource(R.string.tools_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                            maxLines = 1,
                        )
                        else -> BrandTitle()
                    }
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
                        // SCI pill — compact mode toggle, no more clipped text buttons.
                        Surface(
                            onClick = { calculatorViewModel.toggleScientific() },
                            shape = RoundedCornerShape(50),
                            color = if (scientific) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (scientific) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .padding(start = 4.dp)
                                .testTag(AppTags.TOGGLE_SCIENTIFIC)
                                .semantics {
                                    contentDescription = sciCd
                                    role = Role.Switch
                                    selected = scientific
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "SCI",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                )
                            }
                        }
                        IconButton(
                            onClick = { screen = AppScreen.TOOLS },
                            modifier = Modifier
                                .testTag(AppTags.OPEN_TOOLS)
                                .semantics { contentDescription = toolsCd },
                        ) {
                            Icon(Icons.Filled.Build, contentDescription = null)
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
                ToolDestination.PERCENT -> PercentScreen(modifier = contentModifier)
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
