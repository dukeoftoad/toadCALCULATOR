// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.calculator

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * The display. Bottom-anchored above the keypad; single-line expression that
 * auto-shrinks and always keeps its tail visible; live preview beneath while
 * typing; after `=` the result itself becomes the main line — no duplicated
 * number shown twice. Long-press the display to copy the current value.
 */
@Composable
private fun DisplayPanel(state: CalculatorUiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val hScroll = rememberScrollState()
    val justEvaluated = state.justEvaluated && state.equalsError == null
    val memCd = stringResource(R.string.cd_memory_indicator)
    val ansCd = stringResource(R.string.cd_ans_indicator)

    // Keep the tail (caret end) of long expressions in view.
    LaunchedEffect(state.expression) {
        hScroll.animateScrollTo(hScroll.maxValue)
    }

    // The most useful value to copy: preview/final result, else raw expression.
    val copyValue = when {
        state.previewValue != null ->
            NumberFormatter.format(state.previewValue!!, state.precision, grouping = true)
        else -> state.expression
    }

    Column(
        modifier = modifier
            .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
            .pointerInput(copyValue) {
                // Long-press copies the current value. Deliberately NOT
                // clickable/combinedClickable here: clickable adds
                // mergeDescendants semantics that swallow the display Texts'
                // test tags into the merged tree (and hide them from
                // onNodeWithTag), which broke the whole display test suite.
                detectTapGestures(
                    onLongPress = {
                        if (copyValue.isNotEmpty() && copyValue != "0") {
                            clipboard.setText(AnnotatedString(copyValue))
                            Toast.makeText(context, R.string.copied_toast, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            },
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        val expressionText = state.expression.ifEmpty { "0" }
        // Auto-shrink: long expressions stay single-line and readable.
        val fontSize = when {
            expressionText.length > 32 -> 26.sp
            expressionText.length > 24 -> 32.sp
            expressionText.length > 16 -> 38.sp
            else -> 46.sp
        }
        // Memory / Ans status badges — appear above the expression only when the
        // corresponding store holds a value (Phase 9.8).
        if (state.memory != null || state.lastAnswer != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.memory != null) {
                    Text(
                        text = "M",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag(CalcTags.MEMORY_INDICATOR)
                            .semantics { contentDescription = memCd },
                    )
                }
                if (state.lastAnswer != null) {
                    Text(
                        text = "Ans",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag(CalcTags.ANS_INDICATOR)
                            .semantics { contentDescription = ansCd },
                    )
                }
            }
        }
        if (justEvaluated) {
            // After `=` the result IS the main line — rendered as the primary accent.
            // A plain number: no glyph transform (keeps the sign ASCII for tests).
            Text(
                text = expressionText,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
                    .testTag(CalcTags.RESULT),
            )
        } else {
            Text(
                text = displayExpression(expressionText),
                fontSize = fontSize,
                fontWeight = FontWeight.Normal,
                color = if (state.expression.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
                    .testTag(CalcTags.EXPRESSION),
            )
            val error = state.equalsError
            when {
                error != null -> Text(
                    text = errorMessage(error),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag(CalcTags.ERROR),
                )
                state.previewValue != null -> Text(
                    text = NumberFormatter.format(state.previewValue!!, state.precision, grouping = true),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag(CalcTags.RESULT),
                )
            }
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

/**
 * Pretty-prints the raw expression for the display: ASCII operators that the
 * engine parses (`*` `/`) become the standard glyphs (`×` `÷`), and the verbose
 * function names `sqrt(` / `cbrt(` collapse to `√(` / `∛(`. The stored
 * [CalculatorUiState.expression] stays ASCII so history entries re-parse; only
 * the rendered text is swapped. Applied to the expression line only — the
 * result/preview line shows formatted numbers and is left untouched (B9).
 */
internal fun displayExpression(raw: String): String =
    raw
        .replace("sqrt(", "√(")
        .replace("cbrt(", "∛(")
        .replace("*", "×")
        .replace("/", "÷")
