// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.toadcalculator.ui.tools.ToolDestination
import com.toadcalculator.ui.tools.ToolTags
import com.toadcalculator.ui.tools.toolCardTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Root navigation regression: the system back gesture must drill back through
 * the screens (tool → tools home → calculator) instead of closing the app.
 * Composes the real AppRoot (Room/DataStore on Robolectric) and drives the
 * actual OnBackPressedDispatcher, the same path the Android gesture takes.
 */
@RunWith(RobolectricTestRunner::class)
class AppRootUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun back_from_converter_returns_to_tools_home() {
        composeRule.setContent { AppRoot() }
        composeRule.onNodeWithTag(AppTags.OPEN_TOOLS).performClick()
        composeRule.onNodeWithTag(toolCardTag(ToolDestination.UNIT_CONVERTER)).performClick()

        // Converter is open.
        composeRule.onNodeWithTag(ToolTags.CONVERTER_VALUE).assertIsDisplayed()

        // System back must land on the Tools home, not exit the app.
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithTag(toolCardTag(ToolDestination.UNIT_CONVERTER)).assertIsDisplayed()
    }

    @Test
    fun back_from_tools_home_returns_to_calculator() {
        composeRule.setContent { AppRoot() }
        composeRule.onNodeWithTag(AppTags.OPEN_TOOLS).performClick()
        composeRule.onNodeWithTag(toolCardTag(ToolDestination.BMI)).assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithTag(AppTags.OPEN_TOOLS).assertIsDisplayed()
    }
}
