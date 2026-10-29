// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toadcalculator.R
import com.toadcalculator.ui.calculator.CalculatorScreen
import com.toadcalculator.ui.history.HistorySheet
import com.toadcalculator.ui.history.HistoryTags
import com.toadcalculator.viewmodel.CalculatorViewModel

/**
 * Root of the single-activity UI: a compact top bar over the calculator,
 * with the history bottom sheet. Future screens (settings, tools) attach here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val calculatorViewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory)
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val history by calculatorViewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    val cd = stringResource(R.string.cd_history)
                    IconButton(
                        onClick = { showHistory = true },
                        modifier = Modifier
                            .testTag(AppTags.OPEN_HISTORY)
                            .semantics { contentDescription = cd },
                    ) {
                        Icon(Icons.Filled.List, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            CalculatorScreen(
                viewModel = calculatorViewModel,
                modifier = Modifier.fillMaxSize(),
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
}
