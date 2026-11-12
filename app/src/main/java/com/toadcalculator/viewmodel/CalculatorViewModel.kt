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
    /** Calculator memory (M+/M−/MR/MC). null = empty. Survives process death. */
    val memory: BigDecimal? = null,
    /** Last successful "=" result, recallable via the Ans key. Survives process death. */
    val lastAnswer: BigDecimal? = null,
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
            // Settings define the START-UP angle mode — but only when no session
            // toggle was restored from SavedStateHandle. B3: previously a RAD
            // toggle made before process death was silently reset to the settings
            // default on revival.
            if (savedState.get<String>(KEY_ANGLE_MODE) == null) {
                val pref = settingsRepository.angleMode.first()
                _uiState.update { applyPreview(it.copy(angleMode = AngleMode.valueOf(pref.name))) }
            }
        }
    }

    private fun restoreState(): CalculatorUiState {
        val expression = savedState.get<String>(KEY_EXPRESSION) ?: ""
        val scientific = savedState.get<Boolean>(KEY_SCIENTIFIC) ?: false
        val secondMode = savedState.get<Boolean>(KEY_SECOND_MODE) ?: false
        val angleMode = savedState.get<String>(KEY_ANGLE_MODE)?.let {
            runCatching { AngleMode.valueOf(it) }.getOrNull()
        } ?: AngleMode.DEG
        val memory = savedState.get<String>(KEY_MEMORY)?.let {
            runCatching { BigDecimal(it) }.getOrNull()
        }
        val lastAnswer = savedState.get<String>(KEY_LAST_ANSWER)?.let {
            runCatching { BigDecimal(it) }.getOrNull()
        }
        val state = CalculatorUiState(
            expression = expression,
            scientific = scientific,
            secondMode = secondMode,
            angleMode = angleMode,
            memory = memory,
            lastAnswer = lastAnswer,
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
            KeypadKey.MemoryClear -> onMemoryClear()
            KeypadKey.MemoryRecall -> onMemoryRecall()
            KeypadKey.MemoryPlus -> onMemoryPlus()
            KeypadKey.MemoryMinus -> onMemoryMinus()
            KeypadKey.Ans -> onAns()
            else -> insertKey(key)
        }
    }

    fun toggleScientific() {
        val newValue = !_uiState.value.scientific
        savedState[KEY_SCIENTIFIC] = newValue
        // Turning the scientific pad off also collapses the 2nd row.
        savedState[KEY_SECOND_MODE] = false
        _uiState.update { it.copy(scientific = newValue, secondMode = false) }
    }

    private fun toggleSecondMode() {
        val newValue = !_uiState.value.secondMode
        savedState[KEY_SECOND_MODE] = newValue
        _uiState.update { it.copy(secondMode = newValue) }
    }

    private fun insertKey(key: KeypadKey) = insertText(key.text, key.startsNewValue, isDot = key is KeypadKey.Dot)

    /**
     * Inserts [text] at the end of the expression. After "=", values start a new
     * calculation while operators/parens continue from the result. Shared by the
     * static keypad keys and by MR/Ans (which pass a formatted stored value).
     */
    private fun insertText(text: String, startsNewValue: Boolean, isDot: Boolean = false) {
        val s = _uiState.value
        var expression = s.expression
        if (s.justEvaluated) {
            expression = if (startsNewValue) "" else currentResultText(s)
        }
        if (isDot && currentNumberHasDot(expression, expression.length)) return

        val newText = expression + text
        commit(newText, justEvaluated = false)
    }

    /** MC: forget the memory value. */
    private fun onMemoryClear() {
        savedState[KEY_MEMORY] = null
        _uiState.update { it.copy(memory = null) }
    }

    /** MR: insert the stored memory value as a numeric literal (no-op if empty). */
    private fun onMemoryRecall() {
        val mem = _uiState.value.memory ?: return
        insertText(NumberFormatter.formatForInput(mem, _uiState.value.precision), startsNewValue = true)
    }

    /** M+: add the live preview to memory (and remember it as the last answer). */
    private fun onMemoryPlus() {
        val value = _uiState.value.previewValue ?: return
        val newMem = (_uiState.value.memory ?: BigDecimal.ZERO).add(value)
        savedState[KEY_MEMORY] = NumberFormatter.formatForInput(newMem, _uiState.value.precision)
        savedState[KEY_LAST_ANSWER] = NumberFormatter.formatForInput(value, _uiState.value.precision)
        _uiState.update { it.copy(memory = newMem, lastAnswer = value) }
    }

    /** M−: subtract the live preview from memory (and remember it as the last answer). */
    private fun onMemoryMinus() {
        val value = _uiState.value.previewValue ?: return
        val newMem = (_uiState.value.memory ?: BigDecimal.ZERO).subtract(value)
        savedState[KEY_MEMORY] = NumberFormatter.formatForInput(newMem, _uiState.value.precision)
        savedState[KEY_LAST_ANSWER] = NumberFormatter.formatForInput(value, _uiState.value.precision)
        _uiState.update { it.copy(memory = newMem, lastAnswer = value) }
    }

    /** Ans: insert the last successful result as a numeric literal (no-op if none). */
    private fun onAns() {
        val ans = _uiState.value.lastAnswer ?: return
        insertText(NumberFormatter.formatForInput(ans, _uiState.value.precision), startsNewValue = true)
    }

    private fun onBackspace() {
        val s = _uiState.value
        var expression = s.expression
        if (s.justEvaluated) expression = currentResultText(s)
        val (newText, _) = backspaceToken(expression, expression.length)
        commit(newText, justEvaluated = false)
    }

    private fun onClear() {
        savedState[KEY_EXPRESSION] = ""
        _uiState.update {
            it.copy(expression = "", previewValue = null, equalsError = null, justEvaluated = false)
        }
    }

    private fun onEquals() {
        val s = _uiState.value
        val expression = s.expression
        if (expression.isBlank()) return
        // B2: pressing "=" again right after a successful "=" would re-evaluate
        // the formatted result and log a redundant "5 → 5" history entry. The
        // result is already shown and already logged, so this is a no-op.
        if (s.justEvaluated && s.equalsError == null) return
        when (val result = evaluateQuietly(expression, s.angleMode)) {
            is MathResult.Success -> {
                val formatted = NumberFormatter.format(result.value, s.precision)
                viewModelScope.launch {
                    historyRepository.add(expression, formatted)
                }
                savedState[KEY_EXPRESSION] = formatted
                savedState[KEY_LAST_ANSWER] = NumberFormatter.formatForInput(result.value, s.precision)
                _uiState.update {
                    it.copy(
                        expression = formatted,
                        previewValue = result.value,
                        equalsError = null,
                        justEvaluated = true,
                        lastAnswer = result.value,
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
        commit(text, justEvaluated = false)
    }

    fun toggleAngleMode() {
        val newMode = if (_uiState.value.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
        savedState[KEY_ANGLE_MODE] = newMode.name
        _uiState.update {
            applyPreview(it.copy(angleMode = newMode))
        }
    }

    private fun commit(expression: String, justEvaluated: Boolean) {
        savedState[KEY_EXPRESSION] = expression
        _uiState.update {
            applyPreview(
                it.copy(
                    expression = expression,
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
        private const val KEY_SECOND_MODE = "calculator.second_mode"
        private const val KEY_ANGLE_MODE = "calculator.angle_mode"
        private const val KEY_MEMORY = "calculator.memory"
        private const val KEY_LAST_ANSWER = "calculator.last_answer"

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
