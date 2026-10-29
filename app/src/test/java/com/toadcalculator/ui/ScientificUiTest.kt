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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
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

/** Phase 4: one UI test for every scientific key + mode behaviors. */
@RunWith(RobolectricTestRunner::class)
class ScientificUiTest {

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
        composeRule.runOnIdle { viewModel.toggleScientific() }
    }

    private fun press(vararg keys: KeypadKey) {
        keys.forEach { k ->
            val tag = if (k == KeypadKey.Second || k == KeypadKey.AngleToggle) {
                CalcTags.key(k) + "_chip"
            } else {
                CalcTags.key(k)
            }
            composeRule.onNodeWithTag(tag).performClick()
        }
    }

    private fun assertExpression(expected: String) {
        composeRule.onNodeWithTag(CalcTags.EXPRESSION).assert(hasText(expected))
    }

    private fun assertResult(expected: String) {
        composeRule.onNodeWithTag(CalcTags.RESULT).assert(hasText(expected))
    }

    @Test
    fun sin_key_inserts_and_computes() {
        press(KeypadKey.Sin, KeypadKey.Digit('3'), KeypadKey.Digit('0'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("0.5")
    }

    @Test
    fun cos_key_computes() {
        press(KeypadKey.Cos, KeypadKey.Digit('6'), KeypadKey.Digit('0'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("0.5")
    }

    @Test
    fun tan_key_computes() {
        press(KeypadKey.Tan, KeypadKey.Digit('4'), KeypadKey.Digit('5'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("1")
    }

    @Test
    fun ln_key_computes() {
        press(KeypadKey.Ln, KeypadKey.E, KeypadKey.RParen, KeypadKey.Equals)
        assertResult("1")
    }

    @Test
    fun log_key_computes() {
        press(KeypadKey.Log, KeypadKey.Digit('1'), KeypadKey.Digit('0'), KeypadKey.Digit('0'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("2")
    }

    @Test
    fun sqrt_key_computes() {
        press(KeypadKey.Sqrt, KeypadKey.Digit('1'), KeypadKey.Digit('6'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("4")
    }

    @Test
    fun pow_key_computes() {
        press(KeypadKey.Digit('2'), KeypadKey.Pow, KeypadKey.Digit('3'), KeypadKey.Equals)
        assertResult("8")
    }

    @Test
    fun fact_key_computes() {
        press(KeypadKey.Digit('5'), KeypadKey.Fact, KeypadKey.Equals)
        assertResult("120")
    }

    @Test
    fun pi_key_inserts_constant() {
        press(KeypadKey.Pi)
        assertExpression("π")
        press(KeypadKey.Equals)
        assertResult("3.14159265359")
    }

    @Test
    fun e_key_inserts_constant() {
        press(KeypadKey.E)
        assertExpression("e")
        press(KeypadKey.Equals)
        assertResult("2.71828182846")
    }

    @Test
    fun parens_keys_compute() {
        press(KeypadKey.LParen, KeypadKey.Digit('1'), KeypadKey.Plus, KeypadKey.Digit('2'), KeypadKey.RParen,
            KeypadKey.Mul, KeypadKey.Digit('2'), KeypadKey.Equals)
        assertResult("6")
    }

    @Test
    fun angle_chip_toggles_deg_rad() {
        val chip = composeRule.onNodeWithTag(CalcTags.key(KeypadKey.AngleToggle) + "_chip")
        chip.assertIsDisplayed()
        chip.assert(hasText("DEG"))
        press(KeypadKey.AngleToggle)
        chip.assert(hasText("RAD"))
        press(KeypadKey.AngleToggle)
        chip.assert(hasText("DEG"))
    }

    @Test
    fun angle_mode_affects_trig_results() {
        press(KeypadKey.Sin, KeypadKey.Digit('3'), KeypadKey.Digit('0'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("0.5") // DEG
        press(KeypadKey.Clear)
        press(KeypadKey.AngleToggle)
        press(KeypadKey.Sin, KeypadKey.Digit('3'), KeypadKey.Digit('0'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("-0.988031624093") // RAD
    }

    @Test
    fun second_chip_reveals_secondary_keys() {
        composeRule.onNodeWithTag(CalcTags.key(KeypadKey.Asin)).assertDoesNotExist()
        press(KeypadKey.Second)
        composeRule.onNodeWithTag(CalcTags.key(KeypadKey.Asin)).assertIsDisplayed()
        composeRule.onNodeWithTag(CalcTags.key(KeypadKey.Log2)).assertIsDisplayed()
        composeRule.onNodeWithTag(CalcTags.key(KeypadKey.Cbrt)).assertIsDisplayed()
        composeRule.onNodeWithTag(CalcTags.key(KeypadKey.Abs)).assertIsDisplayed()
    }

    @Test
    fun asin_key_computes() {
        press(KeypadKey.Second)
        press(KeypadKey.Asin, KeypadKey.Dot, KeypadKey.Digit('5'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("30")
    }

    @Test
    fun log2_key_computes() {
        press(KeypadKey.Second)
        press(KeypadKey.Log2, KeypadKey.Digit('8'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("3")
    }

    @Test
    fun cbrt_key_computes_negative() {
        press(KeypadKey.Second)
        press(KeypadKey.Cbrt, KeypadKey.Minus, KeypadKey.Digit('8'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("-2")
    }

    @Test
    fun abs_key_computes() {
        press(KeypadKey.Second)
        press(KeypadKey.Abs, KeypadKey.Minus, KeypadKey.Digit('5'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("5")
    }

    @Test
    fun acos_and_atan_keys_exist_and_compute() {
        press(KeypadKey.Second)
        press(KeypadKey.Acos, KeypadKey.Digit('1'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("0")
        composeRule.runOnIdle { viewModel.onKey(KeypadKey.Clear) }
        press(KeypadKey.Atan, KeypadKey.Digit('1'), KeypadKey.RParen, KeypadKey.Equals)
        assertResult("45")
    }

    @Test
    fun switching_modes_preserves_expression() {
        press(KeypadKey.Digit('1'), KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('3'))
        assertExpression("12+3")
        composeRule.runOnIdle { viewModel.toggleScientific() } // off
        composeRule.waitForIdle()
        assertThat(viewModel.uiState.value.expression).isEqualTo("12+3")
        composeRule.runOnIdle { viewModel.toggleScientific() } // on
        composeRule.waitForIdle()
        assertThat(viewModel.uiState.value.expression).isEqualTo("12+3")
        assertExpression("12+3")
    }

    @Test
    fun long_press_backspace_clears_all() {
        press(KeypadKey.Digit('1'), KeypadKey.Digit('2'), KeypadKey.Plus, KeypadKey.Digit('3'))
        composeRule.onNodeWithTag(CalcTags.key(KeypadKey.Backspace)).performTouchInput { longClick() }
        assertExpression("0")
        assertThat(viewModel.uiState.value.expression).isEmpty()
    }
}
