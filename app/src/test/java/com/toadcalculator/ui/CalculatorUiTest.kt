// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.toadcalculator.fakes.FakeHistoryRepository
import com.toadcalculator.fakes.FakeSettingsRepository
import com.toadcalculator.ui.calculator.CalcTags
import com.toadcalculator.ui.calculator.CalculatorScreenContent
import com.toadcalculator.ui.calculator.KeypadKey
import com.toadcalculator.ui.theme.ToadCalculatorTheme
import com.toadcalculator.viewmodel.CalculatorViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalculatorUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val history = FakeHistoryRepository()
    private val settings = FakeSettingsRepository()
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        viewModel = CalculatorViewModel(history, settings, SavedStateHandle())
        composeRule.setContent {
            ToadCalculatorTheme {
                val state by viewModel.uiState.collectAsState()
                CalculatorScreenContent(state = state, onKey = viewModel::onKey)
            }
        }
    }

    private fun press(vararg keys: KeypadKey) {
        keys.forEach { k ->
            composeRule.onNodeWithTag(CalcTags.key(k)).performClick()
        }
    }

    @Test
    fun two_plus_three_equals_five() {
        press(KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('3'), KeypadKey.Equals)
        composeRule.onNodeWithTag(CalcTags.RESULT).assertIsDisplayed()
        composeRule.onNodeWithTag(CalcTags.RESULT).assert(hasText("5"))
    }

    @Test
    fun division_by_zero_shows_inline_error() {
        press(KeypadKey.Digit('1'), KeypadKey.Div, KeypadKey.Digit('0'), KeypadKey.Equals)
        composeRule.onNodeWithTag(CalcTags.ERROR).assertIsDisplayed()
    }

    @Test
    fun all_clear_empties_display() {
        press(KeypadKey.Digit('9'), KeypadKey.Clear)
        composeRule.onNodeWithTag(CalcTags.EXPRESSION).assert(hasText("0"))
    }

    @Test
    fun live_preview_shows_before_equals() {
        press(KeypadKey.Digit('7'), KeypadKey.Mul, KeypadKey.Digit('6'))
        composeRule.onNodeWithTag(CalcTags.RESULT).assertIsDisplayed()
        composeRule.onNodeWithTag(CalcTags.RESULT).assert(hasText("42"))
    }
}
