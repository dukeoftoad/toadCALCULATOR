// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toadcalculator.viewmodel.CalculatorViewModel
import com.toadcalculator.ui.calculator.CalculatorScreen

/**
 * Root of the single-activity UI. Navigation between future screens
 * (history sheet, settings, tools) is state-based; the calculator is home.
 */
@Composable
fun AppRoot() {
    val calculatorViewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory)
    CalculatorScreen(viewModel = calculatorViewModel)
}
