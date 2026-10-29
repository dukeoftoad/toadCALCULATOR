// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.toadcalculator.fakes.FakeHistoryRepository
import com.toadcalculator.fakes.FakeSettingsRepository
import com.toadcalculator.ui.calculator.KeypadKey
import com.toadcalculator.viewmodel.CalculatorViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var history: FakeHistoryRepository
    private lateinit var settings: FakeSettingsRepository
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        history = FakeHistoryRepository()
        settings = FakeSettingsRepository()
        viewModel = CalculatorViewModel(history, settings, SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun type(vararg keys: KeypadKey) {
        keys.forEach { viewModel.onKey(it) }
    }

    private fun state() = viewModel.uiState.value

    @Test
    fun `typing digits updates expression`() {
        type(KeypadKey.Digit('1'), KeypadKey.Dot, KeypadKey.Digit('5'))
        assertThat(state().expression).isEqualTo("1.5")
    }

    @Test
    fun `live preview appears for valid expression`() {
        type(KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('3'))
        assertThat(state().previewValue).isNotNull()
    }

    @Test
    fun `live preview hidden for incomplete expression`() {
        type(KeypadKey.Digit('2'), KeypadKey.Plus)
        assertThat(state().previewValue).isNull()
    }

    @Test
    fun `equals computes and shows result`() = runTest(dispatcher) {
        type(KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('3'), KeypadKey.Equals)
        assertThat(state().expression).isEqualTo("5")
        assertThat(state().justEvaluated).isTrue()
        assertThat(state().previewValue).isNotNull()
        advanceUntilIdle()
    }

    @Test
    fun `equals writes history only on success`() = runTest(dispatcher) {
        type(KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('3'), KeypadKey.Equals)
        advanceUntilIdle()
        assertThat(history.entries.value).hasSize(1)
        assertThat(history.entries.value.single().expression).isEqualTo("2+3")
        assertThat(history.entries.value.single().result).isEqualTo("5")

        type(KeypadKey.Clear)
        type(KeypadKey.Digit('1'), KeypadKey.Div, KeypadKey.Digit('0'), KeypadKey.Equals)
        advanceUntilIdle()
        // Failure adds nothing.
        assertThat(history.entries.value).hasSize(1)
        assertThat(state().equalsError).isNotNull()
    }

    @Test
    fun `typing digit after equals starts new calculation`() {
        type(KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('3'), KeypadKey.Equals)
        type(KeypadKey.Digit('7'))
        assertThat(state().expression).isEqualTo("7")
        assertThat(state().justEvaluated).isFalse()
    }

    @Test
    fun `typing operator after equals continues from result`() {
        type(KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('3'), KeypadKey.Equals)
        type(KeypadKey.Mul)
        assertThat(state().expression).isEqualTo("5*")
    }

    @Test
    fun `backspace after equals edits the result`() {
        type(KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('3'), KeypadKey.Equals)
        type(KeypadKey.Backspace)
        assertThat(state().expression).isEqualTo("")
    }

    @Test
    fun `backspace is token aware in the viewmodel`() {
        type(KeypadKey.Digit('1'), KeypadKey.Dot, KeypadKey.Digit('5'), KeypadKey.Backspace)
        assertThat(state().expression).isEqualTo("")
    }

    @Test
    fun `double dot is rejected`() {
        type(KeypadKey.Digit('1'), KeypadKey.Dot, KeypadKey.Digit('2'), KeypadKey.Dot)
        assertThat(state().expression).isEqualTo("1.2")
    }

    @Test
    fun `clear resets everything`() {
        type(KeypadKey.Digit('9'), KeypadKey.Plus, KeypadKey.Digit('9'), KeypadKey.Equals)
        type(KeypadKey.Clear)
        assertThat(state().expression).isEqualTo("")
        assertThat(state().previewValue).isNull()
        assertThat(state().justEvaluated).isFalse()
    }

    @Test
    fun `load expression from history`() {
        viewModel.loadExpression("12*12")
        assertThat(state().expression).isEqualTo("12*12")
        assertThat(state().previewValue).isNotNull()
    }

    @Test
    fun `angle mode toggles and affects preview`() {
        type(KeypadKey.Digit('3'), KeypadKey.Backspace, KeypadKey.Backspace)
        viewModel.loadExpression("sin(90)")
        viewModel.toggleAngleMode() // now RAD
        assertThat(state().angleMode.name).isEqualTo("RAD")
        val rad = state().previewValue
        viewModel.toggleAngleMode() // back to DEG
        assertThat(state().angleMode.name).isEqualTo("DEG")
        assertThat(state().previewValue).isNotEqualTo(rad)
    }

    @Test
    fun `state survives process death via SavedStateHandle`() {
        type(KeypadKey.Digit('4'), KeypadKey.Mul, KeypadKey.Digit('4'))
        val handle = SavedStateHandle(mapOf("calculator.expression" to state().expression))
        val revived = CalculatorViewModel(history, settings, handle)
        assertThat(revived.uiState.value.expression).isEqualTo("4*4")
        assertThat(revived.uiState.value.previewValue).isNotNull()
    }

    @Test
    fun `failed equals does not change expression`() {
        type(KeypadKey.Digit('1'), KeypadKey.Div, KeypadKey.Digit('0'), KeypadKey.Equals)
        assertThat(state().expression).isEqualTo("1/0")
        assertThat(state().equalsError).isNotNull()
    }

    @Test
    fun `next edit clears inline error`() {
        type(KeypadKey.Digit('1'), KeypadKey.Div, KeypadKey.Digit('0'), KeypadKey.Equals)
        type(KeypadKey.Backspace)
        assertThat(state().equalsError).isNull()
    }
}
