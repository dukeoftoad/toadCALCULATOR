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
class HistoryFlowTest {

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

    @Test
    fun `successful equals lands in history observable flow`() = runTest(dispatcher) {
        type(KeypadKey.Digit('6'), KeypadKey.Mul, KeypadKey.Digit('7'), KeypadKey.Equals)
        advanceUntilIdle()

        val entries = viewModel.history.value
        assertThat(entries).hasSize(1)
        assertThat(entries.single().expression).isEqualTo("6*7")
        assertThat(entries.single().result).isEqualTo("42")
    }

    @Test
    fun `failed equals never writes history`() = runTest(dispatcher) {
        type(KeypadKey.Digit('5'), KeypadKey.Div, KeypadKey.Digit('0'), KeypadKey.Equals)
        advanceUntilIdle()
        assertThat(viewModel.history.value).isEmpty()
    }

    @Test
    fun `delete removes one entry`() = runTest(dispatcher) {
        type(KeypadKey.Digit('1'), KeypadKey.Plus, KeypadKey.Digit('1'), KeypadKey.Equals)
        advanceUntilIdle()
        val entry = viewModel.history.value.single()

        viewModel.deleteHistoryEntry(entry)
        advanceUntilIdle()

        assertThat(viewModel.history.value).isEmpty()
    }

    @Test
    fun `clear history empties everything`() = runTest(dispatcher) {
        type(KeypadKey.Digit('1'), KeypadKey.Plus, KeypadKey.Digit('1'), KeypadKey.Equals)
        type(KeypadKey.Clear)
        type(KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('2'), KeypadKey.Equals)
        advanceUntilIdle()
        assertThat(viewModel.history.value).hasSize(2)

        viewModel.clearHistory()
        advanceUntilIdle()

        assertThat(viewModel.history.value).isEmpty()
    }

    @Test
    fun `loading a history entry restores expression as editable text`() = runTest(dispatcher) {
        type(KeypadKey.Digit('9'), KeypadKey.Mul, KeypadKey.Digit('9'), KeypadKey.Equals)
        advanceUntilIdle()
        val stored = viewModel.history.value.single().expression
        viewModel.onKey(KeypadKey.Clear)

        viewModel.loadExpression(stored)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.expression).isEqualTo("9*9")
        assertThat(viewModel.uiState.value.previewValue).isNotNull()
    }
}
