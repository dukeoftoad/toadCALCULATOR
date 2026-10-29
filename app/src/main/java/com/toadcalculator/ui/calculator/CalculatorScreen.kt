// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.calculator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toadcalculator.R
import com.toadcalculator.math.MathError
import com.toadcalculator.math.NumberFormatter
import com.toadcalculator.viewmodel.CalculatorUiState
import com.toadcalculator.viewmodel.CalculatorViewModel

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalculatorScreenContent(state = state, onKey = viewModel::onKey, modifier = modifier)
}

@Composable
fun CalculatorScreenContent(
    state: CalculatorUiState,
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth >= 600.dp
        if (landscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                DisplayPanel(state = state, modifier = Modifier.weight(0.42f).fillMaxSize())
                KeypadPanel(
                    state = state,
                    onKey = onKey,
                    modifier = Modifier.weight(0.58f).fillMaxSize(),
                    landscape = true,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                DisplayPanel(state = state, modifier = Modifier.weight(1f).fillMaxWidth())
                KeypadPanel(state = state, onKey = onKey, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DisplayPanel(state: CalculatorUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = state.expression.ifEmpty { "0" },
            style = MaterialTheme.typography.displayLarge,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag(CalcTags.EXPRESSION),
        )
        Spacer(modifier = Modifier.padding(4.dp))
        val error = state.equalsError
        when {
            error != null -> Text(
                text = errorMessage(error),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CalcTags.ERROR),
            )
            state.previewValue != null -> Text(
                text = NumberFormatter.format(state.previewValue!!, state.precision, grouping = true),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CalcTags.RESULT),
            )
        }
    }
}

@Composable
internal fun errorMessage(error: MathError): String = when (error) {
    MathError.DivisionByZero -> stringResource(R.string.error_division_by_zero)
    MathError.Undefined -> stringResource(R.string.error_undefined)
    MathError.DomainError -> stringResource(R.string.error_domain)
    MathError.Overflow -> stringResource(R.string.error_overflow)
    is MathError.SyntaxError -> stringResource(R.string.error_syntax, error.position)
    MathError.UnbalancedParentheses -> stringResource(R.string.error_unbalanced)
}
