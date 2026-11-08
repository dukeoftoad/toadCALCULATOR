// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.toadcalculator.ui.tools.BmiScreen
import com.toadcalculator.ui.tools.DateDiffScreen
import com.toadcalculator.ui.tools.PercentScreen
import com.toadcalculator.ui.tools.ToolDestination
import com.toadcalculator.ui.tools.ToolTags
import com.toadcalculator.ui.tools.ToolsHomeScreen
import com.toadcalculator.ui.tools.UnitConverterScreen
import com.toadcalculator.ui.tools.AgeScreen
import com.toadcalculator.ui.tools.toolCardTag
import com.toadcalculator.ui.theme.ToadCalculatorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Phase 6: one happy-path UI test per offline tool. */
@RunWith(RobolectricTestRunner::class)
class ToolsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tools_home_lists_all_five() {
        composeRule.setContent {
            ToadCalculatorTheme { ToolsHomeScreen(onOpen = {}) }
        }
        composeRule.onNodeWithText("Unit converter").assertIsDisplayed()
        composeRule.onNodeWithText("BMI").assertIsDisplayed()
        composeRule.onNodeWithText("Age").assertIsDisplayed()
        composeRule.onNodeWithText("Date difference").assertIsDisplayed()
        composeRule.onNodeWithText("Percentage").assertIsDisplayed()
    }

    @Test
    fun tools_home_cards_open_destination() {
        var opened: ToolDestination? = null
        composeRule.setContent {
            ToadCalculatorTheme {
                ToolsHomeScreen(onOpen = { dest -> opened = dest })
            }
        }
        composeRule.onNodeWithTag(toolCardTag(ToolDestination.BMI)).performClick()
        assertThat(opened).isEqualTo(ToolDestination.BMI)
        composeRule.onNodeWithTag(toolCardTag(ToolDestination.DATE_DIFF)).performClick()
        assertThat(opened).isEqualTo(ToolDestination.DATE_DIFF)
    }

    @Test
    fun converter_one_meter_to_feet() {
        composeRule.setContent {
            ToadCalculatorTheme { UnitConverterScreen() }
        }
        composeRule.onNodeWithTag(ToolTags.CONVERTER_VALUE).performTextReplacement("1")
        composeRule.onNodeWithTag(ToolTags.CONVERTER_RESULT).assertIsDisplayed()
        composeRule.onNodeWithText("3.280839895").assertIsDisplayed()
    }

    @Test
    fun bmi_defaults_show_normal_category() {
        composeRule.setContent {
            ToadCalculatorTheme { BmiScreen() }
        }
        composeRule.onNodeWithTag(ToolTags.BMI_RESULT).assertIsDisplayed()
        composeRule.onNodeWithText("22.86").assertIsDisplayed()
        composeRule.onNodeWithText("Normal weight").assertIsDisplayed()
    }

    @Test
    fun age_screen_renders_result_for_defaults() {
        composeRule.setContent {
            ToadCalculatorTheme { AgeScreen() }
        }
        composeRule.onNodeWithTag(ToolTags.AGE_RESULT).assertIsDisplayed()
        composeRule.onNodeWithText("Total days").assertIsDisplayed()
        composeRule.onNodeWithText("Next birthday").assertIsDisplayed()
    }

    @Test
    fun date_diff_default_year_is_364_days_and_inclusive_is_365() {
        composeRule.setContent {
            ToadCalculatorTheme { DateDiffScreen() }
        }
        composeRule.onNodeWithTag(ToolTags.DIFF_RESULT).assertIsDisplayed()
        composeRule.onNodeWithText("364 days").assertIsDisplayed()
        composeRule.onNodeWithTag("tool_diff_inclusive").performClick()
        composeRule.onNodeWithText("365 days").assertIsDisplayed()
    }

    @Test
    fun percent_tool_default_of_total_mode() {
        composeRule.setContent {
            ToadCalculatorTheme { PercentScreen() }
        }
        // Defaults: a=25, b=200, OF_TOTAL → 25 is 12.5% of 200. The result row
        // appends " %" so match on the substring.
        composeRule.onNodeWithTag(ToolTags.PERCENT_RESULT).assertIsDisplayed()
        composeRule.onNodeWithText("12.5", substring = true).assertIsDisplayed()
    }
}
