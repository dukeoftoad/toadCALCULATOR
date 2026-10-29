// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.calculator

import android.view.HapticFeedbackConstants
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

/** Base keypad: 5 rows × 4 columns, matching the Phase 2 key set. */
private val BASE_ROWS: List<List<KeypadKey>> = listOf(
    listOf(KeypadKey.Clear, KeypadKey.LParen, KeypadKey.RParen, KeypadKey.Backspace),
    listOf(KeypadKey.Digit('7'), KeypadKey.Digit('8'), KeypadKey.Digit('9'), KeypadKey.Div),
    listOf(KeypadKey.Digit('4'), KeypadKey.Digit('5'), KeypadKey.Digit('6'), KeypadKey.Mul),
    listOf(KeypadKey.Digit('1'), KeypadKey.Digit('2'), KeypadKey.Digit('3'), KeypadKey.Minus),
    listOf(KeypadKey.Digit('0'), KeypadKey.Dot, KeypadKey.Equals, KeypadKey.Plus),
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

    if (landscape) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KeypadGrid(
                rows = BASE_ROWS,
                columns = 2,
                modifier = Modifier.weight(1f),
                performKey = performKey,
            )
            KeypadGrid(
                rows = BASE_ROWS,
                columns = 2,
                offset = 2,
                modifier = Modifier.weight(1f),
                performKey = performKey,
            )
        }
    } else {
        KeypadGrid(
            rows = BASE_ROWS,
            columns = 4,
            modifier = modifier
                .fillMaxWidth()
                .padding(6.dp),
            performKey = performKey,
        )
    }
}

@Composable
private fun KeypadGrid(
    rows: List<List<KeypadKey>>,
    columns: Int,
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
                repeat(columns) { c ->
                    val key = row.getOrNull(offset + c) ?: return@repeat
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

@Composable
fun KeypadButton(
    key: KeypadKey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (container, content) = colorsFor(key)
    val label = key.display
    val cd = when (key) {
        KeypadKey.Clear -> stringResource(R.string.cd_key_clear)
        KeypadKey.Backspace -> stringResource(R.string.cd_key_backspace)
        KeypadKey.Equals -> stringResource(R.string.cd_key_equals)
        KeypadKey.LParen -> stringResource(R.string.cd_key_lparen)
        KeypadKey.RParen -> stringResource(R.string.cd_key_rparen)
        KeypadKey.Dot -> stringResource(R.string.cd_key_dot)
        is KeypadKey.Digit -> stringResource(R.string.cd_key_digit, label)
        else -> label
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(CalcTags.key(key))
            .semantics { contentDescription = cd },
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
