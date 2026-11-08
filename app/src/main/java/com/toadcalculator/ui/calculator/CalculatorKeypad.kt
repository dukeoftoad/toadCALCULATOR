// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.calculator

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toadcalculator.R
import com.toadcalculator.viewmodel.CalculatorUiState

/**
 * Base keypad: 5 rows × 4 columns.
 *
 * Layout (refined for thumb reach and modern convention — `=` lives in the
 * bottom-right corner, the operator column anchors the right edge):
 *
 *   AC    (    )    ⌫
 *   7     8    9    ÷
 *   4     5    6    ×
 *   1     2    3    −
 *   0     .    +    =
 */
private val BASE_ROWS: List<List<KeypadKey>> = listOf(
    listOf(KeypadKey.Clear, KeypadKey.LParen, KeypadKey.RParen, KeypadKey.Backspace),
    listOf(KeypadKey.Digit('7'), KeypadKey.Digit('8'), KeypadKey.Digit('9'), KeypadKey.Div),
    listOf(KeypadKey.Digit('4'), KeypadKey.Digit('5'), KeypadKey.Digit('6'), KeypadKey.Mul),
    listOf(KeypadKey.Digit('1'), KeypadKey.Digit('2'), KeypadKey.Digit('3'), KeypadKey.Minus),
    listOf(KeypadKey.Digit('0'), KeypadKey.Dot, KeypadKey.Plus, KeypadKey.Equals),
)

/**
 * Scientific function keys. Rows 1–2 swap behind the 2nd toggle; the constant
 * row (π e % x²) is always visible while the scientific block is open.
 */
private val SCI_PRIMARY_ROWS: List<List<KeypadKey>> = listOf(
    listOf(KeypadKey.Sin, KeypadKey.Cos, KeypadKey.Tan, KeypadKey.Ln),
    listOf(KeypadKey.Log, KeypadKey.Sqrt, KeypadKey.Pow, KeypadKey.Fact),
)

private val SCI_SECONDARY_ROWS: List<List<KeypadKey>> = listOf(
    listOf(KeypadKey.Asin, KeypadKey.Acos, KeypadKey.Atan, KeypadKey.Log2),
    listOf(KeypadKey.Cbrt, KeypadKey.Abs, KeypadKey.Pow, KeypadKey.Fact),
)

private val SCI_CONSTANT_ROW: List<KeypadKey> = listOf(
    KeypadKey.Pi, KeypadKey.E, KeypadKey.Percent, KeypadKey.Square,
)

/**
 * Memory + last-answer row (Phase 9.8). Lives in the scientific block so the
 * basic 5×4 keypad — and its viewport budget — stays unchanged. MC/MR/M+/M−
 * operate on the calculator memory; Ans recalls the last successful result.
 */
private val MEMORY_ROW: List<KeypadKey> = listOf(
    KeypadKey.MemoryClear, KeypadKey.MemoryRecall,
    KeypadKey.MemoryPlus, KeypadKey.MemoryMinus, KeypadKey.Ans,
)

/** Visual spec per key tier: container, content, label size, label weight. */
private data class KeyStyle(
    val container: Color,
    val content: Color,
    val fontSize: TextUnit,
    val fontWeight: FontWeight,
)

@Composable
private fun styleFor(key: KeypadKey): KeyStyle {
    val s = MaterialTheme.colorScheme
    return when (key) {
        // Digits: elevated near-neutral — the largest visual tier.
        is KeypadKey.Digit, KeypadKey.Dot -> KeyStyle(
            s.surfaceContainerHigh, s.onSurface, 24.sp, FontWeight.SemiBold,
        )
        // Operator column: green-tinted containers.
        KeypadKey.Plus, KeypadKey.Minus, KeypadKey.Mul, KeypadKey.Div -> KeyStyle(
            s.primaryContainer, s.onPrimaryContainer, 26.sp, FontWeight.Normal,
        )
        // The single filled accent of the pad.
        KeypadKey.Equals -> KeyStyle(
            s.primary, s.onPrimary, 26.sp, FontWeight.Bold,
        )
        // AC: muted destructive.
        KeypadKey.Clear -> KeyStyle(
            s.errorContainer, s.onErrorContainer, 18.sp, FontWeight.Bold,
        )
        // Row-1 companions: slightly quieter than digits.
        KeypadKey.LParen, KeypadKey.RParen, KeypadKey.Backspace -> KeyStyle(
            s.surfaceContainer, s.onSurface, 22.sp, FontWeight.Normal,
        )
        // Scientific tier: quiet secondary surface.
        else -> KeyStyle(
            s.surfaceContainerLow, s.onSurfaceVariant, 16.sp, FontWeight.SemiBold,
        )
    }
}

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            performKey = performKey,
        )
    }
}

@Composable
private fun ScientificBlock(
    state: CalculatorUiState,
    performKey: (KeypadKey) -> Unit,
    @Suppress("UNUSED_PARAMETER") landscape: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondChip(state = state, onClick = { performKey(KeypadKey.Second) }, modifier = Modifier.weight(1f))
            AngleChip(state = state, onClick = { performKey(KeypadKey.AngleToggle) }, modifier = Modifier.weight(1f))
        }
        // Memory + Ans row — always visible while the scientific pad is open.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MEMORY_ROW.forEach { key ->
                KeypadButton(
                    key = key,
                    onClick = { performKey(key) },
                    height = 50.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Crossfade(targetState = state.secondMode, label = "sci-functions") { second ->
            val rows = if (second) SCI_SECONDARY_ROWS else SCI_PRIMARY_ROWS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            KeypadButton(
                                key = key,
                                onClick = { performKey(key) },
                                height = 50.dp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                // Constants row stays put across the 2nd toggle — muscle memory.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SCI_CONSTANT_ROW.forEach { key ->
                        KeypadButton(
                            key = key,
                            onClick = { performKey(key) },
                            height = 50.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AngleChip(state: CalculatorUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PillChip(
        label = state.angleMode.name,
        onClick = onClick,
        modifier = modifier.testTag(CalcTags.key(KeypadKey.AngleToggle) + "_chip"),
        container = MaterialTheme.colorScheme.tertiaryContainer,
        content = MaterialTheme.colorScheme.onTertiaryContainer,
    )
}

@Composable
private fun SecondChip(state: CalculatorUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    PillChip(
        label = "2nd",
        onClick = onClick,
        modifier = modifier.testTag(CalcTags.key(KeypadKey.Second) + "_chip"),
        container = if (state.secondMode) s.primary else s.surfaceContainerHigh,
        content = if (state.secondMode) s.onPrimary else s.onSurfaceVariant,
        // B16: expose the toggle state to TalkBack as a checked switch.
        selected = state.secondMode,
    )
}

/** Compact rounded toggle used for 2nd / DEG-RAD. */
@Composable
private fun PillChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color,
    content: Color,
    /** When non-null, the chip is announced as a checked switch (B16). */
    selected: Boolean? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .semantics {
                this.contentDescription = label
                if (selected != null) {
                    this.role = Role.Switch
                    this.selected = selected
                }
            },
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KeypadGrid(
    rows: List<List<KeypadKey>>,
    modifier: Modifier = Modifier,
    performKey: (KeypadKey) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    KeypadButton(
                        key = key,
                        onClick = { performKey(key) },
                        onLongClick = if (key is KeypadKey.Backspace) {
                            { performKey(KeypadKey.Clear) }
                        } else {
                            null
                        },
                        height = 56.dp,
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
    height: androidx.compose.ui.unit.Dp = 56.dp,
) {
    val style = styleFor(key)
    val label = key.display
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
        KeypadKey.Square -> stringResource(R.string.cd_key_square)
        KeypadKey.Asin -> stringResource(R.string.cd_key_asin)
        KeypadKey.Acos -> stringResource(R.string.cd_key_acos)
        KeypadKey.Atan -> stringResource(R.string.cd_key_atan)
        KeypadKey.Log2 -> stringResource(R.string.cd_key_log2)
        KeypadKey.Cbrt -> stringResource(R.string.cd_key_cbrt)
        KeypadKey.Abs -> stringResource(R.string.cd_key_abs)
        KeypadKey.Percent -> stringResource(R.string.cd_key_percent)
        KeypadKey.MemoryClear -> stringResource(R.string.cd_key_mc)
        KeypadKey.MemoryRecall -> stringResource(R.string.cd_key_mr)
        KeypadKey.MemoryPlus -> stringResource(R.string.cd_key_mplus)
        KeypadKey.MemoryMinus -> stringResource(R.string.cd_key_mminus)
        KeypadKey.Ans -> stringResource(R.string.cd_key_ans)
        is KeypadKey.Digit -> stringResource(R.string.cd_key_digit, label)
        KeypadKey.Plus -> stringResource(R.string.cd_key_plus)
        KeypadKey.Minus -> stringResource(R.string.cd_key_minus)
        KeypadKey.Mul -> stringResource(R.string.cd_key_mul)
        KeypadKey.Div -> stringResource(R.string.cd_key_div)
    }

    // Subtle press-in scale — the modern "physical key" feel.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        label = "key-press-scale",
    )

    Surface(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(CalcTags.key(key))
            .semantics { contentDescription = cd }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(16.dp),
        color = style.container,
        contentColor = style.content,
    ) {
        // Label centered both axes — digits and glyphs sit in the middle of
        // the key, never glued to the start edge.
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                fontSize = style.fontSize,
                fontWeight = style.fontWeight,
                maxLines = 1,
            )
        }
    }
}
