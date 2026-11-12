// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.toadcalculator.data.AngleModePref
import com.toadcalculator.data.ThemeMode
import com.toadcalculator.fakes.FakeHistoryRepository
import com.toadcalculator.fakes.FakeSettingsRepository
import com.toadcalculator.ui.calculator.KeypadKey
import com.toadcalculator.viewmodel.CalculatorViewModel
import com.toadcalculator.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var history: FakeHistoryRepository
    private lateinit var settings: FakeSettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        history = FakeHistoryRepository()
        settings = FakeSettingsRepository()
        viewModel = SettingsViewModel(settings, history)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state mirrors repository defaults`() = runTest(dispatcher) {
        advanceUntilIdle()
        val s = viewModel.uiState.value
        assertThat(s.theme).isEqualTo(ThemeMode.SYSTEM)
        assertThat(s.precision).isEqualTo(12)
        assertThat(s.angleMode).isEqualTo(AngleModePref.DEG)
        assertThat(s.haptics).isTrue()
    }

    @Test
    fun `loaded flag flips after first repository emission`() = runTest(dispatcher) {
        // B10: the splash gate keys off this flag — false before the flows
        // emit, true afterwards.
        assertThat(viewModel.uiState.value.loaded).isFalse()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.loaded).isTrue()
    }

    @Test
    fun `setters write through to repository`() = runTest(dispatcher) {
        viewModel.setTheme(ThemeMode.DARK)
        viewModel.setPrecision(6)
        viewModel.setAngleMode(AngleModePref.RAD)
        viewModel.setHaptics(false)
        advanceUntilIdle()

        assertThat(settings.theme.value).isEqualTo(ThemeMode.DARK)
        assertThat(settings.precision.value).isEqualTo(6)
        assertThat(settings.angleMode.value).isEqualTo(AngleModePref.RAD)
        assertThat(settings.haptics.value).isFalse()
        assertThat(viewModel.uiState.value.precision).isEqualTo(6)
    }

    @Test
    fun `clear history removes all entries`() = runTest(dispatcher) {
        val calc = CalculatorViewModel(history, settings, SavedStateHandle())
        calc.onKey(KeypadKey.Digit('2'))
        calc.onKey(KeypadKey.Plus)
        calc.onKey(KeypadKey.Digit('2'))
        calc.onKey(KeypadKey.Equals)
        advanceUntilIdle()
        assertThat(history.entries.value).hasSize(1)

        viewModel.clearHistory()
        advanceUntilIdle()

        assertThat(history.entries.value).isEmpty()
    }

    @Test
    fun `calculator reformats result when precision changes without recomputation`() = runTest(dispatcher) {
        val calc = CalculatorViewModel(history, settings, SavedStateHandle())
        calc.loadExpression("1/3")
        advanceUntilIdle()
        val before = calc.uiState.value.previewValue

        settings.setPrecision(6)
        advanceUntilIdle()

        // Same BigDecimal instance: no recomputation, only re-formatting at render time.
        assertThat(calc.uiState.value.precision).isEqualTo(6)
        assertThat(calc.uiState.value.previewValue).isSameInstanceAs(before)
        assertThat(com.toadcalculator.math.NumberFormatter.format(calc.uiState.value.previewValue!!, 6))
            .isEqualTo("0.333333")
    }
}
