// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.calculator

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toadcalculator.R
import com.toadcalculator.viewmodel.CalculatorUiState

/** Base keypad: 5 rows × 4 columns. */
private val BASE_ROWS: List<List<KeypadKey>> = listOf(
    listOf(KeypadKey.Clear, KeypadKey.LParen, KeypadKey.RParen, KeypadKey.Backspace),
    listOf(KeypadKey.Digit('7'), KeypadKey.Digit('8'), KeypadKey.Digit('9'), KeypadKey.Div),
    listOf(KeypadKey.Digit('4'), KeypadKey.Digit('5'), KeypadKey.Digit('6'), KeypadKey.Mul),
    listOf(KeypadKey.Digit('1'), KeypadKey.Digit('2'), KeypadKey.Digit('3'), KeypadKey.Minus),
    listOf(KeypadKey.Digit('0'), KeypadKey.Dot, KeypadKey.Equals, KeypadKey.Plus),
)

/** Scientific function rows, primary (1st) set. Parentheses live in the base grid. */
private val SCI_PRIMARY_ROWS: List<List<KeypadKey>> = listOf(
    listOf(KeypadKey.Sin, KeypadKey.Cos, KeypadKey.Tan, KeypadKey.Ln),
    listOf(KeypadKey.Log, KeypadKey.Sqrt, KeypadKey.Pow, KeypadKey.Fact),
    listOf(KeypadKey.Pi, KeypadKey.E, KeypadKey.Percent),
)

/** Scientific function rows, secondary (2nd) set. */
private val SCI_SECONDARY_ROWS: List<List<KeypadKey>> = listOf(
    listOf(KeypadKey.Asin, KeypadKey.Acos, KeypadKey.Atan, KeypadKey.Log2),
    listOf(KeypadKey.Cbrt, KeypadKey.Abs),
    listOf(KeypadKey.Pi, KeypadKey.E, KeypadKey.Percent),
)

@Composable
fun KeypadPanel(
    state: CalculatorUiState,
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
    landscape: Boolean = false,
) {
    val view = LocalView.current
    val performKey: (KeypadKey) -> Unit = { key ->
        if (state.haptics) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        onKey(key)
    }

    Column(modifier = modifier.fillMaxWidth().padding(6.dp)) {
        AnimatedVisibility(
            visible = state.scientific,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            ScientificBlock(
                state = state,
                performKey = performKey,
                landscape = landscape,
            )
        }
        KeypadGrid(
            rows = BASE_ROWS,
            columnsPerRow = 4,
            modifier = Modifier.fillMaxWidth(),
            performKey = performKey,
        )
    }
}

@Composable
private fun ScientificBlock(
    state: CalculatorUiState,
    performKey: (KeypadKey) -> Unit,
    landscape: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AngleChip(state = state, onClick = { performKey(KeypadKey.AngleToggle) }, modifier = Modifier.weight(1f))
            SecondChip(state = state, onClick = { performKey(KeypadKey.Second) }, modifier = Modifier.weight(1f))
        }
        Crossfade(targetState = state.secondMode, label = "sci-functions") { second ->
            val rows = if (second) SCI_SECONDARY_ROWS else SCI_PRIMARY_ROWS
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { key ->
                            KeypadButton(
                                key = key,
                                onClick = { performKey(key) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AngleChip(state: CalculatorUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = state.angleMode.name
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(CalcTags.key(KeypadKey.AngleToggle) + "_chip"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun SecondChip(state: CalculatorUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(CalcTags.key(KeypadKey.Second) + "_chip"),
        shape = RoundedCornerShape(12.dp),
        color = if (state.secondMode) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (state.secondMode) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    ) {
        Text(
            text = "2nd",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun KeypadGrid(
    rows: List<List<KeypadKey>>,
    columnsPerRow: Int,
    modifier: Modifier = Modifier,
    offset: Int = 0,
    performKey: (KeypadKey) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(columnsPerRow) { c ->
                    val key = row.getOrNull(offset + c) ?: return@repeat
                    KeypadButton(
                        key = key,
                        onClick = { performKey(key) },
                        onLongClick = if (key is KeypadKey.Backspace) {
                            { performKey(KeypadKey.Clear) }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
    key: KeypadKey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val (container, content) = colorsFor(key)
    val label = when (key) {
        KeypadKey.AngleToggle -> "DEG"
        else -> key.display
    }
    val cd = when (key) {
        KeypadKey.Clear -> stringResource(R.string.cd_key_clear)
        KeypadKey.Backspace -> stringResource(R.string.cd_key_backspace)
        KeypadKey.Equals -> stringResource(R.string.cd_key_equals)
        KeypadKey.LParen -> stringResource(R.string.cd_key_lparen)
        KeypadKey.RParen -> stringResource(R.string.cd_key_rparen)
        KeypadKey.Dot -> stringResource(R.string.cd_key_dot)
        KeypadKey.Second -> stringResource(R.string.cd_key_second)
        KeypadKey.AngleToggle -> stringResource(R.string.cd_key_angle)
        KeypadKey.Sin -> stringResource(R.string.cd_key_sin)
        KeypadKey.Cos -> stringResource(R.string.cd_key_cos)
        KeypadKey.Tan -> stringResource(R.string.cd_key_tan)
        KeypadKey.Ln -> stringResource(R.string.cd_key_ln)
        KeypadKey.Log -> stringResource(R.string.cd_key_log)
        KeypadKey.Sqrt -> stringResource(R.string.cd_key_sqrt)
        KeypadKey.Pow -> stringResource(R.string.cd_key_pow)
        KeypadKey.Fact -> stringResource(R.string.cd_key_fact)
        KeypadKey.Pi -> stringResource(R.string.cd_key_pi)
        KeypadKey.E -> stringResource(R.string.cd_key_e)
        KeypadKey.Asin -> stringResource(R.string.cd_key_asin)
        KeypadKey.Acos -> stringResource(R.string.cd_key_acos)
        KeypadKey.Atan -> stringResource(R.string.cd_key_atan)
        KeypadKey.Log2 -> stringResource(R.string.cd_key_log2)
        KeypadKey.Cbrt -> stringResource(R.string.cd_key_cbrt)
        KeypadKey.Abs -> stringResource(R.string.cd_key_abs)
        KeypadKey.Percent -> stringResource(R.string.cd_key_percent)
        is KeypadKey.Digit -> stringResource(R.string.cd_key_digit, label)
        KeypadKey.Plus -> stringResource(R.string.cd_key_plus)
        KeypadKey.Minus -> stringResource(R.string.cd_key_minus)
        KeypadKey.Mul -> stringResource(R.string.cd_key_mul)
        KeypadKey.Div -> stringResource(R.string.cd_key_div)
    }
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(CalcTags.key(key))
            .semantics { contentDescription = cd }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun colorsFor(key: KeypadKey): Pair<Color, Color> {
    val s = MaterialTheme.colorScheme
    return when (key) {
        KeypadKey.Clear -> s.errorContainer to s.onErrorContainer
        KeypadKey.Equals -> s.primary to s.onPrimary
        is KeypadKey.Digit, KeypadKey.Dot -> s.surfaceVariant to s.onSurfaceVariant
        else -> s.secondaryContainer to s.onSecondaryContainer
    }
}
