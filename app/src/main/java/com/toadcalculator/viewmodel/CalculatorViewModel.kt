// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toadcalculator.data.HistoryRepository
import com.toadcalculator.data.SettingsRepository
import com.toadcalculator.math.AngleMode
import com.toadcalculator.math.MathError
import com.toadcalculator.math.MathResult
import com.toadcalculator.math.NumberFormatter
import com.toadcalculator.ui.calculator.KeypadKey
import com.toadcalculator.ui.calculator.backspaceToken
import com.toadcalculator.ui.calculator.currentNumberHasDot
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val expression: String = "",
    val cursor: Int = 0,
    /** Live evaluation of [expression]; null while invalid or empty. */
    val previewValue: BigDecimal? = null,
    /** Error from pressing "=" (rendered inline until the next edit). */
    val equalsError: MathError? = null,
    /** True right after "=": expression holds the formatted result. */
    val justEvaluated: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG,
    val precision: Int = NumberFormatter.DEFAULT_PRECISION,
    val haptics: Boolean = true,
    /** Scientific keypad visible (Phase 4). Survives process death. */
    val scientific: Boolean = false,
    /** 2nd toggle active: secondary function keys shown. */
    val secondMode: Boolean = false,
)

/**
 * Calculator ViewModel. Single source of truth for the expression, following
 * unidirectional data flow: UI emits key events, state flows back down.
 */
class CalculatorViewModel(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    /** History entries, newest first (Phase 3). Eagerly: always-warm for UI + tests. */
    val history: StateFlow<List<com.toadcalculator.data.HistoryEntry>> = historyRepository
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun deleteHistoryEntry(entry: com.toadcalculator.data.HistoryEntry) {
        viewModelScope.launch { historyRepository.delete(entry) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clearAll() }
    }

    init {
        viewModelScope.launch {
            settingsRepository.precision.collect { p ->
                _uiState.update { it.copy(precision = p) }
            }
        }
        viewModelScope.launch {
            settingsRepository.haptics.collect { h ->
                _uiState.update { it.copy(haptics = h) }
            }
        }
        viewModelScope.launch {
            // Settings define the START-UP angle mode; session toggles win afterwards.
            val pref = settingsRepository.angleMode.first()
            _uiState.update { applyPreview(it.copy(angleMode = AngleMode.valueOf(pref.name))) }
        }
    }

    private fun restoreState(): CalculatorUiState {
        val expression = savedState.get<String>(KEY_EXPRESSION) ?: ""
        val scientific = savedState.get<Boolean>(KEY_SCIENTIFIC) ?: false
        val state = CalculatorUiState(
            expression = expression,
            cursor = expression.length,
            scientific = scientific,
        )
        return applyPreview(state)
    }

    /** Keypad entry point. */
    fun onKey(key: KeypadKey) {
        when (key) {
            KeypadKey.Equals -> onEquals()
            KeypadKey.Backspace -> onBackspace()
            KeypadKey.Clear -> onClear()
            KeypadKey.Second -> toggleSecondMode()
            KeypadKey.AngleToggle -> toggleAngleMode()
            else -> insertKey(key)
        }
    }

    fun toggleScientific() {
        val newValue = !_uiState.value.scientific
        savedState[KEY_SCIENTIFIC] = newValue
        _uiState.update { it.copy(scientific = newValue, secondMode = false) }
    }

    private fun toggleSecondMode() {
        _uiState.update { it.copy(secondMode = !it.secondMode) }
    }

    private fun insertKey(key: KeypadKey) {
        val s = _uiState.value
        // After "=", values start a new calculation; operators continue from the result.
        var expression = s.expression
        if (s.justEvaluated) {
            expression = if (key.startsNewValue) "" else currentResultText(s)
        }
        if (key is KeypadKey.Dot && currentNumberHasDot(expression, expression.length)) return

        val newText = expression + key.text
        commit(newText, newText.length, justEvaluated = false)
    }

    private fun onBackspace() {
        val s = _uiState.value
        var expression = s.expression
        if (s.justEvaluated) expression = currentResultText(s)
        val (newText, newCursor) = backspaceToken(expression, expression.length)
        commit(newText, newCursor, justEvaluated = false)
    }

    private fun onClear() {
        savedState[KEY_EXPRESSION] = ""
        _uiState.update {
            it.copy(expression = "", cursor = 0, previewValue = null, equalsError = null, justEvaluated = false)
        }
    }

    private fun onEquals() {
        val s = _uiState.value
        val expression = s.expression
        if (expression.isBlank()) return
        when (val result = evaluateQuietly(expression, s.angleMode)) {
            is MathResult.Success -> {
                val formatted = NumberFormatter.format(result.value, s.precision)
                viewModelScope.launch {
                    historyRepository.add(expression, formatted)
                }
                savedState[KEY_EXPRESSION] = formatted
                _uiState.update {
                    it.copy(
                        expression = formatted,
                        cursor = formatted.length,
                        previewValue = result.value,
                        equalsError = null,
                        justEvaluated = true,
                    )
                }
            }
            is MathResult.Failure -> {
                _uiState.update { it.copy(equalsError = result.error, justEvaluated = false) }
            }
        }
    }

    /** Loads an entry tapped in history back into the input as editable text. */
    fun loadExpression(text: String) {
        commit(text, text.length, justEvaluated = false)
    }

    fun toggleAngleMode() {
        _uiState.update {
            applyPreview(
                it.copy(angleMode = if (it.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG),
            )
        }
    }

    private fun commit(expression: String, cursor: Int, justEvaluated: Boolean) {
        savedState[KEY_EXPRESSION] = expression
        _uiState.update {
            applyPreview(
                it.copy(
                    expression = expression,
                    cursor = cursor,
                    justEvaluated = justEvaluated,
                    equalsError = null,
                ),
            )
        }
    }

    /**
     * Live preview: evaluate the current expression. On success keep the value
     * so precision changes can re-format WITHOUT recomputation; on live-parse
     * failure show nothing (errors surface inline only after "=").
     */
    private fun applyPreview(state: CalculatorUiState): CalculatorUiState {
        if (state.expression.isBlank()) {
            return state.copy(previewValue = null)
        }
        return when (val r = evaluateQuietly(state.expression, state.angleMode)) {
            is MathResult.Success -> state.copy(previewValue = r.value)
            is MathResult.Failure -> state.copy(previewValue = null)
        }
    }

    private fun evaluateQuietly(expression: String, mode: AngleMode): MathResult =
        com.toadcalculator.math.evaluate(expression, mode)

    private fun currentResultText(s: CalculatorUiState): String =
        s.previewValue?.let { NumberFormatter.formatForInput(it, s.precision) } ?: s.expression

    companion object {
        private const val KEY_EXPRESSION = "calculator.expression"
        private const val KEY_SCIENTIFIC = "calculator.scientific"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as com.toadcalculator.ToadApplication
                CalculatorViewModel(
                    historyRepository = app.graph.historyRepository,
                    settingsRepository = app.graph.settingsRepository,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}
